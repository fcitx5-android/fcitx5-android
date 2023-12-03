/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
#include <fcitx-utils/utf8.h>
#include <fcitx-utils/charutils.h>
#include <fcitx/instance.h>
#include <fcitx/candidatelist.h>
#include <fcitx/inputpanel.h>
#include <fcitx/userinterfacemanager.h>

#include "spell_public.h"
#include "../../../../../lib/fcitx5/src/main/cpp/fcitx5/src/im/keyboard/chardata.h" // dirty but works

#include "androidkeyboard.h"

namespace fcitx {

namespace {

class AndroidKeyboardCandidateWord : public CandidateWord {
public:
    AndroidKeyboardCandidateWord(AndroidKeyboardEngine *engine, Text text, std::string commit)
            : CandidateWord(std::move(text)), engine_(engine),
              commit_(std::move(commit)) {}

    void select(InputContext *inputContext) const override {
        inputContext->commitString(commit_);
        inputContext->inputPanel().reset();
        inputContext->updatePreedit();
        inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
        engine_->resetState(inputContext, true);
    }

    [[nodiscard]] const std::string &stringForCommit() const { return commit_; }

private:
    AndroidKeyboardEngine *engine_;
    std::string commit_;
};

} // namespace

AndroidKeyboardEngine::AndroidKeyboardEngine(Instance *instance)
        : instance_(instance) {
    instance_->inputContextManager().registerProperty("androidkeyboardState", &factory_);
    reloadConfig();
    wordHintAction_.setShortText(_("Word hint"));
    wordHintAction_.setLongText(_("Word hint"));
    wordHintAction_.setIcon("tools-check-spelling");
    wordHintAction_.setChecked(*config_.enableWordHint);
    wordHintAction_.connect<SimpleAction::Activated>([this](InputContext *ic) {
        auto enabled = !(*config_.enableWordHint);
        config_.enableWordHint.setValue(enabled);
        wordHintAction_.setChecked(enabled);
        wordHintAction_.update(ic);
    });
    instance_->userInterfaceManager().registerAction("androidkeyboard-word-hint", &wordHintAction_);
}

static inline bool isValidSym(const Key &key) {
    if (key.states()) {
        return false;
    }

    return validSyms.count(key.sym());
}

void AndroidKeyboardEngine::keyEvent(const InputMethodEntry &entry, KeyEvent &event) {
    FCITX_UNUSED(entry);

    // by pass all key release
    if (event.isRelease()) {
        return;
    }

    const auto &key = event.key();

    // and by pass all modifier
    if (key.isModifier()) {
        return;
    }

    auto *inputContext = event.inputContext();
    auto *state = inputContext->propertyFor(&factory_);
    auto &buffer = state->buffer_;

    // check if we can select candidate.
    if (auto candList = inputContext->inputPanel().candidateList()) {
        const int idx = key.keyListIndex(selectionKeys_);
        if (idx >= 0 && idx < candList->size()) {
            event.filterAndAccept();
            candList->candidate(idx).select(inputContext);
            return;
        }
    }

    const bool validSym = isValidSym(key);

    static const KeyList FCITX_HYPHEN_APOS = {Key(FcitxKey_minus), Key(FcitxKey_apostrophe)};
    // check for valid character
    if (key.isSimple() || validSym) {
        // prepend space before input next word
        if (state->prependSpace_ && buffer.empty() &&
            (key.isLAZ() || key.isUAZ() || key.isDigit())) {
            state->prependSpace_ = false;
            inputContext->commitString(" ");
        }
        if (key.isLAZ() || key.isUAZ() || validSym ||
            (!buffer.empty() && key.checkKeyList(FCITX_HYPHEN_APOS))) {
            auto text = Key::keySymToUTF8(key.sym());
            if (updateBuffer(inputContext, text)) {
                return event.filterAndAccept();
            }
        }
    } else if (key.check(FcitxKey_BackSpace)) {
        if (buffer.backspace()) {
            event.filterAndAccept();
            if (buffer.empty()) {
                return reset(entry, event);
            }
            return updateCandidate(entry, inputContext);
        }
    } else if (key.check(FcitxKey_Delete) || key.check(FcitxKey_KP_Delete)) {
        if (buffer.del()) {
            event.filterAndAccept();
            if (buffer.empty()) {
                return reset(entry, event);
            }
            return updateCandidate(entry, inputContext);
        }
    } else if (!buffer.empty()) {
        if (key.check(FcitxKey_Home) || key.check(FcitxKey_KP_Home)) {
            buffer.setCursor(0);
            event.filterAndAccept();
            return updateCandidate(entry, inputContext);
        } else if (key.check(FcitxKey_End) || key.check(FcitxKey_KP_End)) {
            buffer.setCursor(buffer.size());
            event.filterAndAccept();
            return updateCandidate(entry, inputContext);
        } else if (key.check(FcitxKey_Left) || key.check(FcitxKey_KP_Left)) {
            auto cursor = buffer.cursor();
            if (cursor > 0) {
                buffer.setCursor(cursor - 1);
            }
            event.filterAndAccept();
            return updateCandidate(entry, inputContext);
        } else if (key.check(FcitxKey_Right) || key.check(FcitxKey_KP_Right)) {
            auto cursor = buffer.cursor();
            if (cursor < buffer.size()) {
                buffer.setCursor(buffer.cursor() + 1);
            }
            event.filterAndAccept();
            return updateCandidate(entry, inputContext);
        }
    }

    // if we reach here, just commit and discard buffer.
    commitBuffer(inputContext);
    if (state->prependSpace_) {
        state->prependSpace_ = false;
    }
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
    const std::array<KeySym, 10> syms{
            FcitxKey_1, FcitxKey_2, FcitxKey_3, FcitxKey_4, FcitxKey_5,
            FcitxKey_6, FcitxKey_7, FcitxKey_8, FcitxKey_9, FcitxKey_0,
    };

    KeyStates states;
    switch (*config_.chooseModifier) {
        case ChooseModifier::Alt:
            states = KeyState::Alt;
            break;
        case ChooseModifier::Control:
            states = KeyState::Ctrl;
            break;
        case ChooseModifier::Super:
            states = KeyState::Super;
            break;
        case ChooseModifier::NoModifier:
            break;
    }

    for (auto sym: syms) {
        selectionKeys_.emplace_back(sym, states);
    }
}

void AndroidKeyboardEngine::save() {
    safeSaveAsIni(config_, ConfPath);
}

void AndroidKeyboardEngine::setConfig(const RawConfig &config) {
    config_.load(config, true);
    safeSaveAsIni(config_, ConfPath);
    reloadConfig();
}

void AndroidKeyboardEngine::activate(const InputMethodEntry &entry, InputContextEvent &event) {
    FCITX_UNUSED(entry);
    auto *inputContext = event.inputContext();
    wordHintAction_.setChecked(*config_.enableWordHint);
    wordHintAction_.update(inputContext);
    inputContext->statusArea().addAction(StatusGroup::InputMethod, &wordHintAction_);
}

void AndroidKeyboardEngine::deactivate(const InputMethodEntry &entry, InputContextEvent &event) {
    auto *inputContext = event.inputContext();
    // Android would commit composing text when finishing input (we simulate as focus out/in),
    // but not so when switching input method in fcitx
    if (event.type() == EventType::InputContextSwitchInputMethod && inputContext->hasFocus()) {
        commitBuffer(inputContext);
    }
    reset(entry, event);
}

void AndroidKeyboardEngine::reset(const InputMethodEntry &entry, InputContextEvent &event) {
    FCITX_UNUSED(entry);
    auto *inputContext = event.inputContext();
    resetState(inputContext);
    inputContext->inputPanel().reset();
    inputContext->updatePreedit();
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

void AndroidKeyboardEngine::resetState(InputContext *inputContext, bool fromCandidate) {
    auto *state = inputContext->propertyFor(&factory_);
    state->reset();
    if (fromCandidate) {
        // TODO set prependSpace_ to false when cursor moves; maybe it's time to implement SurroundingText
        state->prependSpace_ = *config_.insertSpace;
    }
}

void AndroidKeyboardEngine::updateCandidate(const InputMethodEntry &entry, InputContext *inputContext) {
    inputContext->inputPanel().reset();
    auto *state = inputContext->propertyFor(&factory_);
    std::vector<std::pair<std::string, std::string>> results;
    if (spell()) {
        results = spell()->call<ISpell::hintForDisplay>(entry.languageCode(),
                                                        SpellProvider::Default,
                                                        state->buffer_.userInput(),
                                                        SpellCandidateSize);
    }
    auto candidateList = std::make_unique<CommonCandidateList>();
    for (const auto &result: results) {
        candidateList->append<AndroidKeyboardCandidateWord>(this, Text(result.first), result.second);
    }
    candidateList->setPageSize(*config_.pageSize);
    candidateList->setSelectionKey(selectionKeys_);
    candidateList->setCursorIncludeUnselected(true);
    inputContext->inputPanel().setCandidateList(std::move(candidateList));

    updateUI(inputContext);
}

void AndroidKeyboardEngine::updateUI(InputContext *inputContext) {
    auto [text, cursor] = preeditWithCursor(inputContext);
    if (inputContext->capabilityFlags().test(CapabilityFlag::Preedit)) {
        Text clientPreedit(text, TextFormatFlag::Underline);
        clientPreedit.setCursor(static_cast<int>(cursor));
        inputContext->inputPanel().setClientPreedit(clientPreedit);
        inputContext->updatePreedit();
    } else {
        Text preedit(text);
        preedit.setCursor(static_cast<int>(cursor));
        inputContext->inputPanel().setPreedit(preedit);
    }
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

bool AndroidKeyboardEngine::updateBuffer(InputContext *inputContext, const std::string &chr) {
    auto *entry = instance_->inputMethodEntry(inputContext);
    if (!entry) {
        return false;
    }

    auto *state = inputContext->propertyFor(&factory_);
    const CapabilityFlags noPredictFlag{CapabilityFlag::Password,
                                        CapabilityFlag::NoSpellCheck};
    // no spell hint enabled or no supported dictionary
    if (!*config_.enableWordHint ||
        inputContext->capabilityFlags().testAny(noPredictFlag) ||
        !supportHint(entry->languageCode())) {
        return false;
    }

    auto &buffer = state->buffer_;
    auto [preedit, cursor] = preeditWithCursor(inputContext);
    if (preedit != buffer.userInput()) {
        buffer.clear();
        buffer.type(preedit);
    }

    buffer.type(chr);

    if (buffer.size() >= MaxBufferSize) {
        commitBuffer(inputContext);
        return true;
    }

    updateCandidate(*entry, inputContext);
    return true;
}

void AndroidKeyboardEngine::commitBuffer(InputContext *inputContext) {
    auto [preedit, cursor] = preeditWithCursor(inputContext);
    if (preedit.empty()) {
        return;
    }
    if (inputContext->capabilityFlags().test(CapabilityFlag::CommitStringWithCursor)) {
        inputContext->commitStringWithCursor(preedit, cursor);
    } else {
        inputContext->commitString(preedit);
    }
    resetState(inputContext);
    inputContext->inputPanel().reset();
    inputContext->updatePreedit();
    inputContext->updateUserInterface(UserInterfaceComponent::InputPanel);
}

bool AndroidKeyboardEngine::supportHint(const std::string &language) {
    const bool hasSpell = spell() && spell()->call<ISpell::checkDict>(language);
    return hasSpell;
}

std::pair<std::string, size_t> AndroidKeyboardEngine::preeditWithCursor(InputContext *inputContext) {
    auto *state = inputContext->propertyFor(&factory_);
    return {state->buffer_.userInput(), state->buffer_.cursorByChar()};
}

void AndroidKeyboardEngine::invokeActionImpl(const InputMethodEntry &entry, InvokeActionEvent &event) {
    const int cursor = event.cursor();
    auto inputContext = event.inputContext();
    auto *state = inputContext->propertyFor(&factory_);
    if (event.action() != InvokeActionEvent::Action::LeftClick
        || cursor < 0
        || static_cast<size_t>(cursor) > state->buffer_.size()) {
        return InputMethodEngineV3::invokeActionImpl(entry, event);
    }
    event.filter();
    state->buffer_.setCursor(static_cast<size_t>(cursor));
    updateUI(inputContext);
}

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidKeyboardEngineFactory)
