/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 */
#include <jni.h>

#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>

#include "../androidfrontend/androidfrontend_public.h"

#include "androidnotification.h"

namespace fcitx {

void Notifications::updateConfig() {
    hiddenNotifications_.clear();
    for (const auto &id: config_.hiddenNotifications.value()) {
        hiddenNotifications_.insert(id);
    }
}

void Notifications::reloadConfig() {
    readAsIni(config_, ConfPath);
    updateConfig();
}

void Notifications::save() {
    std::vector<std::string> values_;
    for (const auto &id: hiddenNotifications_) {
        values_.push_back(id);
    }
    config_.hiddenNotifications.setValue(std::move(values_));
    safeSaveAsIni(config_, ConfPath);
}

uint32_t Notifications::sendNotification(
        const std::string &appName,
        uint32_t replaceId,
        const std::string &appIcon,
        const std::string &summary,
        const std::string &body,
        const std::vector<std::string> &actions,
        int32_t timeout,
        NotificationActionCallback actionCallback,
        NotificationClosedCallback closedCallback) {
    // TODO implement Notification
    FCITX_UNUSED(appName);
    FCITX_UNUSED(replaceId);
    FCITX_UNUSED(appIcon);
    FCITX_UNUSED(summary);
    FCITX_UNUSED(body);
    FCITX_UNUSED(actions);
    FCITX_UNUSED(timeout);
    FCITX_UNUSED(actionCallback);
    FCITX_UNUSED(closedCallback);
    return 0;
}

void Notifications::showTip(
        const std::string &tipId,
        const std::string &appName,
        const std::string &appIcon,
        const std::string &summary,
        const std::string &body,
        int32_t timeout) {
    FCITX_UNUSED(appName);
    FCITX_UNUSED(appIcon);
    FCITX_UNUSED(timeout);
    if (hiddenNotifications_.count(tipId)) {
        return;
    }
    std::string const s = summary + ": " + body;
    androidfrontend()->call<IAndroidFrontend::showToast>(s);
}

void Notifications::closeNotification(uint64_t internalId) {
    FCITX_UNUSED(internalId);
}

class NotificationsModuleFactory : public AddonFactory {
    AddonInstance *create(AddonManager *manager) override {
        return new Notifications(manager->instance());
    }
};

}

FCITX_ADDON_FACTORY(fcitx::NotificationsModuleFactory)
