#include <jni.h>
#include <fstream>

#include "libime/pinyin/pinyindictionary.h"

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM * /* jvm */, void * /* reserved */) {
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}


extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_data_PinyinDictManager_pinyinDictConv(JNIEnv *env, jclass clazz, jstring src, jstring dest, jboolean mode) {
    using namespace libime;
    const char *src_file = env->GetStringUTFChars(src, nullptr);
    const char *dest_file = env->GetStringUTFChars(dest, nullptr);
    PinyinDictionary dict;
    dict.load(PinyinDictionary::SystemDict, src_file,
              mode == JNI_TRUE ? PinyinDictFormat::Binary : PinyinDictFormat::Text);

    std::ofstream out;
    out.open(dest_file, std::ios::out | std::ios::binary);
    dict.save(PinyinDictionary::SystemDict, out,
              mode == JNI_TRUE ? PinyinDictFormat::Text : PinyinDictFormat::Binary);
}