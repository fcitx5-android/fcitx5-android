/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_ANDROIDKEYBOARD_H
#define FCITX5_ANDROID_ANDROIDKEYBOARD_H

#include <fcitx-config/iniparser.h>
#include <fcitx-utils/inputbuffer.h>
#include <fcitx-utils/i18n.h>
#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodengine.h>
#include <fcitx/action.h>

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
        Option<bool>
            enableWordHint{this, "EnableWordHint", _("Enable word hint"), true};
        Option<bool>
            hintOnPhysicalKeyboard{this, "WordHintOnPhysicalKeyboard", _("Enable word hint when using physical keyboard"), false};
        Option<bool>
            editorControlledWordHint{this, "EditorControlledWordHint", _("Disable word hint based on editor attributes"), true};
        Option<int, IntConstrain>
            pageSize{this, "PageSize", _("Word hint page size"), 5, IntConstrain(3, 10)};
        OptionWithAnnotation<ChooseModifier, ChooseModifierI18NAnnotation>
            chooseModifier{this, "ChooseModifier", _("Choose key modifier"), ChooseModifier::Alt};
        Option<bool>
            insertSpace{this, "InsertSpace", _("Insert space between words"), false};
)

class AndroidKeyboardEngine;

struct AndroidKeyboardEngineState : public InputContextProperty {
    InputBuffer buffer_;
    std::string origKeyString_;
    bool prependSpace_ = false;

    void reset() {
        buffer_.clear();
        origKeyString_.clear();
        prependSpace_ = false;
    }
};

class AndroidKeyboardEngine final : public InputMethodEngineV3 {
public:
    static int constexpr MaxBufferSize = 20;
    static int constexpr SpellCandidateSize = 20;

    explicit AndroidKeyboardEngine(Instance *instance);

    Instance *instance() { return instance_; }

    void keyEvent(const InputMethodEntry &entry, KeyEvent &event) override;

    std::vector<InputMethodEntry> listInputMethods() override;

    static const inline std::string ConfPath = "conf/androidkeyboard.conf";

    void reloadConfig() override;

    void save() override;

    const Configuration *getConfig() const override { return &config_; }

    void setConfig(const RawConfig &config) override;

    void activate(const InputMethodEntry &entry, InputContextEvent &event) override;

    void deactivate(const InputMethodEntry &entry, InputContextEvent &event) override;

    void reset(const InputMethodEntry &entry, InputContextEvent &event) override;

    void resetState(InputContext *inputContext, bool fromCandidate = false);

    FCITX_ADDON_DEPENDENCY_LOADER(spell, instance_->addonManager());
//    FCITX_ADDON_DEPENDENCY_LOADER(emoji, instance_->addonManager());
//    FCITX_ADDON_DEPENDENCY_LOADER(quickphrase, instance_->addonManager());

    void updateCandidate(const InputMethodEntry &entry, InputContext *inputContext);
    // Update preedit and send ui update.
    void updateUI(InputContext *inputContext);

    auto factory() { return &factory_; }

    // Return true if event is pushed to buffer.
    // Return false if event will be skipped by buffer, usually this means caller
    // need to call commit buffer and forward event manually.
    bool updateBuffer(InputContext *inputContext, const KeyEvent& event);

    // Commit current buffer, also reset the state.
    // See also preeditString().
    void commitBuffer(InputContext *inputContext);

    void invokeActionImpl(const InputMethodEntry &entry, InvokeActionEvent &event) override;

private:
    bool supportHint(const std::string &language);
    /**
     * preedit string and byte cursor
     */
    std::pair<std::string, size_t> preeditWithCursor(InputContext *inputContext);

    Instance *instance_;
    AndroidKeyboardEngineConfig config_;
    KeyList selectionKeys_;
    fcitx::SimpleAction wordHintAction_;

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

#endif //FCITX5_ANDROID_ANDROIDKEYBOARD_H
