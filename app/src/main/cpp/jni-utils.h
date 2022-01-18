#ifndef FCITX5TEST_JNI_UTILS_H
#define FCITX5TEST_JNI_UTILS_H

#include <jni.h>

#include <string>

class JString {
private:
    JNIEnv *env_;
    jstring jstring_;

public:
    JString(JNIEnv *env, const char *chars)
            : env_(env), jstring_(env->NewStringUTF(chars)) {}

    JString(JNIEnv *env, const std::string &string)
            : JString(env, string.c_str()) {}

    ~JString() {
        env_->DeleteLocalRef(jstring_);
    }

    operator jstring() { return jstring_; }

    jstring operator*() { return jstring_; }
};

class JClass {
private:
    JNIEnv *env_;
    jclass jclass_;

public:
    JClass(JNIEnv *env, const char *name)
            : env_(env), jclass_(env->FindClass(name)) {}

    ~JClass() {
        env_->DeleteLocalRef(jclass_);
    }

    operator jclass() { return jclass_; }

    jclass operator*() { return jclass_; }
};

class GlobalRefSingleton {
public:
    JavaVM *jvm;

    jclass Object;

    jclass String;

    jclass Integer;
    jmethodID IntegerInit;

    jclass Fcitx;
    jmethodID HandleFcitxEvent;

    jclass InputMethodEntry;
    jmethodID InputMethodEntryInit;

    jclass RawConfig;
    jfieldID RawConfigName;
    jfieldID RawConfigValue;
    jfieldID RawConfigSubItems;
    jmethodID RawConfigInit;
    jmethodID RawConfigSetSubItems;

    jclass AddonInfo;
    jmethodID AddonInfoInit;

    GlobalRefSingleton(JavaVM *jvm_) : jvm(jvm_) {
        JNIEnv *env;
        jvm->AttachCurrentThread(&env, nullptr);

        Object = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Object")));

        String = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/String")));

        Integer = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("java/lang/Integer")));
        IntegerInit = env->GetMethodID(Integer, "<init>", "(I)V");

        Fcitx = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("me/rocka/fcitx5test/native/Fcitx")));
        HandleFcitxEvent = env->GetStaticMethodID(Fcitx, "handleFcitxEvent", "(I[Ljava/lang/Object;)V");

        InputMethodEntry = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("me/rocka/fcitx5test/native/InputMethodEntry")));
        InputMethodEntryInit = env->GetMethodID(InputMethodEntry, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V");

        RawConfig = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("me/rocka/fcitx5test/native/RawConfig")));
        RawConfigName = env->GetFieldID(RawConfig, "name", "Ljava/lang/String;");
        RawConfigValue = env->GetFieldID(RawConfig, "value", "Ljava/lang/String;");
        RawConfigSubItems = env->GetFieldID(RawConfig, "subItems", "[Lme/rocka/fcitx5test/native/RawConfig;");
        RawConfigInit = env->GetMethodID(RawConfig, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lme/rocka/fcitx5test/native/RawConfig;)V");
        RawConfigSetSubItems = env->GetMethodID(RawConfig, "setSubItems", "([Lme/rocka/fcitx5test/native/RawConfig;)V");

        AddonInfo = reinterpret_cast<jclass>(env->NewGlobalRef(env->FindClass("me/rocka/fcitx5test/native/AddonInfo")));
        AddonInfoInit = env->GetMethodID(AddonInfo, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZZZ)V");
    }

    JNIEnv *AttachEnv() {
        JNIEnv *env;
        jvm->AttachCurrentThread(&env, nullptr);
        return env;
    }
};

#endif //FCITX5TEST_JNI_UTILS_H
