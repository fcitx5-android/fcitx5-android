#include <jni.h>
#include <fstream>

#include "jni-utils.h"

#include "libime/table/tablebaseddictionary.h"

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_data_table_TableDictManager_tableDictConv(JNIEnv *env, jclass clazz, jstring src, jstring dest, jboolean mode) {
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