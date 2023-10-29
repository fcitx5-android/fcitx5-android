/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_ANDROIDNOTIFICATION_H
#define FCITX5_ANDROID_ANDROIDNOTIFICATION_H

#include <functional>
#include <unordered_set>
#include <utility>
#include <fcitx-config/configuration.h>
#include <fcitx-config/iniparser.h>
#include <fcitx-utils/fs.h>
#include <fcitx-utils/i18n.h>
#include <fcitx/addoninstance.h>
#include <fcitx/instance.h>

#include <notifications_public.h>

namespace fcitx {

FCITX_CONFIGURATION(NotificationsConfig,
                    fcitx::Option<std::vector<std::string>> hiddenNotifications{
                            this, "HiddenNotifications",
                            _("Hidden Notifications")};);

class Notifications final : public AddonInstance {
public:
    Notifications(Instance *instance) : instance_(instance) {};
    ~Notifications() = default;

    Instance *instance() { return instance_; }

    void updateConfig();
    void reloadConfig() override;
    void save() override;

    const Configuration *getConfig() const override { return &config_; }

    void setConfig(const RawConfig &config) override {
        config_.load(config, true);
        safeSaveAsIni(config_, ConfPath);
        updateConfig();
    }

    FCITX_ADDON_DEPENDENCY_LOADER(androidfrontend, instance_->addonManager());

    uint32_t sendNotification(const std::string &appName, uint32_t replaceId,
                              const std::string &appIcon,
                              const std::string &summary,
                              const std::string &body,
                              const std::vector<std::string> &actions,
                              int32_t timeout,
                              NotificationActionCallback actionCallback,
                              NotificationClosedCallback closedCallback);

    void showTip(const std::string &tipId, const std::string &appName,
                 const std::string &appIcon, const std::string &summary,
                 const std::string &body, int32_t timeout);

    void closeNotification(uint64_t internalId);

private:
    FCITX_ADDON_EXPORT_FUNCTION(Notifications, sendNotification);
    FCITX_ADDON_EXPORT_FUNCTION(Notifications, showTip);
    FCITX_ADDON_EXPORT_FUNCTION(Notifications, closeNotification);

    static const inline std::string ConfPath = "conf/androidnotification.conf";

    NotificationsConfig config_;
    Instance *instance_;

    std::unordered_set<std::string> hiddenNotifications_;

}; // class Notifications

} // namespace fcitx

#endif //FCITX5_ANDROID_ANDROIDNOTIFICATION_H
