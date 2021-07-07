#include <jni.h>
#include <memory>
#include <android/log.h>

#include <fcitx/instance.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodentry.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/inputmethodmanager.h>
#include <fcitx-utils/eventdispatcher.h>
#include <fcitx-utils/stringutils.h>

#include "androidfrontend/androidfrontend_public.h"

static const char *tag = "fcitx5";
static const size_t androidBufSize = 512;

class AndroidStreamBuf : public std::streambuf {
public:
    explicit AndroidStreamBuf(size_t buf_size) : buf_size_(buf_size) {
        assert(buf_size_ > 0);
        pbuf_ = new char[buf_size_];
        memset(pbuf_, 0, buf_size_);

        setp(pbuf_, pbuf_ + buf_size_);
    }

    ~AndroidStreamBuf() override { delete pbuf_; }

    int overflow(int c) override {
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

    int sync() override {
        auto str_buf = fcitx::stringutils::trim(std::string(pbuf_));
        auto trim_pbuf = str_buf.c_str();

        int res = __android_log_write(ANDROID_LOG_DEBUG, tag, trim_pbuf);

        memset(pbuf_, 0, buf_size_);
        setp(pbase(), pbase() + buf_size_);
        pbump(0);
        return res;
    }

private:
    const size_t buf_size_;
    char *pbuf_;
};


static void jniLog(const std::string &s) {
    __android_log_write(ANDROID_LOG_DEBUG, "JNI", s.c_str());
}


#define DO_IF_NOT_RUNNING(expr) \
    if (!JNIFcitx::getInstance().isRunning()) { \
        jniLog("fcitx is not running!"); \
        expr; \
    }
#define RETURN_IF_NOT_RUNNING DO_IF_NOT_RUNNING(return)
#define RETURN_VALUE_IF_NOT_RUNNING(v) DO_IF_NOT_RUNNING(return v)


// TODO(berberman) move out JNIEnv
class JNIFcitx {
public:
    JNIFcitx() = default;
    JNIFcitx(JNIFcitx const &) = delete;
    void operator=(JNIFcitx const &) = delete;

    static JNIFcitx &getInstance() {
        static JNIFcitx instance;
        return instance;
    }

    bool isRunning (){
        return !(p_instance == nullptr || p_dispatcher == nullptr || p_frontend == nullptr);
    }

    jint startup(JNIEnv *env, jclass clazz, jstring appData, jstring appLib, jstring extData){
        if (p_instance != nullptr) {
            jniLog("fcitx already running");
            return 2;
        }
        jniLog("startupFcitx");

        setenv("SKIP_FCITX_PATH", "true", 1);

        const char *app_data = env->GetStringUTFChars(appData, nullptr);
        const char *app_lib = env->GetStringUTFChars(appLib, nullptr);
        const char *ext_data = env->GetStringUTFChars(extData, nullptr);
        std::string config_home = std::string(ext_data) + "/config";
        std::string data_home = std::string(ext_data) + "/data";
        std::string libime_data = std::string(app_data) + "/fcitx5/libime";
        const char *app_data_libime = libime_data.c_str();

        setenv("HOME", ext_data, 1);
        setenv("XDG_DATA_DIRS", app_data, 1);
        setenv("XDG_CONFIG_HOME", ext_data, 1);
        setenv("XDG_DATA_HOME", ext_data, 1);
        setenv("FCITX_CONFIG_HOME", config_home.c_str(), 1);
        setenv("FCITX_DATA_HOME", data_home.c_str(), 1);
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
            auto &imMgr = p_instance->inputMethodManager();
            auto group = imMgr.currentGroup();
            if (group.inputMethodList().empty()) {
                group.inputMethodList().emplace_back("pinyin");
                imMgr.setGroup(group);
            }

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

    void sendKey(fcitx::Key key){
        p_dispatcher->schedule([this, key]() {
            p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, key, false);
        });
    }

