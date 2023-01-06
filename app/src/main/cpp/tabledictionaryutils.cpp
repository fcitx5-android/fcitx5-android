#include <jni.h>
#include <fstream>

#include "jni-utils.h"

#include "libime/table/tablebaseddictionary.h"

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_data_table_TableManager_tableDictConv(JNIEnv *env, jclass clazz, jstring src, jstring dest, jboolean mode) {
    using namespace libime;
    TableBasedDictionary dict;
    try {
        dict.load(*CString(env, src), mode == JNI_TRUE ? TableFormat::Binary : TableFormat::Text);
        std::ofstream out;
        out.open(*CString(env, dest), std::ios::out | std::ios::binary);
        dict.save(out, mode == JNI_TRUE ? TableFormat::Text : TableFormat::Binary);
    } catch (const std::exception &e) {
        throwJavaException(env, e.what());
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fcitx_fcitx5_android_data_table_TableManager_checkTableDictFormat(JNIEnv *env, jclass clazz, jstring src, jboolean user) {
    using namespace libime;
    TableBasedDictionary dict;
    try {
        if (user == JNI_TRUE) {
            dict.loadUser(CString(env, src), TableFormat::Binary);
        } else {
            dict.load(*CString(env, src), TableFormat::Binary);
        }
    } catch (const std::exception &e) {
        throwJavaException(env, e.what());
    }
    return JNI_TRUE;
}
