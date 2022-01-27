#include <jni.h>
#include <memory>
#include <future>
#include <android/log.h>

#include <event2/event.h>

#include <fcitx/instance.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodentry.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/inputmethodmanager.h>
#include <fcitx-utils/i18n.h>
#include <fcitx-utils/event.h>
#include <fcitx-utils/eventdispatcher.h>
#include <fcitx-utils/stringutils.h>

#include <quickphrase_public.h>
#include <punctuation_public.h>
#include <unicode_public.h>

#include "androidfrontend/androidfrontend_public.h"
#include "androidstreambuf.h"
#include "jni-utils.h"

static void jniLog(const std::string &s) {
    __android_log_write(ANDROID_LOG_DEBUG, "JNI", s.c_str());
}

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

    event_base *get_event_base() {
        fcitx::EventLoop &event_loop = p_instance->eventLoop();
        return static_cast<event_base *>(event_loop.nativeHandle());
    }

    int loopOnce() {
        return event_base_loop(get_event_base(), EVLOOP_ONCE);
    }

    void startup(const std::function<void(fcitx::AddonInstance *)> &setupCallback) {
        char arg0[] = "";
        char *argv[] = {arg0};
        p_instance = std::make_unique<fcitx::Instance>(FCITX_ARRAY_SIZE(argv), argv);
        p_instance->addonManager().registerDefaultLoader(nullptr);
        p_dispatcher = std::make_unique<fcitx::EventDispatcher>();
        p_dispatcher->attach(&p_instance->eventLoop());
        p_instance->initialize();
        auto &addonMgr = p_instance->addonManager();
        p_frontend = addonMgr.addon("androidfrontend");
        p_quickphrase = addonMgr.addon("quickphrase");
        p_punctuation = addonMgr.addon("punctuation", true);
        p_unicode = addonMgr.addon("unicode");
        p_uuid = p_frontend->call<fcitx::IAndroidFrontend::createInputContext>("fcitx5-android");
        setupCallback(p_frontend);
    }

    void sendKey(fcitx::Key key) {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, key, false);
    }

    void select(int idx) {
        p_frontend->call<fcitx::IAndroidFrontend::selectCandidate>(p_uuid, idx);
    }

    bool isInputPanelEmpty() {
        return p_frontend->call<fcitx::IAndroidFrontend::isInputPanelEmpty>(p_uuid);
    }

    void resetInputContext() {
        p_frontend->call<fcitx::IAndroidFrontend::resetInputContext>(p_uuid);
    }

    void repositionCursor(int position) {
        p_frontend->call<fcitx::IAndroidFrontend::repositionCursor>(p_uuid, position);
    }

    void nextInputMethod(bool forward) {
        p_instance->enumerate(forward);
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

    typedef std::tuple<const fcitx::InputMethodEntry *, const std::vector<std::string>> IMStatus;

    IMStatus inputMethodStatus() {
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        auto *engine = p_instance->inputMethodEngine(ic);
        const auto *entry = p_instance->inputMethodEntry(ic);
        if (engine) {
            auto subMode = engine->subMode(*entry, *ic);
            auto subModeLabel = engine->subModeLabel(*entry, *ic);
            auto subModeIcon = engine->subModeIcon(*entry, *ic);
            return std::make_tuple(entry, std::vector{subMode, subModeLabel, subModeIcon});
        } else if (entry) {
            return std::make_tuple(entry, std::vector<std::string>{});
        }
        return std::make_tuple(nullptr, std::vector<std::string>{});
    }

    void setInputMethod(const std::string &ime) {
        p_instance->setCurrentInputMethod(ime);
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
        auto &imMgr = p_instance->inputMethodManager();
        fcitx::InputMethodGroup newGroup(imMgr.currentGroup().name());
        newGroup.setDefaultLayout("us");
        auto &list = newGroup.inputMethodList();
        for (const auto &e : entries) {
            list.emplace_back(e);
        }
        imMgr.setGroup(std::move(newGroup));
        imMgr.save();
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

    std::unique_ptr<fcitx::RawConfig> getAddonConfig(const std::string &addonName) {
        const auto addonInstance = getAddonInstance(addonName);
        if (!addonInstance) {
            return nullptr;
        }
        const auto configuration = addonInstance->getConfig();
        if (!configuration) {
            return nullptr;
        }
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(configuration));
    }

    void setAddonConfig(const std::string &addonName, const fcitx::RawConfig &config) {
        auto addonInstance = getAddonInstance(addonName);
        if (!addonInstance) {
            return;
        }
        addonInstance->setConfig(config);
    }

    void setAddonSubConfig(const std::string &addonName, const std::string &path, const fcitx::RawConfig &config) {
        auto addonInstance = getAddonInstance(addonName);
        if (!addonInstance) {
            return;
        }
        addonInstance->setSubConfig(path, config);
    }

    std::unique_ptr<fcitx::RawConfig> getInputMethodConfig(const std::string &imName) {
        const auto *entry = p_instance->inputMethodManager().entry(imName);
        if (!entry || !entry->isConfigurable()) {
            return nullptr;
        }
        const auto *engine = p_instance->inputMethodEngine(imName);
        if (!engine) {
            return nullptr;
        }
        const auto configuration = engine->getConfigForInputMethod(*entry);
        if (!configuration) {
            return nullptr;
        }
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(configuration));
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
        globalConfig.setEnabledAddons({enabledSet.begin(), enabledSet.end()});
        globalConfig.setDisabledAddons({disabledSet.begin(), disabledSet.end()});
        globalConfig.safeSave();
        p_instance->reloadConfig();
    }

    void triggerQuickPhrase() {
        if (!p_quickphrase) return;
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        p_quickphrase->call<fcitx::IQuickPhrase::trigger>(
                ic, "", "", "", "", fcitx::Key{FcitxKey_None}
        );
    }

    std::pair<std::string, std::string> queryPunctuation(uint32_t unicode, const std::string &language) {
        if (!p_punctuation) {
            std::string s(1, unicode);
            return std::make_pair(s, s);
        }
        return p_punctuation->call<fcitx::IPunctuation::getPunctuation>(language, unicode);
    }

    void triggerUnicode() {
        if (!p_unicode) return;
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        p_unicode->call<fcitx::IUnicode::trigger>(ic);
    }

    void focusInputContext(bool focus) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::focusInputContext>(p_uuid, focus);
    }

    void setCapabilityFlags(uint64_t flags) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::setCapabilityFlags>(p_uuid, flags);
    }

    void save() {
        p_instance->save();
    }

    void exit() {
        p_dispatcher->detach();
        resetGlobalPointers();
    }

    void scheduleEmpty() {
        p_dispatcher->schedule(nullptr);
    }