    void select(jint idx){
        jniLog("select candidate #" + std::to_string(idx));
        p_dispatcher->schedule([this, idx]() {
            p_frontend->call<fcitx::IAndroidFrontend::selectCandidate>(p_uuid, idx);
        });
    }

    bool isInputPanelEmpty(){
        return p_frontend->call<fcitx::IAndroidFrontend::isInputPanelEmpty>(p_uuid);
    }

    void resetInputPanel(){
        p_dispatcher->schedule([this]() {
            p_frontend->call<fcitx::IAndroidFrontend::resetInputPanel>(p_uuid);
        });
    }

    jobjectArray listInputMethods(JNIEnv *env){
        auto &imMgr = p_instance->inputMethodManager();
        auto &group = imMgr.currentGroup();
        auto &list = group.inputMethodList();
        jobjectArray array = env->NewObjectArray(list.size(), env->FindClass("java/lang/String"), nullptr);
        size_t i = 0;
        for (const auto &ime : list) {
            const auto *entry = imMgr.entry(ime.name());
            std::string str = entry->uniqueName() + ":"  + entry->name() + ":" + entry->icon();
            env->SetObjectArrayElement(array, i++, env->NewStringUTF(str.c_str()));
        }
        return array;
    }

    jstring inputMethodStatus(JNIEnv *env) {
        auto &imMgr = p_instance->inputMethodManager();
        std::string uniqueName;
        std::string name = "Not available";
        std::string icon = "input-keyboard";
        std::string altDescription;
        std::string label;
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        if (ic) {
            icon = p_instance->inputMethodIcon(ic);
            if (auto entry = p_instance->inputMethodEntry(ic)) {
                uniqueName = entry->uniqueName();
                name = entry->name();
                label = entry->label();
                if (auto engine = p_instance->inputMethodEngine(ic)) {
                    auto subModeLabel = engine->subModeLabel(*entry, *ic);
                    if (!subModeLabel.empty()) {
                        label = subModeLabel;
                    }
                    altDescription = engine->subMode(*entry, *ic);
                }
            }
        }
        std::string result = uniqueName + ":" + name + ":" + icon + ":" + altDescription + ":" + label;
        return env->NewStringUTF(result.c_str());
    }

    void setInputMethod(std::string string){
        p_dispatcher->schedule([this, ime = std::move(string)]() {
            p_instance->setCurrentInputMethod(ime);
        });
    }

