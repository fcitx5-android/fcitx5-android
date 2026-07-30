/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2017-2017 CSSlayer <wengxt@gmail.com>
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 * SPDX-FileComment: Derived from:
 * - https://github.com/fcitx/fcitx5-chinese-addons/blob/5.1.13/modules/cloudpinyin/cloudpinyin.h
 * - https://github.com/fcitx/fcitx5-chinese-addons/blob/5.1.13/modules/cloudpinyin/cloudpinyin.cpp
 */
#include "cloudpinyin_public.h"

#include <jni.h>
#include <fcitx-config/configuration.h>
#include <fcitx-config/enum.h>
#include <fcitx-config/iniparser.h>
#include <fcitx-utils/eventdispatcher.h>
#include <fcitx-utils/event.h>
#include <fcitx-utils/eventloopinterface.h>
#include <fcitx-utils/fs.h>
#include <fcitx-utils/i18n.h>
#include <fcitx-utils/log.h>
#include <fcitx-utils/misc.h>
#include <fcitx-utils/trackableobject.h>
#include <fcitx/addonfactory.h>
#include <fcitx/addoninstance.h>
#include <fcitx/addonmanager.h>
#include <fcitx/instance.h>

#include <atomic>
#include <cstdint>
#include <cstring>
#include <memory>
#include <mutex>
#include <list>
#include <string>
#include <string_view>
#include <unordered_map>
#include <utility>
#include <vector>

class CloudPinyin;
std::mutex instanceMutex;
CloudPinyin *instance = nullptr;

FCITX_CONFIG_ENUM(CloudPinyinBackend, Google, GoogleCN, Baidu);
FCITX_CONFIGURATION(
    CloudPinyinConfig,
    fcitx::Option<fcitx::KeyList> toggleKey{
        this, "Toggle Key", _("Toggle Key"), {fcitx::Key("Control+Alt+Shift+C")}};
    fcitx::Option<int> minimumLength{
        this, "MinimumPinyinLength", _("Minimum Pinyin Length"), 4};
    fcitx::Option<CloudPinyinBackend> backend{
        this, "Backend", _("Backend"), CloudPinyinBackend::GoogleCN};
    fcitx::OptionWithAnnotation<std::string, fcitx::ToolTipAnnotation> proxy{
        this,
        "Proxy",
        _("Proxy"),
        "",
        {},
        {},
        {_("The proxy format is [scheme]://[host]:[port], for example "
           "http://localhost:1080.")}};);

namespace {

constexpr int MAX_ERROR = 10;
constexpr uint64_t MINUTE_IN_US = 60000000;
constexpr char BRIDGE_CLASS[] =
    "org/fcitx/fcitx5/android/core/CloudPinyinIpc";

JavaVM *javaVm = nullptr;
jclass bridgeClass = nullptr;
jmethodID requestMethod = nullptr;

std::string backendName(CloudPinyinBackend backend) {
    switch (backend) {
    case CloudPinyinBackend::Google:
        return "Google";
    case CloudPinyinBackend::GoogleCN:
        return "GoogleCN";
    case CloudPinyinBackend::Baidu:
        return "Baidu";
    }
    return "";
}

std::string parseResult(CloudPinyinBackend backend,
                        const std::vector<char> &response) {
    const std::string_view result(response.data(), response.size());
    std::string_view startMarker;
    std::string_view endMarker;
    if (backend == CloudPinyinBackend::Baidu) {
        startMarker = "[[\"";
        endMarker = "\",";
    } else {
        startMarker = "\",[\"";
        endMarker = "\"";
    }
    const auto marker = result.find(startMarker);
    if (marker == std::string_view::npos) {
        return {};
    }
    const auto start = marker + startMarker.size();
    const auto end = result.find(endMarker, start);
    return end == std::string_view::npos || end <= start
               ? std::string()
               : std::string(result.substr(start, end - start));
}

JNIEnv *getEnv(bool *attached) {
    *attached = false;
    if (!javaVm) {
        return nullptr;
    }
    JNIEnv *env = nullptr;
    if (javaVm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) ==
        JNI_EDETACHED) {
        if (javaVm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return nullptr;
        }
        *attached = true;
    }
    return env;
}

} // namespace

