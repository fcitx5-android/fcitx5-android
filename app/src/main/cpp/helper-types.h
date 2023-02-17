#ifndef FCITX5_ANDROID_HELPER_TYPES_H
#define FCITX5_ANDROID_HELPER_TYPES_H

#include <fcitx/action.h>
#include <fcitx/menu.h>
#include <fcitx/inputcontext.h>

class InputMethodStatus {
public:
    const fcitx::InputMethodEntry *entry;
    std::string subMode;
    std::string subModeLabel;
    std::string subModeIcon;

    InputMethodStatus(const fcitx::InputMethodEntry *entry,
                      fcitx::InputMethodEngine *engine,
                      fcitx::InputContext *ic)
            : entry(entry) {
        if (engine) {
            subMode = engine->subMode(*entry, *ic);
            subModeLabel = engine->subModeLabel(*entry, *ic);
            subModeIcon = engine->subModeIcon(*entry, *ic);
        }
    }

    InputMethodStatus(const fcitx::InputMethodEntry *entry)
            : entry(entry) {}
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

#endif //FCITX5_ANDROID_HELPER_TYPES_H