    jobjectArray availableInputMethods(JNIEnv* env){
        jclass imEntryClass = env->FindClass("me/rocka/fcitx5test/native/InputMethodEntry");
        jmethodID entryConstructor = env->GetMethodID(imEntryClass, "<init>",
                                                      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V");
        std::vector<const fcitx::InputMethodEntry *> entries;
        p_instance->inputMethodManager().foreachEntries([&](const auto &entry) {
            entries.emplace_back(&entry);
            return true;
        });
        jobjectArray array = env->NewObjectArray(entries.size(), imEntryClass, nullptr);
        size_t i = 0;
        for (const auto &entry : entries) {
            jobject obj = env->NewObject(imEntryClass, entryConstructor,
                                         env->NewStringUTF(entry->uniqueName().c_str()),
                                         env->NewStringUTF(entry->name().c_str()),
                                         env->NewStringUTF(entry->icon().c_str()),
                                         env->NewStringUTF(entry->nativeName().c_str()),
                                         env->NewStringUTF(entry->label().c_str()),
                                         env->NewStringUTF(entry->languageCode().c_str()),
                                         entry->isConfigurable() ? JNI_TRUE : JNI_FALSE
            );
            env->SetObjectArrayElement(array, i++, obj);
            env->DeleteLocalRef(obj);
        }
        return array;
    }

    void setEnabledInputMethods(std::vector<std::string> & entries){
        p_dispatcher->schedule([this, entries]() {
            auto &imMgr = p_instance->inputMethodManager();
            fcitx::InputMethodGroup newGroup(imMgr.currentGroup().name());
            newGroup.setDefaultLayout("us");
            auto &list = newGroup.inputMethodList();
            for (const auto &e : entries) {
                list.emplace_back(e);
            }
            imMgr.setGroup(std::move(newGroup));
            imMgr.save();
        });
    }

    fcitx::RawConfig getGlobalConfig(){
        fcitx::RawConfig cfg;
        p_instance->globalConfig().save(cfg);
        return cfg;
    }

    std::optional<fcitx::RawConfig> getAddonConfig(JNIEnv *env, jstring addon){
        const char *addonName = env->GetStringUTFChars(addon, nullptr);
        const auto *addonInfo = p_instance->addonManager().addonInfo(addonName);
        env->ReleaseStringUTFChars(addon, addonName);
        if (!addonInfo || !addonInfo->isConfigurable()) {
            return std::nullopt;
        }
        const auto *addonInstance = p_instance->addonManager().addon(addonName, true);
        if (!addonInstance) {
            return std::nullopt;
        }
        const auto configuration = addonInstance->getConfig();
        if (!configuration) {
            return std::nullopt;
        }
        fcitx::RawConfig cfg;
        configuration->save(cfg);
        return std::make_optional(cfg);
    }

    std::optional<fcitx::RawConfig> getInputMethodConfig(JNIEnv *env, jstring im){
        const char *imName = env->GetStringUTFChars(im, nullptr);
        const auto *entry = p_instance->inputMethodManager().entry(imName);
        env->ReleaseStringUTFChars(im, imName);
        if (!entry || !entry->isConfigurable()) {
            return std::nullopt;
        }
        const auto *engine = p_instance->inputMethodEngine(imName);
        if (!engine) {
            return std::nullopt;
        }
        const auto configuration = engine->getConfigForInputMethod(*entry);
        if (!configuration) {
            return std::nullopt;
        }
        fcitx::RawConfig cfg;
        configuration->save(cfg);
        return std::make_optional(cfg);
    }

    void saveConfig(){
        p_dispatcher->schedule([this]() {
            p_instance->globalConfig().safeSave();
            p_instance->inputMethodManager().save();
            p_instance->addonManager().saveAll();
        });
    }

    void exit(){
        jniLog("shutting down fcitx");
        p_dispatcher->schedule([this]() {
            p_dispatcher->detach();
            p_instance->exit();
        });
    }

private:

    std::unique_ptr<fcitx::Instance> p_instance{};
    std::unique_ptr<fcitx::EventDispatcher> p_dispatcher{};
    std::unique_ptr<fcitx::AddonInstance> p_frontend{};
    fcitx::ICUUID p_uuid{};

    void resetGlobalPointers() {
        jniLog("resetGlobalPointers");
        p_instance = nullptr;
        p_dispatcher = nullptr;
        p_frontend = nullptr;
    }
};

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM * /* jvm */, void * /* reserved */) {
    static std::ostream stream(new AndroidStreamBuf(androidBufSize));
    fcitx::Log::setLogStream(stream);
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}

extern "C"
JNIEXPORT jint JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_startupFcitx(JNIEnv *env, jclass clazz, jstring appData, jstring appLib, jstring extData) {
   return JNIFcitx::getInstance().startup(env,clazz,appData,appLib,extData);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_exitFcitx(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    JNIFcitx::getInstance().exit();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_saveFcitxConfig(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    JNIFcitx::getInstance().saveConfig();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxString(JNIEnv *env, jclass clazz, jstring key) {
    RETURN_IF_NOT_RUNNING
    const char *k = env->GetStringUTFChars(key, nullptr);
    fcitx::Key parsedKey(k);
    env->ReleaseStringUTFChars(key, k);
    JNIFcitx::getInstance().sendKey(parsedKey);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxChar(JNIEnv *env, jclass clazz, jchar c) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey((const char *) &c);
    JNIFcitx::getInstance().sendKey(parsedKey);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_selectCandidate(JNIEnv *env, jclass clazz, jint idx) {
    RETURN_IF_NOT_RUNNING
    JNIFcitx::getInstance().select(idx);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_isInputPanelEmpty(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(true)
    return JNIFcitx::getInstance().isInputPanelEmpty();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_resetInputPanel(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    JNIFcitx::getInstance().resetInputPanel();
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_listInputMethods(JNIEnv *env, jclass clazz) {
    return JNIFcitx::getInstance().listInputMethods(env);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_inputMethodStatus(JNIEnv *env, jclass clazz) {
    return JNIFcitx::getInstance().inputMethodStatus(env);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setInputMethod(JNIEnv *env, jclass clazz, jstring ime) {
    const char *chars = env->GetStringUTFChars(ime, nullptr);
    std::string string(chars);
    env->ReleaseStringUTFChars(ime, chars);
    JNIFcitx::getInstance().setInputMethod(string);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_availableInputMethods(JNIEnv *env, jclass clazz) {
   return JNIFcitx::getInstance().availableInputMethods(env);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setEnabledInputMethods(JNIEnv *env, jclass clazz, jobjectArray array) {
    size_t size = env->GetArrayLength(array);
    std::vector<std::string> entries;
    for (size_t i = 0; i < size; i++) {
        auto string = (jstring) env->GetObjectArrayElement(array, i);
        const char *chars = env->GetStringUTFChars(string, nullptr);
        entries.emplace_back(chars);
        env->ReleaseStringUTFChars(string, chars);
        env->DeleteLocalRef(string);
    }
    JNIFcitx::getInstance().setEnabledInputMethods(entries);
}

jobject fcitxRawConfigToJObject(JNIEnv *env, jclass cls, jmethodID init, jmethodID setSubItems, const fcitx::RawConfig& cfg) {
    jobject obj = env->NewObject(cls, init,
                                 env->NewStringUTF(cfg.name().c_str()),
                                 env->NewStringUTF(cfg.comment().c_str()),
                                 env->NewStringUTF(cfg.value().c_str()),
                                 nullptr);
    if (!cfg.hasSubItems()) {
        return obj;
    }
    std::vector<const fcitx::RawConfig *> subItems;
    for (const auto& option : cfg.subItems()) {
        const auto& subCfg = cfg.get(option);
        subItems.emplace_back(subCfg.get());
    }
    jobjectArray array = env->NewObjectArray(subItems.size(), cls, nullptr);
    size_t i = 0;
    for (const auto item : subItems) {
        env->SetObjectArrayElement(array, i++, fcitxRawConfigToJObject(env, cls, init, setSubItems, *item));
    }
    env->CallVoidMethod(obj, setSubItems, array);
    env->DeleteLocalRef(array);
    return obj;
}

jobject fcitxRawConfigToJObject(JNIEnv *env, const fcitx::RawConfig& cfg) {
    jclass cls = env->FindClass("me/rocka/fcitx5test/native/RawConfig");
    jmethodID init = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lme/rocka/fcitx5test/native/RawConfig;)V");
    jmethodID setSubItems = env->GetMethodID(cls, "setSubItems", "([Lme/rocka/fcitx5test/native/RawConfig;)V");
    return fcitxRawConfigToJObject(env, cls, init, setSubItems, cfg);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getGlobalConfig(JNIEnv *env, jclass clazz) {
    auto cfg = JNIFcitx::getInstance().getGlobalConfig();
    return fcitxRawConfigToJObject(env, cfg);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getAddonConfig(JNIEnv *env, jclass clazz, jstring addon) {
    auto result = JNIFcitx::getInstance().getAddonConfig(env, addon);
    if(result)
        return fcitxRawConfigToJObject(env, result.value());
    else
        return nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getInputMethodConfig(JNIEnv *env, jclass clazz, jstring im) {
    auto result = JNIFcitx::getInstance().getInputMethodConfig(env, im);
    if(result)
        return fcitxRawConfigToJObject(env, result.value());
    else
        return nullptr;
}
