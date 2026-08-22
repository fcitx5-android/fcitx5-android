/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
#ifndef FCITX5_ANDROID_PLUGINPANEL_H
#define FCITX5_ANDROID_PLUGINPANEL_H

#include <string>
#include <vector>

namespace fcitx {

class InputMethodEntry;

/**
 * Registry shared between libnative-lib (writer, via JNI) and
 * libpluginpanel (reader, in listInputMethods). Thread-safe.
 *
 * Entries registered here become input method entries of the
 * "pluginpanel" addon. The pluginpanel engine itself is a shell: it
 * produces no candidates — the panel UI is rendered by the plugin and
 * candidates/commits go through the host's IPC.
 */
namespace pluginpanel {

// libnative-lib is built with -fvisibility=hidden; the registry functions
// must be exported so that libpluginpanel (linked against libnative-lib)
// can resolve them at runtime.
#define FCITX_PLUGINPANEL_EXPORT __attribute__((visibility("default")))

FCITX_PLUGINPANEL_EXPORT
void registerEntry(const std::string &uniqueName, const std::string &name,
                   const std::string &languageCode);

FCITX_PLUGINPANEL_EXPORT
void unregisterEntry(const std::string &uniqueName);

FCITX_PLUGINPANEL_EXPORT
std::vector<InputMethodEntry> listEntries();

} // namespace pluginpanel
} // namespace fcitx

#endif // FCITX5_ANDROID_PLUGINPANEL_H
