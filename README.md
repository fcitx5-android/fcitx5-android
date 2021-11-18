# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, print to logcat, and dispatch event to JVM side.

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

![open SDK Manager](https://user-images.githubusercontent.com/48406926/142432806-d3ee3c16-beee-409e-9f6b-60c352c3f230.png)

![install SDK Platform](https://user-images.githubusercontent.com/48406926/142432902-d979ceda-1c12-4c9c-a59f-ffa201457861.png)

![install SDK Build-Tools](https://user-images.githubusercontent.com/48406926/142432955-380ccd4a-df11-46ae-a520-3c13eac38960.png)

![install NDK](https://user-images.githubusercontent.com/48406926/142433006-9dfbeb20-b3e3-4230-aa28-625efb58f936.png)

![install CMake](https://user-images.githubusercontent.com/48406926/142433080-a4ad2446-889a-479c-837a-c2b5ad74b104.png)

### Additional patches

There are no additional patches needed for now, because we switched to a forked version of [fcitx5-chinese-addons](https://github.com/rocka/fcitx5-chinese-addons/tree/android), and we hope to find some way to upstream those changes.

## PoC

<details>
<summary>ScreenShots and Videos</summary>

<img src="https://user-images.githubusercontent.com/13914967/123126973-704c1680-d47c-11eb-8852-aa44d4516dcd.png" width="360px">

[Video in Telegram group](https://t.me/archlinuxcn_offtopic/3462389)

[Video on Mastodon](https://sn.angry.im/web/statuses/106452677691097114)

</details>

<details>
<summary>Logcat</summary>

```
D/JNI: startupFcitx
D/fcitx5: I2021-06-30 23:30:42.037640 instance.cpp:1371] Override Enabled Addons: {}
D/fcitx5: I2021-06-30 23:30:42.037898 instance.cpp:1372] Override Disabled Addons: {}
D/fcitx5: I2021-06-30 23:30:42.045989 addonmanager.cpp:189] Loaded addon unicode
D/fcitx5: I2021-06-30 23:30:42.047548 addonmanager.cpp:189] Loaded addon quickphrase
D/fcitx5: I2021-06-30 23:30:42.049194 addonmanager.cpp:189] Loaded addon pinyinhelper
D/fcitx5: I2021-06-30 23:30:42.050042 addonmanager.cpp:189] Loaded addon androidfrontend
D/fcitx5: E2021-06-30 23:30:42.057642 instance.cpp:1381] Couldn't find keyboard-us
D/fcitx5: I2021-06-30 23:30:42.060428 addonmanager.cpp:189] Loaded addon punctuation
D/fcitx5: E2021-06-30 23:30:42.392059 pinyin.cpp:659] Failed to load pinyin history: io fail: unspecified iostream_category error
D/fcitx5: I2021-06-30 23:30:42.413191 addonmanager.cpp:189] Loaded addon pinyin
D/fcitx5: I2021-06-30 23:30:42.414280 addonmanager.cpp:189] Loaded addon fullwidth
D/fcitx5: I2021-06-30 23:30:42.415101 addonmanager.cpp:189] Loaded addon chttrans
D/FcitxEvent: type=3, params=[2]拼,
D/fcitx5: I2021-06-30 23:30:42.416938 androidfrontend.cpp:87] bulkCandidateList: no or empty candidateList
D/FcitxEvent: type=0, params=[0]
D/fcitx5: I2021-06-30 23:30:43.501732 addonmanager.cpp:189] Loaded addon spell
D/FcitxEvent: type=2, params=[2]n,你
D/fcitx5: I2021-06-30 23:30:43.516066 androidfrontend.cpp:125] KeyEvent(key=Key(n states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=3, params=[2],
D/FcitxEvent: type=0, params=[1250]你,年,那,呢,能,内,您,女,男,拿,难,牛,南...
I/zygote64: Do partial code cache collection, code=30KB, data=28KB
I/zygote64: After code cache collection, code=30KB, data=28KB
I/zygote64: Increasing code cache capacity to 128KB
D/FcitxEvent: type=2, params=[2]ni,你
D/fcitx5: I2021-06-30 23:30:43.672417 androidfrontend.cpp:125] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[183]你,尼,泥,妮,逆,腻,拟,呢,倪,妳,溺,祢,匿...
D/FcitxEvent: type=2, params=[2]ni h,你好
D/fcitx5: I2021-06-30 23:30:43.965315 androidfrontend.cpp:125] KeyEvent(key=Key(h states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[190]你好,你会,你还,你,你和,你很,霓虹,尼,泥...
D/FcitxEvent: type=2, params=[2]ni ha,你哈
D/fcitx5: I2021-06-30 23:30:44.107517 androidfrontend.cpp:125] KeyEvent(key=Key(a states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[184]你哈,你,尼,泥,妮,逆,腻,拟,呢,倪,妳,溺...
I/zygote64: Do partial code cache collection, code=61KB, data=47KB
I/zygote64: After code cache collection, code=54KB, data=44KB
I/zygote64: Increasing code cache capacity to 256KB
D/FcitxEvent: type=2, params=[2]ni hao,你好
D/fcitx5: I2021-06-30 23:30:44.328185 androidfrontend.cpp:125] KeyEvent(key=Key(o states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[184]你好,你,尼,泥,妮,逆,腻,拟,呢,倪,妳,溺...
D/FcitxEvent: type=2, params=[2]ni hao s,你好事
D/fcitx5: I2021-06-30 23:30:44.599255 androidfrontend.cpp:125] KeyEvent(key=Key(s states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[186]你好事,你好受,你好,你,尼,泥,妮,逆,腻...
D/FcitxEvent: type=2, params=[2]ni hao sh,你好事
D/fcitx5: I2021-06-30 23:30:44.762583 androidfrontend.cpp:125] KeyEvent(key=Key(h states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[186]你好事,你好受,你好,你,尼,泥,妮,逆,腻...
D/FcitxEvent: type=2, params=[2]ni hao shi,你好事
D/fcitx5: I2021-06-30 23:30:44.963864 androidfrontend.cpp:125] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[186]你好事,你好是,你好,你,尼,泥,妮,逆,腻...
D/FcitxEvent: type=2, params=[2]ni hao shi j,你好时间
D/fcitx5: I2021-06-30 23:30:45.189018 androidfrontend.cpp:125] KeyEvent(key=Key(j states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[186]你好时间,你好世界,你好,你,尼,泥,妮...
D/FcitxEvent: type=2, params=[2]ni hao shi ji,你好世纪
D/fcitx5: I2021-06-30 23:30:45.353536 androidfrontend.cpp:125] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[186]你好世纪,你好实际,你好,你,尼,泥,妮...
D/FcitxEvent: type=2, params=[2]ni hao shi jie,你好世界
D/fcitx5: I2021-06-30 23:30:45.621923 androidfrontend.cpp:125] KeyEvent(key=Key(e states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[186]你好世界,你好时节,你好,你,尼,泥,妮...
D/JNI: select candidate #0
D/FcitxEvent: type=1, params=[1]你好世界
D/FcitxEvent: type=2, params=[2],
D/FcitxEvent: type=2, params=[2],
D/fcitx5: I2021-06-30 23:30:46.240326 androidfrontend.cpp:87] bulkCandidateList: no or empty candidateList
D/FcitxEvent: type=0, params=[0]
I/zygote64: Do full code cache collection, code=118KB, data=99KB
I/zygote64: After code cache collection, code=104KB, data=60KB
D/FcitxEvent: type=2, params=[2]s,是
D/fcitx5: I2021-06-30 23:30:48.395683 androidfrontend.cpp:125] KeyEvent(key=Key(s states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[3117]是,说,上,时,三,水,生,所,山,事,少...
D/FcitxEvent: type=2, params=[2]sh,是
D/fcitx5: I2021-06-30 23:30:48.498730 androidfrontend.cpp:125] KeyEvent(key=Key(h states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[1883]是,说,上,时,水,生,山,事,少,书,神...
D/FcitxEvent: type=2, params=[2]shi,是
D/fcitx5: I2021-06-30 23:30:48.635653 androidfrontend.cpp:125] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[406]是,时,事,使,石,师,诗,十,市,式,世...
D/FcitxEvent: type=2, params=[2]shi j,时间
D/fcitx5: I2021-06-30 23:30:48.894473 androidfrontend.cpp:125] KeyEvent(key=Key(j states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[452]时间,世界,事件,实际,世纪,实践,是...
D/FcitxEvent: type=2, params=[2]shi ji,世纪
D/fcitx5: I2021-06-30 23:30:49.103983 androidfrontend.cpp:125] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[417]世纪,实际,十几,是,时机,事迹,史记...
D/FcitxEvent: type=2, params=[2]shi jie,世界
D/fcitx5: I2021-06-30 23:30:49.326505 androidfrontend.cpp:125] KeyEvent(key=Key(e states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[425]世界,是,时节,师姐,时,视界,石阶...
D/FcitxEvent: type=2, params=[2]shi ji en,实际恩
D/fcitx5: I2021-06-30 23:30:49.477205 androidfrontend.cpp:125] KeyEvent(key=Key(n states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[419]实际恩,实际嗯,实际,世纪,十几,是...
D/FcitxEvent: type=2, params=[2]shi jie ni,世界你
D/fcitx5: I2021-06-30 23:30:49.736813 androidfrontend.cpp:125] KeyEvent(key=Key(i states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[426]世界你,世界,是,时节,师姐,时,视界...
D/FcitxEvent: type=2, params=[2]shi jie ni h,世界你好
D/fcitx5: I2021-06-30 23:30:49.931826 androidfrontend.cpp:125] KeyEvent(key=Key(h states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[426]世界你好,世界,是,时节,师姐,时,视界...
I/zygote64: Do partial code cache collection, code=124KB, data=98KB
I/zygote64: After code cache collection, code=124KB, data=98KB
I/zygote64: Increasing code cache capacity to 512KB
D/FcitxEvent: type=2, params=[2]shi jie ni ha,世界你哈
D/fcitx5: I2021-06-30 23:30:50.095093 androidfrontend.cpp:125] KeyEvent(key=Key(a states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[426]世界你哈,世界,是,时节,师姐,时,视界...
D/FcitxEvent: type=2, params=[2]shi jie ni hao,世界你好
D/fcitx5: I2021-06-30 23:30:50.309284 androidfrontend.cpp:125] KeyEvent(key=Key(o states=0), isRelease=0, accepted=1)
D/FcitxEvent: type=0, params=[426]世界你好,世界,是,时节,师姐,时,视界...
D/JNI: select candidate #0
D/FcitxEvent: type=1, params=[1]世界你好
D/FcitxEvent: type=2, params=[2],
D/FcitxEvent: type=2, params=[2],
D/fcitx5: I2021-06-30 23:30:50.985314 androidfrontend.cpp:87] bulkCandidateList: no or empty candidateList
D/FcitxEvent: type=0, params=[0]
```

</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
