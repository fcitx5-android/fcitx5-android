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

#include "fcitx5/src/modules/quickphrase/quickphrase_public.h"

#include "androidfrontend/androidfrontend_public.h"
#include "androidstreambuf.h"

class Fcitx {
public:
    Fcitx() = default;
    Fcitx(Fcitx const &) = delete;
    void operator=(Fcitx const &) = delete;

    static Fcitx &Instance() {
        static Fcitx instance;
        return instance;
    }

    bool isRunning() {
        return p_instance != nullptr && p_dispatcher != nullptr && p_frontend != nullptr;
    }

    int startup(std::function<void(fcitx::AddonInstance *)> setupCallback) {
        char arg0[] = "";
        char *argv[] = {arg0};
        p_instance = std::make_unique<fcitx::Instance>(FCITX_ARRAY_SIZE(argv), argv);
        p_instance->addonManager().registerDefaultLoader(nullptr);
        p_dispatcher = std::make_unique<fcitx::EventDispatcher>();
        p_dispatcher->attach(&p_instance->eventLoop());

        p_dispatcher->schedule([&, this]() {
            auto &addonMgr = p_instance->addonManager();
            p_frontend = addonMgr.addon("androidfrontend");
            p_quickphrase = addonMgr.addon("quickphrase");
            p_uuid = p_frontend->call<fcitx::IAndroidFrontend::createInputContext>("fcitx5-android");
            setupCallback(p_frontend);
        });

        int code = -1;
        try {
            code = p_instance->exec();
        } catch (const fcitx::InstanceQuietQuit &) {
            FCITX_INFO() << "fcitx exited quietly";
            code = 0;
        } catch (const std::exception &e) {
            FCITX_ERROR() << "fcitx exited with exception: " << e.what();
            code = 1;
        }
        resetGlobalPointers();
        return code;
    }

