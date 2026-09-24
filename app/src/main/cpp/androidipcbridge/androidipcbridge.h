/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_ANDROIDIPCBRIDGE_H
#define FCITX5_ANDROID_ANDROIDIPCBRIDGE_H

#include <functional>
#include <future>
#include <unordered_set>
#include <utility>
#include <fcitx-config/configuration.h>
#include <fcitx-config/iniparser.h>
#include <fcitx-utils/fs.h>
#include <fcitx-utils/i18n.h>
#include <fcitx/addoninstance.h>
#include <fcitx/addonfactory.h>
#include <fcitx/instance.h>

#include <androidipcbridge_public.h>

namespace fcitx {

FCITX_CONFIGURATION(AndroidIPCBridgeConfig
)

class AndroidIPCBridge final : public AddonInstance {
public:
    explicit AndroidIPCBridge(Instance *instance);

    Instance *instance() { return instance_; }

    void reloadConfig() override;

    void save() override;

    [[nodiscard]] const Configuration *getConfig() const override { return &config_; }

    void setConfig(const RawConfig &config) override;

    void notify(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params);

    void request(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params, AndroidIPCCallback callback);

    void setRequestHandler(const AndroidIPCRequestHandler &handler);

    void handleResponse(int id, int status, const std::string &msg, const AndroidIPCPayload &payload);

private:
    FCITX_ADDON_EXPORT_FUNCTION(AndroidIPCBridge, notify);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidIPCBridge, request);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidIPCBridge, setRequestHandler);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidIPCBridge, handleResponse);

    static const inline char *configPath_ = "conf/androidipcbridge.conf";
    AndroidIPCBridgeConfig config_;

    Instance *instance_;
    EventDispatcher &dispatcher_;

    AndroidIPCRequestHandler androidRequestHandler_ = [](int, const std::string &, const std::string &, const AndroidIPCPayload &) {};

    int requestId_ = 0;
    std::unordered_map<int, AndroidIPCCallback> pendingRequests_;

}; // class Notifications

class AndroidIPCBridgeModuleFactory : public AddonFactory {
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidIPCBridge(manager->instance());
    }
};

FCITX_ADDON_FACTORY_V2(androidipcbridge, fcitx::AndroidIPCBridgeModuleFactory)

} // namespace fcitx

#endif //FCITX5_ANDROID_ANDROIDIPCBRIDGE_H
