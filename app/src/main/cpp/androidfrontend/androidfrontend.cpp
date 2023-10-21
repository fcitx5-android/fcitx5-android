#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputcontextmanager.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/focusgroup.h>
#include <fcitx/inputpanel.h>
#include <fcitx-utils/event.h>

#include "androidfrontend.h"

namespace fcitx {

class AndroidInputContext : public InputContextV2 {
public:
    AndroidInputContext(AndroidFrontend *frontend,
                        InputContextManager &inputContextManager,
                        int uid,
                        const std::string &pkgName)
            : InputContextV2(inputContextManager, pkgName),
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
        frontend_->commitString(text, -1);
    }

    void commitStringWithCursorImpl(const std::string &text, size_t cursor) override {
        frontend_->commitString(text, static_cast<int>(cursor));
    }

    void forwardKeyImpl(const ForwardKeyEvent &key) override {
        frontend_->forwardKey(key.rawKey(), key.isRelease());
    }

    void deleteSurroundingTextImpl(int offset, unsigned int size) override {
        const int before = -offset;
        const int after = offset + static_cast<int>(size);
        if (before < 0 || after < 0) {
            FCITX_WARN() << "Invalid deleteSurrounding request: offset=" << offset << ", size=" << size;
            return;
        }
        frontend_->deleteSurrounding(before, after);
    }

    void updatePreeditImpl() override {
        frontend_->updateClientPreedit(filterText(inputPanel().clientPreedit()));
    }

