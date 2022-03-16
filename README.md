# fcitx5-android

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
git clone git@github.com:fcitx5-android/fcitx5-android.git
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

## Screenshots

|light|dark|
|:-:|:-:|
|<img src="https://user-images.githubusercontent.com/13914967/145842174-c2a1b9ae-1e15-4722-8c27-986ef8cc7163.png" width="360px">|<img src="https://user-images.githubusercontent.com/13914967/145842188-465b6677-3d4a-432f-a499-9a9bf877b617.png" width="360px">|

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
