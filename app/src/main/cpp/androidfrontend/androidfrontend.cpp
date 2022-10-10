#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputcontextmanager.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/focusgroup.h>
#include <fcitx/inputpanel.h>
#include <fcitx/instance.h>

#include "androidfrontend.h"

namespace fcitx {

class AndroidInputContext : public InputContext {
public:
    AndroidInputContext(AndroidFrontend *frontend,
                        InputContextManager &inputContextManager,
                        int uid)
            : InputContext(inputContextManager, std::to_string(uid)),
              frontend_(frontend),
              uid_(uid) {
        created();
    }

    ~AndroidInputContext() override {
        frontend_->releaseInputContext(uid_);
        destroy();
    }

    [[nodiscard]] const char *frontend() const override { return "androidfrontend"; }

    void commitStringImpl(const std::string &text) override {
        frontend_->commitString(text);
    }

    void forwardKeyImpl(const ForwardKeyEvent &key) override {
        frontend_->forwardKey(key.rawKey(), key.isRelease());
    }

    void deleteSurroundingTextImpl(int offset, unsigned int size) override {
        FCITX_INFO() << "DeleteSurrounding: " << offset << " " << size;
    }

    void updatePreeditImpl() override {
        // if PreeditInApplication is disabled, this function is not called
        // moved to `updateClientSideUIImpl`
    }

    void updateClientSideUIImpl() override {
        InputPanel &ip = inputPanel();
        frontend_->updatePreedit(
                frontend_->instance()->outputFilter(this, ip.preedit()),
                frontend_->instance()->outputFilter(this, ip.clientPreedit())
        );
        frontend_->updateInputPanelAux(filterText(ip.auxUp()), filterText(ip.auxDown()));
        std::vector<std::string> candidates;
        const auto &list = ip.candidateList();
        if (list) {
            const auto &bulk = list->toBulk();
            if (bulk) {
                const int size = bulk->totalSize();
                for (int i = 0; i < size; i++) {
                    auto &candidate = bulk->candidateFromAll(i);
                    // maybe unnecessary; I don't see anywhere using `CandidateWord::setPlaceHolder`
                    // if (candidate.isPlaceHolder()) continue;
                    candidates.emplace_back(filterText(candidate.text()));
                }
            } else {
                const int size = list->size();
                for (int i = 0; i < size; i++) {
                    candidates.emplace_back(filterText(list->candidate(i).text()));
                }
            }
        }
        frontend_->updateCandidateList(candidates);
    }

    bool selectCandidate(int idx) {
        const auto &list = inputPanel().candidateList();
        if (!list) {
            return false;
        }
        const auto &bulk = list->toBulk();
        try {
            if (bulk) {
                bulk->candidateFromAll(idx).select(this);
            } else {
                list->candidate(idx).select(this);
            }
        } catch (const std::invalid_argument &e) {
            FCITX_WARN() << "selectCandidate index out of range";
            return false;
        }
        return true;
    }

private:
    AndroidFrontend *frontend_;
    int uid_;

