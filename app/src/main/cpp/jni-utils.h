#ifndef FCITX5TEST_JNI_UTILS_H
#define FCITX5TEST_JNI_UTILS_H

#include <jni.h>

#include <string>

class JString {
private:
    JNIEnv *env_;
    jstring jstring_;

public:
    JString(JNIEnv *env, const std::string &string)
            : env_(env), jstring_(env->NewStringUTF(string.c_str())) {}

    JString(JNIEnv *env, const char *chars)
            : env_(env), jstring_(env->NewStringUTF(chars)) {}

    ~JString() {
        env_->DeleteLocalRef(jstring_);
    }

    operator jstring() { return jstring_; }

    jstring operator*() { return jstring_; }
};

#endif //FCITX5TEST_JNI_UTILS_H
