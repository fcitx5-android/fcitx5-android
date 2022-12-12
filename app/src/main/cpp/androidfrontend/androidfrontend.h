#ifndef _FCITX5_ANDROID_ANDROIDFRONTEND_H_
#define _FCITX5_ANDROID_ANDROIDFRONTEND_H_

#include <fcitx-utils/i18n.h>

#include "androidfrontend_public.h"
#include "inputcontextcache.h"

namespace fcitx {

class AndroidFrontend : public AddonInstance {
public:
    AndroidFrontend(Instance *instance);

    Instance *instance() { return instance_; }

    void updateCandidateList(const std::vector<std::string> &candidates);
    void commitString(const std::string &str);
    void updatePreedit(const Text &preedit, const Text &clientPreedit);
    void updateInputPanelAux(const std::string &auxUp, const std::string &auxDown);
    void releaseInputContext(const int uid);

    void keyEvent(const Key &key, bool isRelease, const int timestamp);
    void forwardKey(const Key &key, bool isRelease);
    bool selectCandidate(int idx);
    bool isInputPanelEmpty();
    void resetInputContext();
    void repositionCursor(int idx);
    void focusInputContext(bool focus);
    void activateInputContext(const int uid);
    void deactivateInputContext(const int uid);
    InputContext *activeInputContext() const;
    void setCapabilityFlags(uint64_t flag);
    void setCandidateListCallback(const CandidateListCallback &callback);
    void setCommitStringCallback(const CommitStringCallback &callback);
    void setPreeditCallback(const PreeditCallback &callback);
    void setInputPanelAuxCallback(const InputPanelAuxCallback &callback);
    void setKeyEventCallback(const KeyEventCallback &callback);
    void setInputMethodChangeCallback(const InputMethodChangeCallback &callback);
    void setStatusAreaUpdateCallback(const StatusAreaUpdateCallback &callback);

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
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCandidateListCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCommitStringCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setPreeditCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setInputPanelAuxCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setKeyEventCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setInputMethodChangeCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setStatusAreaUpdateCallback);

    Instance *instance_;
    FocusGroup focusGroup_;
    InputContext *activeIC_;
    InputContextCache icCache_;
    std::vector<std::unique_ptr<HandlerTableEntry<EventHandler>>> eventHandlers_;
    std::unique_ptr<EventSource> statusAreaDefer_;
    bool statusAreaUpdated_;

    void handleStatusAreaUpdate();

    CandidateListCallback candidateListCallback = [](const std::vector<std::string> &) {};
    CommitStringCallback commitStringCallback = [](const std::string &) {};
    PreeditCallback preeditCallback = [](const std::string &, const int, const std::string &, const int) {};
    InputPanelAuxCallback inputPanelAuxCallback = [](const std::string &, const std::string &) {};
    KeyEventCallback keyEventCallback = [](const int, const uint32_t, const uint32_t, const bool, const int) {};
    InputMethodChangeCallback imChangeCallback = [] {};
    StatusAreaUpdateCallback statusAreaUpdateCallback = [] {};
};
} // namespace fcitx

#endif //_FCITX5_ANDROID_ANDROIDFRONTEND_H_
