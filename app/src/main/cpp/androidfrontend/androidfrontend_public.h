/*
 * SPDX-FileCopyrightText: 2020-2020 CSSlayer <wengxt@gmail.com>
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 */
#ifndef _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_PUBLIC_H_
#define _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_PUBLIC_H_

#include <string>
#include <fcitx/candidatelist.h>
#include <fcitx/addoninstance.h>
#include <fcitx/inputcontext.h>

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, createInputContext,
                             ICUUID(const std::string &));

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, destroyInputContext, void(ICUUID));

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, keyEvent,
                             void(ICUUID, const Key &, bool isRelease));

FCITX_ADDON_DECLARE_FUNCTION(AndroidFrontend, candidateList,
                             std::shared_ptr<CandidateList>(ICUUID));

#endif // _FCITX_FRONTEND_ANDROIDFRONTEND_ANDROIDFRONTEND_PUBLIC_H_
