self: super:

{

  ourSDK = rec {
    cmakeVersion = "3.18.1";
    buildToolsVersion = "31.0.0";
    platformToolsVersion = "31.0.3";
    platformVersion = "31";
    ndkVersion = "23.0.7344513-rc4";
    abiVersions = [ "arm64-v8a" "armeabi-v7a" ];
    androidComposition = super.androidenv.composeAndroidPackages {
      inherit platformToolsVersion ndkVersion;
      buildToolsVersions = [ buildToolsVersion ];
      platformVersions = [ platformVersion ];
      inherit abiVersions;
      cmakeVersions = [ cmakeVersion ];
      includeNDK = true;
      includeEmulator = false;
    };
  };
}
