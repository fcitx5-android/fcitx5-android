/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

#include <fcitx/addoninstance.h>
#include <fcitx/addonmanager.h>
#include <fcitx-utils/event.h>

#include <nlohmann/json.hpp>

#include "androidcloudpinyin.h"

namespace {

constexpr int MAX_ERROR = 10;
constexpr uint64_t minInUs = 60000000;

}

CloudPinyin::CloudPinyin(fcitx::AddonManager *manager)
        : instance_(manager->instance()),
          eventLoop_(manager->eventLoop()),
          dispatcher_(instance_->eventDispatcher()) {
    resetError_ = eventLoop_->addTimeEvent(
            CLOCK_MONOTONIC, fcitx::now(CLOCK_MONOTONIC), minInUs,
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
}

const fcitx::Configuration *CloudPinyin::getConfig() const {
    return &config_;
}

void CloudPinyin::setConfig(const fcitx::RawConfig &config) {
    config_.load(config, true);
    fcitx::safeSaveAsIni(config_, configPath_);
}

void CloudPinyin::request(const std::string &pinyin, CloudPinyinCallback callback) {
    if (errorCount_ >= MAX_ERROR ||
        static_cast<int>(pinyin.size()) < config_.minimumLength.value()) {
        callback(pinyin, "");
        return;
    }
    if (auto *value = cache_.find(pinyin)) {
        callback(pinyin, *value);
        return;
    }
    nlohmann::json json{
            {"backend", config_.backend.value()},
            {"pinyin",  pinyin}
    };
    if (!config_.proxy->empty()) {
        json["proxy"] = config_.proxy.value();
    }
    const auto &cbor = nlohmann::json::to_cbor(json);
    std::span<const std::byte> span{reinterpret_cast<const std::byte *>(cbor.data()), cbor.size()};
    androidipcbridge()->call<fcitx::IAndroidIPCBridge::requestWithTimeout>(
            "cloud_pinyin", "request", span, 1000000 * 5, // timeout in 5 seconds
            [&, pinyin, callback = std::move(callback)](auto &response) {
                if (response.status != 0) {
                    errorCount_ += 1;
                    if (errorCount_ >= MAX_ERROR && resetError_) {
                        FCITX_ERROR() << "Cloud pinyin reaches max error. "
                                         "Retry in 5 minutes.";
                        resetError_->setNextInterval(minInUs * 5);
                        resetError_->setOneShot();
                    }
                    callback(pinyin, "");
                    return;
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
