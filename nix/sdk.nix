self: super:

{

  ourSDK = rec {
    cmakeVersion = "3.18.1";
    buildToolsVersion = "30.0.3";
    platformToolsVersion = "31.0.2";
    platformVersion = "30";
    ndkVersion = "23.0.7421159-rc5";
    abiVersion = "arm64-v8a";
    androidComposition = super.androidenv.composeAndroidPackages {
      inherit platformToolsVersion ndkVersion;
      buildToolsVersions = [ buildToolsVersion ];
      platformVersions = [ platformVersion ];
      abiVersions = [ abiVersion ];
      cmakeVersions = [ cmakeVersion ];
      includeNDK = true;
    };
  };
}
