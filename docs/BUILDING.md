# Building

Builds and tests run on GitHub Actions (`.github/workflows/build-debug-apk.yml`), not locally.
Every push to `main` builds the debug APK and runs the unit tests. Download the APK from the run's artifacts (`Pocketpen-debug`).

Requirements if you do build locally: JDK 17+, Android SDK (`platforms;android-37.0`, `build-tools;36.0.0`, `ndk;28.0.13004108`), then `./gradlew assembleDebug`.
Debug builds install as `com.pocketpen.keyboard.debug` and can coexist with HeliBoard and FlorisBoard.