template <typename Key, typename Value> class LRUCache {
public:
    explicit LRUCache(size_t capacity) : capacity_(capacity) {}

    Value *find(const Key &key) {
        auto iter = entries_.find(key);
        if (iter == entries_.end()) {
            return nullptr;
        }
        order_.splice(order_.begin(), order_, iter->second.second);
        return &iter->second.first;
    }

    void insert(const Key &key, Value value) {
        auto iter = entries_.find(key);
        if (iter != entries_.end()) {
            iter->second.first = std::move(value);
            order_.splice(order_.begin(), order_, iter->second.second);
            return;
        }
        if (entries_.size() == capacity_) {
            entries_.erase(order_.back());
            order_.pop_back();
        }
        order_.push_front(key);
        entries_.emplace(key, std::make_pair(std::move(value), order_.begin()));
    }

private:
    size_t capacity_;
    std::list<Key> order_;
    std::unordered_map<Key, std::pair<Value, typename std::list<Key>::iterator>>
        entries_;
};

class CloudPinyin : public fcitx::AddonInstance,
                    public fcitx::TrackableObject<CloudPinyin> {
public:
    explicit CloudPinyin(fcitx::AddonManager *manager)
        : eventLoop_(manager->eventLoop()),
          dispatcher_(manager->instance()->eventDispatcher()) {
        resetError_ = eventLoop_->addTimeEvent(
            CLOCK_MONOTONIC, fcitx::now(CLOCK_MONOTONIC), MINUTE_IN_US,
            [this](fcitx::EventSourceTime *, uint64_t) {
                resetError();
                return true;
            });
        resetError_->setEnabled(false);
        std::lock_guard<std::mutex> lock(instanceMutex);
        instance = this;
        reloadConfig();
    }

    ~CloudPinyin() override {
        std::lock_guard<std::mutex> lock(instanceMutex);
        if (instance == this) {
            instance = nullptr;
        }
    }

    void reloadConfig() override {
        fcitx::readAsIni(config_, "conf/cloudpinyin.conf");
    }

    const fcitx::Configuration *getConfig() const override { return &config_; }

    void setConfig(const fcitx::RawConfig &config) override {
        config_.load(config, true);
        fcitx::safeSaveAsIni(config_, "conf/cloudpinyin.conf");
    }

    void request(const std::string &pinyin, CloudPinyinCallback callback) {
        if (static_cast<int>(pinyin.size()) < config_.minimumLength.value()) {
            callback(pinyin, "");
            return;
        }
        if (errorCount_ >= MAX_ERROR) {
            callback(pinyin, "");
            return;
        }
        if (auto *cached = cache_.find(pinyin)) {
            callback(pinyin, *cached);
            return;
        }

        const auto requestId = nextRequestId_.fetch_add(1);
        const auto backend = config_.backend.value();
        {
            std::lock_guard<std::mutex> lock(requestMutex_);
            pending_.emplace(requestId, PendingRequest{pinyin, std::move(callback), backend});
        }
        if (!requestIpc(requestId, pinyin, backendName(backend),
                        config_.proxy.value())) {
            complete(requestId, 0, {}, "No cloud pinyin provider");
        }
    }

    const fcitx::KeyList &toggleKey() const { return config_.toggleKey.value(); }

    void resetError() {
        errorCount_ = 0;
        resetError_->setEnabled(false);
    }

    void complete(uint64_t requestId, int httpStatus, std::vector<char> response,
                  std::string error) {
        PendingRequest request;
        {
            std::lock_guard<std::mutex> lock(requestMutex_);
            auto iter = pending_.find(requestId);
            if (iter == pending_.end()) {
                return;
            }
            request = std::move(iter->second);
            pending_.erase(iter);
        }
        dispatcher_.scheduleWithContext(
            watch(), [this, request = std::move(request), httpStatus,
                      response = std::move(response), error = std::move(error)]() mutable {
                if (httpStatus != 200 || !error.empty()) {
                    errorCount_++;
                    if (errorCount_ == MAX_ERROR) {
                        FCITX_ERROR() << "Cloud pinyin reaches max error. "
                                         "Retry in 5 minutes.";
                        resetError_->setNextInterval(MINUTE_IN_US * 5);
                        resetError_->setOneShot();
                    }
                }
                const auto hanzi = httpStatus == 200 && error.empty()
                                       ? parseResult(request.backend, response)
                                       : std::string();
                request.callback(request.pinyin, hanzi);
                if (!hanzi.empty()) {
                    cache_.insert(request.pinyin, hanzi);
                }
                return true;
            });
    }

    void providerUnavailable() {
        std::vector<uint64_t> requestIds;
        {
            std::lock_guard<std::mutex> lock(requestMutex_);
            requestIds.reserve(pending_.size());
            for (const auto &[requestId, _] : pending_) {
                requestIds.push_back(requestId);
            }
        }
        for (const auto requestId : requestIds) {
            complete(requestId, 0, {}, "Cloud pinyin provider disconnected");
        }
    }

private:
    struct PendingRequest {
        std::string pinyin;
        CloudPinyinCallback callback;
        CloudPinyinBackend backend;
    };

    bool requestIpc(uint64_t requestId, const std::string &pinyin,
                    const std::string &backend, const std::string &proxy) {
        bool attached = false;
        auto *env = getEnv(&attached);
        if (!env || !bridgeClass || !requestMethod) {
            return false;
        }
        const auto pinyinValue = env->NewStringUTF(pinyin.c_str());
        const auto backendValue = env->NewStringUTF(backend.c_str());
        const auto proxyValue = env->NewStringUTF(proxy.c_str());
        const auto accepted = env->CallStaticBooleanMethod(
            bridgeClass, requestMethod, static_cast<jlong>(requestId),
            pinyinValue, backendValue, proxyValue);
        env->DeleteLocalRef(pinyinValue);
        env->DeleteLocalRef(backendValue);
        env->DeleteLocalRef(proxyValue);
        const bool failed = env->ExceptionCheck();
        if (failed) {
            env->ExceptionClear();
        }
        if (attached) {
            javaVm->DetachCurrentThread();
        }
        return !failed && accepted;
    }

    FCITX_ADDON_EXPORT_FUNCTION(CloudPinyin, request);
    FCITX_ADDON_EXPORT_FUNCTION(CloudPinyin, toggleKey);
    FCITX_ADDON_EXPORT_FUNCTION(CloudPinyin, resetError);

    fcitx::EventLoop *eventLoop_;
    fcitx::EventDispatcher &dispatcher_;
    std::unique_ptr<fcitx::EventSourceTime> resetError_;
    CloudPinyinConfig config_;
    std::atomic<uint64_t> nextRequestId_{1};
    std::mutex requestMutex_;
    std::unordered_map<uint64_t, PendingRequest> pending_;
    LRUCache<std::string, std::string> cache_{2048};
    int errorCount_ = 0;
};

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *) {
    javaVm = vm;
    JNIEnv *env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }
    auto localClass = env->FindClass(BRIDGE_CLASS);
    if (!localClass) {
        return JNI_ERR;
    }
    bridgeClass = static_cast<jclass>(env->NewGlobalRef(localClass));
    env->DeleteLocalRef(localClass);
    requestMethod = env->GetStaticMethodID(
        bridgeClass, "request",
        "(JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)Z");
    return requestMethod ? JNI_VERSION_1_6 : JNI_ERR;
}

