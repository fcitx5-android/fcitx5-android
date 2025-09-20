# Repository Guidelines

## Project Structure & Modules
- `app/`: Android app (Kotlin, JNI in `src/main/cpp`, resources in `src/main/res`). Tests in `src/test` (unit) and `src/androidTest` (instrumented).
- `lib/`: Native and shared libraries (`fcitx5`, `libime`, addons). Submodules live under `src/main/cpp/*`.
- `plugin/`: Standalone plugin APKs (e.g., `rime`, `hangul`, `jyutping`, `chewing`, `unikey`, `thai`, `clipboard-filter`).
- `codegen/`: Kotlin JVM code generation (KSP/KotlinPoet).
- `build-logic/`: Gradle convention plugins and shared build config.
- Root Gradle files: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`.

## Build, Test, and Development
- Fetch submodules (required): `git submodule update --init --recursive`.
- Standard debug build: `./gradlew :app:assembleDebug`.
- Install on device: `./gradlew :app:installDebug` (device/emulator required).
- Release app and plugins (CI parity):
  - `./gradlew :app:assembleRelease`
  - `./gradlew :assembleReleasePlugins`
- Tests:
  - Unit tests: `./gradlew testDebugUnitTest`
  - Instrumented tests: `./gradlew connectedDebugAndroidTest` (device/emulator required)
- Nix dev shell (optional): `nix develop` (then use `./gradlew ...`).

## Coding Style & Naming
- Language: Kotlin (Java 11 target); Android XML resources; C/C++ via CMake.
- Kotlin: 4‑space indent, idiomatic Kotlin (prefer `val`, null‑safety). Classes/files `PascalCase.kt`; functions/vars `lowerCamelCase`; constants `UPPER_SNAKE_CASE`.
- Packages: `org.fcitx.fcitx5.android.*` and subpackages for features.
- Android resources: lowercase_snake_case (e.g., `activity_main.xml`, `ic_launcher`, `app_name`). Avoid wildcard imports.
- Keep modules cohesive: app UI in `app`, shared/native code in `lib`, plugin‑specific code in `plugin/<name>` with `res/xml/plugin.xml`.

## Testing Guidelines
- Frameworks: JUnit4 for unit tests; AndroidX instrumentation for `androidTest`.
- Naming: `SomethingTest.kt`; keep tests alongside corresponding package.
- Coverage: aim for meaningful coverage of core logic (serialization, config, input flow). Prefer fast unit tests; reserve device tests for integration.

## Commit & Pull Request Guidelines
- Commits: concise, imperative subject (e.g., “Fix …”, “Add …”, “Update …”). Group related changes; reference issues (`#123`) when relevant.
- PRs: clear description, rationale, and testing notes; include screenshots/GIFs for UI changes. Ensure submodules are up to date and CI builds pass (`:app:assembleRelease` and `:assembleReleasePlugins`).

## Configuration Tips
- Match Android SDK/NDK/CMake versions in `build-logic/.../Versions.kt` (CI uses these). Ensure `local.properties` has a valid `sdk.dir`.
- Clear conflicting JVM env vars (e.g., `_JAVA_OPTIONS`, `JAVA_TOOL_OPTIONS`) if Gradle/CMake detection fails.
