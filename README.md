# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, print to logcat, and dispatch event to JVM side. Also there is a minimal virtual keyboard.

## Build

### Dependencies

- Android SDK Platform & Build-Tools 30 or newer version
- Android NDK (Side by side) 23 & cmake 3.18.1, they can be installed using SDK Manager in Android Studio or `sdkmanager` command line. **Note:** NDK 21 & 22 are confirmed not working with this project.
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)
- GNU Gettext (`msgfmt` binary)

### How to set up development environment

First, clone this repository and fetch all submodules:

```sh
git clone git@github.com:rocka/fcitx5-android-poc.git
git submodule update --init --recursive
```

Install extra-cmake-modules from your distribution software repository:

```sh
# For Arch
sudo pacman -S extra-cmake-modules
# For Debian/Ubuntu
sudo apt install extra-cmake-modules
```

Install Android SDK Platform, Android SDK Build-Tools, Android NDK and cmake via SDK Manager in Android Studio:

<details>
<summary>Detailed steps (screenshots)</summary>

![open SDK Manager](https://user-images.githubusercontent.com/48406926/142432806-d3ee3c16-beee-409e-9f6b-60c352c3f230.png)

![install SDK Platform](https://user-images.githubusercontent.com/48406926/142432902-d979ceda-1c12-4c9c-a59f-ffa201457861.png)

![install SDK Build-Tools](https://user-images.githubusercontent.com/48406926/142432955-380ccd4a-df11-46ae-a520-3c13eac38960.png)

![install NDK](https://user-images.githubusercontent.com/48406926/142433006-9dfbeb20-b3e3-4230-aa28-625efb58f936.png)

![install CMake](https://user-images.githubusercontent.com/48406926/142433080-a4ad2446-889a-479c-837a-c2b5ad74b104.png)

</details>

### Additional patches

No patching needed! We can run mainline fcitx5 on Android! Yay!

## PoC

### Screenshots

|light|dark|
|:-:|:-:|
|<img src="https://user-images.githubusercontent.com/13914967/145842174-c2a1b9ae-1e15-4722-8c27-986ef8cc7163.png" width="360px">|<img src="https://user-images.githubusercontent.com/13914967/145842188-465b6677-3d4a-432f-a499-9a9bf877b617.png" width="360px">|

### Logcat

<details>
<summary>(click to expand)</summary>

```
D/JNI: startupFcitx: starting...
D/JNI: fcitx is not running!
D/fcitx5: I2021-12-13 23:51:05.375939 instance.cpp:1404] Override Enabled Addons: {}
D/fcitx5: I2021-12-13 23:51:05.376194 instance.cpp:1405] Override Disabled Addons: {}
D/fcitx5: I2021-12-13 23:51:05.403293 addonmanager.cpp:191] Loaded addon unicode
D/fcitx5: I2021-12-13 23:51:05.438039 addonmanager.cpp:191] Loaded addon quickphrase
D/fcitx5: I2021-12-13 23:51:05.455416 addonmanager.cpp:191] Loaded addon pinyinhelper
D/fcitx5: I2021-12-13 23:51:05.465427 addonmanager.cpp:191] Loaded addon androidkeyboard
D/fcitx5: I2021-12-13 23:51:05.471885 addonmanager.cpp:191] Loaded addon androidfrontend
D/fcitx5: I2021-12-13 23:51:05.485193 inputmethodmanager.cpp:198] Found 1 input method(s) in addon androidkeyboard
D/fcitx5: I2021-12-13 23:51:05.502692 addonmanager.cpp:191] Loaded addon punctuation
D/JNI: startupFcitx: setupCallback
D/FcitxEvent: Ready[0]
D/me.rocka.fcitx5test.FcitxDaemon$fcitx$1$1: FcitxDaemon onReady
D/fcitx5: I2021-12-13 23:51:11.512154 addonmanager.cpp:191] Loaded addon pinyin
D/fcitx5: I2021-12-13 23:51:11.524024 addonmanager.cpp:191] Loaded addon fullwidth
D/fcitx5: I2021-12-13 23:51:11.530895 addonmanager.cpp:191] Loaded addon chttrans
D/FcitxEvent: Change[1]InputMethodEntry(uniqueName=shuangpin, name=Shuangpin, icon=fcitx-shuangpin, nativeName=双拼, label=双, languageCode=zh_CN, isConfigurable=true, subMode=InputMethodSubMode(name=自然码, label=, icon=))
D/FcitxEvent: Preedit[3], , -1
D/FcitxEvent: Aux[2]Shuangpin (自然码), 
D/FcitxEvent: Candidate[0]
D/FcitxEvent: Preedit[3], , -1
D/FcitxEvent: Aux[2], 
D/FcitxEvent: Candidate[0]
D/fcitx5: I2021-12-13 23:51:16.302527 addonmanager.cpp:191] Loaded addon spell
D/fcitx5: I2021-12-13 23:51:16.376900 androidfrontend.cpp:133] KeyEvent(key=Key(n states=0), isRelease=0, accepted=1)
D/FcitxEvent: Preedit[3]n, , -1
D/FcitxEvent: Aux[2], 
D/FcitxEvent: Candidate[1255]呢, n, 你, 年, 那, 能, 内, 您, 女, 男
D/fcitx5: I2021-12-13 23:51:16.981463 androidfrontend.cpp:133] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: Preedit[3]ni, , -1
D/FcitxEvent: Aux[2], 
D/FcitxEvent: Candidate[183]你, 呢, 尼, 泥, 妮, 逆, 腻, 拟, 倪, 妳
D/fcitx5: I2021-12-13 23:51:17.818163 androidfrontend.cpp:133] KeyEvent(key=Key(h states=0), isRelease=0, accepted=1)
D/FcitxEvent: Preedit[3]ni h, , -1
D/FcitxEvent: Aux[2], 
D/FcitxEvent: Candidate[203]你好, nih, 你会, 你还, 你, 你和, 你很, 霓虹, 呢, 尼
D/fcitx5: I2021-12-13 23:51:18.105176 androidfrontend.cpp:133] KeyEvent(key=Key(k states=0), isRelease=0, accepted=1)
D/FcitxEvent: Preedit[3]ni hk, , -1
D/FcitxEvent: Aux[2], 
D/FcitxEvent: Candidate[185]你好, 你, 呢, 尼, 泥, 妮, 逆, 腻, 拟, 倪
D/JNI: selectCandidate: #0
D/FcitxEvent: Commit[1]你好
D/FcitxEvent: Preedit[3], , -1
D/FcitxEvent: Aux[2], 
D/FcitxEvent: Candidate[0]
```

</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
