#ifndef _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_PUBLIC_H_
#define _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_PUBLIC_H_

#include <fcitx/inputcontext.h>

typedef std::function<void(const std::vector<std::string> &)> CandidateListCallback;
typedef std::function<void(const std::string &)> CommitStringCallback;
typedef std::function<void(const std::string &, const int, const std::string &, const int)> PreeditCallback;
typedef std::function<void(const std::string &, const std::string &)> InputPanelAuxCallback;
typedef std::function<void(const uint32_t, const uint32_t, const uint32_t, const bool, const int64_t)> KeyEventCallback;
typedef std::function<void()> InputMethodChangeCallback;
typedef std::function<void()> StatusAreaUpdateCallback;

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, createInputContext,
                             ICUUID(const std::string &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, destroyInputContext, void(ICUUID))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, keyEvent,
                             void(ICUUID, const Key &, bool isRelease, const int64_t timestamp))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, selectCandidate,
                             void(ICUUID, int idx))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, isInputPanelEmpty,
                             bool(ICUUID))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, resetInputContext,
                             void(ICUUID))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, repositionCursor,
                             void(ICUUID, int))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, focusInputContext,
                             void(ICUUID, bool))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCapabilityFlags,
                             void(ICUUID, uint64_t))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCandidateListCallback,
                             void(const CandidateListCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCommitStringCallback,
                             void(const CommitStringCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setPreeditCallback,
                             void(const PreeditCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setInputPanelAuxCallback,
                             void(const InputPanelAuxCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setKeyEventCallback,
                             void(const KeyEventCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setInputMethodChangeCallback,
                             void(const InputMethodChangeCallback &))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setStatusAreaUpdateCallback,
                             void(const StatusAreaUpdateCallback &))

#endif // _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_PUBLIC_H_
