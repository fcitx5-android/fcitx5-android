/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
#ifndef _FCITX5_ANDROID_ANDROIDFRONTEND_PUBLIC_H_
#define _FCITX5_ANDROID_ANDROIDFRONTEND_PUBLIC_H_

#include <fcitx/inputcontext.h>
#include <fcitx-utils/key.h>

typedef std::function<void(const std::vector<std::string> &, const int)> CandidateListCallback;
typedef std::function<void(const std::string &, const int)> CommitStringCallback;
typedef std::function<void(const fcitx::Text &)> ClientPreeditCallback;
typedef std::function<void(const fcitx::Text &, const fcitx::Text &, const fcitx::Text &)> InputPanelCallback;
typedef std::function<void(const int, const uint32_t, const uint32_t, const bool, const int)> KeyEventCallback;
typedef std::function<void()> InputMethodChangeCallback;
typedef std::function<void()> StatusAreaUpdateCallback;
typedef std::function<void(const int, const int)> DeleteSurroundingCallback;
typedef std::function<void(const std::string &)> ToastCallback;

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, keyEvent,
                             void(const fcitx::Key &, bool isRelease, const int timestamp))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, selectCandidate,
                             bool(int idx))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, isInputPanelEmpty,
                             bool())

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, resetInputContext,
                             void())

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, repositionCursor,
                             void(int))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, focusInputContext,
                             void(bool))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, activateInputContext,
                             void(const int, const std::string &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, activeInputContext,
                             InputContext * ())

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, deactivateInputContext,
                             void(const int))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCapabilityFlags,
                             void(uint64_t))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, getCandidates,
                             std::vector<std::string>(const int, const int))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, showToast,
                             void(const std::string &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCandidateListCallback,
                             void(const CandidateListCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCommitStringCallback,
                             void(const CommitStringCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setPreeditCallback,
                             void(const ClientPreeditCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setInputPanelAuxCallback,
                             void(const InputPanelCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setKeyEventCallback,
                             void(const KeyEventCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setInputMethodChangeCallback,
                             void(const InputMethodChangeCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setStatusAreaUpdateCallback,
                             void(const StatusAreaUpdateCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setDeleteSurroundingCallback,
                             void(const DeleteSurroundingCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setToastCallback,
                             void(const ToastCallback &))

#endif // _FCITX5_ANDROID_ANDROIDFRONTEND_PUBLIC_H_