    std::string filterText(const Text &orig) {
        return frontend_->instance()->outputFilter(this, orig).toString();
    }
};

AndroidFrontend::AndroidFrontend(Instance *instance)
        : instance_(instance),
          focusGroup_("android", instance->inputContextManager()),
          activeIC_(nullptr),
          icCache_(),
          eventHandlers_() {
    eventHandlers_.emplace_back(instance_->watchEvent(
            EventType::InputContextInputMethodActivated,
            EventWatcherPhase::Default,
            [this](Event &event) { imChangeCallback(); }
    ));
    eventHandlers_.emplace_back(instance_->watchEvent(
            EventType::InputContextUpdateUI,
            EventWatcherPhase::Default,
            [this](Event &event) {
                auto &e = static_cast<InputContextUpdateUIEvent &>(event);
                if (e.component() == UserInterfaceComponent::StatusArea) {
                    statusAreaUpdateCallback();
                }
            }
    ));
}

void AndroidFrontend::keyEvent(const Key &key, bool isRelease, const int timestamp) {
    auto *ic = focusGroup_.focusedInputContext();
    if (!ic) return;
    KeyEvent keyEvent(ic, key, isRelease);
    ic->keyEvent(keyEvent);
    if (!keyEvent.accepted()) {
        auto sym = key.sym();
        keyEventCallback(sym, key.states(), Key::keySymToUnicode(sym), isRelease, timestamp);
    }
}

void AndroidFrontend::forwardKey(const Key &key, bool isRelease) {
    auto sym = key.sym();
    keyEventCallback(sym, key.states(), Key::keySymToUnicode(sym), isRelease, -1);
}

void AndroidFrontend::commitString(const std::string &str) {
    commitStringCallback(str);
}

void AndroidFrontend::updateCandidateList(const std::vector<std::string> &candidates) {
    candidateListCallback(candidates);
}

void AndroidFrontend::updatePreedit(const Text &preedit, const Text &clientPreedit) {
    preeditCallback(preedit.toString(), preedit.cursor(), clientPreedit.toString(), clientPreedit.cursor());
}

void AndroidFrontend::updateInputPanelAux(const std::string &auxUp, const std::string &auxDown) {
    inputPanelAuxCallback(auxUp, auxDown);
}

void AndroidFrontend::releaseInputContext(const int uid) {
    icCache_.release(uid);
}

bool AndroidFrontend::selectCandidate(int idx) {
    auto *ic = dynamic_cast<AndroidInputContext *>(focusGroup_.focusedInputContext());
    if (!ic) return false;
    return ic->selectCandidate(idx);
}

bool AndroidFrontend::isInputPanelEmpty() {
    auto *ic = focusGroup_.focusedInputContext();
    if (!ic) return true;
    return ic->inputPanel().empty();
}

void AndroidFrontend::resetInputContext() {
    auto *ic = focusGroup_.focusedInputContext();
    if (!ic) return;
    ic->reset();
}

void AndroidFrontend::repositionCursor(int position) {
    auto *ic = focusGroup_.focusedInputContext();
    if (!ic) return;
    auto engine = instance_->inputMethodEngine(ic);
    InvokeActionEvent event(InvokeActionEvent::Action::LeftClick, position, ic);
    engine->invokeAction(*(instance_->inputMethodEntry(ic)), event);
}

void AndroidFrontend::focusInputContext(bool focus) {
    if (focus) {
        if (!activeIC_) return;
        activeIC_->focusIn();
    } else {
        auto *ic = focusGroup_.focusedInputContext();
        if (!ic) return;
        ic->focusOut();
    }
}

void AndroidFrontend::activateInputContext(const int uid) {
    auto *ptr = icCache_.find(uid);
    if (ptr) {
        activeIC_ = ptr->get();
    } else {
        auto *ic = new AndroidInputContext(this, instance_->inputContextManager(), uid);
        activeIC_ = ic;
        icCache_.insert(uid, ic);
        ic->setFocusGroup(&focusGroup_);
    }
}

InputContext *AndroidFrontend::activeInputContext() const {
    return activeIC_;
}

void AndroidFrontend::deactivateInputContext(const int uid) {
    auto *ptr = icCache_.find(uid);
    if (!ptr) return;
    focusGroup_.setFocusedInputContext(nullptr);
    activeIC_ = nullptr;
}

void AndroidFrontend::setCapabilityFlags(uint64_t flag) {
    if (!activeIC_) return;
    activeIC_->setCapabilityFlags(CapabilityFlags(flag));
}

void AndroidFrontend::setCandidateListCallback(const CandidateListCallback &callback) {
    candidateListCallback = callback;
}

void AndroidFrontend::setCommitStringCallback(const CommitStringCallback &callback) {
    commitStringCallback = callback;
}

void AndroidFrontend::setPreeditCallback(const PreeditCallback &callback) {
    preeditCallback = callback;
}

void AndroidFrontend::setInputPanelAuxCallback(const InputPanelAuxCallback &callback) {
    inputPanelAuxCallback = callback;
}

void AndroidFrontend::setKeyEventCallback(const KeyEventCallback &callback) {
    keyEventCallback = callback;
}

void AndroidFrontend::setInputMethodChangeCallback(const InputMethodChangeCallback &callback) {
    imChangeCallback = callback;
}

void AndroidFrontend::setStatusAreaUpdateCallback(const StatusAreaUpdateCallback &callback) {
    statusAreaUpdateCallback = callback;
}

class AndroidFrontendFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidFrontend(manager->instance());
    }
};

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidFrontendFactory)
