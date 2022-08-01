# fcitx5-android

An attempt to run fcitx5 on Android.

[![Jenkins Build Status](https://img.shields.io/jenkins/s/https/jenkins.fcitx-im.org/job/android/job/fcitx5-android.svg)](https://jenkins.fcitx-im.org/job/android/job/fcitx5-android/)

## Project status

### Implemented

- Virtual Keyboard (layout not customizable yet)
- Expandable candidate view
- Clipboard management (plain text only)
- Themeing (custom color scheme and background image)
- Popup preview on key press

### Work in progress

- Customiziable keyboard layout
- More input methods
- Long press popup keyboard for convenient symbol input
- User-friendly symbol / emoji selector

## Screenshots

|拼音, builtin light theme, key border enabled|自然码双拼, builtin dark theme, border disabled|
|:-:|:-:|
|<img src="https://user-images.githubusercontent.com/13914967/172801207-0a229424-9a19-4d06-bdd4-2accf61f4de5.png" width="360px">|<img src="https://user-images.githubusercontent.com/13914967/172801229-e7d51003-d80c-462d-a756-a0c22e1d7dee.png" width="360px">|

## Get involved

Trello kanban: https://trello.com/b/gftk6ZdV/kanban

Discuss on Telegram: https://t.me/fcitx5_android

## Build

### Dependencies

- Android SDK Platform & Build-Tools 31 or newer version
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

![open SDK Manager](https://user-images.githubusercontent.com/48406926/142432806-d3ee3c16-beee-409e-9f6b-60c352c3f230.png)

![install SDK Platform](https://user-images.githubusercontent.com/48406926/142432902-d979ceda-1c12-4c9c-a59f-ffa201457861.png)

![install SDK Build-Tools](https://user-images.githubusercontent.com/48406926/142432955-380ccd4a-df11-46ae-a520-3c13eac38960.png)

![install NDK](https://user-images.githubusercontent.com/48406926/142433006-9dfbeb20-b3e3-4230-aa28-625efb58f936.png)

![install CMake](https://user-images.githubusercontent.com/48406926/142433080-a4ad2446-889a-479c-837a-c2b5ad74b104.png)

</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
