#include <jni.h>
#include <memory>
#include <unistd.h>
#include <android/log.h>

#include <fcitx/instance.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodmanager.h>
#include <fcitx-utils/eventdispatcher.h>

#include "androidfrontend/androidfrontend_public.h"

// https://codelab.wordpress.com/2014/11/03/how-to-use-standard-output-streams-for-logging-in-android-apps/
static int pfd[2];
static pthread_t thr;
static const char *tag = "fcitx5";

static void *logger_thread(void *) {
    ssize_t read_size;
    char buf[128];
    while ((read_size = read(pfd[0], buf, sizeof buf - 1)) > 0) {
        if (buf[read_size - 1] == '\n') --read_size;
        /* add null-terminator */
        buf[read_size] = '\0';
        __android_log_write(ANDROID_LOG_DEBUG, tag, buf);
    }
    return nullptr;
}

void start_logger() {
    /* make stdout line-buffered and stderr unbuffered */
    setvbuf(stdout, nullptr, _IOLBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);
    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);
    /* spawn the logging thread */
    pthread_create(&thr, nullptr, logger_thread, nullptr);
    pthread_detach(thr);
}

static void frontendLog(const std::string& s) {
    __android_log_write(ANDROID_LOG_DEBUG, "androidfrontend", s.c_str());
}

std::unique_ptr<fcitx::Instance> p_instance;
std::unique_ptr<fcitx::EventDispatcher> p_dispatcher;
std::unique_ptr<fcitx::AddonInstance> p_frontend;
fcitx::ICUUID p_uuid;

extern "C"
JNIEXPORT jint JNICALL
Java_me_rocka_fcitx5test_native_JNI_startupFcitx(JNIEnv *env, jobject obj, jstring appData, jstring appLib, jstring extData, jstring appDataLibime) {
    // debug log
    start_logger();

    setenv("SKIP_FCITX_PATH", "true", 1);

    const char* app_data = env->GetStringUTFChars(appData, nullptr);
    const char* app_lib = env->GetStringUTFChars(appLib, nullptr);
    const char* ext_data = env->GetStringUTFChars(extData, nullptr);
    const char* app_data_libime = env->GetStringUTFChars(appDataLibime, nullptr);

    setenv("HOME", ext_data, 1);
    setenv("XDG_DATA_DIRS", app_data, 1);
    setenv("XDG_CONFIG_HOME", ext_data, 1);
    setenv("XDG_DATA_HOME", ext_data, 1);
    setenv("FCITX_ADDON_DIRS", app_lib, 1);
    setenv("LIBIME_MODEL_DIRS", app_data_libime, 1);
    setenv("LIBIME_INSTALL_PKGDATADIR", app_data_libime, 1);

    env->ReleaseStringUTFChars(appData, app_data);
    env->ReleaseStringUTFChars(appLib, app_lib);
    env->ReleaseStringUTFChars(extData, ext_data);
    env->ReleaseStringUTFChars(appDataLibime, app_data_libime);

    jclass hostClass = env->GetObjectClass(obj);
    jclass stringClass = env->FindClass("java/lang/String");
    jmethodID handleFcitxEvent = env->GetMethodID(hostClass, "handleFcitxEvent", "(I[Ljava/lang/Object;)V");
    auto candidateListCallback = [&](const std::shared_ptr<fcitx::BulkCandidateList>& candidateList){
        frontendLog("candidateListCallback");
        if (!candidateList) {
            jobjectArray vararg = env->NewObjectArray(0, stringClass, nullptr);
            env->CallVoidMethod(obj, handleFcitxEvent, 0, vararg);
            return;
        }
        int size = candidateList->totalSize();
        jobjectArray vararg = env->NewObjectArray(size, stringClass, nullptr);
        frontendLog(std::to_string(size) + " candidates");
        for (int i = 0; i < size; i++) {
            auto &candidate = candidateList->candidateFromAll(i);
            if (candidate.isPlaceHolder()) {
                continue;
            }
            // TODO: apply `p_instance->outputFilter(ic, candidate.text())` ?
            auto text = candidate.text().toString();
            env->SetObjectArrayElement(vararg, i, env->NewStringUTF(text.c_str()));
        }
        env->CallVoidMethod(obj, handleFcitxEvent, 0, vararg);
    };
    auto commitStringCallback = [&](const std::string& str){
        frontendLog("commitStringCallback");
        jobjectArray vararg = env->NewObjectArray(1, stringClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(str.c_str()));
        env->CallVoidMethod(obj, handleFcitxEvent, 1, vararg);
    };

    char arg0[] = "";
    char *argv[] = { arg0 };
    p_instance = std::make_unique<fcitx::Instance>(FCITX_ARRAY_SIZE(argv), argv);
    p_instance->addonManager().registerDefaultLoader(nullptr);
    p_dispatcher = std::make_unique<fcitx::EventDispatcher>();
    p_dispatcher->attach(&p_instance->eventLoop());

    p_dispatcher->schedule([&](){
        auto defaultGroup = p_instance->inputMethodManager().currentGroup();
        defaultGroup.inputMethodList().clear();
        defaultGroup.inputMethodList().emplace_back("pinyin");
        defaultGroup.setDefaultInputMethod("");
        p_instance->inputMethodManager().setGroup(defaultGroup);

        auto *androidfrontend = p_instance->addonManager().addon("androidfrontend");
        androidfrontend->call<fcitx::IAndroidFrontend::setCandidateListCallback>(candidateListCallback);
        androidfrontend->call<fcitx::IAndroidFrontend::setCommitStringCallback>(commitStringCallback);
        auto uuid = androidfrontend->call<fcitx::IAndroidFrontend::createInputContext>("fcitx5-android");
        p_frontend.reset(androidfrontend);
        p_uuid = (uuid);
    });

    try {
        return p_instance->exec();
    } catch (const fcitx::InstanceQuietQuit &) {
    } catch (const std::exception &e) {
        __android_log_write(ANDROID_LOG_ERROR, "fcitx5", "Received exception:");
        __android_log_write(ANDROID_LOG_ERROR, "fcitx5", e.what());
        return 1;
    }
    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_JNI_sendKeyToFcitx__Ljava_lang_String_2(JNIEnv *env, jobject /* this */, jstring key) {
    const char* k = env->GetStringUTFChars(key, nullptr);
    fcitx::Key parsedKey(k);
    env->ReleaseStringUTFChars(key, k);
    p_dispatcher->schedule([parsedKey]() {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, parsedKey, false);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_JNI_sendKeyToFcitx__C(JNIEnv *env, jobject /* this */, jchar c) {
    fcitx::Key parsedKey((const char*) &c);
    p_dispatcher->schedule([parsedKey]() {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, parsedKey, false);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_JNI_selectCandidate(JNIEnv *env, jobject /* this */, jint idx) {
    frontendLog("select candidate #" + std::to_string(idx));
    p_dispatcher->schedule([idx]() {
        p_frontend->call<fcitx::IAndroidFrontend::selectCandidate>(p_uuid, idx);
    });
}
