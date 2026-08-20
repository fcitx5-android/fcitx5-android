/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
#include <jni.h>

#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>

#include "androidipcbridge.h"

namespace fcitx {

AndroidIPCBridge::AndroidIPCBridge(Instance *instance)
        : instance_(instance),
          dispatcher_(instance->eventDispatcher()) {
    reloadConfig();
}

void AndroidIPCBridge::reloadConfig() {
    readAsIni(config_, configPath_);
}

void AndroidIPCBridge::save() {
    safeSaveAsIni(config_, configPath_);
}

void AndroidIPCBridge::setConfig(const fcitx::RawConfig &config) {
    config_.load(config, true);
    safeSaveAsIni(config_, configPath_);
}

void AndroidIPCBridge::notify(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params) {
    androidRequestHandler_(-1, plugin, method, params);
}

void AndroidIPCBridge::request(const std::string &plugin, const std::string &method, const AndroidIPCPayload &payload, AndroidIPCCallback callback) {
    const int id = requestId_++;
    pendingRequests_[id] = std::move(callback);
    androidRequestHandler_(id, plugin, method, payload);
}

void AndroidIPCBridge::setRequestHandler(const AndroidIPCRequestHandler &handler) {
    androidRequestHandler_ = handler;
}

void AndroidIPCBridge::handleResponse(const int id, const int status, const std::string &msg, const AndroidIPCPayload &payload) {
    auto node = pendingRequests_.extract(id);
    if (node.empty()) {
        FCITX_WARN() << "No matching AndroidIPCBridge request #" << id;
    } else {
        auto callback = node.mapped();
        callback({status, msg, payload});
    }
}

}
