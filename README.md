# fcitx5-android

An attempt to run fcitx5 on Android.

[![Jenkins Build Status](https://img.shields.io/jenkins/s/https/jenkins.fcitx-im.org/job/android/job/fcitx5-android.svg)](https://jenkins.fcitx-im.org/job/android/job/fcitx5-android/)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/org.fcitx.fcitx5.android)

## Project status

### Implemented

- Virtual Keyboard (layout not customizable yet)
- Expandable candidate view
- Clipboard management (plain text only)
- Theming (custom color scheme and background image)
- Popup preview on key press
- Long press popup keyboard for convenient symbol input
- Symbol and Emoji picker

### Work in progress

- Customizable keyboard layout
- More input methods

## Screenshots

|拼音, Material Light theme, key border enabled|自然码双拼, Pixel Dark theme, key border disabled|
|:-:|:-:|
|<img src="https://user-images.githubusercontent.com/13914967/202180575-04b6db41-ff24-4bef-899a-8051fc0243f5.png" width="360px">|<img src="https://user-images.githubusercontent.com/13914967/202180709-457e4897-961f-48a6-8fb2-b6560568a122.png" width="360px">|

|Emoji picker, Pixel Light theme, key border enabled|Symbol picker, Material Dark theme, key border disabled|
|:-:|:-:|
|<img src="https://user-images.githubusercontent.com/13914967/202181845-6a5f6bb2-a877-468c-851a-fd7e66e64ed4.png" width="360px">|<img src="https://user-images.githubusercontent.com/13914967/202181861-dd253439-1d5e-4f5f-9535-934f28796a6b.png" width="360px">|

## Get involved

Trello kanban: https://trello.com/b/gftk6ZdV/kanban

Matrix Room: https://matrix.to/#/#fcitx5-android:mozilla.org

Discuss on Telegram: https://t.me/+hci-DrFVWUM3NTUx ([@fcitx5_android](https://t.me/fcitx5_android) originally)

## Build

### Dependencies

- Android SDK Platform & Build-Tools 33.
- Android NDK (Side by side) 25 & CMake 3.22.1, they can be installed using SDK Manager in Android Studio or `sdkmanager` command line. **Note:** NDK 21 & 22 are confirmed not working with this project.
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)
- GNU Gettext >= 0.20 (for `msgfmt` binary; or install `appstream` if you really have to use gettext <= 0.19.)

### How to set up development environment

First, clone this repository and fetch all submodules:

```sh
git clone git@github.com:fcitx5-android/fcitx5-android.git
git submodule update --init --recursive
```

Install extra-cmake-modules from your distribution software repository:

```sh
# For Arch Linux (Arch has gettext in it's base meta package)
sudo pacman -S extra-cmake-modules
# For Debian/Ubuntu
sudo apt install extra-cmake-modules gettext
```

Install Android SDK Platform, Android SDK Build-Tools, Android NDK and cmake via SDK Manager in Android Studio:

<details>
<summary>Detailed steps (screenshots)</summary>

![Open SDK Manager](https://user-images.githubusercontent.com/13914967/202184493-3ee1546b-0a83-4cc9-9e41-d20b0904a0cf.png)

![Install SDK Platform](https://user-images.githubusercontent.com/13914967/202184534-340a9e7c-7c42-49bd-9cf5-1ec9dcafcf32.png)

![Install SDK Build-Tools](https://user-images.githubusercontent.com/13914967/202185945-0c7a9f39-1fcc-4018-9c81-b3d2bf1c2d3f.png)

![Install NDK](https://user-images.githubusercontent.com/13914967/202185601-0cf877ea-e148-4b88-bd2f-70533189b3d4.png)

![Install CMake](https://user-images.githubusercontent.com/13914967/202184655-3c1ab47c-432f-4bd7-a508-92096482de50.png)

</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
