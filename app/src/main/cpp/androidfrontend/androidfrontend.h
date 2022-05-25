#ifndef _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
#define _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_

#include <fcitx-utils/i18n.h>

#include "androidfrontend_public.h"

namespace fcitx {

class AndroidFrontend : public AddonInstance {
public:
    AndroidFrontend(Instance *instance);
    ~AndroidFrontend();

    Instance *instance() { return instance_; }

    void updateCandidateList(const std::vector<std::string> &candidates);
    void commitString(const std::string &str);
    void updatePreedit(const Text &preedit, const Text &clientPreedit);
    void updateInputPanelAux(const std::string &auxUp, const std::string &auxDown);

    ICUUID createInputContext(const std::string &program);
    void destroyInputContext(ICUUID uuid);
    void keyEvent(ICUUID uuid, const Key &key, bool isRelease);
    void forwardKey(const Key &key, bool isRelease);
    void selectCandidate(ICUUID uuid, int idx);
    bool isInputPanelEmpty(ICUUID uuid);
    void resetInputContext(ICUUID uuid);
    void repositionCursor(ICUUID uuid, int idx);
    void focusInputContext(ICUUID uuid, bool focus);
    void setCapabilityFlags(ICUUID uuid, uint64_t flag);
    void setCandidateListCallback(const CandidateListCallback &callback);
    void setCommitStringCallback(const CommitStringCallback &callback);
    void setPreeditCallback(const PreeditCallback &callback);
    void setInputPanelAuxCallback(const InputPanelAuxCallback &callback);
    void setKeyEventCallback(const KeyEventCallback &callback);
    void setInputMethodChangeCallback(const InputMethodChangeCallback &callback);
    void setStatusAreaUpdateCallback(const StatusAreaUpdateCallback &callback);

private:
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, createInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, destroyInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, keyEvent);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, selectCandidate);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, isInputPanelEmpty);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, resetInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, repositionCursor);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, focusInputContext);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCapabilityFlags);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCandidateListCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCommitStringCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setPreeditCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setInputPanelAuxCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setKeyEventCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setInputMethodChangeCallback);
    FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setStatusAreaUpdateCallback);

    Instance *instance_;
    std::vector<std::unique_ptr<fcitx::HandlerTableEntry<fcitx::EventHandler>>> eventHandlers_;

    CandidateListCallback candidateListCallback = [](const std::vector<std::string> &) {};
    CommitStringCallback commitStringCallback = [](const std::string &) {};
    PreeditCallback preeditCallback = [](const std::string &, const int, const std::string &, const int) {};
    InputPanelAuxCallback inputPanelAuxCallback = [](const std::string &, const std::string &) {};
    KeyEventCallback keyEventCallback = [](const uint32_t, const uint32_t, const uint32_t, const bool) {};
    InputMethodChangeCallback imChangeCallback = [] {};
    StatusAreaUpdateCallback statusAreaUpdateCallback = [] {};
};
} // namespace fcitx

#endif //_FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