extern "C" JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_CloudPinyinIpc_nativeOnResponse(
    JNIEnv *env, jclass, jlong requestId, jint httpStatus, jbyteArray response,
    jstring error) {
    std::vector<char> data;
    const auto size = env->GetArrayLength(response);
    data.resize(size);
    env->GetByteArrayRegion(response, 0, size,
                            reinterpret_cast<jbyte *>(data.data()));
    const char *errorChars = env->GetStringUTFChars(error, nullptr);
    const std::string errorValue(errorChars);
    env->ReleaseStringUTFChars(error, errorChars);

    std::lock_guard<std::mutex> lock(instanceMutex);
    if (instance) {
        instance->complete(static_cast<uint64_t>(requestId), httpStatus,
                           std::move(data), errorValue);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_org_fcitx_fcitx5_android_core_CloudPinyinIpc_nativeOnProviderUnavailable(
    JNIEnv *, jclass) {
    std::lock_guard<std::mutex> lock(instanceMutex);
    if (instance) {
        instance->providerUnavailable();
    }
}

class CloudPinyinFactory : public fcitx::AddonFactory {
public:
    fcitx::AddonInstance *create(fcitx::AddonManager *manager) override {
        return new CloudPinyin(manager);
    }
};

FCITX_ADDON_FACTORY_V2(cloudpinyin, CloudPinyinFactory);
