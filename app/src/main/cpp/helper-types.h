#ifndef FCITX5_ANDROID_HELPER_TYPES_H
#define FCITX5_ANDROID_HELPER_TYPES_H

#include <fcitx/action.h>
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

    ActionEntity(fcitx::Action *act, fcitx::InputContext *ic) :
            id(act->id()), isSeparator(act->isSeparator()), isCheckable(act->isCheckable()),
            isChecked(act->isChecked(ic)),
            name(act->name()), icon(act->icon(ic)), shortText(act->shortText(ic)),
            longText(act->longText(ic)) {}
};

#endif //FCITX5_ANDROID_HELPER_TYPES_H
