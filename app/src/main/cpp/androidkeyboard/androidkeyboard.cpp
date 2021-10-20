#include <fcitx-utils/utf8.h>
#include <fcitx/instance.h>
#include <fcitx/candidatelist.h>
#include <fcitx/inputpanel.h>

#include "../fcitx5/src/im/keyboard/chardata.h"

#include "androidkeyboard.h"

#define FCITX_KEYBOARD_MAX_BUFFER 20

namespace fcitx {

namespace {

class AndroidKeyboardCandidateWord : public CandidateWord {
public:
    AndroidKeyboardCandidateWord(AndroidKeyboardEngine *engine, Text text)
            : CandidateWord(std::move(text)), engine_(engine) {}

    void select(InputContext *inputContext) const override {
        auto commit = text().toString();
        inputContext->inputPanel().reset();
        inputContext->updatePreedit();
        inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
        inputContext->commitString(commit);
        engine_->resetState(inputContext);
    }

private:
    AndroidKeyboardEngine *engine_;
};

} // namespace

AndroidKeyboardEngine::AndroidKeyboardEngine(Instance *instance) : instance_(instance) {
    instance_->inputContextManager().registerProperty("keyboardState", &factory_);
    reloadConfig();
    deferEvent_ = instance_->eventLoop().addDeferEvent([this](EventSource *) {
        deferEvent_.reset();
        return true;
    });
}

AndroidKeyboardEngine::~AndroidKeyboardEngine() {}

static inline bool isValidSym(const Key &key) {
    if (key.states()) {
        return false;
    }

    return validSyms.count(key.sym());
}

static inline bool isValidCharacter(const std::string &c) {
    if (c.empty()) {
        return false;
    }

    uint32_t code;
    auto iter = utf8::getNextChar(c.begin(), c.end(), &code);

    return iter == c.end() && validChars.count(code);
}

static KeyList FCITX_HYPHEN_APOS = Key::keyListFromString("minus apostrophe");

void AndroidKeyboardEngine::keyEvent(const InputMethodEntry &entry, KeyEvent &event) {
    FCITX_UNUSED(entry);
    auto *inputContext = event.inputContext();

    // by pass all key release
    if (event.isRelease()) {
        return;
    }

    auto *state = inputContext->propertyFor(&factory_);
    if (state->repeatStarted_ &&
        !event.rawKey().states().test(KeyState::Repeat)) {
        state->repeatStarted_ = false;
    }

    // and by pass all modifier
    if (event.key().isModifier()) {
        return;
    }

    auto &buffer = state->buffer_;
    auto keystr = Key::keySymToUTF8(event.key().sym());

    // check compose first.
    auto composeResult =
            instance_->processComposeString(inputContext, event.key().sym());

    // compose is invalid, ignore it.
    if (!composeResult) {
        return event.filterAndAccept();
    }

    auto compose = *composeResult;

    // check if we can select candidate.
    if (auto candList = inputContext->inputPanel().candidateList()) {
        int idx = event.key().keyListIndex(selectionKeys_);
        if (idx >= 0 && idx < candList->size()) {
            event.filterAndAccept();
            candList->candidate(idx).select(inputContext);
            return;
        }
    }

    bool validCharacter = isValidCharacter(compose);
    bool validSym = isValidSym(event.key());

    // check for valid character
    if (validCharacter || event.key().isSimple() || validSym) {
        if (validCharacter || event.key().isLAZ() || event.key().isUAZ() ||
            validSym ||
            (!buffer.empty() && event.key().checkKeyList(FCITX_HYPHEN_APOS))) {
            auto text = !compose.empty() ? compose
                                         : Key::keySymToUTF8(event.key().sym());
            if (updateBuffer(inputContext, text)) {
                return event.filterAndAccept();
            }
        }
    } else if (event.key().check(FcitxKey_BackSpace)) {
        if (buffer.backspace()) {
            event.filterAndAccept();
            if (buffer.empty()) {
                return reset(entry, event);
            }
            return updateCandidate(entry, inputContext);
        }
    }

    // if we reach here, just commit and discard buffer.
    commitBuffer(inputContext);
    // and now we want to forward key.
    if (!compose.empty()) {
        event.filterAndAccept();
        inputContext->commitString(compose);
    }
}

std::vector<InputMethodEntry> AndroidKeyboardEngine::listInputMethods() {
    std::vector<InputMethodEntry> result;
    result.emplace_back(std::move(
            InputMethodEntry("keyboard-us", _("Keyboard"), "en", "androidkeyboard")
            .setLabel("us")
            .setIcon("input-keyboard")
            .setConfigurable(true)));
    return result;
}

void AndroidKeyboardEngine::reloadConfig() {
    // nothing to reload
}

const Configuration *AndroidKeyboardEngine::getSubConfig(const std::string &path) const {
    return nullptr;
}

void AndroidKeyboardEngine::setSubConfig(const std::string &, const RawConfig &) {
    // nothing to set
}

void AndroidKeyboardEngine::reset(const InputMethodEntry &entry, InputContextEvent &event) {
    auto *inputContext = event.inputContext();
    // The reason that we do not commit here is we want to force the behavior.
    // When client get unfocused, the framework will try to commit the string.
    if (event.type() != EventType::InputContextFocusOut) {
        commitBuffer(inputContext);
    } else {
        resetState(inputContext);
    }
    inputContext->inputPanel().reset();
    inputContext->updatePreedit();
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

void AndroidKeyboardEngine::resetState(InputContext *inputContext) {
    auto *state = inputContext->propertyFor(&factory_);
    state->reset();
    instance_->resetCompose(inputContext);
}

void AndroidKeyboardEngine::updateCandidate(const InputMethodEntry &entry, InputContext *inputContext) {

    inputContext->inputPanel().reset();
    auto *state = inputContext->propertyFor(&factory_);
    // handle spell and emoji
    auto candidateList = std::make_unique<CommonCandidateList>();
    candidateList->setPageSize(*config_.pageSize);
    candidateList->setCursorIncludeUnselected(true);
    inputContext->inputPanel().setCandidateList(std::move(candidateList));

    updateUI(inputContext);
}

void AndroidKeyboardEngine::updateUI(InputContext *inputContext) {
    Text preedit(preeditString(inputContext), TextFormatFlag::Underline);
    if (auto length = preedit.textLength()) {
        preedit.setCursor(length);
    }
    inputContext->inputPanel().setClientPreedit(preedit);
    if (!inputContext->capabilityFlags().test(CapabilityFlag::Preedit)) {
        inputContext->inputPanel().setPreedit(preedit);
    }
    inputContext->updatePreedit();
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

bool AndroidKeyboardEngine::updateBuffer(InputContext *inputContext, const std::string &chr) {
    auto *entry = instance_->inputMethodEntry(inputContext);
    if (!entry) {
        return false;
    }

    auto *state = inputContext->propertyFor(&factory_);
//    const CapabilityFlags noPredictFlag{CapabilityFlag::Password,
//                                        CapabilityFlag::NoSpellCheck,
//                                        CapabilityFlag::Sensitive};
    auto &buffer = state->buffer_;
    auto preedit = preeditString(inputContext);
    if (preedit != buffer.userInput()) {
        buffer.clear();
        buffer.type(preedit);
    }

    buffer.type(chr);

    if (buffer.size() >= FCITX_KEYBOARD_MAX_BUFFER) {
        commitBuffer(inputContext);
        return true;
    }

    updateCandidate(*entry, inputContext);
    return true;
}

void AndroidKeyboardEngine::commitBuffer(InputContext *inputContext) {
    auto preedit = preeditString(inputContext);
    if (preedit.empty()) {
        return;
    }
    inputContext->commitString(preedit);
    resetState(inputContext);
    inputContext->inputPanel().reset();
    inputContext->updatePreedit();
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

std::string AndroidKeyboardEngine::preeditString(InputContext *inputContext) {
    auto *state = inputContext->propertyFor(&factory_);
    auto candidateList = inputContext->inputPanel().candidateList();
    std::string preedit;
    return state->buffer_.userInput();
}

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidKeyboardEngineFactory)
