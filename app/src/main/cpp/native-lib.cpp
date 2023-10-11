#include <jni.h>

#include <sys/stat.h>

#include <memory>
#include <future>
#include <fstream>

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
#include <fcitx-utils/standardpath.h>
#include <fcitx-utils/stringutils.h>

#include <quickphrase_public.h>
#include <unicode_public.h>
#include <clipboard_public.h>

#include <libime/pinyin/pinyindictionary.h>
#include <libime/table/tablebaseddictionary.h>

#include "androidfrontend/androidfrontend_public.h"
#include "jni-utils.h"
#include "nativestreambuf.h"
#include "helper-types.h"
#include "object-conversion.h"


class Fcitx {
public:
    Fcitx() = default;
    Fcitx(Fcitx const &) = delete;
    void operator=(Fcitx const &) = delete;

    static Fcitx &Instance() {
        static Fcitx instance;
        return instance;
    }

    static void setLogStream(std::ostream &stream, bool verbose) {
        fcitx::Log::setLogStream(stream);
        if (verbose) {
            fcitx::Log::setLogRule("*=5,notimedate");
        } else {
            fcitx::Log::setLogRule("notimedate");
        }
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
        p_unicode = addonMgr.addon("unicode");
        p_clipboard = addonMgr.addon("clipboard", true);
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

    void sendKey(fcitx::Key key, bool up, int timestamp) {
        p_frontend->call<fcitx::IAndroidFrontend::keyEvent>(key, up, timestamp);
    }

    bool select(int idx) {
        return p_frontend->call<fcitx::IAndroidFrontend::selectCandidate>(idx);
    }

    bool isInputPanelEmpty() {
        return p_frontend->call<fcitx::IAndroidFrontend::isInputPanelEmpty>();
    }

    void resetInputContext() {
        p_frontend->call<fcitx::IAndroidFrontend::resetInputContext>();
    }

    void repositionCursor(int position) {
        p_frontend->call<fcitx::IAndroidFrontend::repositionCursor>(position);
    }

    void toggleInputMethod() {
        p_instance->toggle();
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
        return entries;
    }

    InputMethodStatus inputMethodStatus() {
        auto *ic = p_frontend->call<fcitx::IAndroidFrontend::activeInputContext>();
        auto *engine = p_instance->inputMethodEngine(ic);
        const auto *entry = p_instance->inputMethodEntry(ic);
        if (engine) {
            return {entry, engine, ic};
        }
        return {entry};
    }

    void setInputMethod(const std::string &ime) {
        auto *ic = p_frontend->call<fcitx::IAndroidFrontend::activeInputContext>();
        if (!ic) return;
        // this method remembers input method for each InputContext,
        // while Instance::setCurrentInputMethod(std::string) doesn't
        p_instance->setCurrentInputMethod(ic, ime, true);
    }

    std::vector<const fcitx::InputMethodEntry *> availableInputMethods() {
        std::vector<const fcitx::InputMethodEntry *> entries;
        p_instance->inputMethodManager().foreachEntries([&](const auto &entry) {
            entries.emplace_back(&entry);
            return true;
        });
        return entries;
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

    static fcitx::RawConfig mergeConfigDesc(const fcitx::Configuration &conf) {
        fcitx::RawConfig topLevel;
        auto cfg = topLevel.get("cfg", true);
        conf.save(*cfg);
        auto desc = topLevel.get("desc", true);
        conf.dumpDescription(*desc);
        return topLevel;
    }

    std::unique_ptr<fcitx::RawConfig> getGlobalConfig() {
        const auto &configuration = p_instance->globalConfig().config();
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(configuration));
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
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(*configuration));
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
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(*configuration));
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
        return std::make_unique<fcitx::RawConfig>(mergeConfigDesc(*configuration));
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

