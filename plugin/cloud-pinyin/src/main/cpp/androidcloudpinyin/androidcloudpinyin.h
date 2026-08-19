/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

#ifndef FCITX5_ANDROID_ANDROIDCLOUDPINYIN_H
#define FCITX5_ANDROID_ANDROIDCLOUDPINYIN_H

#include <fcitx/addonfactory.h>
#include <fcitx/addoninstance.h>
#include <fcitx/instance.h>
#include <fcitx-config/configuration.h>
#include <fcitx-config/enum.h>
#include <fcitx-config/iniparser.h>
#include <fcitx-utils/eventdispatcher.h>
#include <fcitx-utils/eventloopinterface.h>
#include <fcitx-utils/i18n.h>
#include <fcitx-utils/misc.h>

#include <androidipcbridge_public.h>

#include "../../../../../../lib/fcitx5-chinese-addons/src/main/cpp/fcitx5-chinese-addons/modules/cloudpinyin/cloudpinyin_public.h"

#include "lrucache.h"

FCITX_CONFIG_ENUM(CloudPinyinBackend, Google, GoogleCN, Baidu);

FCITX_CONFIGURATION(
        CloudPinyinConfig,
        fcitx::Option<fcitx::KeyList> toggleKey{this, "Toggle Key", _("Toggle Key"),
                                                {fcitx::Key("Control+Alt+Shift+C")}};
                fcitx::Option<int> minimumLength{this, "MinimumPinyinLength",
                                                 _("Minimum Pinyin Length"), 4};
                fcitx::Option<CloudPinyinBackend> backend{this, "Backend", _("Backend"),
                                                          CloudPinyinBackend::GoogleCN};
                fcitx::OptionWithAnnotation<std::string, fcitx::ToolTipAnnotation> proxy{
                        this,
                        "Proxy",
                        _("Proxy"),
                        "",
                        {},
                        {},
                        {_("The proxy format must be the one that is supported by cURL. "
                           "Usually it is in the format of [scheme]://[host]:[port], e.g. "
                           "http://localhost:1080.")}};
)

class CloudPinyin : public fcitx::AddonInstance {
public:
    CloudPinyin(fcitx::AddonManager *manager);
    ~CloudPinyin() override;

    void reloadConfig() override;
    const fcitx::Configuration *getConfig() const override;
    void setConfig(const fcitx::RawConfig &config) override;

    void request(const std::string &pinyin, CloudPinyinCallback callback);
    const fcitx::KeyList &toggleKey() const;
    void resetError();

    FCITX_ADDON_DEPENDENCY_LOADER(androidipcbridge, instance_->addonManager());
private:
    FCITX_ADDON_EXPORT_FUNCTION(CloudPinyin, request);
    FCITX_ADDON_EXPORT_FUNCTION(CloudPinyin, toggleKey);
    FCITX_ADDON_EXPORT_FUNCTION(CloudPinyin, resetError);

    CloudPinyinConfig config_;
    std::string configPath_ = "conf/cloudpinyin.conf";

    void syncConfig();

    fcitx::Instance *instance_;
    fcitx::EventLoop *eventLoop_;
    fcitx::EventDispatcher &dispatcher_;

    int errorCount_ = 0;
    LRUCache<std::string, std::string> cache_{1024};
    std::unique_ptr<fcitx::EventSourceTime> resetError_;
};

class CloudPinyinFactory : public fcitx::AddonFactory {
public:
    fcitx::AddonInstance *create(fcitx::AddonManager *manager) override {
        return new CloudPinyin(manager);
    }
};

FCITX_ADDON_FACTORY_V2(cloudpinyin, CloudPinyinFactory);

#endif //FCITX5_ANDROID_ANDROIDCLOUDPINYIN_H