    void sendKey(fcitx::Key key) {
        p_dispatcher->schedule([this, key]() {
            p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, key, false);
        });
    }

    void select(int idx) {
        p_dispatcher->schedule([this, idx]() {
            p_frontend->call<fcitx::IAndroidFrontend::selectCandidate>(p_uuid, idx);
        });
    }

    bool isInputPanelEmpty() {
        return p_frontend->call<fcitx::IAndroidFrontend::isInputPanelEmpty>(p_uuid);
    }

    void resetInputPanel() {
        p_dispatcher->schedule([this]() {
            p_frontend->call<fcitx::IAndroidFrontend::resetInputPanel>(p_uuid);
        });
    }

    std::vector<const fcitx::InputMethodEntry *> listInputMethods() {
        const auto &imMgr = p_instance->inputMethodManager();
        const auto &list = imMgr.currentGroup().inputMethodList();
        std::vector<const fcitx::InputMethodEntry *> entries;
        for (const auto &ime : list) {
            const auto *entry = imMgr.entry(ime.name());
            entries.emplace_back(entry);
        }
        return std::move(entries);
    }

    const fcitx::InputMethodEntry *inputMethodStatus() {
        // TODO(rocka): report `subMode` and `subModeLabel`
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        if (ic) {
            return p_instance->inputMethodEntry(ic);
        }
        return nullptr;
    }

    void setInputMethod(std::string string) {
        p_dispatcher->schedule([this, ime = std::move(string)]() {
            p_instance->setCurrentInputMethod(ime);
        });
    }

    std::vector<const fcitx::InputMethodEntry *> availableInputMethods() {
        std::vector<const fcitx::InputMethodEntry *> entries;
        p_instance->inputMethodManager().foreachEntries([&](const auto &entry) {
            entries.emplace_back(&entry);
            return true;
        });
        return std::move(entries);
    }

    void setEnabledInputMethods(std::vector<std::string> &entries) {
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

    static fcitx::RawConfig mergeConfigDesc(const fcitx::Configuration *conf) {
        fcitx::RawConfig topLevel;
        auto cfg = topLevel.get("cfg", true);
        conf->save(*cfg);
        auto desc = topLevel.get("desc", true);
        conf->dumpDescription(*desc);
        return topLevel;
    }

    fcitx::RawConfig getGlobalConfig() {
        const auto &configuration = p_instance->globalConfig().config();
        return mergeConfigDesc(&configuration);
    }

    void setGlobalConfig(const fcitx::RawConfig &config) {
        p_instance->globalConfig().load(config, true);
        if (p_instance->globalConfig().safeSave()) {
            p_instance->reloadConfig();
        }
    }

    fcitx::AddonInstance *getAddonInstance(const std::string &addon) {
        const auto *addonInfo = p_instance->addonManager().addonInfo(addon);
        if (!addonInfo || !addonInfo->isConfigurable()) {
            return nullptr;
        }
        return p_instance->addonManager().addon(addon, true);
    }

    std::optional<fcitx::RawConfig> getAddonConfig(const std::string &addonName) {
        const auto addonInstance = getAddonInstance(addonName);
        if (!addonInstance) {
            return std::nullopt;
        }
        const auto configuration = addonInstance->getConfig();
        if (!configuration) {
            return std::nullopt;
        }
        return std::make_optional(mergeConfigDesc(configuration));
    }

    void setAddonConfig(const std::string &addonName, const fcitx::RawConfig &config) {
        auto addonInstance = getAddonInstance(addonName);
        if (!addonInstance) {
            return;
        }
        addonInstance->setConfig(config);
    }

    std::optional<fcitx::RawConfig> getInputMethodConfig(const std::string &imName) {
        const auto *entry = p_instance->inputMethodManager().entry(imName);
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
        return std::make_optional(mergeConfigDesc(configuration));
    }

    void setInputMethodConfig(const std::string &imName, const fcitx::RawConfig &config) {
        const auto *entry = p_instance->inputMethodManager().entry(imName);
        if (!entry || !entry->isConfigurable()) {
            return;
        }
        auto *engine = p_instance->inputMethodEngine(imName);
        if (!engine) {
            return;
        }
        engine->setConfigForInputMethod(*entry, config);
    }

    std::map<const fcitx::AddonInfo *, bool> getAddons() {
        auto &globalConfig = p_instance->globalConfig();
        auto &addonManager = p_instance->addonManager();
        const auto &enabledAddons = globalConfig.enabledAddons();
        std::unordered_set<std::string> enabledSet(enabledAddons.begin(), enabledAddons.end());
        const auto &disabledAddons = globalConfig.disabledAddons();
        std::unordered_set<std::string> disabledSet(disabledAddons.begin(), disabledAddons.end());
        std::map<const fcitx::AddonInfo *, bool> addons;
        for (const auto category : {fcitx::AddonCategory::InputMethod,
                                    fcitx::AddonCategory::Frontend,
                                    fcitx::AddonCategory::Loader,
                                    fcitx::AddonCategory::Module,
                                    fcitx::AddonCategory::UI}) {
            const auto names = addonManager.addonNames(category);
            for (const auto &name : names) {
                const auto *info = addonManager.addonInfo(name);
                if (!info) {
                    continue;
                }
                bool enabled = info->isDefaultEnabled();
                if (disabledSet.count(info->uniqueName())) {
                    enabled = false;
                } else if (enabledSet.count(info->uniqueName())) {
                    enabled = true;
                }
                addons.insert({info, enabled});
            }
        }
        return addons;
    }

    void setAddonState(const std::map<std::string, bool> &state) {
        auto &globalConfig = p_instance->globalConfig();
        auto &addonManager = p_instance->addonManager();
        const auto &enabledAddons = globalConfig.enabledAddons();
        std::set<std::string> enabledSet(enabledAddons.begin(), enabledAddons.end());
        const auto &disabledAddons = globalConfig.disabledAddons();
        std::set<std::string> disabledSet(disabledAddons.begin(), disabledAddons.end());
        for (const auto &item : state) {
            const auto *info = addonManager.addonInfo(item.first);
            if (!info) {
                continue;
            }
            const bool enabled = item.second;
            const auto &uniqueName = info->uniqueName();
            if (enabled == info->isDefaultEnabled()) {
                enabledSet.erase(uniqueName);
                disabledSet.erase(uniqueName);
            } else if (enabled) {
                enabledSet.insert(uniqueName);
                disabledSet.erase(uniqueName);
            } else {
                enabledSet.erase(uniqueName);
                disabledSet.insert(uniqueName);
            }
        }
        p_dispatcher->schedule([this, e = std::move(enabledSet), d = std::move(disabledSet)] {
            auto &globalConfig = p_instance->globalConfig();
            globalConfig.setEnabledAddons({e.begin(), e.end()});
            globalConfig.setDisabledAddons({d.begin(), d.end()});
            globalConfig.safeSave();
            p_instance->reloadConfig();
        });
    }

    void triggerQuickPhrase() {
        if (p_quickphrase) {
            p_dispatcher->schedule([this]() {
                auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
                p_quickphrase->call<fcitx::IQuickPhrase::trigger>(
                        ic, "", "", "", "", fcitx::Key{FcitxKey_None}
                );
            });
        }
    }

    void saveConfig() {
        p_dispatcher->schedule([this]() {
            p_instance->globalConfig().safeSave();
            p_instance->inputMethodManager().save();
            p_instance->addonManager().saveAll();
        });
    }

    void exit() {
        p_dispatcher->schedule([this]() {
            p_dispatcher->detach();
            p_instance->exit();
        });
    }

private:
    std::unique_ptr<fcitx::Instance> p_instance{};
    std::unique_ptr<fcitx::EventDispatcher> p_dispatcher{};
    fcitx::AddonInstance *p_frontend = nullptr;
    fcitx::AddonInstance *p_quickphrase = nullptr;
    fcitx::ICUUID p_uuid{};

    void resetGlobalPointers() {
        p_instance.reset();
        p_dispatcher.reset();
        p_frontend = nullptr;
        p_uuid = {};
    }
};