    std::vector<AddonStatus> getAddons() {
        auto &globalConfig = p_instance->globalConfig();
        auto &addonManager = p_instance->addonManager();
        const auto &enabledAddons = globalConfig.enabledAddons();
        std::unordered_set<std::string> enabledSet(enabledAddons.begin(), enabledAddons.end());
        const auto &disabledAddons = globalConfig.disabledAddons();
        std::unordered_set<std::string>
                disabledSet(disabledAddons.begin(), disabledAddons.end());
        std::vector<AddonStatus> addons;
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
                addons.emplace_back(AddonStatus(info, enabled));
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
        auto *ic = p_frontend->call<fcitx::IAndroidFrontend::activeInputContext>();
        if (!ic) return;
        p_quickphrase->call<fcitx::IQuickPhrase::trigger>(
                ic, "", "", "", "", fcitx::Key{FcitxKey_None}
        );
    }

    void triggerUnicode() {
        if (!p_unicode) return;
        auto *ic = p_frontend->call<fcitx::IAndroidFrontend::activeInputContext>();
        if (!ic) return;
        p_unicode->call<fcitx::IUnicode::trigger>(ic);
    }

    void setClipboard(const std::string &string) {
        if (!p_clipboard) return;
        p_clipboard->call<fcitx::IClipboard::setClipboard>("", string);
    }

    void focusInputContext(bool focus) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::focusInputContext>(focus);
    }

    void activateInputContext(int uid, const std::string &pkgName) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::activateInputContext>(uid, pkgName);
    }

    void deactivateInputContext(int uid) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::deactivateInputContext>(uid);
    }

    void setCapabilityFlags(uint64_t flags) {
        if (!p_frontend) return;
        p_frontend->call<fcitx::IAndroidFrontend::setCapabilityFlags>(flags);
    }

    std::vector<ActionEntity> statusAreaActions() {
        auto actions = std::vector<ActionEntity>();
        auto *ic = p_frontend->call<fcitx::IAndroidFrontend::activeInputContext>();
        if (!ic) return actions;
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
        auto *ic = p_frontend->call<fcitx::IAndroidFrontend::activeInputContext>();
        if (!ic) return;
        auto action = p_instance->userInterfaceManager().lookupActionById(id);
        if (!action) return;
        action->activate(ic);
    }

    std::vector<std::string> getCandidates(int offset, int limit) {
        return p_frontend->call<fcitx::IAndroidFrontend::getCandidates>(offset, limit);
    }

    void save() {
        p_instance->save();
    }

    void exit() {
        // Make sure that the exec doesn't get blocked
        event_base_loopexit(get_event_base(), nullptr);
        // Normally, we would use exec to drive the event loop.
        // Since we are calling loopOnce in JVM repeatedly, we shouldn't have used this function.
        // However, exit events would lose chance to be called in this case.
        // To fix that, we call exec on exit to execute exit events.
        p_instance->eventLoop().exec();
        p_dispatcher->detach();
        p_instance->exit();
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
    fcitx::AddonInstance *p_unicode = nullptr;
    fcitx::AddonInstance *p_clipboard = nullptr;

    void resetGlobalPointers() {
        p_instance.reset();
        p_dispatcher.reset();
        p_frontend = nullptr;
        p_quickphrase = nullptr;
        p_unicode = nullptr;
        p_clipboard = nullptr;
    }
};

#define DO_IF_NOT_RUNNING(expr) \
    if (!Fcitx::Instance().isRunning()) { \
        FCITX_WARN() << "Fcitx is not running!"; \
        expr; \
    }
#define RETURN_IF_NOT_RUNNING DO_IF_NOT_RUNNING(return)
#define RETURN_VALUE_IF_NOT_RUNNING(v) DO_IF_NOT_RUNNING(return (v))