    void updateInputPanel() {
        // Normally input method engine should check CapabilityFlag::Preedit before update clientPreedit,
        // and fcitx5 won't trigger UpdatePreeditEvent when that flag is not present, in which case
        // InputContext::updatePreeditImpl() won't be called.
        // However on Android, androidkeyboard uses clientPreedit unconditionally in order to provide
        // a more integrated experience, so we need to check clientPreedit update manually even if
        // clientPreedit is not enabled.
        const InputPanel &ip = inputPanel();
        if (!isPreeditEnabled() && frontend_->instance()->inputMethod(this) == "keyboard-us") {
            frontend_->updateClientPreedit(filterText(ip.clientPreedit()));
        }
        frontend_->updateInputPanel(
                filterText(ip.preedit()),
                filterText(ip.auxUp()),
                filterText(ip.auxDown())
        );
        std::vector<std::string> candidates;
        int size = 0;
        const auto &list = ip.candidateList();
        if (list) {
            const auto &bulk = list->toBulk();
            if (bulk) {
                size = bulk->totalSize();
                // limit candidate count to 16 (for paging)
                const int limit = size < 0 ? 16 : std::min(size, 16);
                for (int i = 0; i < limit; i++) {
                    try {
                        auto &candidate = bulk->candidateFromAll(i);
                        // maybe unnecessary; I don't see anywhere using `CandidateWord::setPlaceHolder`
                        // if (candidate.isPlaceHolder()) continue;
                        candidates.emplace_back(filterString(candidate.text()));
                    } catch (const std::invalid_argument &e) {
                        size = static_cast<int>(candidates.size());
                        break;
                    }
                }
            } else {
                size = list->size();
                for (int i = 0; i < size; i++) {
                    candidates.emplace_back(filterString(list->candidate(i).text()));
                }
            }
        }
        frontend_->updateCandidateList(candidates, size);
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

    std::vector<std::string> getCandidates(const int offset, const int limit) {
        std::vector<std::string> candidates;
        const auto &list = inputPanel().candidateList();
        if (list) {
            const int last = offset + limit;
            const auto &bulk = list->toBulk();
            if (bulk) {
                const int totalSize = bulk->totalSize();
                const int end = totalSize < 0 ? last : std::min(totalSize, last);
                for (int i = offset; i < end; i++) {
                    try {
                        auto &candidate = bulk->candidateFromAll(i);
                        candidates.emplace_back(filterString(candidate.text()));
                    } catch (const std::invalid_argument &e) {
                        break;
                    }
                }
            } else {
                const int end = std::min(list->size(), last);
                for (int i = offset; i < end; i++) {
                    candidates.emplace_back(filterString(list->candidate(i).text()));
                }
            }
        }
        return candidates;
    }

private:
    AndroidFrontend *frontend_;
    int uid_;

    inline Text filterText(const Text &orig) {
        return frontend_->instance()->outputFilter(this, orig);
    }

    inline std::string filterString(const Text &orig) {
        return filterText(orig).toString();
    }
};

AndroidFrontend::AndroidFrontend(Instance *instance)
        : instance_(instance),
          focusGroup_("android", instance->inputContextManager()),
          activeIC_(nullptr),
          icCache_(),
          eventHandlers_(),
          statusAreaDefer_(),
          statusAreaUpdated_(false) {
    eventHandlers_.emplace_back(instance_->watchEvent(
            EventType::InputContextInputMethodActivated,
            EventWatcherPhase::Default,
            [this](Event &event) {
                FCITX_UNUSED(event);
                imChangeCallback();
            }
    ));
    eventHandlers_.emplace_back(instance_->watchEvent(
            EventType::InputContextUpdateUI,
            EventWatcherPhase::Default,
            [this](Event &event) {
                auto &e = static_cast<InputContextUpdateUIEvent &>(event);
                switch (e.component()) {
                    case UserInterfaceComponent::InputPanel: {
                        auto *ic = dynamic_cast<AndroidInputContext *>(activeIC_);
                        if (ic) ic->updateInputPanel();
                        break;
                    }
                    case UserInterfaceComponent::StatusArea: {
                        handleStatusAreaUpdate();
                        break;
                    }
                }
            }
    ));
}

void AndroidFrontend::keyEvent(const Key &key, bool isRelease, const int timestamp) {
    auto *ic = activeIC_;
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

void AndroidFrontend::commitString(const std::string &str, const int cursor) {
    commitStringCallback(str, cursor);
}

void AndroidFrontend::updateCandidateList(const std::vector<std::string> &candidates, const int size) {
    candidateListCallback(candidates, size);
}

void AndroidFrontend::updateClientPreedit(const Text &clientPreedit) {
    preeditCallback(clientPreedit);
}

void AndroidFrontend::updateInputPanel(const Text &preedit, const Text &auxUp, const Text &auxDown) {
    inputPanelAuxCallback(preedit, auxUp, auxDown);
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
    InvokeActionEvent event(InvokeActionEvent::Action::LeftClick, position, ic);
    ic->invokeAction(event);
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

void AndroidFrontend::activateInputContext(const int uid, const std::string &pkgName) {
    auto *ptr = icCache_.find(uid);
    if (ptr) {
        activeIC_ = ptr->get();
    } else {
        auto *ic = new AndroidInputContext(this, instance_->inputContextManager(), uid, pkgName);
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

std::vector<std::string> AndroidFrontend::getCandidates(const int offset, const int limit) {
    auto *ic = dynamic_cast<AndroidInputContext *>(focusGroup_.focusedInputContext());
    if (!ic) return {};
    return ic->getCandidates(offset, limit);
}

void AndroidFrontend::deleteSurrounding(const int before, const int after) {
    deleteSurroundingCallback(before, after);
}

void AndroidFrontend::showToast(const std::string &s) {
    toastCallback(s);
}

void AndroidFrontend::setCommitStringCallback(const CommitStringCallback &callback) {
    commitStringCallback = callback;
}

void AndroidFrontend::setPreeditCallback(const ClientPreeditCallback &callback) {
    preeditCallback = callback;
}

void AndroidFrontend::setInputPanelAuxCallback(const InputPanelCallback &callback) {
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

void AndroidFrontend::handleStatusAreaUpdate() {
    if (statusAreaUpdated_) return;
    statusAreaUpdated_ = true;
    statusAreaDefer_ = instance_->eventLoop().addDeferEvent([this](EventSource *) {
        statusAreaUpdateCallback();
        statusAreaUpdated_ = false;
        statusAreaDefer_ = nullptr;
        return true;
    });
}

void AndroidFrontend::setDeleteSurroundingCallback(const DeleteSurroundingCallback &callback) {
    deleteSurroundingCallback = callback;
}

void AndroidFrontend::setToastCallback(const ToastCallback &callback) {
    toastCallback = callback;
}

class AndroidFrontendFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidFrontend(manager->instance());
    }
};

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidFrontendFactory)
