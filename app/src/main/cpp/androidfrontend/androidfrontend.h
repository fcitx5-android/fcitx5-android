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

        void commitString(const std::string &expect);

    private:
        ICUUID createInputContext(const std::string &program);
        void destroyInputContext(ICUUID uuid);
        void keyEvent(ICUUID uuid, const Key &key, bool isRelease);
        std::shared_ptr<BulkCandidateList> candidateList(ICUUID uuid);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, createInputContext);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, destroyInputContext);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, keyEvent);
        FCITX_ADDON_EXPORT_FUNCTION(AndroidFrontend, candidateList);

        Instance *instance_;
    };
} // namespace fcitx

#endif //_FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_H_
