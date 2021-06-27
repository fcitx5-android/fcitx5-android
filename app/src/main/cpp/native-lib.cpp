#include <jni.h>
#include <memory>
#include <unistd.h>
#include <android/log.h>

#include <fcitx/instance.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodmanager.h>
#include <fcitx-utils/eventdispatcher.h>
#include <fcitx-utils/stringutils.h>

#include "androidfrontend/androidfrontend_public.h"

static const char *tag = "fcitx5";
static const size_t androidBufSize = 512;

class AndroidStreamBuf : public std::streambuf {
public:
    explicit AndroidStreamBuf(size_t buf_size);
    ~AndroidStreamBuf() override;

    int overflow(int c) override;
    int sync() override;

private:
    const size_t buf_size_;
    char *pbuf_;
};


AndroidStreamBuf::AndroidStreamBuf(size_t buf_size) : buf_size_(buf_size) {
    assert(buf_size_ > 0);
    pbuf_ = new char[buf_size_];
    memset(pbuf_, 0, buf_size_);

    setp(pbuf_, pbuf_ + buf_size_);
}

AndroidStreamBuf::~AndroidStreamBuf() { delete pbuf_; }

int AndroidStreamBuf::sync() {
    auto str_buf = fcitx::stringutils::trim(std::string(pbuf_));
    auto trim_pbuf = str_buf.c_str();

    int res = __android_log_write(ANDROID_LOG_DEBUG, tag, trim_pbuf);

    memset(pbuf_, 0, buf_size_);
    setp(pbase(), pbase() + buf_size_);
    pbump(0);
    return res;
}

int AndroidStreamBuf::overflow(int c) {
    if (-1 == sync()) {
        return traits_type::eof();
    } else {
        // put c into buffer after successful sync
        if (!traits_type::eq_int_type(c, traits_type::eof())) {
            sputc(traits_type::to_char_type(c));
        }

        return traits_type::not_eof(c);
    }
}

static void jniLog(const std::string &s) {
    __android_log_write(ANDROID_LOG_DEBUG, "JNI", s.c_str());
}

std::unique_ptr<fcitx::Instance> p_instance(nullptr);
std::unique_ptr<fcitx::EventDispatcher> p_dispatcher(nullptr);
std::unique_ptr<fcitx::AddonInstance> p_frontend(nullptr);
fcitx::ICUUID p_uuid;

void resetGlobalPointers() {
    jniLog("resetGlobalPointers");
    p_instance = nullptr;
    p_dispatcher = nullptr;
    p_frontend = nullptr;
}

