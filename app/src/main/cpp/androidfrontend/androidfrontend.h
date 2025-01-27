/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */

#ifndef FCITX5_ANDROID_ANDROIDFRONTEND_H
#define FCITX5_ANDROID_ANDROIDFRONTEND_H

#include <fcitx/instance.h>
#include <fcitx/addoninstance.h>
#include <fcitx-utils/i18n.h>

#include "androidfrontend_public.h"
#include "inputcontextcache.h"

namespace fcitx {

class AndroidInputContext;

class AndroidFrontend : public AddonInstance {
public:
    explicit AndroidFrontend(Instance *instance);

    Instance *instance() { return instance_; }

    void updateCandidateList(const std::vector<std::string> &candidates, const int size);
    void commitString(const std::string &str, const int cursor);
    void updateClientPreedit(const Text &clientPreedit);
    void updateInputPanel(const Text &preedit, const Text &auxUp, const Text &auxDown);
    void releaseInputContext(const int uid);
    void updatePagedCandidate(const PagedCandidateEntity &paged);

    void keyEvent(const Key &key, bool isRelease, const int timestamp);
    void forwardKey(const Key &key, bool isRelease);
    bool selectCandidate(int idx);
    bool isInputPanelEmpty();
    void resetInputContext();
    void repositionCursor(int idx);
    void focusInputContext(bool focus);
    void activateInputContext(const int uid, const std::string &pkgName);
    void deactivateInputContext(const int uid);
    [[nodiscard]] InputContext *activeInputContext() const;
    void setCapabilityFlags(uint64_t flag);
    std::vector<std::string> getCandidates(const int offset, const int limit);
    std::vector<CandidateAction> getCandidateActions(const int idx);
    void triggerCandidateAction(const int idx, const int actionIdx);
    void deleteSurrounding(const int before, const int after);
    void showToast(const std::string &s);
    void setCandidatePagingMode(const int mode);
    void offsetCandidatePage(int delta);
    void setCandidateListCallback(const CandidateListCallback &callback);
    void setCommitStringCallback(const CommitStringCallback &callback);
    void setPreeditCallback(const ClientPreeditCallback &callback);
    void setInputPanelAuxCallback(const InputPanelCallback &callback);
    void setKeyEventCallback(const KeyEventCallback &callback);
    void setInputMethodChangeCallback(const InputMethodChangeCallback &callback);
    void setStatusAreaUpdateCallback(const StatusAreaUpdateCallback &callback);
    void setDeleteSurroundingCallback(const DeleteSurroundingCallback &callback);
    void setToastCallback(const ToastCallback &callback);
    void setPagedCandidateCallback(const PagedCandidateCallback &callback);

private:
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, keyEvent);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, selectCandidate);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, isInputPanelEmpty);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, resetInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, repositionCursor);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, focusInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, activateInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, activeInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, deactivateInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCapabilityFlags);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, getCandidates);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, getCandidateActions);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, triggerCandidateAction);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, showToast);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCandidatePagingMode);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, offsetCandidatePage);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCandidateListCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCommitStringCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setPreeditCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setInputPanelAuxCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setKeyEventCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setInputMethodChangeCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setStatusAreaUpdateCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setDeleteSurroundingCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setToastCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setPagedCandidateCallback);

    Instance *instance_;
    FocusGroup focusGroup_;
    AndroidInputContext *activeIC_;
    InputContextCache icCache_;
    std::vector<std::unique_ptr<HandlerTableEntry<EventHandler>>> eventHandlers_;
    int pagingMode_;

    CandidateListCallback candidateListCallback = [](const std::vector<std::string> &, const int) {};
    CommitStringCallback commitStringCallback = [](const std::string &, const int) {};
    ClientPreeditCallback preeditCallback = [](const Text &) {};
    InputPanelCallback inputPanelCallback = [](const fcitx::Text &, const fcitx::Text &, const Text &) {};
    KeyEventCallback keyEventCallback = [](const int, const uint32_t, const uint32_t, const bool, const int) {};
    InputMethodChangeCallback imChangeCallback = [] {};
    StatusAreaUpdateCallback statusAreaUpdateCallback = [] {};
    DeleteSurroundingCallback deleteSurroundingCallback = [](const int, const int) {};
    ToastCallback toastCallback = [](const std::string &) {};
    PagedCandidateCallback pagedCandidateCallback = [](const PagedCandidateEntity &) {};
};
} // namespace fcitx

#endif //FCITX5_ANDROID_ANDROIDFRONTEND_H
