# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, print to logcat, and dispatch event to JVM side. Also there is a minimal virtual keyboard.

## Build

### Dependencies

- Android SDK Platform & Build-Tools 30 or newer version
- Android NDK (Side by side) 23 & cmake 3.18.1, they can be installed using SDK Manager in Android Studio or `sdkmanager` command line. **Note:** NDK 21 & 22 are confirmed not working with this project.
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)

### How to set up development environment

First, clone this repository and fetch all submodules:

```
git clone git@github.com:rocka/fcitx5-android-poc.git
git submodule update --init --recursive
```

Install extra-cmake-modules from your distribution software repository:

```
# For Arch
sudo pacman -S extra-cmake-modules
# For Debian/Ubuntu
sudo apt install extra-cmake-modules
```

Install Android SDK Platform, Android SDK Build-Tools, Android NDK and cmake via SDK Manager in Android Studio:

<details>
<summary>Detailed steps (Screenshot)</summary>

![open SDK Manager](https://user-images.githubusercontent.com/48406926/142432806-d3ee3c16-beee-409e-9f6b-60c352c3f230.png)

![install SDK Platform](https://user-images.githubusercontent.com/48406926/142432902-d979ceda-1c12-4c9c-a59f-ffa201457861.png)

![install SDK Build-Tools](https://user-images.githubusercontent.com/48406926/142432955-380ccd4a-df11-46ae-a520-3c13eac38960.png)

![install NDK](https://user-images.githubusercontent.com/48406926/142433006-9dfbeb20-b3e3-4230-aa28-625efb58f936.png)

![install CMake](https://user-images.githubusercontent.com/48406926/142433080-a4ad2446-889a-479c-837a-c2b5ad74b104.png)

</details>

### Additional patches

There are no additional patches needed for now, because we switched to a forked version of [fcitx5-chinese-addons](https://github.com/rocka/fcitx5-chinese-addons/tree/android), and we hope to find some way to upstream those changes.

## PoC

<details>
<summary>ScreenShots and Videos</summary>

<img src="https://user-images.githubusercontent.com/13914967/142570113-7676d0bc-7902-4a8b-85be-a78b901fc0e9.png" width="360px">

[Video in Telegram group](https://t.me/fcitx5_android/2969)

</details>

<details>
<summary>Logcat</summary>

```
D/JNI: startupFcitx: starting...
D/fcitx5: I2021-11-19 13:21:09.468726 instance.cpp:1392] Override Enabled Addons: {}
D/fcitx5: I2021-11-19 13:21:09.468852 instance.cpp:1393] Override Disabled Addons: {}
D/fcitx5: I2021-11-19 13:21:09.474386 addonmanager.cpp:190] Loaded addon unicode
D/fcitx5: I2021-11-19 13:21:09.494566 addonmanager.cpp:190] Loaded addon quickphrase
D/fcitx5: I2021-11-19 13:21:09.495698 addonmanager.cpp:190] Loaded addon pinyinhelper
D/fcitx5: I2021-11-19 13:21:09.497883 addonmanager.cpp:190] Loaded addon androidkeyboard
D/fcitx5: I2021-11-19 13:21:09.498661 addonmanager.cpp:190] Loaded addon androidfrontend
D/fcitx5: I2021-11-19 13:21:09.503314 inputmethodmanager.cpp:198] Found 1 input method(s) in addon androidkeyboard
D/fcitx5: I2021-11-19 13:21:09.505464 addonmanager.cpp:190] Loaded addon punctuation
D/fcitx5: I2021-11-19 13:21:09.903108 addonmanager.cpp:190] Loaded addon pinyin
D/fcitx5: I2021-11-19 13:21:09.905544 addonmanager.cpp:190] Loaded addon fullwidth
D/fcitx5: I2021-11-19 13:21:09.907832 addonmanager.cpp:190] Loaded addon chttrans
D/JNI: startupFcitx: setupCallback
D/FcitxEvent: type=4, params=[0]
D/FcitxEvent: type=3, params=[2]拼, 
D/FcitxEvent: type=0, params=[0]
W/System: A resource failed to call close. 
I/chatty: uid=10231(me.rocka.fcitx5test) FinalizerDaemon identical 38 lines
W/System: A resource failed to call close. 
D/FcitxEvent: type=3, params=[2], 
D/FcitxEvent: type=0, params=[0]
D/fcitx5: I2021-11-19 13:21:11.086549 addonmanager.cpp:190] Loaded addon spell
D/FcitxEvent: type=2, params=[2]n, 你
D/fcitx5: I2021-11-19 13:21:11.148572 androidfrontend.cpp:139] KeyEvent(key=Key(n states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[1251]你, n, 年, 那, 呢, 能, 内, 您, 女, 男
D/FcitxEvent: type=2, params=[2]ni, 你
D/fcitx5: I2021-11-19 13:21:11.526026 androidfrontend.cpp:139] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[184]你, ni, 尼, 泥, 妮, 逆, 腻, 拟, 呢, 倪
D/FcitxEvent: type=2, params=[2]ni h, 你好
D/fcitx5: I2021-11-19 13:21:12.405492 androidfrontend.cpp:139] KeyEvent(key=Key(h states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[193]你好, nih, 你会, 你, 你还, 你和, 你后, 你很, 你或, 霓虹
D/FcitxEvent: type=2, params=[2]ni ha, 你哈
D/fcitx5: I2021-11-19 13:21:12.692733 androidfrontend.cpp:139] KeyEvent(key=Key(a states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[185]你哈, nihal, 你, 尼, 泥, 妮, 逆, 腻, 拟, 呢
D/FcitxEvent: type=2, params=[2]ni hao, 你好
D/fcitx5: I2021-11-19 13:21:12.888586 androidfrontend.cpp:139] KeyEvent(key=Key(o states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[184]你好, 你, 尼, 泥, 妮, 逆, 腻, 拟, 呢, 倪
D/JNI: selectCandidate: #0
D/FcitxEvent: type=1, params=[1]你好
D/FcitxEvent: type=2, params=[2], 
D/FcitxEvent: type=0, params=[0]
```

</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
