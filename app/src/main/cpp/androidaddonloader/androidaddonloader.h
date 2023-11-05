#ifndef FCITX5_ANDROID_ANDROIDADDONLOADER_H
#define FCITX5_ANDROID_ANDROIDADDONLOADER_H

/**
 * @see https://github.com/fcitx/fcitx5/blob/5.1.1/src/lib/fcitx/addonloader_p.cpp
 */

#include <stdexcept>
#include <fcitx-utils/library.h>
#include <fcitx-utils/standardpath.h>
#include <fcitx/addonloader.h>
#include <fcitx/addonfactory.h>
#include <fcitx/addoninfo.h>
#include <fcitx/addoninstance.h>
#include <fcitx/addonloader.h>

namespace fcitx {

class AndroidSharedLibraryFactory {
public:
    AndroidSharedLibraryFactory(Library lib) : library_(std::move(lib)) {
        auto *funcPtr = library_.resolve("fcitx_addon_factory_instance");
        if (!funcPtr) {
            throw std::runtime_error(library_.error());
        }
        auto func = Library::toFunction<AddonFactory *()>(funcPtr);
        factory_ = func();
        if (!factory_) {
            throw std::runtime_error("Failed to get a factory");
        }
    }

    AddonFactory *factory() { return factory_; }

private:
    Library library_;
    AddonFactory *factory_;
};

typedef std::unordered_map<std::string, std::unordered_set<std::string>> AndroidLibraryDependency;

class AndroidSharedLibraryLoader : public AddonLoader {
public:
    AndroidSharedLibraryLoader(AndroidLibraryDependency dependency);
    ~AndroidSharedLibraryLoader() = default;
    std::string type() const override { return "SharedLibrary"; }

    AddonInstance *load(const AddonInfo &info, AddonManager *manager) override;

private:
    StandardPath standardPath_;
    std::unordered_map<std::string, std::unique_ptr<AndroidSharedLibraryFactory>> registry_;
    AndroidLibraryDependency dependency_;
};

}

#endif //FCITX5_ANDROID_ANDROIDADDONLOADER_H
