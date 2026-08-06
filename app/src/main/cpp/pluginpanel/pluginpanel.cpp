/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
#include "pluginpanel.h"

#include <fcitx/addonfactory.h>
#include <fcitx/addonmanager.h>
#include <fcitx/inputmethodengine.h>

namespace fcitx {
namespace {

class PluginPanelEngine final : public InputMethodEngine {
public:
    explicit PluginPanelEngine(Instance *) {}

    std::vector<InputMethodEntry> listInputMethods() override {
        return pluginpanel::listEntries();
    }

    void keyEvent(const InputMethodEntry &entry, KeyEvent &keyEvent) override {
        // The panel engine is a shell: keys are handled by the plugin's own
        // surface, nothing to do here.
        keyEvent.filterAndAccept();
    }
};

class PluginPanelEngineFactory : public AddonFactory {
public:
    AddonInstance *create(AddonManager *manager) override {
        return new PluginPanelEngine(manager->instance());
    }
};

} // namespace
} // namespace fcitx

FCITX_ADDON_FACTORY(fcitx::PluginPanelEngineFactory)
