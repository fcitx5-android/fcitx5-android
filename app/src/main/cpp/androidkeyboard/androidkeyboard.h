#ifndef _FCITX_IM_ANDROIDKEYBOARD_ANDROIDKEYBOARD_H_
#define _FCITX_IM_ANDROIDKEYBOARD_ANDROIDKEYBOARD_H_

#define FCITX_GETTEXT_DOMAIN "fcitx5"

#include <fcitx-config/iniparser.h>
#include <fcitx-utils/inputbuffer.h>
#include <fcitx-utils/i18n.h>
#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodengine.h>

namespace fcitx {

class Instance;

enum class ChooseModifier {
    NoModifier, Alt, Control, Super
};

FCITX_CONFIG_ENUM_NAME_WITH_I18N(
        ChooseModifier,
        N_("None"), N_("Alt"), N_("Control"), N_("Super")
);

FCITX_CONFIGURATION(
        AndroidKeyboardEngineConfig,
        Option<int, IntConstrain>
            pageSize{this, "PageSize", _("Page size"), 5, IntConstrain(3, 10)};
        Option<bool>
            enableWordHint{this, "EnableWordHint", _("Completion"), true};
        OptionWithAnnotation<ChooseModifier, ChooseModifierI18NAnnotation>
            chooseModifier{this, "Choose Modifier", _("Choose key modifier"), ChooseModifier::Alt};
)

class AndroidKeyboardEngine;

enum class CandidateMode {
    Hint, LongPress
};

struct AndroidKeyboardEngineState : public InputContextProperty {
    InputBuffer buffer_;
    CandidateMode mode_ = CandidateMode::Hint;
    std::string origKeyString_;

    void reset() {
        buffer_.clear();
        mode_ = CandidateMode::Hint;
        origKeyString_.clear();
    }
};

class AndroidKeyboardEngine final : public InputMethodEngine {
public:
    AndroidKeyboardEngine(Instance *instance);
    ~AndroidKeyboardEngine() = default;

    Instance *instance() { return instance_; }

    void keyEvent(const InputMethodEntry &entry, KeyEvent &event) override;

    std::vector<InputMethodEntry> listInputMethods() override;

    static const inline std::string ConfPath = "conf/androidkeyboard.conf";

    void reloadConfig() override;

    void save() override;

    const Configuration *getConfig() const override { return &config_; }

    void setConfig(const RawConfig &config) override;

    void reset(const InputMethodEntry &entry, InputContextEvent &event) override;

    void resetState(InputContext *inputContext);

    FCITX_ADDON_DEPENDENCY_LOADER(spell, instance_->addonManager());
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
    bool supportHint(const std::string &language);
    std::string preeditString(InputContext *inputContext);

    Instance *instance_;
    AndroidKeyboardEngineConfig config_;
    KeyList selectionKeys_;
    bool enableWordHint_;

    FactoryFor<AndroidKeyboardEngineState> factory_{
            [](InputContext &) { return new AndroidKeyboardEngineState; }
    };
};

class AndroidKeyboardEngineFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new AndroidKeyboardEngine(manager->instance());
    }
};

}

#endif //_FCITX_IM_ANDROIDKEYBOARD_ANDROIDKEYBOARD_H_
