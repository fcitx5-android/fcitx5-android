#include <jni.h>
#include <fstream>

#include "jni-utils.h"

#include "libime/pinyin/pinyindictionary.h"

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_data_pinyin_PinyinDictManager_pinyinDictConv(JNIEnv *env, jclass clazz, jstring src, jstring dest, jboolean mode) {
    using namespace libime;
    PinyinDictionary dict;
    try {
        dict.load(PinyinDictionary::SystemDict, *CString(env, src),
                  mode == JNI_TRUE ? PinyinDictFormat::Binary : PinyinDictFormat::Text);
        std::ofstream out;
        out.open(*CString(env, dest), std::ios::out | std::ios::binary);
        dict.save(PinyinDictionary::SystemDict, out,
                  mode == JNI_TRUE ? PinyinDictFormat::Text : PinyinDictFormat::Binary);
    } catch (const std::exception &e) {
        throwJavaException(env, e.what());
    }
}