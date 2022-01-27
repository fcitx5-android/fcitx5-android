#include <fcitx-utils/utf8.h>
#include <fcitx-utils/charutils.h>
#include <fcitx/instance.h>
#include <fcitx/candidatelist.h>
#include <fcitx/inputpanel.h>

#include "../fcitx5/src/modules/spell/spell_public.h"
#include "../fcitx5/src/im/keyboard/chardata.h"

#include "androidkeyboard.h"

#define FCITX_KEYBOARD_MAX_BUFFER 20

namespace fcitx {

namespace {

enum class SpellType {
    AllLower, Mixed, FirstUpper, AllUpper
};

SpellType guessSpellType(const std::string &input) {
    if (input.size() <= 1) {
        if (charutils::isupper(input[0])) {
            return SpellType::FirstUpper;
        }
        return SpellType::AllLower;
    }

    if (std::all_of(input.begin(), input.end(),
                    [](char c) { return charutils::isupper(c); })) {
        return SpellType::AllUpper;
    }

    if (std::all_of(input.begin() + 1, input.end(),
                    [](char c) { return charutils::islower(c); })) {
        if (charutils::isupper(input[0])) {
            return SpellType::FirstUpper;
        }
        return SpellType::AllLower;
    }

    return SpellType::Mixed;
}

std::string formatWord(const std::string &input, SpellType type) {
    if (type == SpellType::Mixed || type == SpellType::AllLower) {
        return input;
    }
    if (guessSpellType(input) != SpellType::AllLower) {
        return input;
    }
    std::string result;
    if (type == SpellType::AllUpper) {
        result.reserve(input.size());
        std::transform(input.begin(), input.end(), std::back_inserter(result),
                       charutils::toupper);
    } else {
        // FirstUpper
        result = input;
        if (!result.empty()) {
            result[0] = charutils::toupper(result[0]);
        }
    }
    return result;
}

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

AndroidKeyboardEngine::AndroidKeyboardEngine(Instance *instance)
        : instance_(instance), enableWordHint_(true) {
    instance_->inputContextManager().registerProperty("keyboardState", &factory_);
    reloadConfig();
}

static inline bool isValidSym(const Key &key) {
    if (key.states()) {
        return false;
    }

    return validSyms.count(key.sym());
}

void AndroidKeyboardEngine::keyEvent(const InputMethodEntry &entry, KeyEvent &event) {
    FCITX_UNUSED(entry);
    auto *inputContext = event.inputContext();

    // by pass all key release
    if (event.isRelease()) {
        return;
    }

    auto *state = inputContext->propertyFor(&factory_);

    // and by pass all modifier
    if (event.key().isModifier()) {
        return;
    }

    auto &buffer = state->buffer_;
    auto keystr = Key::keySymToUTF8(event.key().sym());

    // check if we can select candidate.
    if (auto candList = inputContext->inputPanel().candidateList()) {
        int idx = event.key().keyListIndex(selectionKeys_);
        if (idx >= 0 && idx < candList->size()) {
            event.filterAndAccept();
            candList->candidate(idx).select(inputContext);
            return;
        }
    }

    bool validSym = isValidSym(event.key());

    static KeyList FCITX_HYPHEN_APOS = {Key(FcitxKey_minus), Key(FcitxKey_apostrophe)};
    // check for valid character
    if (event.key().isSimple() || validSym) {
        if (event.key().isLAZ() || event.key().isUAZ() || validSym ||
            (!buffer.empty() && event.key().checkKeyList(FCITX_HYPHEN_APOS))) {
            auto text = Key::keySymToUTF8(event.key().sym());
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
}

std::vector<InputMethodEntry> AndroidKeyboardEngine::listInputMethods() {
    std::vector<InputMethodEntry> result;
    result.emplace_back(std::move(
            InputMethodEntry("keyboard-us", _("English"), "en", "androidkeyboard")
                    .setLabel("En")
                    .setIcon("input-keyboard")
                    .setConfigurable(true)));
    return result;
}

void AndroidKeyboardEngine::reloadConfig() {
    readAsIni(config_, ConfPath);
    selectionKeys_.clear();
    KeySym syms[] = {
            FcitxKey_1, FcitxKey_2, FcitxKey_3, FcitxKey_4, FcitxKey_5,
            FcitxKey_6, FcitxKey_7, FcitxKey_8, FcitxKey_9, FcitxKey_0,
    };

    KeyStates states;
    switch (config_.chooseModifier.value()) {
        case ChooseModifier::Alt:
            states = KeyState::Alt;
            break;
        case ChooseModifier::Control:
            states = KeyState::Ctrl;
            break;
        case ChooseModifier::Super:
            states = KeyState::Super;
            break;
        default:
            break;
    }

    for (auto sym : syms) {
        selectionKeys_.emplace_back(sym, states);
    }
    enableWordHint_ = config_.enableWordHint.value();
}

void AndroidKeyboardEngine::save() {
    safeSaveAsIni(config_, ConfPath);
}

void AndroidKeyboardEngine::setConfig(const RawConfig &config) {
    config_.load(config, true);
    safeSaveAsIni(config_, ConfPath);
    reloadConfig();
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
    std::vector<std::string> results;
    if (spell()) {
        results = spell()->call<ISpell::hint>(entry.languageCode(),
                                              state->buffer_.userInput(),
                                              config_.pageSize.value());
    }
    auto candidateList = std::make_unique<CommonCandidateList>();
    auto spellType = guessSpellType(state->buffer_.userInput());
    for (const auto &result : results) {
        candidateList->append<AndroidKeyboardCandidateWord>(
                this, Text(formatWord(result, spellType)));
    }
    candidateList->setPageSize(*config_.pageSize);
    candidateList->setSelectionKey(selectionKeys_);
    candidateList->setCursorIncludeUnselected(true);
    state->mode_ = CandidateMode::Hint;
    inputContext->inputPanel().setCandidateList(std::move(candidateList));

    updateUI(inputContext);
}

void AndroidKeyboardEngine::updateUI(InputContext *inputContext) {
    auto *state = inputContext->propertyFor(&factory_);
    Text preedit(state->buffer_.userInput(), TextFormatFlag::Underline);
    preedit.setCursor(state->buffer_.cursor());
    inputContext->inputPanel().setClientPreedit(preedit);
    // we don't want preedit here ...
//    if (!inputContext->capabilityFlags().test(CapabilityFlag::Preedit)) {
//        inputContext->inputPanel().setPreedit(preedit);
//    }
    inputContext->updatePreedit();
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

bool AndroidKeyboardEngine::updateBuffer(InputContext *inputContext, const std::string &chr) {
    auto *entry = instance_->inputMethodEntry(inputContext);
    if (!entry) {
        return false;
    }

    auto *state = inputContext->propertyFor(&factory_);
    const CapabilityFlags noPredictFlag{CapabilityFlag::Password,
                                        CapabilityFlag::NoSpellCheck,
                                        CapabilityFlag::Sensitive};
    // no spell hint enabled or no supported dictionary
    if (!enableWordHint_ ||
        inputContext->capabilityFlags().testAny(noPredictFlag) ||
        !supportHint(entry->languageCode())) {
        return false;
    }

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

bool AndroidKeyboardEngine::supportHint(const std::string &language) {
    const bool hasSpell = spell() && spell()->call<ISpell::checkDict>(language);
    return hasSpell;
}

std::string AndroidKeyboardEngine::preeditString(InputContext *inputContext) {
    auto *state = inputContext->propertyFor(&factory_);
    return state->buffer_.userInput();
}

void AndroidKeyboardEngine::invokeActionImpl(const InputMethodEntry &entry, InvokeActionEvent &event) {
    auto inputContext = event.inputContext();
    if (event.cursor() < 0 ||
        event.action() != InvokeActionEvent::Action::LeftClick) {
        return InputMethodEngineV3::invokeActionImpl(entry, event);
    }
    event.filter();
    auto *state = inputContext->propertyFor(&factory_);
    state->buffer_.setCursor(event.cursor());
    updateUI(inputContext);
}

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidKeyboardEngineFactory)
