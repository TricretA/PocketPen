# Contributing to Pocketpen

Thank you for your interest in Pocketpen.

## Getting started

1. Install JDK 21 and the Android SDK, or open the project in Android Studio.
2. Build with `./gradlew assembleDebug`.
3. Install the APK from `app/build/outputs/apk/debug/` and enable the keyboard in system settings.

## Guidelines

- Keep pull requests small and focused on one change.
- Match the style of the surrounding code. The Kotlin package name stays `helium314.*` so upstream HeliBoard changes remain easy to merge.
- New settings follow the existing pattern: a title, a one-line plain description and a switch or simple choice.
- Do not add analytics, tracking or network calls that run without the user triggering a feature.
- Describe how you tested your change, including the device and Android version.

## Reporting issues

Open an issue with the app version, your Android version, the app you were typing in and steps to reproduce. For security issues, use GitHub's private security advisories instead of a public issue.

## Where to look

- Keyboard layouts: `app/src/main/assets/layouts`
- Typing input handling: `InputLogic`
- Settings screens: `app/src/main/java/helium314/keyboard/settings`
- AI, voice and clipboard features: the `ai`, `voice`, `clipboardplus` and `keyboard/clipboard` packages

By contributing you agree that your work is licensed under GPL-3.0.
