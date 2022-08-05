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
#include <fcitx/userinterfacemanager.h>
#include <fcitx/action.h>
#include <fcitx/statusarea.h>
#include <fcitx-utils/i18n.h>
#include <fcitx-utils/event.h>
#include <fcitx-utils/eventdispatcher.h>
#include <fcitx-utils/stringutils.h>

#include <quickphrase_public.h>
#include <punctuation_public.h>
#include <unicode_public.h>
#include <clipboard_public.h>

#include "androidfrontend/androidfrontend_public.h"
#include "jni-utils.h"
#include "nativestreambuf.h"
#include "helper-types.h"


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
        p_clipboard = addonMgr.addon("clipboard", true);
        p_uuid = p_frontend->call<fcitx::IAndroidFrontend::createInputContext>("fcitx5-android");
        setupCallback(p_frontend);
    }

    void reloadConfig() {
        p_instance->reloadConfig();
        p_instance->refresh();
        auto &addonManager = p_instance->addonManager();
        for (const auto category: {fcitx::AddonCategory::InputMethod,
                                   fcitx::AddonCategory::Frontend,
                                   fcitx::AddonCategory::Loader,
                                   fcitx::AddonCategory::Module,
                                   fcitx::AddonCategory::UI}) {
            const auto names = addonManager.addonNames(category);
            for (const auto &name: names) {
                p_instance->reloadAddonConfig(name);
            }
        }
    }

    void sendKey(fcitx::Key key, bool up = false) {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(p_uuid, key, up);
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
        for (const auto &ime: list) {
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
        for (const auto &e: entries) {
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

    std::unique_ptr<fcitx::RawConfig> getGlobalConfig() {
        const auto &configuration = p_instance->globalConfig().config();
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(&configuration));
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

    std::unique_ptr<fcitx::RawConfig> getAddonSubConfig(const std::string &addonName, const std::string &path) {
        const auto addonInstance = getAddonInstance(addonName);
        if (!addonInstance) {
            return nullptr;
        }
        const auto configuration = addonInstance->getSubConfig(path);
        if (!configuration) {
            return nullptr;
        }
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(configuration));
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
        for (const auto category: {fcitx::AddonCategory::InputMethod,
                                   fcitx::AddonCategory::Frontend,
                                   fcitx::AddonCategory::Loader,
                                   fcitx::AddonCategory::Module,
                                   fcitx::AddonCategory::UI}) {
            const auto names = addonManager.addonNames(category);
            for (const auto &name: names) {
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
        for (const auto &item: state) {
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

    std::pair<std::string, std::string> queryPunctuation(uint16_t unicode, const std::string &language) {
        if (!p_punctuation) {
            std::string s(1, static_cast<char>(unicode));
            return std::make_pair(s, s);
        }
        return p_punctuation->call<fcitx::IPunctuation::getPunctuation>(language, unicode);
    }

    void triggerUnicode() {
        if (!p_unicode) return;
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        p_unicode->call<fcitx::IUnicode::trigger>(ic);
    }

    void setClipboard(const std::string &string) {
        if (!p_clipboard) return;
        p_clipboard->call<fcitx::IClipboard::setClipboard>("", string);
    }

    void focusInputContext(bool focus) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::focusInputContext>(p_uuid, focus);
    }

    void setCapabilityFlags(uint64_t flags) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::setCapabilityFlags>(p_uuid, flags);
    }

    std::vector<ActionEntity> statusAreaActions() {
        auto actions = std::vector<ActionEntity>();
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        for (auto group: {fcitx::StatusGroup::BeforeInputMethod,
                          fcitx::StatusGroup::InputMethod,
                          fcitx::StatusGroup::AfterInputMethod}) {
            for (auto act: ic->statusArea().actions(group)) {
                actions.emplace_back(ActionEntity(act, ic));
            }
        }
        return actions;
    }

    void activateAction(int id) {
        auto *ic = p_instance->inputContextManager().findByUUID(p_uuid);
        auto action = p_instance->userInterfaceManager().lookupActionById(id);
        if (!action) return;
        action->activate(ic);
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
    fcitx::AddonInstance *p_clipboard = nullptr;
    fcitx::ICUUID p_uuid{};

    void resetGlobalPointers() {
        p_instance.reset();
        p_dispatcher.reset();
        p_frontend = nullptr;
        p_quickphrase = nullptr;
        p_punctuation = nullptr;
        p_unicode = nullptr;
        p_clipboard = nullptr;
        p_uuid = {};
    }
};


#define DO_IF_NOT_RUNNING(expr) \
    if (!Fcitx::Instance().isRunning()) { \
        FCITX_WARN() << "Fcitx is not running!"; \
        expr; \
    }
#define RETURN_IF_NOT_RUNNING DO_IF_NOT_RUNNING(return)
#define RETURN_VALUE_IF_NOT_RUNNING(v) DO_IF_NOT_RUNNING(return (v))

static GlobalRefSingleton *GlobalRef;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void * /* reserved */) {
    GlobalRef = new GlobalRefSingleton(jvm);
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}

typedef void (*log_callback_t)(const char *);

extern "C" void setup_log_stream(bool verbose, log_callback_t callback) {
    static native_streambuf log_streambuf;
    log_streambuf.set_callback(callback);
    static std::ostream stream(&log_streambuf);
    fcitx::Log::setLogStream(stream);
    if (verbose) {
        fcitx::Log::setLogRule("*=5,notimedate");
    } else {
        fcitx::Log::setLogRule("notimedate");
    }
}

jobject fcitxInputMethodEntryWithSubModeToJObject(JNIEnv *env, const fcitx::InputMethodEntry *entry, const std::vector<std::string> &subMode);

jobject fcitxActionToJObject(JNIEnv *env, const ActionEntity &act) {
    jobjectArray menu = nullptr;
    if (act.menu) {
        const int size = static_cast<int>(act.menu->size());
        menu = env->NewObjectArray(size, GlobalRef->Action, nullptr);
        for (int i = 0; i < size; i++) {
            env->SetObjectArrayElement(menu, i, fcitxActionToJObject(env, act.menu->at(i)));
        }
    }
    auto obj = env->NewObject(GlobalRef->Action, GlobalRef->ActionInit,
                              act.id,
                              act.isSeparator,
                              act.isCheckable,
                              act.isChecked,
                              *JString(env, act.name),
                              *JString(env, act.icon),
                              *JString(env, act.shortText),
                              *JString(env, act.longText),
                              menu
    );
    if (menu) {
        env->DeleteLocalRef(menu);
    }
    return obj;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_startupFcitx(JNIEnv *env, jclass clazz, jstring locale, jstring appData, jstring appLib, jstring extData) {
    if (Fcitx::Instance().isRunning()) {
        FCITX_ERROR() << "Fcitx is already running!";
        return;
    }
    FCITX_INFO() << "Starting...";

    setenv("SKIP_FCITX_PATH", "true", 1);

    auto locale_ = CString(env, locale);
    auto appData_ = CString(env, appData);
    auto appLib_ = CString(env, appLib);
    auto extData_ = CString(env, extData);

    std::string config_home = fcitx::stringutils::joinPath(*extData_, "config");
    std::string data_home = fcitx::stringutils::joinPath(*extData_, "data");
    std::string usr_share = fcitx::stringutils::joinPath(*appData_, "usr", "share");
    std::string locale_dir = fcitx::stringutils::joinPath(usr_share, "locale");
    std::string libime_data = fcitx::stringutils::joinPath(usr_share, "libime");

    setenv("LANGUAGE", locale_, 1);
    setenv("FCITX_LOCALE", locale_, 1);
    setenv("HOME", extData_, 1);
    setenv("XDG_DATA_DIRS", usr_share.c_str(), 1);
    setenv("FCITX_CONFIG_HOME", config_home.c_str(), 1);
    setenv("FCITX_DATA_HOME", data_home.c_str(), 1);
    setenv("FCITX_ADDON_DIRS", appLib_, 1);
    setenv("LIBIME_MODEL_DIRS", libime_data.c_str(), 1);

    const char *locale_dir_char = locale_dir.c_str();
    fcitx::registerDomain("fcitx5", locale_dir_char);
    fcitx::registerDomain("fcitx5-chinese-addons", locale_dir_char);
    fcitx::registerDomain("fcitx5-lua", locale_dir_char);
    fcitx::registerDomain("fcitx5-unikey", locale_dir_char);
    fcitx::registerDomain("fcitx5-android", locale_dir_char);

    auto candidateListCallback = [](const std::vector<std::string> &candidateList) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(static_cast<int>(candidateList.size()), GlobalRef->String, nullptr));
        int i = 0;
        for (const auto &s: candidateList) {
            env->SetObjectArrayElement(vararg, i++, JString(env, s));
        }
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 0, *vararg);
    };
    auto commitStringCallback = [](const std::string &str) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(1, GlobalRef->String, nullptr));
        env->SetObjectArrayElement(vararg, 0, JString(env, str));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 1, *vararg);
    };
    auto preeditCallback = [](const std::string &preedit, const int cursor, const std::string &clientPreedit, const int clientCursor) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(4, GlobalRef->Object, nullptr));
        env->SetObjectArrayElement(vararg, 0, JString(env, preedit));
        env->SetObjectArrayElement(vararg, 1, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, cursor));
        env->SetObjectArrayElement(vararg, 2, JString(env, clientPreedit));
        env->SetObjectArrayElement(vararg, 3, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, clientCursor));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 2, *vararg);
    };
    auto inputPanelAuxCallback = [](const std::string &auxUp, const std::string &auxDown) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(2, GlobalRef->String, nullptr));
        env->SetObjectArrayElement(vararg, 0, JString(env, auxUp));
        env->SetObjectArrayElement(vararg, 1, JString(env, auxDown));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 3, *vararg);
    };
    auto readyCallback = []() {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(0, GlobalRef->Object, nullptr));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 4, *vararg);
    };
    auto keyEventCallback = [](const uint32_t sym, const uint32_t states, const uint32_t unicode, const bool up) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(4, GlobalRef->Object, nullptr));
        env->SetObjectArrayElement(vararg, 0, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, sym));
        env->SetObjectArrayElement(vararg, 1, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, states));
        env->SetObjectArrayElement(vararg, 2, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, unicode));
        env->SetObjectArrayElement(vararg, 3, env->NewObject(GlobalRef->Boolean, GlobalRef->BooleanInit, up));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 5, *vararg);
    };
    auto imChangeCallback = []() {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(1, GlobalRef->Object, nullptr));
        const auto status = Fcitx::Instance().inputMethodStatus();
        auto obj = JRef(env, fcitxInputMethodEntryWithSubModeToJObject(env, std::get<0>(status), std::get<1>(status)));
        env->SetObjectArrayElement(vararg, 0, obj);
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 6, *vararg);
    };
    auto statusAreaUpdateCallback = []() {
        auto env = GlobalRef->AttachEnv();
        const auto actions = Fcitx::Instance().statusAreaActions();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(static_cast<int>(actions.size()), GlobalRef->Action, nullptr));
        int i = 0;
        for (const auto &a: actions) {
            auto obj = JRef(env, fcitxActionToJObject(env, a));
            env->SetObjectArrayElement(vararg, i++, obj);
        }
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 7, *vararg);
    };

    Fcitx::Instance().startup([&](auto *androidfrontend) {
        FCITX_INFO() << "Setting up callback";
        readyCallback();
        androidfrontend->template call<fcitx::IAndroidFrontend::setCandidateListCallback>(candidateListCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setCommitStringCallback>(commitStringCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setPreeditCallback>(preeditCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setInputPanelAuxCallback>(inputPanelAuxCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setKeyEventCallback>(keyEventCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setInputMethodChangeCallback>(imChangeCallback);
        androidfrontend->template call<fcitx::IAndroidFrontend::setStatusAreaUpdateCallback>(statusAreaUpdateCallback);
    });
    FCITX_INFO() << "Finishing startup";
}

extern "C"
JNIEXPORT jstring JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxTranslation(JNIEnv *env, jclass clazz, jstring domain, jstring str) {
    const char *t = fcitx::translateDomain(*CString(env, domain), *CString(env, str));
    return env->NewStringUTF(t);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_exitFcitx(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().exit();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_saveFcitxState(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().save();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_reloadFcitxConfig(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().reloadConfig();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_sendKeyToFcitxString(JNIEnv *env, jclass clazz, jstring key, jint state, jboolean up) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey{fcitx::Key::keySymFromString(CString(env, key)),
                         fcitx::KeyState(static_cast<uint32_t>(state))};
    Fcitx::Instance().sendKey(parsedKey);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_sendKeyToFcitxChar(JNIEnv *env, jclass clazz, jchar c, jint state, jboolean up) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey{fcitx::Key::keySymFromString((const char *) &c),
                         fcitx::KeyState(static_cast<uint32_t>(state))};
    Fcitx::Instance().sendKey(parsedKey);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_sendKeySymToFcitx(JNIEnv *env, jclass clazz, jint sym, jint state, jboolean up) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key key{fcitx::KeySym(static_cast<uint32_t>(sym)),
                   fcitx::KeyState(static_cast<uint32_t>(state))};
    Fcitx::Instance().sendKey(key, up);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_selectCandidate(JNIEnv *env, jclass clazz, jint idx) {
    RETURN_IF_NOT_RUNNING
    FCITX_DEBUG() << "selectCandidate: #" << idx;
    Fcitx::Instance().select(idx);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_isInputPanelEmpty(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(true)
    return Fcitx::Instance().isInputPanelEmpty();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_resetInputContext(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().resetInputContext();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_repositionCursor(JNIEnv *env, jclass clazz, jint position) {
    FCITX_DEBUG() << "repositionCursor: to " << position;
    Fcitx::Instance().repositionCursor(position);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_nextInputMethod(JNIEnv *env, jclass clazz, jboolean forward) {
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
    jobjectArray array = env->NewObjectArray(static_cast<int>(entries.size()), GlobalRef->InputMethodEntry, nullptr);
    int i = 0;
    for (const auto &entry: entries) {
        auto obj = JRef(env, fcitxInputMethodEntryToJObject(env, entry));
        env->SetObjectArrayElement(array, i++, obj);
    }
    return array;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_listInputMethods(JNIEnv *env, jclass clazz) {
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
Java_org_fcitx_fcitx5_android_core_Fcitx_inputMethodStatus(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto &status = Fcitx::Instance().inputMethodStatus();
    return fcitxInputMethodEntryWithSubModeToJObject(env, std::get<0>(status), std::get<1>(status));
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setInputMethod(JNIEnv *env, jclass clazz, jstring ime) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().setInputMethod(CString(env, ime));
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_availableInputMethods(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto entries = Fcitx::Instance().availableInputMethods();
    return fcitxInputMethodEntriesToJObjectArray(env, entries);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setEnabledInputMethods(JNIEnv *env, jclass clazz, jobjectArray array) {
    RETURN_IF_NOT_RUNNING
    int size = env->GetArrayLength(array);
    std::vector<std::string> entries;
    for (int i = 0; i < size; i++) {
        auto string = JRef<jstring>(env, env->GetObjectArrayElement(array, i));
        entries.emplace_back(CString(env, string));
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
    auto array = JRef<jobjectArray>(env, env->NewObjectArray(static_cast<int>(cfg.subItemsSize()), GlobalRef->RawConfig, nullptr));
    int i = 0;
    for (const auto &item: cfg.subItems()) {
        auto jItem = JRef(env, fcitxRawConfigToJObject(env, *cfg.get(item)));
        env->SetObjectArrayElement(array, i++, jItem);
    }
    env->CallVoidMethod(obj, GlobalRef->RawConfigSetSubItems, *array);
    return obj;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxGlobalConfig(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto cfg = Fcitx::Instance().getGlobalConfig();
    return fcitxRawConfigToJObject(env, *cfg);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxAddonConfig(JNIEnv *env, jclass clazz, jstring addon) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto result = Fcitx::Instance().getAddonConfig(CString(env, addon));
    return result ? fcitxRawConfigToJObject(env, *result) : nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxAddonSubConfig(JNIEnv *env, jclass clazz, jstring addon, jstring path) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto result = Fcitx::Instance().getAddonSubConfig(CString(env, addon), CString(env, path));
    return result ? fcitxRawConfigToJObject(env, *result) : nullptr;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxInputMethodConfig(JNIEnv *env, jclass clazz, jstring im) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto result = Fcitx::Instance().getInputMethodConfig(CString(env, im));
    return result ? fcitxRawConfigToJObject(env, *result) : nullptr;
}

void jobjectFillRawConfig(JNIEnv *env, jobject jConfig, fcitx::RawConfig &config) {
    auto subItems = JRef<jobjectArray>(env, env->GetObjectField(jConfig, GlobalRef->RawConfigSubItems));
    if (*subItems == nullptr) {
        auto value = JRef<jstring>(env, env->GetObjectField(jConfig, GlobalRef->RawConfigValue));
        config = CString(env, value);
    } else {
        int size = env->GetArrayLength(subItems);
        for (int i = 0; i < size; i++) {
            auto item = JRef(env, env->GetObjectArrayElement(subItems, i));
            auto name = JRef<jstring>(env, env->GetObjectField(item, GlobalRef->RawConfigName));
            auto subConfig = config.get(CString(env, name), true);
            jobjectFillRawConfig(env, item, *subConfig);
        }
    }
}

fcitx::RawConfig jobjectToRawConfig(JNIEnv *env, jobject jConfig) {
    fcitx::RawConfig config;
    jobjectFillRawConfig(env, jConfig, config);
    return config;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setFcitxGlobalConfig(JNIEnv *env, jclass clazz, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setGlobalConfig(rawConfig);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setFcitxAddonConfig(JNIEnv *env, jclass clazz, jstring addon, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setAddonConfig(CString(env, addon), rawConfig);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setFcitxAddonSubConfig(JNIEnv *env, jclass clazz, jstring addon, jstring path, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setAddonSubConfig(CString(env, addon), CString(env, path), rawConfig);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setFcitxInputMethodConfig(JNIEnv *env, jclass clazz, jstring im, jobject config) {
    RETURN_IF_NOT_RUNNING
    auto rawConfig = jobjectToRawConfig(env, config);
    Fcitx::Instance().setInputMethodConfig(CString(env, im), rawConfig);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxAddons(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto &addons = Fcitx::Instance().getAddons();
    jobjectArray array = env->NewObjectArray(static_cast<int>(addons.size()), GlobalRef->AddonInfo, nullptr);
    int i = 0;
    for (const auto addon: addons) {
        const auto *info = addon.first;
        auto obj = JRef(env, env->NewObject(GlobalRef->AddonInfo, GlobalRef->AddonInfoInit,
                                            *JString(env, info->uniqueName()),
                                            *JString(env, info->name().match()),
                                            *JString(env, info->comment().match()),
                                            static_cast<int32_t>(info->category()),
                                            info->isConfigurable(),
                                            addon.second,
                                            info->onDemand()
        ));
        env->SetObjectArrayElement(array, i++, obj);
    }
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setFcitxAddonState(JNIEnv *env, jclass clazz, jobjectArray name, jbooleanArray state) {
    RETURN_IF_NOT_RUNNING
    int nameLength = env->GetArrayLength(name);
    int stateLength = env->GetArrayLength(state);
    if (nameLength != stateLength) {
        FCITX_WARN() << "Addon name and state length mismatch!";
        return;
    }
    std::map<std::string, bool> map;
    const auto enabled = env->GetBooleanArrayElements(state, nullptr);
    for (int i = 0; i < nameLength; i++) {
        auto jName = JRef<jstring>(env, env->GetObjectArrayElement(name, i));
        map.insert({CString(env, jName), enabled[i]});
    }
    env->ReleaseBooleanArrayElements(state, enabled, 0);
    Fcitx::Instance().setAddonState(map);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_triggerQuickPhraseInput(JNIEnv *env, jclass clazz) {
    Fcitx::Instance().triggerQuickPhrase();
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_queryPunctuation(JNIEnv *env, jclass clazz, jchar c, jstring language) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto pair = Fcitx::Instance().queryPunctuation(c, CString(env, language));
    jobjectArray array = env->NewObjectArray(2, GlobalRef->String, nullptr);
    env->SetObjectArrayElement(array, 0, JString(env, pair.first));
    env->SetObjectArrayElement(array, 1, JString(env, pair.second));
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_triggerUnicodeInput(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().triggerUnicode();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setFcitxClipboard(JNIEnv *env, jclass clazz, jstring string) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().setClipboard(CString(env, string));
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_focusInputContext(JNIEnv *env, jclass clazz, jboolean focus) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().focusInputContext(focus == JNI_TRUE);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setCapabilityFlags(JNIEnv *env, jclass clazz, jlong flags) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().setCapabilityFlags(static_cast<uint64_t>(flags));
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxStatusAreaActions(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto actions = Fcitx::Instance().statusAreaActions();
    jobjectArray array = env->NewObjectArray(static_cast<int>(actions.size()), GlobalRef->Action, nullptr);
    for (int i = 0; i < actions.size(); i++) {
        auto obj = JRef(env, fcitxActionToJObject(env, actions[i]));
        env->SetObjectArrayElement(array, i, obj);
    }
    return array;
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_activateUserInterfaceAction(JNIEnv *env, jclass clazz, jint id) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().activateAction(static_cast<int>(id));
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_loopOnce(JNIEnv *env, jclass clazz) {
    Fcitx::Instance().loopOnce();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_scheduleEmpty(JNIEnv *env, jclass clazz) {
    Fcitx::Instance().scheduleEmpty();
}