private:
    std::unique_ptr<fcitx::Instance> p_instance;
    std::unique_ptr<fcitx::EventDispatcher> p_dispatcher;
    fcitx::AddonInstance *p_frontend = nullptr;
    fcitx::AddonInstance *p_quickphrase = nullptr;
    fcitx::AddonInstance *p_punctuation = nullptr;
    fcitx::AddonInstance *p_unicode = nullptr;
    fcitx::ICUUID p_uuid{};

    void resetGlobalPointers() {
        p_instance.reset();
        p_dispatcher.reset();
        p_frontend = nullptr;
        p_quickphrase = nullptr;
        p_punctuation = nullptr;
        p_unicode = nullptr;
        p_uuid = {};
    }
};


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

static GlobalRefSingleton *GlobalRef;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void * /* reserved */) {
    GlobalRef = new GlobalRefSingleton(jvm);
    static std::ostream stream(new AndroidStreamBuf("fcitx5", 512));
    fcitx::Log::setLogStream(stream);
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}

jobject fcitxInputMethodEntryWithSubModeToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry, const std::vector<std::string> &subMode);

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_startupFcitx(JNIEnv *env, jclass clazz, jstring locale, jstring appData, jstring appLib, jstring extData) {
    if (Fcitx::Instance().isRunning()) {
        jniLog("startupFcitx: already running!");
        return;
    }
    jniLog("startupFcitx: starting...");

    setenv("SKIP_FCITX_PATH", "true", 1);

    const char *locale_char = env->GetStringUTFChars(locale, nullptr);
    const char *app_data = env->GetStringUTFChars(appData, nullptr);
    const char *app_lib = env->GetStringUTFChars(appLib, nullptr);
    const char *ext_data = env->GetStringUTFChars(extData, nullptr);
    std::string lang = fcitx::stringutils::split(locale_char, ":")[0];
    std::string config_home = fcitx::stringutils::joinPath(ext_data, "config");
    std::string data_home = fcitx::stringutils::joinPath(ext_data, "data");
    std::string usr_share = fcitx::stringutils::joinPath(app_data, "usr", "share");
    std::string locale_dir = fcitx::stringutils::joinPath(usr_share, "locale");
    const char *locale_dir_char = locale_dir.c_str();
    std::string libime_data = fcitx::stringutils::joinPath(usr_share, "libime");

    setenv("LANG", lang.c_str(), 1);
    setenv("LANGUAGE", locale_char, 1);
    setenv("FCITX_LOCALE", locale_char, 1);
    setenv("HOME", ext_data, 1);
    setenv("XDG_DATA_DIRS", usr_share.c_str(), 1);
    setenv("FCITX_CONFIG_HOME", config_home.c_str(), 1);
    setenv("FCITX_DATA_HOME", data_home.c_str(), 1);
    setenv("FCITX_ADDON_DIRS", app_lib, 1);
    setenv("LIBIME_MODEL_DIRS", libime_data.c_str(), 1);

    fcitx::registerDomain("fcitx5", locale_dir_char);
    fcitx::registerDomain("fcitx5-chinese-addons", locale_dir_char);

    env->ReleaseStringUTFChars(locale, locale_char);
    env->ReleaseStringUTFChars(appData, app_data);
    env->ReleaseStringUTFChars(appLib, app_lib);
    env->ReleaseStringUTFChars(extData, ext_data);

    auto candidateListCallback = [](const std::vector<std::string> &candidateList) {
        auto env = GlobalRef->AttachEnv();
        size_t size = candidateList.size();
        jobjectArray vararg = env->NewObjectArray(size, GlobalRef->String, nullptr);
        size_t i = 0;
        for (const auto &s : candidateList) {
            env->SetObjectArrayElement(vararg, i++, JString(env, s));
        }
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 0, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto commitStringCallback = [](const std::string &str) {
        auto env = GlobalRef->AttachEnv();
        jobjectArray vararg = env->NewObjectArray(1, GlobalRef->String, nullptr);
        env->SetObjectArrayElement(vararg, 0, JString(env, str));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 1, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto preeditCallback = [](const std::string &preedit, const std::string &clientPreedit, const int cursor) {
        auto env = GlobalRef->AttachEnv();
        jobjectArray vararg = env->NewObjectArray(3, GlobalRef->Object, nullptr);
        env->SetObjectArrayElement(vararg, 0, JString(env, preedit));
        env->SetObjectArrayElement(vararg, 1, JString(env, clientPreedit));
        env->SetObjectArrayElement(vararg, 2, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, cursor));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 2, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto inputPanelAuxCallback = [](const std::string &auxUp, const std::string &auxDown) {
        auto env = GlobalRef->AttachEnv();
        jobjectArray vararg = env->NewObjectArray(2, GlobalRef->String, nullptr);
        env->SetObjectArrayElement(vararg, 0, JString(env, auxUp));
        env->SetObjectArrayElement(vararg, 1, JString(env, auxDown));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 3, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto readyCallback = []() {
        auto env = GlobalRef->AttachEnv();
        jobjectArray vararg = env->NewObjectArray(0, GlobalRef->Object, nullptr);
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 4, vararg);
        env->DeleteLocalRef(vararg);
    };
    auto keyEventCallback = [](const int code, const std::string &sym) {
        auto env = GlobalRef->AttachEnv();
        jobjectArray vararg = env->NewObjectArray(2, GlobalRef->Object, nullptr);
        auto integer = env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, code);
        env->SetObjectArrayElement(vararg, 0, integer);
        env->SetObjectArrayElement(vararg, 1, JString(env, sym));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 5, vararg);
        env->DeleteLocalRef(vararg);
        env->DeleteLocalRef(integer);
    };
    auto imChangeCallback = []() {
        auto env = GlobalRef->AttachEnv();
        jobjectArray vararg = env->NewObjectArray(1, GlobalRef->Object, nullptr);
        const auto status = Fcitx::Instance().inputMethodStatus();
        auto obj = fcitxInputMethodEntryWithSubModeToJObject(env, std::get<0>(status), std::get<1>(status));
        env->SetObjectArrayElement(vararg, 0, obj);
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 6, vararg);
        env->DeleteLocalRef(vararg);
        env->DeleteLocalRef(obj);
    };

    Fcitx::Instance().startup([&](auto *androidfrontend) {
        jniLog("startupFcitx: setupCallback");
        readyCallback();
        androidfrontend->template call<fcitx::IAndroidFrontend::setCandidateListCallback>(candidateListCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setCommitStringCallback>(commitStringCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setPreeditCallback>(preeditCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setInputPanelAuxCallback>(inputPanelAuxCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setKeyEventCallback>(keyEventCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setInputMethodChangeCallback>(imChangeCallback);
    });
    jniLog("startupFcitx done");
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_exitFcitx(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().exit();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_saveFcitxState(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().save();
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
Java_me_rocka_fcitx5test_native_Fcitx_sendKeyToFcitxInt(JNIEnv *env, jclass clazz, jint i) {
    RETURN_IF_NOT_RUNNING
    auto key = fcitx::Key::fromKeyCode(i);
    Fcitx::Instance().sendKey(key);
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
Java_me_rocka_fcitx5test_native_Fcitx_resetInputContext(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().resetInputContext();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_repositionCursor(JNIEnv *env, jclass clazz, jint position) {
    jniLog("repositionCursor: to " + std::to_string(position));
    Fcitx::Instance().repositionCursor(position);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_nextInputMethod(JNIEnv *env, jclass clazz, jboolean forward) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().nextInputMethod(forward == JNI_TRUE);
}

jobject fcitxInputMethodEntryToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry) {
    return env->NewObject(GlobalRef->InputMethodEntry, GlobalRef->InputMethodEntryInit,
                          *JString(env, entry->uniqueName()),
                          *JString(env, entry->name()),
                          *JString(env, entry->icon()),
                          *JString(env, entry->nativeName()),
                          *JString(env, entry->label()),
                          *JString(env, entry->languageCode()),
                          entry->isConfigurable()
    );
}

jobjectArray fcitxInputMethodEntriesToJObjectArray(JNIEnv *env, const std::vector<const fcitx::InputMethodEntry *> &entries) {
    jobjectArray array = env->NewObjectArray(entries.size(), GlobalRef->InputMethodEntry, nullptr);
    size_t i = 0;
    for (const auto &entry : entries) {
        jobject obj = fcitxInputMethodEntryToJObject(env, entry);
        env->SetObjectArrayElement(array, i++, obj);
        env->DeleteLocalRef(obj);
    }
    return array;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_listInputMethods(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto entries = Fcitx::Instance().listInputMethods();
    return fcitxInputMethodEntriesToJObjectArray(env, entries);
}

jobject fcitxInputMethodEntryWithSubModeToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry, const std::vector<std::string> &subMode) {
    if (!entry) return nullptr;
    if (subMode.empty()) return fcitxInputMethodEntryToJObject(env, entry);
    return env->NewObject(GlobalRef->InputMethodEntry, GlobalRef->InputMethodEntryInitWithSubMode,
                          *JString(env, entry->uniqueName()),
                          *JString(env, entry->name()),
                          *JString(env, entry->icon()),
                          *JString(env, entry->nativeName()),
                          *JString(env, entry->label()),
                          *JString(env, entry->languageCode()),
                          entry->isConfigurable(),
                          *JString(env, subMode[0]),
                          *JString(env, subMode[1]),
                          *JString(env, subMode[2])
    );
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_inputMethodStatus(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto &status = Fcitx::Instance().inputMethodStatus();
    return fcitxInputMethodEntryWithSubModeToJObject(env, std::get<0>(status), std::get<1>(status));
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
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto entries = Fcitx::Instance().availableInputMethods();
    return fcitxInputMethodEntriesToJObjectArray(env, entries);
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

jobject fcitxRawConfigToJObject(JNIEnv *env, const fcitx::RawConfig &cfg) {
    jobject obj = env->NewObject(GlobalRef->RawConfig, GlobalRef->RawConfigInit,
                                 *JString(env, cfg.name()),
                                 *JString(env, cfg.comment()),
                                 *JString(env, cfg.value()),
                                 nullptr);
    if (!cfg.hasSubItems()) {
        return obj;
    }
    jobjectArray array = env->NewObjectArray(cfg.subItemsSize(), GlobalRef->RawConfig, nullptr);
    size_t i = 0;
    for (const auto &item : cfg.subItems()) {
        jobject jItem = fcitxRawConfigToJObject(env, *cfg.get(item));
        env->SetObjectArrayElement(array, i++, jItem);
        env->DeleteLocalRef(jItem);
    }
    env->CallVoidMethod(obj, GlobalRef->RawConfigSetSubItems, array);
    env->DeleteLocalRef(array);
    return obj;
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
    return result ? fcitxRawConfigToJObject(env, *result) : nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_getFcitxInputMethodConfig(JNIEnv *env, jclass clazz, jstring im) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto result = Fcitx::Instance().getInputMethodConfig(jstringToString(env, im));
    return result ? fcitxRawConfigToJObject(env, *result) : nullptr;
}

void jobjectFillRawConfig(JNIEnv *env, jobject jConfig, fcitx::RawConfig &config) {
    auto subItems = reinterpret_cast<jobjectArray>(env->GetObjectField(jConfig, GlobalRef->RawConfigSubItems));
    if (subItems == nullptr) {
        auto jValue = reinterpret_cast<jstring>(env->GetObjectField(jConfig, GlobalRef->RawConfigValue));
        config = jstringToString(env, jValue);
        env->DeleteLocalRef(jValue);
    } else {
        size_t size = env->GetArrayLength(subItems);
        for (size_t i = 0; i < size; i++) {
            jobject item = env->GetObjectArrayElement(subItems, i);
            auto jName = reinterpret_cast<jstring>(env->GetObjectField(item, GlobalRef->RawConfigName));
            auto name = jstringToString(env, jName);
            auto subConfig = config.get(name, true);
            jobjectFillRawConfig(env, item, *subConfig);
            env->DeleteLocalRef(jName);
            env->DeleteLocalRef(item);
        }
    }
    env->DeleteLocalRef(subItems);
}

fcitx::RawConfig jobjectToRawConfig(JNIEnv *env, jobject jConfig) {
    fcitx::RawConfig config;
    jobjectFillRawConfig(env, jConfig, config);
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
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto &addons = Fcitx::Instance().getAddons();
    jobjectArray array = env->NewObjectArray(addons.size(), GlobalRef->AddonInfo, nullptr);
    size_t i = 0;
    for (const auto addon : addons) {
        const auto *info = addon.first;
        jobject obj = env->NewObject(GlobalRef->AddonInfo, GlobalRef->AddonInfoInit,
                                     *JString(env, info->uniqueName()),
                                     *JString(env, info->name().match()),
                                     *JString(env, info->comment().match()),
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

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_queryPunctuation(JNIEnv *env, jclass clazz, jchar c, jstring language) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto pair = Fcitx::Instance().queryPunctuation(c, jstringToString(env, language));
    jobjectArray array = env->NewObjectArray(2, GlobalRef->String, nullptr);
    env->SetObjectArrayElement(array, 0, JString(env, pair.first));
    env->SetObjectArrayElement(array, 1, JString(env, pair.second));
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_triggerUnicodeInput(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().triggerUnicode();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_focusInputContext(JNIEnv *env, jclass clazz, jboolean focus) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().focusInputContext(focus == JNI_TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setCapabilityFlags(JNIEnv *env, jclass clazz, jlong flags) {
    RETURN_IF_NOT_RUNNING
    uint64_t u;
    std::memcpy(&u, &flags, sizeof(uint64_t));
    Fcitx::Instance().setCapabilityFlags(u);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_setAddonSubConfig(JNIEnv *env, jclass clazz, jstring addon, jstring path, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setAddonSubConfig(jstringToString(env, addon), jstringToString(env, path), rawConfig);
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_loopOnce(JNIEnv *env, jclass clazz) {
    Fcitx::Instance().loopOnce();
}

extern "C"
JNIEXPORT void JNICALL
Java_me_rocka_fcitx5test_native_Fcitx_scheduleEmpty(JNIEnv *env, jclass clazz) {
    Fcitx::Instance().scheduleEmpty();
}