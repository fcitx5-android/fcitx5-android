/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2016-2016 CSSlayer <wengxt@gmail.com>
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 * SPDX-FileComment: Modified from https://github.com/fcitx/fcitx5/blob/5.1.1/src/lib/fcitx/addonloader.cpp
 */
#include "androidaddonloader.h"

#define FCITX_LIBRARY_SUFFIX ".so"

namespace fcitx {

AndroidSharedLibraryLoader::AndroidSharedLibraryLoader(AndroidLibraryDependency dependency)
        : dependency_(std::move(dependency)) {}

AddonInstance *AndroidSharedLibraryLoader::load(const AddonInfo &info,
                                                AddonManager *manager) {
    auto iter = registry_.find(info.uniqueName());
    if (iter == registry_.end()) {
        std::string libname = info.library();
        Flags<LibraryLoadHint> flag = LibraryLoadHint::DefaultHint;
        if (stringutils::startsWith(libname, "export:")) {
            libname = libname.substr(7);
            flag |= LibraryLoadHint::ExportExternalSymbolsHint;
        }
        auto file = libname + FCITX_LIBRARY_SUFFIX;
        auto libs = standardPath_.locateAll(StandardPath::Type::Addon, file);
        if (libs.empty()) {
            FCITX_ERROR() << "Could not locate library " << file
                          << " for addon " << info.uniqueName() << ".";
        }
        // ========== Android specific start ========== //
        auto deps = dependency_.find(libname);
        if (deps != dependency_.end()) {
            for (const auto &dep: deps->second) {
                auto depFile = dep + FCITX_LIBRARY_SUFFIX;
                auto depPaths = standardPath_.locateAll(StandardPath::Type::Addon, depFile);
                if (depPaths.empty()) {
                    FCITX_ERROR() << "Could not locate dependency " << depFile
                                  << " for library " << file << ".";
                } else {
                    for (const auto &depPath: depPaths) {
                        Library depLib(depPath);
                        if (!depLib.load()) {
                            FCITX_ERROR() << "Failed to load dependency " << depPath
                                          << " for library " << file << ".";
                        } else {
                            FCITX_INFO() << "Loaded dependency " << depFile
                                         << " for library " << file << ".";
                            break;
                        }
                    }
                }
            }
        }
        // ========== Android specific end ========== //
        for (const auto &libraryPath: libs) {
            Library lib(libraryPath);
            if (!lib.load(flag)) {
                FCITX_ERROR()
                    << "Failed to load library for addon " << info.uniqueName()
                    << " on " << libraryPath << ". Error: " << lib.error();
                continue;
            }
            try {
                registry_.emplace(
                        info.uniqueName(),
                        std::make_unique<AndroidSharedLibraryFactory>(std::move(lib)));
            } catch (const std::exception &e) {
            }
            break;
        }
        iter = registry_.find(info.uniqueName());
    }

    if (iter == registry_.end()) {
        return nullptr;
    }

    try {
        return iter->second->factory()->create(manager);
    } catch (const std::exception &e) {
        FCITX_ERROR() << "Failed to create addon: " << info.uniqueName() << " "
                      << e.what();
    } catch (...) {
        FCITX_ERROR() << "Failed to create addon: " << info.uniqueName();
    }
    return nullptr;
}

}
