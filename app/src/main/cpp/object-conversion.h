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
                          *JString(env, entry->addon()),
                          entry->isConfigurable()
    );
}

jobjectArray fcitxInputMethodEntriesToJObjectArray(JNIEnv *env, const std::vector<const fcitx::InputMethodEntry *> &entries) {
    int size = static_cast<int>(entries.size());
    jobjectArray array = env->NewObjectArray(size, GlobalRef->InputMethodEntry, nullptr);
    for (int i = 0; i < size; i++) {
        auto obj = JRef(env, fcitxInputMethodEntryToJObject(env, entries[i]));
        env->SetObjectArrayElement(array, i, obj);
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
                          *JString(env, status.addon),
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
    int size = static_cast<int>(cfg.subItemsSize());
    auto subItems = cfg.subItems();
    auto array = JRef<jobjectArray>(env, env->NewObjectArray(size, GlobalRef->RawConfig, nullptr));
    for (int i = 0; i < size; i++) {
        auto item = JRef(env, fcitxRawConfigToJObject(env, *cfg.get(subItems[i])));
        env->SetObjectArrayElement(array, i, item);
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
    int size = static_cast<int>(strings.size());
    jobjectArray array = env->NewObjectArray(size, GlobalRef->String, nullptr);
    for (int i = 0; i < size; i++) {
        env->SetObjectArrayElement(array, i, JString(env, strings[i]));
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
            auto menuItem = JRef(env, fcitxActionToJObject(env, act.menu->at(i)));
            env->SetObjectArrayElement(menu, i, menuItem);
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
    auto fmtArray = env->GetIntArrayElements(fmt, nullptr);
    for (int i = 0; i < size; i++) {
        env->SetObjectArrayElement(str, i, JString(env, text.stringAt(i)));
        fmtArray[i] = text.formatAt(i).toInteger();
    }
    env->ReleaseIntArrayElements(fmt, fmtArray, 0);
    auto obj = env->CallStaticObjectMethod(GlobalRef->FormattedText, GlobalRef->FormattedTextFromByteCursor,
                                           *str,
                                           *fmt,
                                           text.cursor()
    );
    return obj;
}

jobject fcitxCandidateActionToObject(JNIEnv *env, const CandidateActionEntity &act) {
    auto obj = env->NewObject(GlobalRef->CandidateAction, GlobalRef->CandidateActionInit,
                              act.id,
                              *JString(env, act.text),
                              act.isSeparator,
                              *JString(env, act.icon),
                              act.isCheckable,
                              act.isChecked
    );
    return obj;
}

jobject candidateEntityToObject(JNIEnv *env, const CandidateEntity &c) {
    auto obj = env->NewObject(GlobalRef->Candidate, GlobalRef->CandidateInit,
                              *JString(env, c.label),
                              *JString(env, c.text),
                              *JString(env, c.comment),
                              c.spaceBetweenComment
    );
    return obj;
}

#endif //FCITX5_ANDROID_OBJECT_CONVERSION_H