static void jniLog(const std::string &s) {
    __android_log_write(ANDROID_LOG_DEBUG, "JNI", s.c_str());
}

#define DO_IF_NOT_RUNNING(expr) \
    if (!Fcitx::Instance().isRunning()) { \
        jniLog("fcitx is not running!"); \
        expr; \
    }
#define RETURN_IF_NOT_RUNNING DO_IF_NOT_RUNNING(return)
#define RETURN_VALUE_IF_NOT_RUNNING(v) DO_IF_NOT_RUNNING(return v)

std::string jstringToString(JNIEnv *env, jstring j) {
    const char *c = env->GetStringUTFChars(j, nullptr);
    std::string s(c);
    env->ReleaseStringUTFChars(j, c);
    return s;
}

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM * /* jvm */, void * /* reserved */) {
    static std::ostream stream(new AndroidStreamBuf("fcitx5", 512));
    fcitx::Log::setLogStream(stream);
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}

jobject fcitxInputMethodEntryToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry);

extern "C"
JNIEXPORT jint JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_startupFcitx(JNIEnv *env, jclass clazz, jstring appData, jstring appLib, jstring extData) {
    if (Fcitx::Instance().isRunning()) {
        jniLog("startupFcitx: already running!");
        return 2;
    }
    jniLog("startupFcitx: starting...");

    setenv("SKIP_FCITX_PATH", "true", 1);

    const char *app_data = env->GetStringUTFChars(appData, nullptr);
    const char *app_lib = env->GetStringUTFChars(appLib, nullptr);
    const char *ext_data = env->GetStringUTFChars(extData, nullptr);
    std::string config_home = fcitx::stringutils::joinPath(ext_data, "config");
    std::string data_home = fcitx::stringutils::joinPath(ext_data, "data");
    std::string libime_data = fcitx::stringutils::joinPath(app_data, "fcitx5", "libime");
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

    jclass ObjectClass = env->FindClass("java/lang/Object");
    jclass IntegerClass = env->FindClass("java/lang/Integer");
    jmethodID IntegerInit = env->GetMethodID(IntegerClass, "<init>", "(I)V");
    jmethodID handleFcitxEvent = env->GetStaticMethodID(clazz, "handleFcitxEvent", "(I[Ljava/lang/Object;)V");
    auto candidateListCallback = [&](const std::vector<std::string> &candidateList) {
        size_t size = candidateList.size();
        jobjectArray vararg = env->NewObjectArray(size, ObjectClass, nullptr);
        size_t i = 0;
        for (const auto &s : candidateList) {
            env->SetObjectArrayElement(vararg, i++, env->NewStringUTF(s.c_str()));
        }
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 0, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto commitStringCallback = [&](const std::string &str) {
        jobjectArray vararg = env->NewObjectArray(1, ObjectClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(str.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 1, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto preeditCallback = [&](const std::string &preedit, const std::string &clientPreedit) {
        jobjectArray vararg = env->NewObjectArray(2, ObjectClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(preedit.c_str()));
        env->SetObjectArrayElement(vararg, 1, env->NewStringUTF(clientPreedit.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 2, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto inputPanelAuxCallback = [&](const std::string &auxUp, const std::string &auxDown) {
        jobjectArray vararg = env->NewObjectArray(2, ObjectClass, nullptr);
        env->SetObjectArrayElement(vararg, 0, env->NewStringUTF(auxUp.c_str()));
        env->SetObjectArrayElement(vararg, 1, env->NewStringUTF(auxDown.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 3, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto readyCallback = [&]() {
        jobjectArray vararg = env->NewObjectArray(0, ObjectClass, nullptr);
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 4, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto keyEventCallback = [&](const int code, const std::string &sym) {
        jobjectArray vararg = env->NewObjectArray(2, ObjectClass, nullptr);
        auto integer = env->NewObject(IntegerClass, IntegerInit, code);
        env->SetObjectArrayElement(vararg, 0, integer);
        env->SetObjectArrayElement(vararg, 1, env->NewStringUTF(sym.c_str()));
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 5, vararg);
        env->DeleteLocalRef(vararg);
        env->DeleteLocalRef(integer);
    };
    auto imChangeCallback = [&]() {
        jobjectArray vararg = env->NewObjectArray(1, ObjectClass, nullptr);
        const auto *entry = Fcitx::Instance().inputMethodStatus();
        auto obj = fcitxInputMethodEntryToJObject(env, entry);
        env->SetObjectArrayElement(vararg, 0, obj);
        env->CallStaticVoidMethod(clazz, handleFcitxEvent, 6, vararg);
        env->DeleteLocalRef(vararg);
        env->DeleteLocalRef(obj);
    };

    int code = Fcitx::Instance().startup([&](auto *androidfrontend) {
        jniLog("startupFcitx: setupCallback");
        readyCallback();
        androidfrontend->template call<fcitx::IAndroidFrontend::setCandidateListCallback>(candidateListCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setCommitStringCallback>(commitStringCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setPreeditCallback>(preeditCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setInputPanelAuxCallback>(inputPanelAuxCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setKeyEventCallback>(keyEventCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setInputMethodChangeCallback>(imChangeCallback);
    });
    jniLog("startupFcitx: returned with code " + std::to_string(code));
    return code;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_exitFcitx(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().exit();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_saveFcitxConfig(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().saveConfig();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxString(JNIEnv *env, jclass clazz, jstring key) {
    RETURN_IF_NOT_RUNNING
    const char *k = env->GetStringUTFChars(key, nullptr);
    fcitx::Key parsedKey(k);
    env->ReleaseStringUTFChars(key, k);
    Fcitx::Instance().sendKey(parsedKey);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxChar(JNIEnv *env, jclass clazz, jchar c) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey((const char *) &c);
    Fcitx::Instance().sendKey(parsedKey);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_selectCandidate(JNIEnv *env, jclass clazz, jint idx) {
    RETURN_IF_NOT_RUNNING
    jniLog("selectCandidate: #" + std::to_string(idx));
    Fcitx::Instance().select(idx);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_isInputPanelEmpty(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(true)
    return Fcitx::Instance().isInputPanelEmpty();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_resetInputPanel(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().resetInputPanel();
}

// TODO: avoid unnecessary `FindClass` and `GetMethodID` when called in loop
jobject fcitxInputMethodEntryToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry) {
    jclass imEntryClass = env->FindClass("me/rocka/fcitx5test/native/InputMethodEntry");
    jmethodID entryConstructor = env->GetMethodID(imEntryClass, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Z)V");
    return env->NewObject(imEntryClass, entryConstructor,
                          env->NewStringUTF(entry->uniqueName().c_str()),
                          env->NewStringUTF(entry->name().c_str()),
                          env->NewStringUTF(entry->icon().c_str()),
                          env->NewStringUTF(entry->nativeName().c_str()),
                          env->NewStringUTF(entry->label().c_str()),
                          env->NewStringUTF(entry->languageCode().c_str()),
                          entry->isConfigurable()
    );
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_listInputMethods(JNIEnv *env, jclass clazz) {
    jclass imEntryClass = env->FindClass("me/rocka/fcitx5test/native/InputMethodEntry");
    RETURN_VALUE_IF_NOT_RUNNING(env->NewObjectArray(0, imEntryClass, nullptr))
    const auto entries = Fcitx::Instance().listInputMethods();
    jobjectArray array = env->NewObjectArray(entries.size(), imEntryClass, nullptr);
    size_t i = 0;
    for (const auto &entry : entries) {
        jobject obj = fcitxInputMethodEntryToJObject(env, entry);
        env->SetObjectArrayElement(array, i++, obj);
        env->DeleteLocalRef(obj);
    }
    return array;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_inputMethodStatus(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto *entry = Fcitx::Instance().inputMethodStatus();
    return fcitxInputMethodEntryToJObject(env, entry);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setInputMethod(JNIEnv *env, jclass clazz, jstring ime) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().setInputMethod(jstringToString(env, ime));
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_availableInputMethods(JNIEnv *env, jclass clazz) {
    jclass imEntryClass = env->FindClass("me/rocka/fcitx5test/native/InputMethodEntry");
    RETURN_VALUE_IF_NOT_RUNNING(env->NewObjectArray(0, imEntryClass, nullptr))
    auto entries = Fcitx::Instance().availableInputMethods();
    jobjectArray array = env->NewObjectArray(entries.size(), imEntryClass, nullptr);
    size_t i = 0;
    for (const auto &entry : entries) {
        jobject obj = fcitxInputMethodEntryToJObject(env, entry);
        env->SetObjectArrayElement(array, i++, obj);
        env->DeleteLocalRef(obj);
    }
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setEnabledInputMethods(JNIEnv *env, jclass clazz, jobjectArray array) {
    RETURN_IF_NOT_RUNNING
    size_t size = env->GetArrayLength(array);
    std::vector<std::string> entries;
    for (size_t i = 0; i < size; i++) {
        auto string = reinterpret_cast<jstring>(env->GetObjectArrayElement(array, i));
        entries.emplace_back(jstringToString(env, string));
        env->DeleteLocalRef(string);
    }
    Fcitx::Instance().setEnabledInputMethods(entries);
}

jobject fcitxRawConfigToJObject(JNIEnv *env, jclass cls, jmethodID init, jmethodID setSubItems, const fcitx::RawConfig &cfg) {
    jobject obj = env->NewObject(cls, init,
                                 env->NewStringUTF(cfg.name().c_str()),
                                 env->NewStringUTF(cfg.comment().c_str()),
                                 env->NewStringUTF(cfg.value().c_str()),
                                 nullptr);
    if (!cfg.hasSubItems()) {
        return obj;
    }
    jobjectArray array = env->NewObjectArray(cfg.subItemsSize(), cls, nullptr);
    size_t i = 0;
    for (const auto &option : cfg.subItems()) {
        env->SetObjectArrayElement(array, i++, fcitxRawConfigToJObject(env, cls, init, setSubItems, *cfg.get(option)));
    }
    env->CallVoidMethod(obj, setSubItems, array);
    env->DeleteLocalRef(array);
    return obj;
}

jobject fcitxRawConfigToJObject(JNIEnv *env, const fcitx::RawConfig &cfg) {
    jclass cls = env->FindClass("me/rocka/fcitx5test/native/RawConfig");
    jmethodID init = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Lme/rocka/fcitx5test/native/RawConfig;)V");
    jmethodID setSubItems = env->GetMethodID(cls, "setSubItems", "([Lme/rocka/fcitx5test/native/RawConfig;)V");
    return fcitxRawConfigToJObject(env, cls, init, setSubItems, cfg);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getFcitxGlobalConfig(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto cfg = Fcitx::Instance().getGlobalConfig();
    return fcitxRawConfigToJObject(env, cfg);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getFcitxAddonConfig(JNIEnv *env, jclass clazz, jstring addon) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto result = Fcitx::Instance().getAddonConfig(jstringToString(env, addon));
    if (result)
        return fcitxRawConfigToJObject(env, result.value());
    else
        return nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getFcitxInputMethodConfig(JNIEnv *env, jclass clazz, jstring im) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto result = Fcitx::Instance().getInputMethodConfig(jstringToString(env, im));
    if (result)
        return fcitxRawConfigToJObject(env, result.value());
    else
        return nullptr;
}

void jobjectFillRawConfig(JNIEnv *env, jclass cls, jfieldID fName, jfieldID fValue, jfieldID fSubItems, jobject jConfig, fcitx::RawConfig &config) {
    auto subItems = reinterpret_cast<jobjectArray>(env->GetObjectField(jConfig, fSubItems));
    if (subItems == nullptr) {
        auto jValue = reinterpret_cast<jstring>(env->GetObjectField(jConfig, fValue));
        config = jstringToString(env, jValue);
        env->DeleteLocalRef(jValue);
    } else {
        size_t size = env->GetArrayLength(subItems);
        for (size_t i = 0; i < size; i++) {
            jobject item = env->GetObjectArrayElement(subItems, i);
            auto jName = reinterpret_cast<jstring>(env->GetObjectField(item, fName));
            auto name = jstringToString(env, jName);
            auto subConfig = config.get(name, true);
            jobjectFillRawConfig(env, cls, fName, fValue, fSubItems, item, *subConfig);
            env->DeleteLocalRef(jName);
            env->DeleteLocalRef(item);
        }
    }
    env->DeleteLocalRef(subItems);
}

fcitx::RawConfig jobjectToRawConfig(JNIEnv *env, jobject jConfig) {
    fcitx::RawConfig config;
    jclass cls = env->FindClass("me/rocka/fcitx5test/native/RawConfig");
    jfieldID fName = env->GetFieldID(cls, "name", "Ljava/lang/String;");
    jfieldID fValue = env->GetFieldID(cls, "value", "Ljava/lang/String;");
    jfieldID fSubItems = env->GetFieldID(cls, "subItems", "[Lme/rocka/fcitx5test/native/RawConfig;");
    jobjectFillRawConfig(env, cls, fName, fValue, fSubItems, jConfig, config);
    return config;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setFcitxGlobalConfig(JNIEnv *env, jclass clazz, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setGlobalConfig(rawConfig);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setFcitxAddonConfig(JNIEnv *env, jclass clazz, jstring addon, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setAddonConfig(jstringToString(env, addon), rawConfig);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setFcitxInputMethodConfig(JNIEnv *env, jclass clazz, jstring im, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setInputMethodConfig(jstringToString(env, im), rawConfig);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getFcitxAddons(JNIEnv *env, jclass clazz) {
    jclass cls = env->FindClass("me/rocka/fcitx5test/native/AddonInfo");
    RETURN_VALUE_IF_NOT_RUNNING(env->NewObjectArray(0, cls, nullptr))
    const auto &addons = Fcitx::Instance().getAddons();
    jmethodID init = env->GetMethodID(cls, "<init>", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IZZZ)V");
    jobjectArray array = env->NewObjectArray(addons.size(), cls, nullptr);
    size_t i = 0;
    for (const auto addon : addons) {
        const auto *info = addon.first;
        jobject obj = env->NewObject(cls, init,
                                     env->NewStringUTF(info->uniqueName().c_str()),
                                     env->NewStringUTF(info->name().match().c_str()),
                                     env->NewStringUTF(info->comment().match().c_str()),
                                     static_cast<int32_t>(info->category()),
                                     info->isConfigurable(),
                                     addon.second,
                                     info->onDemand()
        );
        env->SetObjectArrayElement(array, i++, obj);
        env->DeleteLocalRef(obj);
    }
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setFcitxAddonState(JNIEnv *env, jclass clazz, jobjectArray name, jbooleanArray state) {
    RETURN_IF_NOT_RUNNING
    size_t nameLength = env->GetArrayLength(name);
    size_t stateLength = env->GetArrayLength(state);
    if (nameLength != stateLength) {
        jniLog("setFcitxAddonState: name and state length mismatch");
        return;
    }
    std::map<std::string, bool> map;
    const auto enabled = env->GetBooleanArrayElements(state, nullptr);
    for (size_t i = 0; i < nameLength; i++) {
        auto jName = reinterpret_cast<jstring>(env->GetObjectArrayElement(name, i));
        map.insert({jstringToString(env, jName), enabled[i]});
        env->DeleteLocalRef(jName);
    }
    env->ReleaseBooleanArrayElements(state, enabled, 0);
    Fcitx::Instance().setAddonState(map);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_triggerQuickPhraseInput(JNIEnv *env, jclass clazz) {
    Fcitx::Instance().triggerQuickPhrase();
}
