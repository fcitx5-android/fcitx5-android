# fcitx5-android-poc

An attempt to run fcitx5 on Android.

## Project status

It can build, run, print to logcat, and dispatch event to JVM side.

## Build

### Dependencies

- Android SDK Platform & Build-Tools 30
- Android NDK (Side by side) 23 & cmake 3.18.1, they can be installed using SDK Manager in Android Studio or `sdkmanager` command line. **Note:** you may need to install Android Studio Beta for Android NDK 23, or use `sdkmanager` from Android SDK Command-line Tools. NDK 21 & 22 are confirmed not working with this project.
- [KDE/extra-cmake-modules](https://github.com/KDE/extra-cmake-modules)

### `libime` data

I don't know why cmake won't download and generate those data. Just install [libime](https://archlinux.org/packages/community/x86_64/libime/), and copy `/usr/{lib,share}/libime/*` to `app/src/main/assets/fcitx5/libime/`.

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
I/me.rocka.fcitx5test.MainActivity: /data/user/0/me.rocka.fcitx5test/fcitx5/addon, /data/user/0/me.rocka.fcitx5test/fcitx5/inputmethod, /data/user/0/me.rocka.fcitx5test/fcitx5/libime
D/androidfrontend: startupFcitx ...
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.484533 instance.cpp:1371] Override Enabled Addons: {}
    I2021-06-23 23:29:32.484649 instance.cpp:1372] Overri
D/fcitx5: de Disabled Addons: {}
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.500251
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: unicode
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.502117
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: androidfrontend
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.509935
D/fcitx5:  
D/fcitx5: inputmethodmanager.cpp:
D/fcitx5: 117
D/fcitx5: ] No valid input method group in configuration. 
D/fcitx5: Building a default one
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.511463 
D/fcitx5: instance.cpp:
D/fcitx5: 730] 
D/fcitx5: Items in 
D/fcitx5: Default
D/fcitx5: : 
D/fcitx5: [
D/fcitx5: InputMethodGroupItem(
D/fcitx5: keyboard-us
D/fcitx5: ,layout=
D/fcitx5: )]
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.511638 
D/fcitx5: instance.cpp:
D/fcitx5: 735
D/fcitx5: ] 
D/fcitx5: Generated groups: [
D/fcitx5: Default
D/fcitx5: ]
D/fcitx5: E
D/fcitx5: 2021-06-23 23:29:32.512076 
D/fcitx5: instance.cpp:
D/fcitx5: 1381] 
D/fcitx5: Couldn't find keyboard-us
W/Thread-2: type=1400 audit(0.0:10130): avc: denied { read } for name="uuid" dev="proc" ino=4490765 scontext=u:r:untrusted_app:s0:c512,c768 tcontext=u:object_r:proc:s0 tclass=file permissive=0
D/fcitx5: I2021-06-23 23:29:32.517618 addonmanager.cpp:189] Loaded addon punctuation
D/fcitx5: E
D/fcitx5: 2021-06-23 23:29:32.797390
D/fcitx5:  
D/fcitx5: pinyin.cpp
D/fcitx5: :
D/fcitx5: 659
D/fcitx5: ] 
D/fcitx5: Failed to load pinyin history: 
D/fcitx5: io fail: unspecified iostream_category error
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:32.818567
D/fcitx5:  
D/fcitx5: addonmanager.cpp
D/fcitx5: :
D/fcitx5: 189
D/fcitx5: ] 
D/fcitx5: Loaded addon 
D/fcitx5: pinyin
D/androidfrontend: preeditCallback
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=n, clientPreedit=你)
D/FcitxEvent: type: 2, args: [2]n,你
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:33.556001
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 39
D/fcitx5: ] 
D/fcitx5: preedit: "n"; clientPreedit: 你
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:33.556418
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: KeyEvent key: n isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:33.556674
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 59
D/fcitx5: ] 
D/fcitx5: updateClientSideUIImpl: 721 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 721 candidates
D/FcitxEvent: type: 0, args: [721]你,年,那,呢,能,内,您,...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你, 年, 那, 呢, 能, 内...
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2]ni,你
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:33.695244
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 39
D/fcitx5: ] 
D/fcitx5: preedit: "ni"; clientPreedit: 你
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:33.697131
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: KeyEvent key: i isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:33.697409
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 59
D/fcitx5: ] 
D/fcitx5: updateClientSideUIImpl: 106 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 106 candidates
D/FcitxEvent: type: 0, args: [106]你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni, clientPreedit=你)
I/me.rocka.fcitx5test.MainActivity: candidate update: [你, 尼, 泥, 妮, 逆, 腻...
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2]ni h,你好
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni h, clientPreedit=你好)
D/androidfrontend: candidateListCallback
D/androidfrontend: 113 candidates
D/fcitx5: I2021-06-23 23:29:33.955087 androidfrontend.cpp:39] preedit: "ni h"; clientPreedit: 你好
    I2021-06-23 23:29:33.955502 androidf