static auto stream = std::ostream(new AndroidStreamBuf(androidBufSize));

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM * /* jvm */, void * /* reserved */) {
    fcitx::Log::setLogStream(stream);
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_startupFcitx(JNIEnv *env, jclass clazz, jstring appData, jstring appLib, jstring extData) {
    if (p_instance != nullptr) {
        jniLog("fcitx already running");
        return 2;
    }
    jniLog("startupFcitx");

    setenv("SKIP_FCITX_PATH", "true", 1);

    const char *app_data = env->GetStringUTFChars(appData, nullptr);
    const char *app_lib = env->GetStringUTFChars(appLib, nullptr);
    const char *ext_data = env->GetStringUTFChars(extData, nullptr);
    std::string libime_data = std::string(app_data) + "/fcitx5/libime";
    const char *app_data_libime = libime_data.c_str();

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

    jclass hostClass = clazz;
    jclass stringClass = env->FindClass("java/lang/String");
    jmethodID handleFcitxEvent = env->GetStaticMethodID(hostClass, "handleFcitxEvent", "(I[Ljava/lang/Object;)V");
    auto candidateListCallback = [&](const std::vector<std::string> &candidateList) {
        size_t size = candidateList.size();
        jobjectArray vararg = env->NewObjectArray(size, stringClass, nullptr);
        size_t i = 0;
        for(const auto& s : candidateList) {
            env->SetObjectArrayElement(vararg, i++, env->NewStringUTF(s.c_str()));
        }
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 0, vararg);
    };
    auto commitStringCallback = [&](const std::string &str) {
        jobjectArray vararg = env->NewObjectArray(1, stringClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(str.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 1, vararg);
    };
    auto preeditCallback = [&](const std::string &preedit, const std::string &clientPreedit) {
        jobjectArray vararg = env->NewObjectArray(2, stringClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(preedit.c_str()));
        env->SetObjectArrayElement(vararg, 1, env->NewStringUTF(clientPreedit.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 2, vararg);
    };
    auto inputPanelAuxCallback = [&](const std::string &auxUp, const std::string &auxDown) {
        jobjectArray vararg = env->NewObjectArray(2, stringClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(auxUp.c_str()));
        env->SetObjectArrayElement(vararg, 1, env->NewStringUTF(auxDown.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 3, vararg);
    };

    char arg0[] = "";
    char *argv[] = { arg0 };
    p_instance = std::make_unique<fcitx::Instance>(FCITX_ARRAY_SIZE(argv), argv);
    p_instance->addonManager().registerDefaultLoader(nullptr);
    p_dispatcher = std::make_unique<fcitx::EventDispatcher>();
    p_dispatcher->attach(&p_instance->eventLoop());

    p_dispatcher->schedule([&]() {
        auto defaultGroup = p_instance->inputMethodManager().currentGroup();
        defaultGroup.inputMethodList().clear();
        defaultGroup.inputMethodList().emplace_back("pinyin");
        defaultGroup.setDefaultInputMethod("");
        p_instance->inputMethodManager().setGroup(defaultGroup);

        auto *androidfrontend = p_instance->addonManager().addon("androidfrontend");
        androidfrontend->call<fcitx::IAndroidFrontend::setCandidateListCallback>(candidateListCallback);
        androidfrontend->call<fcitx::IAndroidFrontend::setCommitStringCallback>(commitStringCallback);
        androidfrontend->call<fcitx::IAndroidFrontend::setPreeditCallback>(preeditCallback);
        androidfrontend->call<fcitx::IAndroidFrontend::setInputPanelAuxCallback>(inputPanelAuxCallback);
        auto uuid = androidfrontend->call<fcitx::IAndroidFrontend::createInputContext>("fcitx5-android");
        p_frontend.reset(androidfrontend);
        p_uuid = uuid;
    });

    try {
        int code = p_instance->exec();
        resetGlobalPointers();
        return code;
    } catch (const fcitx::InstanceQuietQuit &) {
    } catch (const std::exception &e) {
        jniLog("fcitx exited with exception:");
        jniLog(e.what());
        resetGlobalPointers();
        return 1;
    }
    resetGlobalPointers();
    return 0;
}

#define DO_IF_NOT_RUNNING(expr) \
    if (p_instance == nullptr || p_dispatcher == nullptr || p_frontend == nullptr) { \
        jniLog("fcitx is not running!"); \
        expr; \
    }
#define RETURN_IF_NOT_RUNNING DO_IF_NOT_RUNNING(return)
#define RETURN_VALUE_IF_NOT_RUNNING(v) DO_IF_NOT_RUNNING(return v)

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_exitFcitx(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    jniLog("shutting down fcitx");
    p_dispatcher->schedule([]() {
        p_dispatcher->detach();
        p_instance->exit();
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxString(JNIEnv *env, jclass clazz, jstring key) {
    RETURN_IF_NOT_RUNNING
    const char *k = env->GetStringUTFChars(key, nullptr);
    fcitx::Key parsedKey(k);
    env->ReleaseStringUTFChars(key, k);
    p_dispatcher->schedule([parsedKey]() {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, parsedKey, false);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxChar(JNIEnv *env, jclass clazz, jchar c) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey((const char *) &c);
    p_dispatcher->schedule([parsedKey]() {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, parsedKey, false);
    });
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_selectCandidate(JNIEnv *env, jclass clazz, jint idx) {
    RETURN_IF_NOT_RUNNING
    jniLog("select candidate #" + std::to_string(idx));
    p_dispatcher->schedule([idx]() {
        p_frontend->call<fcitx::IAndroidFrontend::selectCandidate>(p_uuid, idx);
    });
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_isInputPanelEmpty(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(true)
    return p_frontend->call<fcitx::IAndroidFrontend::isInputPanelEmpty>(p_uuid);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_resetInputPanel(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    p_dispatcher->schedule([]() {
        p_frontend->call<fcitx::IAndroidFrontend::resetInputPanel>(p_uuid);
    });
}
