#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputcontextmanager.h>
#include <fcitx/inputpanel.h>

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
        auto preedit = inputPanel().preedit().toString();
        auto clientPreedit = inputPanel().clientPreedit().toString();
        frontend_->updatePreedit(preedit, clientPreedit);
    }

    BulkCandidateList* bulkCandidateList() {
        auto candidateList = inputPanel().candidateList();
        if (!candidateList || candidateList->empty()) {
            FCITX_INFO() << "bulkCandidateList: no or empty candidateList";
            return nullptr;
        }
        return candidateList->toBulk();
    }

    void updateClientSideUIImpl() override {
        std::vector<std::string> candidates;
        auto list = bulkCandidateList();
        if (list == nullptr) {
            frontend_->updateCandidateList(candidates);
            return;
        }
        int size = list->totalSize();
        for (int i = 0; i < size; i++) {
            auto &candidate = list->candidateFromAll(i);
            if (candidate.isPlaceHolder()) {
                continue;
            }
            candidates.push_back(candidate.text().toString());
        }
        frontend_->updateCandidateList(candidates);
    }

    bool selectCandidate(int idx) {
        auto list = bulkCandidateList();
        if (list == nullptr) {
            return false;
        }
        int size = list->totalSize();
        if (idx >= size) {
            return false;
        }
        list->candidateFromAll(idx).select(this);
        return true;
    }

private:
    AndroidFrontend *frontend_;
};

AndroidFrontend::AndroidFrontend(Instance *instance)
    : instance_(instance),
      commitStringCallback({}),
      candidateListCallback({}),
      preeditCallback({}) {}

AndroidFrontend::~AndroidFrontend() = default;

ICUUID
AndroidFrontend::createInputContext(const std::string &program) {
    auto *ic = new AndroidInputContext(this, instance_->inputContextManager(), program);
    CapabilityFlags flags;
    flags |= CapabilityFlag::Preedit;
    flags |= CapabilityFlag::ClientSideInputPanel;
    ic->setCapabilityFlags(flags);
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
}

void AndroidFrontend::commitString(const std::string &str) {
    if (commitStringCallback) {
        commitStringCallback(str);
    }
}

void AndroidFrontend::updateCandidateList(const std::vector<std::string> &candidates) {
    if (candidateListCallback) {
        candidateListCallback(candidates);
    }
}

void AndroidFrontend::updatePreedit(const std::string &preedit, const std::string &clientPreedit) {
    if (preeditCallback) {
        preeditCallback(preedit, clientPreedit);
    }
}

void AndroidFrontend::selectCandidate(ICUUID uuid, int idx) {
    auto *ic = dynamic_cast<AndroidInputContext*>(instance_->inputContextManager().findByUUID(uuid));
    ic->selectCandidate(idx);
}

void AndroidFrontend::setCandidateListCallback(const CandidateListCallback& callback) {
    candidateListCallback = callback;
}

void AndroidFrontend::setCommitStringCallback(const CommitStringCallback & callback) {
    commitStringCallback = callback;
}

void AndroidFrontend::setPreeditCallback(const PreeditCallback &callback) {
    preeditCallback = callback;
}

    class AndroidFrontendFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidFrontend(manager->instance());
    }
};

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidFrontendFactory);