D/fcitx5: rontend.cpp:117] KeyEvent key: h isRelease: 0 accepted: 1
    I2021-06-23 23:29:33.955602 androidfrontend.cpp:59] updateClientSideU
D/fcitx5: IImpl: 113 candidates
D/FcitxEvent: type: 0, args: [113]你好,你会,你还,你,你和,你很,霓虹,尼,泥,拟合...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好, 你会, 你还, 你...
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2]ni ha,你哈
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.121912 androidfrontend.cpp:
D/fcitx5: 39] 
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni ha, clientPreedit=你哈)
D/fcitx5: preedit: "ni ha"; clientPreedit: 你哈
D/androidfrontend: candidateListCallback
D/androidfrontend: 107 candidates
D/fcitx5: I2021-06-23 23:29:34.122393 androidfrontend.cpp:117] KeyEvent key: a isRelease: 0 accepted: 1
    I2021-06-23 23:29:34.122484 andro
D/fcitx5: idfrontend.cpp:59] updateClientSideUIImpl: 107 candidates
D/FcitxEvent: type: 0, args: [107]你哈,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你哈, 你, 尼, 泥, 妮...
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2]ni hao,你好
D/fcitx5: I
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao, clientPreedit=你好)
D/fcitx5: 2021-06-23 23:29:34.342379
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :39] 
D/fcitx5: preedit: "ni hao"; clientPreedit: 你好
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.343558
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: KeyEvent key: o isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.344142
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 59
D/fcitx5: ] 
D/fcitx5: updateClientSideUIImpl: 107 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 107 candidates
D/FcitxEvent: type: 0, args: [107]你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好, 你, 尼, 泥, 妮, 逆...
D/androidfrontend: preeditCallback
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao s, clientPreedit=你好事)
D/FcitxEvent: type: 2, args: [2]ni hao s,你好事
D/fcitx5: I2021-06-23 23:29:34.577992 androidfrontend.cpp:39] preedit: "ni hao s"; clientPreedit: 你好事
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.578674
D/fcitx5:  
D/fcitx5: androidfrontend.cpp:
D/fcitx5: 117] 
D/fcitx5: KeyEvent key: s isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.579132
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :59] 
D/fcitx5: updateClientSideUIImpl: 109 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 109 candidates
D/FcitxEvent: type: 0, args: [109]你好事,你好受,你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好事, 你好受, 你好, 你...
D/androidfrontend: preeditCallback
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao sh, clientPreedit=你好事)
D/FcitxEvent: type: 2, args: [2]ni hao sh,你好事
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.778517
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 39
D/fcitx5: ] 
D/fcitx5: preedit: "ni hao sh"; clientPreedit: 你好事
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.779629
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: KeyEvent key: h isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.780201
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 59
D/fcitx5: ] 
D/fcitx5: updateClientSideUIImpl: 109 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 109 candidates
D/FcitxEvent: type: 0, args: [109]你好事,你好受,你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好事, 你好受, 你好, 你...
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2]ni hao shi,你好事
D/fcitx5: I2021-06-23 23:29:34.952250 androidfrontend.cpp:39] preedit: "ni hao shi"; clientPreedit: 你好事
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao shi, clientPreedit=你好事)
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.953220 androidfrontend.cpp:
D/fcitx5: 117] 
D/fcitx5: KeyEvent key: i isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:34.953472 androidfrontend.cpp:59] 
D/fcitx5: updateClientSideUIImpl: 109 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 109 candidates
D/FcitxEvent: type: 0, args: [109]你好事,你好是,你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好事, 你好是, 你好, 你...
D/androidfrontend: preeditCallback
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao shi j, clientPreedit=你好时间)
D/FcitxEvent: type: 2, args: [2]ni hao shi j,你好时间
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.155800
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 39
D/fcitx5: ] 
D/fcitx5: preedit: "ni hao shi j"; clientPreedit: 你好时间
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.156419
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: KeyEvent key: j isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.156687
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 59
D/fcitx5: ] 
D/fcitx5: updateClientSideUIImpl: 109 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 109 candidates
D/FcitxEvent: type: 0, args: [109]你好时间,你好世界,你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好时间, 你好世界, 你好...
D/androidfrontend: preeditCallback
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao shi ji, clientPreedit=你好世纪)
D/FcitxEvent: type: 2, args: [2]ni hao shi ji,你好世纪
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.365671 
D/fcitx5: androidfrontend.cpp:
D/fcitx5: 39] 
D/fcitx5: preedit: "ni hao shi ji"; clientPreedit: 你好世纪
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.366147 androidfrontend.cpp
D/fcitx5: :117] 
D/fcitx5: KeyEvent key: i isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.366265 androidfrontend.cpp
D/fcitx5: :59
D/fcitx5: ] updateClientSideUIImpl: 109 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 109 candidates
D/FcitxEvent: type: 0, args: [109]你好世纪,你好实际,你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好世纪, 你好实际, 你好...
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2]ni hao shi jie,你好世界
D/fcitx5: I
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=ni hao shi jie, clientPreedit=你好世界)
D/fcitx5: 2021-06-23 23:29:35.584027
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 39
D/fcitx5: ] 
D/fcitx5: preedit: "ni hao shi jie"; clientPreedit: 你好世界
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.584601
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 117
D/fcitx5: ] 
D/fcitx5: KeyEvent key: e isRelease: 0 accepted: 1
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:35.584851
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 59
D/fcitx5: ] 
D/fcitx5: updateClientSideUIImpl: 109 candidates
D/androidfrontend: candidateListCallback
D/androidfrontend: 109 candidates
D/FcitxEvent: type: 0, args: [109]你好世界,你好时节,你好,你,尼,泥,妮,逆,腻,拟,呢...
I/me.rocka.fcitx5test.MainActivity: candidate update: [你好世界, 你好时节, 你好...
D/androidfrontend: select candidate #0
D/androidfrontend: commitStringCallback
I/me.rocka.fcitx5test.MainActivity: commit update: 你好世界
D/FcitxEvent: type: 1, args: [1]你好世界
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:36.209345
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 24
D/fcitx5: ] 
D/fcitx5: Commit: 你好世界
D/androidfrontend: preeditCallback
D/FcitxEvent: type: 2, args: [2],
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:36.211133
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 39
D/fcitx5: ] 
D/fcitx5: preedit: ""; clientPreedit: 
D/fcitx5: I
D/fcitx5: 2021-06-23 23:29:36.215067
D/fcitx5:  
D/fcitx5: androidfrontend.cpp
D/fcitx5: :
D/fcitx5: 45
D/fcitx5: ] 
D/fcitx5: bulkCandidateList: no or empty candidateList
D/androidfrontend: candidateListCallback
D/androidfrontend: 0 candidates
D/FcitxEvent: type: 0, args: [0]
I/me.rocka.fcitx5test.MainActivity: preedit update: PreeditEventData(preedit=, clientPreedit=)
I/me.rocka.fcitx5test.MainActivity: candidate update: []
```

</details>

## Nix

Appropriate Android SDK with NDK is available in the development shell.  The `gradlew` should work out-of-the-box, so you can install the app to your phone with `./gradlew installDebug` after applying the patch mentioned above. For development, you may want to install the unstable version of Android Studio, and point the project SDK path to `$ANDROID_SDK_ROOT` defined in the shell. Notice that Android Studio may generate wrong `local.properties` which sets the SDK location to `~/Android/SDK` (installed by SDK Manager). In such case, you need specify `sdk.dir` as the project SDK in that file manually, in case Android Studio sticks to the wrong global SDK.
