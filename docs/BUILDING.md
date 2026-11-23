# Building

Requirements: JDK 21, Android SDK (`platforms;android-37.0`, `build-tools;36.0.0`, `ndk;28.0.13004108`).

```bash
echo "sdk.dir=$HOME/android-sdk" > local.properties   # git-ignored
./gradlew :app:assembleDebug -x lint --max-workers=2
# -> app/build/outputs/apk/debug/MyKeys_<version>-debug.apk
```

If dependency downloads fail with HTTP 429 (rate limiting through a proxy), just re-run: Gradle caches what it already fetched.
Debug builds install as `com.personal.keyboard.debug` and can coexist with HeliBoard/FlorisBoard.
