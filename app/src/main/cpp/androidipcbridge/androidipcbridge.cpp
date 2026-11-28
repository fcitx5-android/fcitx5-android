/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
#include <jni.h>

#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx-utils/event.h>

#include "androidipcbridge.h"

namespace fcitx {

AndroidIPCBridge::AndroidIPCBridge(Instance *instance)
        : instance_(instance),
          dispatcher_(instance->eventDispatcher()) {}

void AndroidIPCBridge::notify(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params) {
    androidRequestHandler_(-1, plugin, method, params);
}

void AndroidIPCBridge::request(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params, AndroidIPCCallback callback) {
    const int id = requestId_++;
    pendingRequests_.emplace(id, std::move(callback));
    androidRequestHandler_(id, plugin, method, params);
}

void AndroidIPCBridge::requestWithTimeout(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params, int timeout, AndroidIPCCallback callback) {
    const int id = requestId_++;
    pendingRequests_.emplace(id, std::move(callback));
    pendingTimers_.emplace(id, instance()->eventLoop().addTimeEvent(
            CLOCK_MONOTONIC, now(CLOCK_MONOTONIC) + timeout, 0,
            [=, this](EventSourceTime *, uint64_t) {
                FCITX_WARN() << "Request #" << id << " to " << plugin << "." << method
                             << " timeout";
                pendingTimers_.erase(id);
                auto node = pendingRequests_.extract(id);
                if (!node.empty()) {
                    auto callback = node.mapped();
                    callback({-3, "timeout", std::span<std::byte>()});
                }
                return true;
            }));
    androidRequestHandler_(id, plugin, method, params);
}

void AndroidIPCBridge::setRequestHandler(const AndroidIPCRequestHandler &handler) {
    androidRequestHandler_ = handler;
}

void AndroidIPCBridge::handleResponse(const int id, const int status, const std::string &msg, const AndroidIPCPayload &payload) {
    pendingTimers_.erase(id);
    auto node = pendingRequests_.extract(id);
    if (node.empty()) {
        FCITX_WARN() << "No matching request #" << id;
    } else {
        auto callback = node.mapped();
        callback({status, msg, payload});
    }
}

}
