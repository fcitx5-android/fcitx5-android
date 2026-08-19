/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

#include <fcitx/addoninstance.h>
#include <fcitx/addonmanager.h>
#include <fcitx-utils/event.h>

#include "androidcloudpinyin.h"

namespace {

constexpr int MAX_ERROR = 10;
constexpr uint64_t minInUs = 60000000;

}

CloudPinyin::CloudPinyin(fcitx::AddonManager *manager)
        : instance_(manager->instance()),
          eventLoop_(manager->eventLoop()),
          dispatcher_(instance_->eventDispatcher()) {
    resetError_ =
            eventLoop_->addTimeEvent(CLOCK_MONOTONIC, fcitx::now(CLOCK_MONOTONIC), minInUs,
                                     [this](fcitx::EventSourceTime *, uint64_t) {
                                         resetError();
                                         return true;
                                     });
    if (resetError_) {
        resetError_->setEnabled(false);
    }
    reloadConfig();
}

CloudPinyin::~CloudPinyin() = default;

void CloudPinyin::reloadConfig() {
    readAsIni(config_, configPath_);
    syncConfig();
}

const fcitx::Configuration *CloudPinyin::getConfig() const {
    return &config_;
}

void CloudPinyin::setConfig(const fcitx::RawConfig &config) {
    config_.load(config, true);
    fcitx::safeSaveAsIni(config_, configPath_);
    syncConfig();
}

void CloudPinyin::syncConfig() {
    std::vector<std::byte> backend {static_cast<const std::byte>(config_.backend.value())};
    androidipcbridge()->call<fcitx::IAndroidIPCBridge::notify>("cloud_pinyin", "set_backend", backend);
    const std::string &proxyString = config_.proxy.value();
    std::vector<std::byte> proxy(proxyString.size());
    std::memcpy(proxy.data(), proxyString.data(), proxyString.size());
    androidipcbridge()->call<fcitx::IAndroidIPCBridge::notify>("cloud_pinyin", "set_proxy", proxy);
}

void CloudPinyin::request(const std::string &pinyin, CloudPinyinCallback callback) {
    if (static_cast<int>(pinyin.size()) < config_.minimumLength.value()) {
        callback(pinyin, "");
        return;
    }
    if (auto *value = cache_.find(pinyin)) {
        callback(pinyin, *value);
    }
    std::vector<std::byte> vec(pinyin.size());
    std::memcpy(vec.data(), pinyin.data(), pinyin.size());
    androidipcbridge()->call<fcitx::IAndroidIPCBridge::request>(
            "cloud_pinyin", "request", vec,
            [&, pinyin, callback = std::move(callback)](auto &response) {
                if (response.status != 200) {
                    errorCount_ += 1;
                    if (errorCount_ >= MAX_ERROR && resetError_) {
                        FCITX_ERROR() << "Cloud pinyin reaches max error. "
                                         "Retry in 5 minutes.";
                        resetError_->setNextInterval(minInUs * 5);
                        resetError_->setOneShot();
                    }
                }
                std::string hanzi = response.msg;
                callback(pinyin, hanzi);
                if (!hanzi.empty()) {
                    cache_.insert(pinyin, hanzi);
                }
            });
}

const fcitx::KeyList &CloudPinyin::toggleKey() const {
    return config_.toggleKey.value();
}

void CloudPinyin::resetError() {
    resetError_->setEnabled(false);
    errorCount_ = 0;
}
