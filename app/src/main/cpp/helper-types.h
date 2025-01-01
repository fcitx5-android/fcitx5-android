/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_HELPER_TYPES_H
#define FCITX5_ANDROID_HELPER_TYPES_H

#include <fcitx/action.h>
#include <fcitx/menu.h>
#include <fcitx/inputcontext.h>
#include <fcitx/candidateaction.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/inputmethodentry.h>
#include <fcitx/candidatelist.h>

#include <utility>

class InputMethodStatus {
public:
    // fcitx::InputMethodEntry
    std::string uniqueName;
    std::string name;
    std::string nativeName;
    std::string icon;
    std::string label;
    std::string languageCode;
    std::string addon;
    bool configurable = false;
    // fcitx::InputMethodEngine
    std::string subMode;
    std::string subModeLabel;
    std::string subModeIcon;

    InputMethodStatus(const fcitx::InputMethodEntry *entry,
                      fcitx::InputMethodEngine *engine,
                      fcitx::InputContext *ic) {
        uniqueName = entry->uniqueName();
        name = entry->name();
        nativeName = entry->nativeName();
        icon = entry->icon();
        label = entry->label();
        languageCode = entry->languageCode();
        addon = entry->addon();
        configurable = entry->isConfigurable();
        subMode = engine->subMode(*entry, *ic);
        subModeLabel = engine->subModeLabel(*entry, *ic);
        subModeIcon = engine->subModeIcon(*entry, *ic);
    }
};

class AddonStatus {
public:
    const fcitx::AddonInfo *info;
    bool enabled;

    AddonStatus(const fcitx::AddonInfo *info, bool enabled) :
            info(info),
            enabled(enabled) {}
};

class ActionEntity {
public:
    int id;
    bool isSeparator;
    bool isCheckable;
    bool isChecked;
    std::string name;
    std::string icon;
    std::string shortText;
    std::string longText;
    std::optional<std::vector<ActionEntity>> menu;

    ActionEntity(fcitx::Action *act, fcitx::InputContext *ic) :
            id(act->id()),
            isSeparator(act->isSeparator()),
            isCheckable(act->isCheckable()),
            isChecked(act->isChecked(ic)),
            name(act->name()),
            icon(act->icon(ic)),
            shortText(act->shortText(ic)),
            longText(act->longText(ic)) {
        const auto m = act->menu();
        if (m) {
            menu = std::vector<ActionEntity>();
            for (auto a: m->actions()) {
                menu->emplace_back(ActionEntity(a, ic));
            }
        }
    }
};

class CandidateActionEntity {
public:
    int id;
    std::string text;
    bool isSeparator;
    std::string icon;
    bool isCheckable;
    bool isChecked;

    explicit CandidateActionEntity(const fcitx::CandidateAction &act) :
            id(act.id()),
            text(act.text()),
            isSeparator(act.isSeparator()),
            icon(act.icon()),
            isCheckable(act.isCheckable()),
            isChecked(act.isChecked()) {}
};

class CandidateEntity {
public:
    std::string label;
    std::string text;
    std::string comment;

    explicit CandidateEntity(std::string label, std::string text, std::string comment) :
            label(std::move(label)),
            text(std::move(text)),
            comment(std::move(comment)) {}
};

class PagedCandidateEntity {
public:
    std::vector<CandidateEntity> candidates;
    int cursorIndex;
    fcitx::CandidateLayoutHint layoutHint;
    bool hasPrev;
    bool hasNext;

    explicit PagedCandidateEntity(std::vector<CandidateEntity> candidates,
                                  int cursorIndex,
                                  fcitx::CandidateLayoutHint layoutHint,
                                  bool hasPrev,
                                  bool hasNext) :
            candidates(std::move(candidates)),
            cursorIndex(cursorIndex),
            layoutHint(layoutHint),
            hasPrev(hasPrev),
            hasNext(hasNext) {}

    static PagedCandidateEntity Empty;

private:
    PagedCandidateEntity() :
            candidates({}), cursorIndex(-1), layoutHint(fcitx::CandidateLayoutHint::NotSet),
            hasPrev(false), hasNext(false) {}
};

PagedCandidateEntity PagedCandidateEntity::Empty = PagedCandidateEntity();

#endif //FCITX5_ANDROID_HELPER_TYPES_H
