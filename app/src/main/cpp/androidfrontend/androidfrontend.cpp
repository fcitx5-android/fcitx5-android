/*
 * SPDX-FileCopyrightText: 2021-2021 Rocket Aaron <i@rocka.me>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 */

#include "androidfrontend.h"
#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputcontextmanager.h>
#include <fcitx/inputpanel.h>

namespace fcitx {

class AndroidInputContext : public InputContext {
public:
    AndroidInputContext(AndroidFrontend *frontend,
                        InputContextManager &inputContextManager,
                        const std::string &program)
        : InputContext(inputContextManager, program), frontend_(frontend) {
        created();
    }
    ~AndroidInputContext() { destroy(); }

    const char *frontend() const override { return "androidfrontend"; }

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

private:
    AndroidFrontend *frontend_;
};

AndroidFrontend::AndroidFrontend(Instance *instance) : instance_(instance) {}

AndroidFrontend::~AndroidFrontend() {
}

ICUUID
AndroidFrontend::createInputContext(const std::string &program) {
    auto *ic =
        new AndroidInputContext(this, instance_->inputContextManager(), program);
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
    FCITX_INFO() << "KeyEvent key: " << key.toString()
                 << " isRelease: " << isRelease
                 << " accepted: " << keyEvent.accepted();
}

void AndroidFrontend::commitString(const std::string &expect) {
    //
}

class AndroidFrontendFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidFrontend(manager->instance());
    }
};

} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::AndroidFrontendFactory);
