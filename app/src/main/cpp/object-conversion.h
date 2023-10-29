/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_OBJECT_CONVERSION_H
#define FCITX5_ANDROID_OBJECT_CONVERSION_H

#include <jni.h>

#include <fcitx/action.h>

#include "jni-utils.h"
#include "helper-types.h"

jobject fcitxInputMethodEntryToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry) {
    return env->NewObject(GlobalRef->InputMethodEntry, GlobalRef->InputMethodEntryInit,
                          *JString(env, entry->uniqueName()),
                          *JString(env, entry->name()),
                          *JString(env, entry->icon()),
                          *JString(env, entry->nativeName()),
                          *JString(env, entry->label()),
                          *JString(env, entry->languageCode()),
                          entry->isConfigurable()
    );
}

jobjectArray fcitxInputMethodEntriesToJObjectArray(JNIEnv *env, const std::vector<const fcitx::InputMethodEntry *> &entries) {
    jobjectArray array = env->NewObjectArray(static_cast<int>(entries.size()), GlobalRef->InputMethodEntry, nullptr);
    int i = 0;
    for (const auto &entry: entries) {
        auto obj = JRef(env, fcitxInputMethodEntryToJObject(env, entry));
        env->SetObjectArrayElement(array, i++, obj);
    }
    return array;
}

jobject fcitxInputMethodStatusToJObject(JNIEnv *env, const InputMethodStatus &status) {
    return env->NewObject(GlobalRef->InputMethodEntry, GlobalRef->InputMethodEntryInitWithSubMode,
                          *JString(env, status.uniqueName),
                          *JString(env, status.name),
                          *JString(env, status.icon),
                          *JString(env, status.nativeName),
                          *JString(env, status.label),
                          *JString(env, status.languageCode),
                          status.configurable,
                          *JString(env, status.subMode),
                          *JString(env, status.subModeLabel),
                          *JString(env, status.subModeIcon)
    );
}

jobject fcitxRawConfigToJObject(JNIEnv *env, const fcitx::RawConfig &cfg) {
    jobject obj = env->NewObject(GlobalRef->RawConfig, GlobalRef->RawConfigInit,
                                 *JString(env, cfg.name()),
                                 *JString(env, cfg.comment()),
                                 *JString(env, cfg.value()),
                                 nullptr);
    if (!cfg.hasSubItems()) {
        return obj;
    }
    auto array = JRef<jobjectArray>(env, env->NewObjectArray(static_cast<int>(cfg.subItemsSize()), GlobalRef->RawConfig, nullptr));
    int i = 0;
    for (const auto &item: cfg.subItems()) {
        auto jItem = JRef(env, fcitxRawConfigToJObject(env, *cfg.get(item)));
        env->SetObjectArrayElement(array, i++, jItem);
    }
    env->CallVoidMethod(obj, GlobalRef->RawConfigSetSubItems, *array);
    return obj;
}

void jobjectFillRawConfig(JNIEnv *env, jobject jConfig, fcitx::RawConfig &config) {
    auto subItems = JRef<jobjectArray>(env, env->GetObjectField(jConfig, GlobalRef->RawConfigSubItems));
    if (*subItems == nullptr) {
        auto value = JRef<jstring>(env, env->GetObjectField(jConfig, GlobalRef->RawConfigValue));
        config = CString(env, value);
    } else {
        int size = env->GetArrayLength(subItems);
        for (int i = 0; i < size; i++) {
            auto item = JRef(env, env->GetObjectArrayElement(subItems, i));
            auto name = JRef<jstring>(env, env->GetObjectField(item, GlobalRef->RawConfigName));
            auto subConfig = config.get(CString(env, name), true);
            jobjectFillRawConfig(env, item, *subConfig);
        }
    }
}

fcitx::RawConfig jobjectToRawConfig(JNIEnv *env, jobject jConfig) {
    fcitx::RawConfig config;
    jobjectFillRawConfig(env, jConfig, config);
    return config;
}

jobjectArray stringVectorToJStringArray(JNIEnv *env, const std::vector<std::string> &strings) {
    jobjectArray array = env->NewObjectArray(static_cast<int>(strings.size()), GlobalRef->String, nullptr);
    int i = 0;
    for (const auto &s: strings) {
        env->SetObjectArrayElement(array, i++, JString(env, s));
    }
    return array;
}

jobject fcitxAddonStatusToJObject(JNIEnv *env, const AddonStatus &status) {
    const auto info = status.info;
    return env->NewObject(GlobalRef->AddonInfo, GlobalRef->AddonInfoInit,
                          *JString(env, info->uniqueName()),
                          *JString(env, info->name().match()),
                          *JString(env, info->comment().match()),
                          static_cast<int32_t>(info->category()),
                          info->isConfigurable(),
                          status.enabled,
                          info->isDefaultEnabled(),
                          info->onDemand(),
                          stringVectorToJStringArray(env, info->dependencies()),
                          stringVectorToJStringArray(env, info->optionalDependencies())
    );
}

jobject fcitxActionToJObject(JNIEnv *env, const ActionEntity &act) {
    jobjectArray menu = nullptr;
    if (act.menu) {
        const int size = static_cast<int>(act.menu->size());
        menu = env->NewObjectArray(size, GlobalRef->Action, nullptr);
        for (int i = 0; i < size; i++) {
            env->SetObjectArrayElement(menu, i, fcitxActionToJObject(env, act.menu->at(i)));
        }
    }
    auto obj = env->NewObject(GlobalRef->Action, GlobalRef->ActionInit,
                              act.id,
                              act.isSeparator,
                              act.isCheckable,
                              act.isChecked,
                              *JString(env, act.name),
                              *JString(env, act.icon),
                              *JString(env, act.shortText),
                              *JString(env, act.longText),
                              menu
    );
    if (menu) {
        env->DeleteLocalRef(menu);
    }
    return obj;
}

jobject fcitxTextToJObject(JNIEnv *env, const fcitx::Text &text) {
    const int size = static_cast<int>(text.size());
    auto str = JRef<jobjectArray>(env, env->NewObjectArray(size, GlobalRef->String, nullptr));
    auto fmt = JRef<jintArray>(env, env->NewIntArray(size));
    int flag = static_cast<int>(fcitx::TextFormatFlag::NoFlag);
    for (int i = 0; i < size; i++) {
        env->SetObjectArrayElement(str, i, *JString(env, text.stringAt(i)));
        flag = text.formatAt(i).toInteger();
        env->SetIntArrayRegion(fmt, i, 1, &flag);
    }
    auto obj = env->CallStaticObjectMethod(GlobalRef->FormattedText, GlobalRef->FormattedTextFromByteCursor,
                                           *str,
                                           *fmt,
                                           text.cursor()
    );
    return obj;
}

#endif //FCITX5_ANDROID_OBJECT_CONVERSION_H
