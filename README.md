# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, and print to stdout.

## Build

### Dependencies

- Android SDK Platform & Build-Tools 30
- Android NDK (Side by side) 23 & cmake 3.18.1 . **Note:** you may need to install Android Studio Beta for Android NDK 23, or use `sdkmanager` from Android SDK Command-line Tools. NDK 21 & 22 are confirmed not working with this project.
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)

### Patch CMakeLists.txt

If cmake complaint about "cannot find ...", just comment out those lines. Believe me, it will build. See my patches here: https://gist.github.com/rocka/f25d29bc6ceb31033543fd95eba09bf9

### `libime` data

I don't know why cmake won't download and generate those data. Just install [libime](https://archlinux.org/packages/community/x86_64/libime/), and copy `/usr/{lib,share}/libime/*` to `app/src/main/assets/fcitx5/libime/`.

### `fcitx5-chinese-addons` dict

Make it read dict path from environment variable, we need to specify that path at runtime.

```diff
diff --git a/im/pinyin/pinyin.cpp b/im/pinyin/pinyin.cpp
index 2f98f7f..1cceb7e 100644
--- a/im/pinyin/pinyin.cpp
+++ b/im/pinyin/pinyin.cpp
@@ -607,7 +607,7 @@ PinyinEngine::PinyinEngine(Instance *instance)
             libime::DefaultLanguageModelResolver::instance()
                 .languageModelFileForLanguage("zh_CN")));
     ime_->dict()->load(libime::PinyinDictionary::SystemDict,
-                       LIBIME_INSTALL_PKGDATADIR "/sc.dict",
+                       stringutils::joinPath(getenv("LIBIME_INSTALL_PKGDATADIR"), "sc.dict").c_str(),
                        libime::PinyinDictFormat::Binary);
     prediction_.setUserLanguageModel(ime_->model());
```

## PoC

<details>
<summary>Logcat</summary>

```
D/fcitx5: I2021-06-15 00:32:44.666513 instance.cpp:1371] Override Enabled Addons: {}
    I2021-06-15 00:32:44.666658 instance.cpp:1372] Overr
D/fcitx5: ide Disabled Addons: {}
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:44.674399
D/fcitx5: addonmanager.cpp:
D/fcitx5: 189]
D/fcitx5: Loaded addon unicode
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:44.675208
D/fcitx5:
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ]
D/fcitx5: Loaded addon
D/fcitx5: androidfrontend
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:44.676556
D/fcitx5:
D/fcitx5: inputmethodmanager.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ]
D/fcitx5: No valid input method group in configuration.
D/fcitx5: Building a default one
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:44.677021
D/fcitx5:
D/fcitx5: instance.cpp
D/fcitx5: :
D/fcitx5: 730
D/fcitx5: ]
D/fcitx5: Items in
D/fcitx5: Default
D/fcitx5: :
D/fcitx5: [
D/fcitx5: InputMethodGroupItem(
D/fcitx5: keyboard-us
D/fcitx5: ,layout=
D/fcitx5: )
D/fcitx5: ]
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:44.677383
D/fcitx5:
D/fcitx5: instance.cpp
D/fcitx5: :
D/fcitx5: 735
D/fcitx5: ]
D/fcitx5: Generated groups:
D/fcitx5: [
D/fcitx5: Default
D/fcitx5: ]
D/fcitx5: E
D/fcitx5: 2021-06-15 00:32:44.678391
D/fcitx5:
D/fcitx5: instance.cpp
D/fcitx5: :
D/fcitx5: 1381
D/fcitx5: ]
D/fcitx5: Couldn't find keyboard-us
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:44.682066
D/fcitx5:
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ]
D/fcitx5: Loaded addon
D/fcitx5: punctuation
D/fcitx5: E
D/fcitx5: 2021-06-15 00:32:45.040030
D/fcitx5:
D/fcitx5: pinyin.cpp
D/fcitx5: :
D/fcitx5: 647
D/fcitx5: ]
D/fcitx5: Failed to load pinyin history:
D/fcitx5: io fail: unspecified iostream_category error
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:45.070853
D/fcitx5:
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ]
D/fcitx5: Loaded addon
D/fcitx5: pinyin
W/Thread-2: type=1400 audit(0.0:192): avc: denied { read } for name="uuid" dev="proc" ino=40929 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:proc:s0 tclass=file permissive=0
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:45.711658 androidfrontend.cpp:70] KeyEvent key: n isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:45.775222
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 70
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: i
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:45.923111 androidfrontend.cpp
D/fcitx5: :70] KeyEvent key:
D/fcitx5: h isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:45.991609
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 70
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: a
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-15 00:32:46.105018
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 70
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: o
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: 5 Candidates:
D/fcitx5: 0: 你好
D/fcitx5: 1: 你
D/fcitx5: 2: 尼
D/fcitx5: 3: 泥
D/fcitx5: 4: 妮
D/Candidate: 你好,你,尼,泥,妮
```
</details>
