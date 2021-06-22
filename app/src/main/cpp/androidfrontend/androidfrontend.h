#ifndef _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
#define _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_

#include <list>
#include <string>
#include <fcitx/instance.h>
#include <fcitx/addoninstance.h>

#include "androidfrontend_public.h"

namespace fcitx {

    class AndroidFrontend : public AddonInstance {
    public:
        AndroidFrontend(Instance *instance);
        ~AndroidFrontend();

        Instance *instance() { return instance_; }

        void updateCandidateList(const std::vector<std::string> &candidates);
        void commitString(const std::string &str);

    private:
        CandidateListCallback candidateListCallback;
        CommitStringCallback commitStringCallback;
        ICUUID createInputContext(const std::string &program);
        void destroyInputContext(ICUUID uuid);
        void keyEvent(ICUUID uuid, const Key &key, bool isRelease);
        void selectCandidate(ICUUID uuid, int idx);
        void setCandidateListCallback(const CandidateListCallback& callback);
        void setCommitStringCallback(const CommitStringCallback& callback);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, createInputContext);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, destroyInputContext);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, keyEvent);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, selectCandidate);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCandidateListCallback);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, setCommitStringCallback);

        Instance *instance_;
    };
} // namespace fcitx

#endif //_FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
