# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, and print to stdout.

## Build

### Dependencies

- Android SDK Platform & Build-Tools 30
- Android NDK (Side by side) 23 & cmake 3.18.1
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)

### Patch CMakeLists.txt

If cmake complaint about "cannot find ...", just comment out those lines. Believe me, it will build.

### `libime` data

I don't know why cmake won't download and generate those data. Just install [libime](https://archlinux.org/packages/community/x86_64/libime/), and copy `/usr/{lib,share}/libime/*` to `app/src/main/assets/libime/`.

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
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.301125 instance.cpp
D/fcitx5: :
D/fcitx5: 1371]
D/fcitx5: Override Enabled Addons: {
D/fcitx5: unicode,
D/fcitx5: punctuation,
D/fcitx5: androidfrontend, pinyin
D/fcitx5: }
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.301305 instance.cpp:
D/fcitx5: 1372]
D/fcitx5: Override Disabled Addons: {all
D/fcitx5: }
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.339251
D/fcitx5: addonmanager.cpp:
D/fcitx5: 189]
D/fcitx5: Loaded addon unicode
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.339849 addonmanager.cpp
D/fcitx5: :189]
D/fcitx5: Loaded addon androidfrontend
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.342312 inputmethodmanager.cpp
D/fcitx5: :117
D/fcitx5: ] No valid input method group in configuration.
D/fcitx5: Building a default one
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.343421
D/fcitx5:  instance.cpp
D/fcitx5: :
D/fcitx5: 730]
D/fcitx5: Items in Default
D/fcitx5: : [
D/fcitx5: InputMethodGroupItem(
D/fcitx5: keyboard-us,layout=
D/fcitx5: )]
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.343485 instance.cpp
D/fcitx5: :735]
D/fcitx5: Generated groups: [
D/fcitx5: Default]
D/fcitx5: E
D/fcitx5: 2021-06-10 21:14:01.343650 instance.cpp
D/fcitx5: :1381]
D/fcitx5: Couldn't find keyboard-us
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.346341 addonmanager.cpp:189] Loaded addon punctuation
D/fcitx5: E
D/fcitx5: 2021-06-10 21:14:01.636885
D/fcitx5:
D/fcitx5: pinyin.cpp
D/fcitx5: :
D/fcitx5: 647
D/fcitx5: ]
D/fcitx5: Failed to load pinyin history:
D/fcitx5: io fail: unspecified iostream_category error
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.706428
D/fcitx5:
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ]
D/fcitx5: Loaded addon
D/fcitx5: pinyin
W/ocka.fcitx5test: type=1400 audit(0.0:26994): avc: denied { read } for name="uuid" dev="proc" ino=15533979 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:proc:s0 tclass=file permissive=0
W/ocka.fcitx5test: type=1400 audit(0.0:26995): avc: denied { read } for name="uuid" dev="proc" ino=15533979 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:proc:s0 tclass=file permissive=0
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.735381
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 71
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: n
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.742409
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 71
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: i
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.771056
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 71
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: h
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.776967
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 71
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: a
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.790069
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 71
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: o
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.790626
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 29
D/fcitx5: ]
D/fcitx5: Commit:
D/fcitx5: 你好
D/fcitx5: I
D/fcitx5: 2021-06-10 21:14:01.791372
D/fcitx5:
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 71
D/fcitx5: ]
D/fcitx5: KeyEvent key:
D/fcitx5: 1
D/fcitx5:  isRelease:
D/fcitx5: 0
D/fcitx5:  accepted:
D/fcitx5: 1
```
</details>
