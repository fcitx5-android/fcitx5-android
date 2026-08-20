/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_ANDROIDIPCBRIDGE_PUBLIC_H
#define FCITX5_ANDROID_ANDROIDIPCBRIDGE_PUBLIC_H

using AndroidIPCPayload = std::span<const std::byte>;

struct AndroidIPCResponse {
    int status;
    std::string msg;
    AndroidIPCPayload payload;
};

typedef std::function<void(const AndroidIPCResponse &response)> AndroidIPCCallback;

typedef std::function<void(int id, const std::string &plugin, const std::string &method, const AndroidIPCPayload &params)> AndroidIPCRequestHandler;

FCITX_ADDON_DECLARE_FUNCTION(AndroidIPCBridge, notify,
                             void(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params))

FCITX_ADDON_DECLARE_FUNCTION(AndroidIPCBridge, request,
                             void(const std::string &plugin, const std::string &method, const AndroidIPCPayload &params, AndroidIPCCallback callback))

FCITX_ADDON_DECLARE_FUNCTION(AndroidIPCBridge, setRequestHandler,
                             void(const AndroidIPCRequestHandler &handler))

FCITX_ADDON_DECLARE_FUNCTION(AndroidIPCBridge, handleResponse,
                             void(int id, int status, const std::string &msg, const const AndroidIPCPayload &payload))

#endif // FCITX5_ANDROID_ANDROIDIPCBRIDGE_PUBLIC_H