GlobalRefSingleton *GlobalRef;

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *jvm, void * /* reserved */) {
    GlobalRef = new GlobalRefSingleton(jvm);
    // return supported JNI version; or it will crash
    return JNI_VERSION_1_6;
}

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wunused-parameter"

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_setupLogStream(JNIEnv *env, jclass clazz, jboolean verbose) {
    static native_streambuf log_streambuf;
    static std::ostream stream(&log_streambuf);
    Fcitx::setLogStream(stream, verbose);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_startupFcitx(JNIEnv *env, jclass clazz, jstring locale, jstring appData, jstring appLib, jstring extData, jstring extCache, jobjectArray extDomains) {
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
    auto extCache_ = CString(env, extCache);

    std::string lang_ = fcitx::stringutils::split(*locale_, ":")[0];
    std::string config_home = fcitx::stringutils::joinPath(*extData_, "config");
    std::string data_home = fcitx::stringutils::joinPath(*extData_, "data");
    std::string usr_share = fcitx::stringutils::joinPath(*appData_, "usr", "share");
    std::string locale_dir = fcitx::stringutils::joinPath(usr_share, "locale");
    std::string libime_data = fcitx::stringutils::joinPath(usr_share, "libime");
    std::string lua_path = fcitx::stringutils::concat(
            fcitx::stringutils::joinPath(data_home, "lua", "?.lua"), ";",
            fcitx::stringutils::joinPath(data_home, "lua", "?", "init.lua"), ";",
            fcitx::stringutils::joinPath(usr_share, "lua", "5.4", "?.lua"), ";",
            fcitx::stringutils::joinPath(usr_share, "lua", "5.4", "?", "init.lua"), ";",
            ";" // double semicolon, for default path defined in luaconf.h
    );
    std::string lua_cpath = fcitx::stringutils::concat(
            fcitx::stringutils::joinPath(data_home, "lua", "?.so"), ";",
            fcitx::stringutils::joinPath(usr_share, "lua", "5.4", "?.so"), ";",
            ";"
    );

    // for fcitx default profile [DefaultInputMethod]
    setenv("LANG", lang_.c_str(), 1);
    // for libintl-lite loading gettext .mo translations
    setenv("LANGUAGE", locale_, 1);
    // for fcitx i18nstring loading translations in .conf files
    setenv("FCITX_LOCALE", locale_, 1);
    setenv("HOME", extData_, 1);
    // system StandardPath::Type::Data
    setenv("XDG_DATA_DIRS", usr_share.c_str(), 1);
    // user StandardPath::Type::PkgConfig
    setenv("FCITX_CONFIG_HOME", config_home.c_str(), 1);
    // user StandardPath::Type::PkgData
    setenv("FCITX_DATA_HOME", data_home.c_str(), 1);
    // system StandardPath::Type::Addon
    setenv("FCITX_ADDON_DIRS", appLib_, 1);
    // libime language model dir
    setenv("LIBIME_MODEL_DIRS", libime_data.c_str(), 1);
    // user StandardPath::Type::Data
    setenv("XDG_DATA_HOME", data_home.c_str(), 1);
    // user StandardPath::Type::Cache
    setenv("XDG_CACHE_HOME", extCache_, 1);
    // user StandardPath::Type::Config
    setenv("XDG_CONFIG_HOME", config_home.c_str(), 1);
    // user StandardPath::Type::Runtime
    setenv("XDG_RUNTIME_DIR", extCache_, 1);
    setenv("LUA_PATH", lua_path.c_str(), 1);
    setenv("LUA_CPATH", lua_cpath.c_str(), 1);

    const char *locale_dir_char = locale_dir.c_str();
    fcitx::registerDomain("fcitx5", locale_dir_char);
    fcitx::registerDomain("fcitx5-lua", locale_dir_char);
    fcitx::registerDomain("fcitx5-chinese-addons", locale_dir_char);
    fcitx::registerDomain("fcitx5-android", locale_dir_char);

    int extDomainsSize = env->GetArrayLength(extDomains);
    for (int i = 0; i < extDomainsSize; i++) {
        auto domain = JRef<jstring>(env, env->GetObjectArrayElement(extDomains, i));
        fcitx::registerDomain(CString(env, domain), locale_dir_char);
    }

    auto candidateListCallback = [](const std::vector<std::string> &candidates, const int size) {
        auto env = GlobalRef->AttachEnv();
        auto candidatesArray = JRef<jobjectArray>(env, env->NewObjectArray(static_cast<int>(candidates.size()), GlobalRef->String, nullptr));
        int i = 0;
        for (const auto &s: candidates) {
            env->SetObjectArrayElement(candidatesArray, i++, JString(env, s));
        }
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(2, GlobalRef->Object, nullptr));
        auto candidatesCount = JRef(env, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, size));
        env->SetObjectArrayElement(vararg, 0, *candidatesCount);
        env->SetObjectArrayElement(vararg, 1, *candidatesArray);
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 0, *vararg);
    };
    auto commitStringCallback = [](const std::string &str, const int cursor) {
        auto env = GlobalRef->AttachEnv();
        auto stringCursor = JRef(env, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, cursor));
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(2, GlobalRef->Object, nullptr));
        env->SetObjectArrayElement(vararg, 0, JString(env, str));
        env->SetObjectArrayElement(vararg, 1, stringCursor);
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 1, *vararg);
    };
    auto preeditCallback = [](const fcitx::Text &clientPreedit) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(1, GlobalRef->FormattedText, nullptr));
        env->SetObjectArrayElement(vararg, 0, fcitxTextToJObject(env, clientPreedit));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 2, *vararg);
    };
    auto inputPanelAuxCallback = [](const fcitx::Text &preedit, const fcitx::Text &auxUp, const fcitx::Text &auxDown) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(3, GlobalRef->FormattedText, nullptr));
        env->SetObjectArrayElement(vararg, 0, fcitxTextToJObject(env, preedit));
        env->SetObjectArrayElement(vararg, 1, fcitxTextToJObject(env, auxUp));
        env->SetObjectArrayElement(vararg, 2, fcitxTextToJObject(env, auxDown));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 3, *vararg);
    };
    auto readyCallback = []() {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(0, GlobalRef->Object, nullptr));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 4, *vararg);
    };
    auto keyEventCallback = [](const int sym, const uint32_t states, const uint32_t unicode, const bool up, const int timestamp) {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(5, GlobalRef->Object, nullptr));
        env->SetObjectArrayElement(vararg, 0, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, sym));
        env->SetObjectArrayElement(vararg, 1, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, states));
        env->SetObjectArrayElement(vararg, 2, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, unicode));
        env->SetObjectArrayElement(vararg, 3, env->NewObject(GlobalRef->Boolean, GlobalRef->BooleanInit, up));
        env->SetObjectArrayElement(vararg, 4, env->NewObject(GlobalRef->Integer, GlobalRef->IntegerInit, timestamp));
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->HandleFcitxEvent, 5, *vararg);
    };
    auto imChangeCallback = []() {
        auto env = GlobalRef->AttachEnv();
        auto vararg = JRef<jobjectArray>(env, env->NewObjectArray(1, GlobalRef->Object, nullptr));
        const auto status = Fcitx::Instance().inputMethodStatus();
        auto obj = JRef(env, fcitxInputMethodStatusToJObject(env, status));
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
    auto toastCallback = [](const std::string &s) {
        auto env = GlobalRef->AttachEnv();
        env->CallStaticVoidMethod(GlobalRef->Fcitx, GlobalRef->ShowToast, *JString(env, s));
    };

    umask(007);
    fcitx::StandardPath::global().syncUmask();

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
        androidfrontend->template call<fcitx::IAndroidFrontend::setToastCallback>(toastCallback);
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
Java_org_fcitx_fcitx5_android_core_Fcitx_sendKeyToFcitxString(JNIEnv *env, jclass clazz, jstring key, jint state, jboolean up, jint timestamp) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey{fcitx::Key::keySymFromString(CString(env, key)),
                         fcitx::KeyStates(static_cast<uint32_t>(state))};
    Fcitx::Instance().sendKey(parsedKey, up, timestamp);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_sendKeyToFcitxChar(JNIEnv *env, jclass clazz, jchar c, jint state, jboolean up, jint timestamp) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key parsedKey{fcitx::Key::keySymFromString((const char *) &c),
                         fcitx::KeyStates(static_cast<uint32_t>(state))};
    Fcitx::Instance().sendKey(parsedKey, up, timestamp);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_sendKeySymToFcitx(JNIEnv *env, jclass clazz, jint sym, jint state, jboolean up, jint timestamp) {
    RETURN_IF_NOT_RUNNING
    fcitx::Key key{fcitx::KeySym(static_cast<uint32_t>(sym)),
                   fcitx::KeyStates(static_cast<uint32_t>(state))};
    Fcitx::Instance().sendKey(key, up, timestamp);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_selectCandidate(JNIEnv *env, jclass clazz, jint idx) {
    RETURN_VALUE_IF_NOT_RUNNING(false)
    FCITX_DEBUG() << "selectCandidate: #" << idx;
    return Fcitx::Instance().select(idx);
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
Java_org_fcitx_fcitx5_android_core_Fcitx_toggleInputMethod(JNIEnv *env, jclass clazz) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().toggleInputMethod();
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_nextInputMethod(JNIEnv *env, jclass clazz, jboolean forward) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().nextInputMethod(forward == JNI_TRUE);
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_listInputMethods(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto entries = Fcitx::Instance().listInputMethods();
    return fcitxInputMethodEntriesToJObjectArray(env, entries);
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_inputMethodStatus(JNIEnv *env, jclass clazz) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    const auto &status = Fcitx::Instance().inputMethodStatus();
    return fcitxInputMethodStatusToJObject(env, status);
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
        auto obj = JRef(env, fcitxAddonStatusToJObject(env, addon));
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
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().triggerQuickPhrase();
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
    Fcitx::Instance().focusInputContext(focus);
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_activateInputContext(JNIEnv *env, jclass clazz, jint uid, jstring pkg_name) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().activateInputContext(uid, CString(env, pkg_name));
}

extern "C"
JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_deactivateInputContext(JNIEnv *env, jclass clazz, jint uid) {
    RETURN_IF_NOT_RUNNING
    Fcitx::Instance().deactivateInputContext(uid);
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
    int size = static_cast<int>(actions.size());
    jobjectArray array = env->NewObjectArray(size, GlobalRef->Action, nullptr);
    for (int i = 0; i < size; i++) {
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
JNIEXPORT jobjectArray JNICALL
Java_org_fcitx_fcitx5_android_core_Fcitx_getFcitxCandidates(JNIEnv *env, jclass clazz, jint offset, jint limit) {
    RETURN_VALUE_IF_NOT_RUNNING(nullptr)
    auto candidates = Fcitx::Instance().getCandidates(static_cast<int>(offset), static_cast<int>(limit));
    int size = static_cast<int>(candidates.size());
    jobjectArray array = env->NewObjectArray(size, GlobalRef->String, nullptr);
    for (int i = 0; i < size; i++) {
        auto str = JString(env, candidates[i]);
        env->SetObjectArrayElement(array, i, str);
    }
    return array;
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

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Key_parse(JNIEnv *env, jclass clazz, jstring raw) {
    fcitx::Key key(*CString(env, raw));
    return env->NewObject(
            GlobalRef->Key,
            GlobalRef->KeyInit,
            key.sym(),
            key.states(),
            *JString(env, key.toString(fcitx::KeyStringFormat::Portable)),
            *JString(env, key.toString(fcitx::KeyStringFormat::Localized))
    );
}

extern "C"
JNIEXPORT jobject JNICALL
Java_org_fcitx_fcitx5_android_core_Key_create(JNIEnv *env, jclass clazz, jint sym, jint states) {
    fcitx::Key key{fcitx::KeySym(static_cast<uint32_t>(sym)),
                   fcitx::KeyStates(static_cast<uint32_t>(states))};
    return env->NewObject(
            GlobalRef->Key,
            GlobalRef->KeyInit,
            key.sym(),
            key.states(),
            *JString(env, key.toString(fcitx::KeyStringFormat::Portable)),
            *JString(env, key.toString(fcitx::KeyStringFormat::Localized))
    );
}

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

#pragma GCC diagnostic pop
