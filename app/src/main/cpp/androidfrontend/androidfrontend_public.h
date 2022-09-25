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

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, keyEvent,
                             void(const Key &, bool isRelease, const int64_t timestamp))

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
                             void(const int))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, activeInputContext,
                             InputContext * ())

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, deactivateInputContext,
                             void(const int))

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, setCapabilityFlags,
                             void(uint64_t))

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
