# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, and print to logcat.

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
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.644201 instance.cpp:
D/fcitx5: 1371] Override Enabled Addons: 
D/fcitx5: {}
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.644339 instance.cpp:
D/fcitx5: 1372] Override Disabled Addons: 
D/fcitx5: {}
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.697704 addonmanager.cpp:
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon unicode
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.752736
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: quickphrase
D/fcitx5: I2021-06-18 22:15:01.778334 addonmanager.cpp:189] Loaded addon imselector
    I2021-06-18 22:15:01.779049 addonmanager.cpp:189] Loa
D/fcitx5: ded addon androidfrontend
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.784554
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: pinyinhelper
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.811568
D/fcitx5:  
D/fcitx5: inputmethodmanager.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: No valid input method group in configuration. 
D/fcitx5: Building a default one
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.812048
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
D/fcitx5: 2021-06-18 22:15:01.812359
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
D/fcitx5: 2021-06-18 22:15:01.812695
D/fcitx5:  
D/fcitx5: instance.cpp
D/fcitx5: :
D/fcitx5: 1381
D/fcitx5: ] 
D/fcitx5: Couldn't find keyboard-us
W/Thread-2: type=1400 audit(0.0:9717): avc: denied { read } for name="uuid" dev="proc" ino=4488195 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:proc:s0 tclass=file permissive=0
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:01.834927 addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189] 
D/fcitx5: Loaded addon 
D/fcitx5: punctuation
D/fcitx5: E
D/fcitx5: 2021-06-18 22:15:02.209061
D/fcitx5:  
D/fcitx5: pinyin.cpp
D/fcitx5: :
D/fcitx5: 647
D/fcitx5: ] 
D/fcitx5: Failed to load pinyin history: 
D/fcitx5: io fail: unspecified iostream_category error
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:02.299407
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: pinyin
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:02.305780
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: cloudpinyin
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:02.689011
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: spell
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:04.757577
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 83
D/fcitx5: ] 
D/fcitx5: KeyEvent key: n isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:04.785501
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 83
D/fcitx5: ] 
D/fcitx5: KeyEvent key: i isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:04.833655
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 83
D/fcitx5: ] 
D/fcitx5: KeyEvent key: h isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:04.858009
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 83
D/fcitx5: ] 
D/fcitx5: KeyEvent key: a isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:04.872927
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 83
D/fcitx5: ] 
D/fcitx5: KeyEvent key: o isRelease: 0 accepted: 1
D/androidfrontend: candidateListCallback
D/androidfrontend: 108 candidates
D/FcitxEvent: CandidateList, [108]你好,你,尼,泥,妮,逆,腻,拟,呢,倪,妳,溺,👋,祢,匿,霓,昵,睨,怩,猊,擬,膩,鲵,旎,坭,伲,铌,輗,袮,貎,儗,麑,抳,柅,暱,埿,禰,惄,薿,孨,聻,蜺,苨,迡,檷,嫟,眤,籾,秜,縌,腝,馜,鯢,氼,狔,孴,婗,痆,懝,胒,隬,棿,齯,晲,淣,㘈,掜,抐,愵,屰,屔,嬺,堄,儞,聣,伱,䵒,䵑,䦵,䝚,臡,䛏,䘽,蚭,蛪,䘦,䘌,觬,誽,譺,䕥,跜,䁥,㹸,㵫,郳,鈮,鑈,㲻,㮏,㪒,㩘,鯓,㦐,㥾,㠜,㞾,𣲷
D/androidfrontend: select candidate #42
D/androidfrontend: candidateListCallback
D/androidfrontend: 90 candidates
D/FcitxEvent: CandidateList, [90]好,号,浩,豪,耗,毫,郝,昊,嚎,皓,號,蒿,灏,蚝,壕,镐,濠,嗥,哈,薅,貉,颢,晧,皞,暠,蠔,灝,滈,淏,呺,恏,鎬,鄗,皜,顥,澔,秏,嚆,譹,暤,諕,竓,哠,籇,藃,茠,傐,儫,椃,䪽,䧫,㘪,嘷,噑,䧚,虠,㙱,薧,峼,䝥,悎,薃,昦,㚪,聕,䝞,暭,曍,鰝,毜,㝀,㞻,䚽,䒵,㬶,㠙,皥,㬔,獆,獋,獔,皡,㩝,䯫,蛤,虾,铪,奤,鉿,丷
D/androidfrontend: select candidate #42
D/fcitx5: I
D/fcitx5: 2021-06-18 22:15:05.848549
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 29
D/fcitx5: ] 
D/fcitx5: Commit: 苨哠
D/androidfrontend: commitStringCallback
D/FcitxEvent: CommitString, 苨哠
D/androidfrontend: candidateListCallback
D/FcitxEvent: CandidateList, [0]
```
</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.