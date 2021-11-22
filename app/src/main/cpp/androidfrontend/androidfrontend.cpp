#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputcontextmanager.h>
#include <fcitx/inputpanel.h>
#include <fcitx/instance.h>

#include "androidfrontend.h"

namespace fcitx {

class AndroidInputContext : public InputContext {
public:
    AndroidInputContext(AndroidFrontend *frontend,
                        InputContextManager &inputContextManager,
                        const std::string &program)
            : InputContext(inputContextManager, program), frontend_(frontend) {
        created();
    }

    ~AndroidInputContext() override { destroy(); }

    [[nodiscard]] const char *frontend() const override { return "androidfrontend"; }

    void commitStringImpl(const std::string &text) override {
        frontend_->commitString(text);
    }

    void forwardKeyImpl(const ForwardKeyEvent &key) override {
        FCITX_INFO() << "ForwardKey: " << key.key();
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
        auto preedit = ip.preedit().toString();
        auto clientPreedit = ip.clientPreedit().toString();
        frontend_->updatePreedit(preedit, clientPreedit);
        auto auxUp = ip.auxUp().toString();
        auto auxDown = ip.auxDown().toString();
        frontend_->updateInputPanelAux(auxUp, auxDown);
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
                    candidates.emplace_back(std::move(candidate.text().toString()));
                }
            } else {
                const int size = list->size();
                for (int i = 0; i < size; i++) {
                    candidates.emplace_back(std::move(list->candidate(i).text().toString()));
                }
            }
        }
        frontend_->updateCandidateList(candidates);
    }

    void selectCandidate(int idx) {
        const auto &list = inputPanel().candidateList();
        if (!list) {
            return;
        }
        const auto &bulk = list->toBulk();
        if (bulk) {
            try {
                bulk->candidateFromAll(idx).select(this);
            } catch (const std::invalid_argument &e) {
                FCITX_WARN() << "BulkCandidateList index out of range";
            }
        } else {
            const int size = list->size();
            if (idx >= 0 && idx < size) {
                list->candidate(idx).select(this);
            } else {
                FCITX_WARN() << "CandidateList index out of range";
            }
        }
    }

private:
    AndroidFrontend *frontend_;
};

AndroidFrontend::AndroidFrontend(Instance *instance)
        : instance_(instance) {
    eventHandlers_.emplace_back(instance_->watchEvent(
            EventType::InputContextSwitchInputMethod,
            EventWatcherPhase::Default,
            [this](Event &event) { imChangeCallback(); }
    ));
}

AndroidFrontend::~AndroidFrontend() = default;

ICUUID AndroidFrontend::createInputContext(const std::string &program) {
    auto *ic = new AndroidInputContext(this, instance_->inputContextManager(), program);
    ic->setCapabilityFlags(CapabilityFlags{
            CapabilityFlag::Preedit,
            CapabilityFlag::ClientSideInputPanel
    });
    // focus needed for `InputContext::reset` to work
    ic->focusIn();
    return ic->uuid();
}

void AndroidFrontend::destroyInputContext(ICUUID uuid) {
    auto *ic = instance_->inputContextManager().findByUUID(uuid);
    delete ic;
}

void AndroidFrontend::keyEvent(ICUUID uuid, const Key &key, bool isRelease) {
    auto *ic = instance_->inputContextManager().findByUUID(uuid);
    if (!ic) {
        return;
    }
    KeyEvent keyEvent(ic, key, isRelease);
    ic->keyEvent(keyEvent);
    FCITX_INFO() << "KeyEvent(key=" << key
                 << ", isRelease=" << isRelease
                 << ", accepted=" << keyEvent.accepted() << ")";
    if (!keyEvent.accepted()) {
        auto sym = key.sym();
        keyEventCallback(fcitx::Key::keySymToUnicode(sym), fcitx::Key::keySymToString(sym));
    }
}

void AndroidFrontend::commitString(const std::string &str) {
    commitStringCallback(str);
}

void AndroidFrontend::updateCandidateList(const std::vector<std::string> &candidates) {
    candidateListCallback(candidates);
}

void AndroidFrontend::updatePreedit(const std::string &preedit, const std::string &clientPreedit) {
    preeditCallback(preedit, clientPreedit);
}

void AndroidFrontend::updateInputPanelAux(const std::string &auxUp, const std::string &auxDown) {
    inputPanelAuxCallback(auxUp, auxDown);
}

void AndroidFrontend::selectCandidate(ICUUID uuid, int idx) {
    auto *ic = dynamic_cast<AndroidInputContext *>(instance_->inputContextManager().findByUUID(uuid));
    ic->selectCandidate(idx);
}

bool AndroidFrontend::isInputPanelEmpty(ICUUID uuid) {
    auto *ic = instance_->inputContextManager().findByUUID(uuid);
    return ic->inputPanel().empty();
}

void AndroidFrontend::resetInputPanel(ICUUID uuid) {
    auto *ic = instance_->inputContextManager().findByUUID(uuid);
    // `InputPanel::reset()` seems to have no effect
    // ic->inputPanel().reset();
    ic->reset(ResetReason::LostFocus);
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

class AndroidFrontendFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidFrontend(manager->instance());
    }
};

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidFrontendFactory);
