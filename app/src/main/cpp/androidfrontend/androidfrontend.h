/*
 * SPDX-FileCopyrightText: 2021-2021 Rocket Aaron <i@rocka.me>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 */

#ifndef _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
#define _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_

#include <list>
#include <string>
#include "fcitx/addoninstance.h"
#include "fcitx/instance.h"
#include "androidfrontend_public.h"

namespace fcitx {

    class AndroidFrontend : public AddonInstance {
    public:
        AndroidFrontend(Instance *instance);
        ~AndroidFrontend();

        Instance *instance() { return instance_; }

        void updateCandidateList(const std::shared_ptr<CandidateList>& candidateList);
        void commitString(const std::string &expect);

    private:
        std::shared_ptr<CandidateList> cachedCandidateList;
        std::shared_ptr<BulkCandidateList> cachedBulkCandidateList;
        ICUUID createInputContext(const std::string &program);
        void destroyInputContext(ICUUID uuid);
        void keyEvent(ICUUID uuid, const Key &key, bool isRelease);
        std::shared_ptr<BulkCandidateList> candidateList(ICUUID uuid);
        void selectCandidate(ICUUID uuid, int idx);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, createInputContext);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, destroyInputContext);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, keyEvent);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, candidateList);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, selectCandidate);

        Instance *instance_;
    };
} // namespace fcitx

#endif //_FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
