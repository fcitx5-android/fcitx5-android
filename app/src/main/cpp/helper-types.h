#ifndef FCITX5_ANDROID_HELPER_TYPES_H
#define FCITX5_ANDROID_HELPER_TYPES_H

#include <fcitx/action.h>
#include <fcitx/menu.h>
#include <fcitx/inputcontext.h>

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
