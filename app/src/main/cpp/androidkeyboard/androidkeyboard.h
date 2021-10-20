#ifndef _FCITX_IM_ANDROIDKEYBOARD_ANDROIDKEYBOARD_H_
#define _FCITX_IM_ANDROIDKEYBOARD_ANDROIDKEYBOARD_H_

#include <fcitx-config/iniparser.h>
#include <fcitx-utils/inputbuffer.h>
#include <fcitx-utils/event.h>
#include <fcitx-utils/i18n.h>
#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodengine.h>

namespace fcitx {

class Instance;

FCITX_CONFIGURATION(
        AndroidKeyboardEngineConfig,
        Option<int, IntConstrain>
                pageSize{this, "PageSize", _("Page size"), 5, IntConstrain(3, 10)};
)

class AndroidKeyboardEngine;

struct AndroidKeyboardEngineState : public InputContextProperty {
    InputBuffer buffer_;
    std::string origKeyString_;
    bool repeatStarted_ = false;

    void reset() {
        origKeyString_.clear();
        buffer_.clear();
        repeatStarted_ = false;
    }
};

class AndroidKeyboardEnginePrivate;

class AndroidKeyboardEngine final : public InputMethodEngine {
public:
    AndroidKeyboardEngine(Instance *instance);
    ~AndroidKeyboardEngine();

    Instance *instance() { return instance_; }

    void keyEvent(const InputMethodEntry &entry, KeyEvent &event) override;

    std::vector<InputMethodEntry> listInputMethods() override;

    void reloadConfig() override;

    const Configuration *getConfig() const override { return &config_; }

    void setConfig(const RawConfig &config) override {
        config_.load(config, true);
        safeSaveAsIni(config_, "conf/androidkeyboard.conf");
        reloadConfig();
    }

    const Configuration *getSubConfig(const std::string &path) const override;

    void setSubConfig(const std::string &, const fcitx::RawConfig &) override;

    void reset(const InputMethodEntry &entry, InputContextEvent &event) override;

    void resetState(InputContext *inputContext);

//    FCITX_ADDON_DEPENDENCY_LOADER(spell, instance_->addonManager());
//    FCITX_ADDON_DEPENDENCY_LOADER(emoji, instance_->addonManager());
//    FCITX_ADDON_DEPENDENCY_LOADER(quickphrase, instance_->addonManager());

    void updateCandidate(const InputMethodEntry &entry, InputContext *inputContext);
    // Update preedit and send ui update.
    void updateUI(InputContext *inputContext);

    auto factory() { return &factory_; }

    // Return true if chr is pushed to buffer.
    // Return false if chr will be skipped by buffer, usually this means caller
    // need to call commit buffer and forward chr manually.
    bool updateBuffer(InputContext *inputContext, const std::string &chr);

    // Commit current buffer, also reset the state.
    // See also preeditString().
    void commitBuffer(InputContext *inputContext);

private:

    std::string preeditString(InputContext *inputContext);

    Instance *instance_;
    AndroidKeyboardEngineConfig config_;
    KeyList selectionKeys_;
    std::unique_ptr<EventSource> deferEvent_;

    FactoryFor<AndroidKeyboardEngineState> factory_{
            [](InputContext &) { return new AndroidKeyboardEngineState; }
    };

    std::unordered_set<std::string> longPressBlocklistSet_;

    std::unique_ptr<EventSourceTime> cancelLastEvent_;
};

class AndroidKeyboardEngineFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidKeyboardEngine(manager->instance());
    }
};

}

#endif //_FCITX_IM_ANDROIDKEYBOARD_ANDROIDKEYBOARD_H_
