/*
 * SPDX-FileCopyrightText: 2021-2021 Rocket Aaron <i@rocka.me>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 */

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
        FCITX_INFO() << "Commit: " << text;
        frontend_->commitString(text);
    }

    void forwardKeyImpl(const ForwardKeyEvent &key) override {
        FCITX_INFO() << "ForwardKey: " << key.key();
    }

    void deleteSurroundingTextImpl(int offset, unsigned int size) override {
        FCITX_INFO() << "DeleteSurrounding: " << offset << " " << size;
    }

    void updatePreeditImpl() override {
        FCITX_INFO() << "Update preedit: "
                     << inputPanel().clientPreedit().toString();
    }

    void updateClientSideUIImpl() override {
        frontend_->updateCandidateList(inputPanel().candidateList());
    }

private:
    AndroidFrontend *frontend_;
};

AndroidFrontend::AndroidFrontend(Instance *instance)
    : instance_(instance),
      cachedCandidateList(std::shared_ptr<CandidateList>(nullptr)),
      cachedBulkCandidateList(std::shared_ptr<BulkCandidateList>(nullptr)),
      commitStringCallback({}),
      candidateListCallback({}) {}

AndroidFrontend::~AndroidFrontend() = default;

ICUUID
AndroidFrontend::createInputContext(const std::string &program) {
    auto *ic = new AndroidInputContext(this, instance_->inputContextManager(), program);
    ic->setCapabilityFlags(CapabilityFlag::Preedit);
    ic->setCapabilityFlags(CapabilityFlag::ClientSideInputPanel);
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
    FCITX_INFO() << "KeyEvent key: " + key.toString()
                 + " isRelease: " + std::to_string(isRelease)
                 + " accepted: " + std::to_string(keyEvent.accepted());
}

void AndroidFrontend::commitString(const std::string &str) {
    if (commitStringCallback) {
        commitStringCallback(str);
    }
}

void AndroidFrontend::updateCandidateList(const std::shared_ptr<CandidateList>& candidateList) {
    if (candidateList) {
        cachedCandidateList = candidateList;
        cachedBulkCandidateList.reset(candidateList->toBulk());
    } else {
        cachedCandidateList = nullptr;
        cachedBulkCandidateList = nullptr;
    }
    if (candidateListCallback) {
        candidateListCallback(cachedBulkCandidateList);
    }
}

std::shared_ptr<BulkCandidateList>
AndroidFrontend::candidateList(ICUUID uuid) {
    return std::shared_ptr<BulkCandidateList>(cachedBulkCandidateList);
}

void AndroidFrontend::selectCandidate(ICUUID uuid, int idx) {
    auto *ic = instance_->inputContextManager().findByUUID(uuid);
    if (cachedBulkCandidateList) {
        cachedBulkCandidateList->candidateFromAll(idx).select(ic);
    }
}

void AndroidFrontend::setCandidateListCallback(const CandidateListCallback& callback) {
    candidateListCallback = callback;
}

void AndroidFrontend::setCommitStringCallback(const CommitStringCallback & callback) {
    commitStringCallback = callback;
}

class AndroidFrontendFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidFrontend(manager->instance());
    }
};

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidFrontendFactory);
