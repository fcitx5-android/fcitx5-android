/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2016-2016 CSSlayer <wengxt@gmail.com>
 * SPDX-FileCopyrightText: Copyright 2023-2025 Fcitx5 for Android Contributors
 * SPDX-FileComment: Modified from https://github.com/fcitx/fcitx5/blob/5.1.14/src/lib/fcitx/addonloader_p.h
 */

#ifndef FCITX5_ANDROID_ANDROIDADDONLOADER_H
#define FCITX5_ANDROID_ANDROIDADDONLOADER_H

#include <memory>
#include <stdexcept>
#include <string>
#include <unordered_map>
#include <utility>
#include <vector>

#include <fcitx-utils/library.h>
#include <fcitx-utils/standardpaths.h>
#include <fcitx-utils/stringutils.h>
#include <fcitx/addonfactory.h>
#include <fcitx/addoninfo.h>
#include <fcitx/addoninstance.h>
#include <fcitx/addonloader.h>

namespace fcitx {

namespace {
constexpr char FCITX_ADDON_FACTORY_ENTRY[] = "fcitx_addon_factory_instance";
}

class AndroidSharedLibraryFactory {
public:
    explicit AndroidSharedLibraryFactory(const AddonInfo &info, std::vector<Library> libraries)
            : libraries_(std::move(libraries)) {
        std::string v2Name = stringutils::concat(FCITX_ADDON_FACTORY_ENTRY, "_",
                                                 info.uniqueName());
        if (libraries_.empty()) {
            throw std::runtime_error("Got empty libraries.");
        }

        // Only resolve with last library.
        auto &library = libraries_.back();
        auto *funcPtr = library.resolve(v2Name.data());
        if (!funcPtr) {
            funcPtr = library.resolve(FCITX_ADDON_FACTORY_ENTRY);
        }
        if (!funcPtr) {
            throw std::runtime_error(library.error());
        }
        auto func = Library::toFunction<AddonFactory *()>(funcPtr);
        factory_ = func();
        if (!factory_) {
            throw std::runtime_error("Failed to get a factory");
        }
    }

    AddonFactory *factory() { return factory_; }

private:
    std::vector<Library> libraries_;
    AddonFactory *factory_;
};

class AndroidSharedLibraryLoader : public AddonLoader {
public:
    [[nodiscard]] std::string type() const override { return "SharedLibrary"; }

    AddonInstance *load(const AddonInfo &info, AddonManager *manager) override;

private:
    // Android specific: create a new StandardPaths instance in case FCITX_ADDON_DIRS changes
    StandardPaths standardPaths_ = StandardPaths(
            "fcitx5",
            std::unordered_map<std::string, std::vector<std::filesystem::path>>{},
            Flags<StandardPathsOption>(StandardPathsOption::SkipBuiltInPath)
    );
    // Android specific end
    std::unordered_map<std::string, std::unique_ptr<AndroidSharedLibraryFactory>>
            registry_;
};

} // namespace fcitx

#endif //FCITX5_ANDROID_ANDROIDADDONLOADER_H
