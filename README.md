# Pocketpen

A personal Android keyboard with a clipboard that remembers and AI writing tools built in.

Pocketpen is a fork of [HeliBoard](https://github.com/Helium314/HeliBoard), which is itself based on the Android Open Source Project keyboard. It keeps that proven typing engine and adds the things a daily driver should have.

## What it adds

**Clipboard that remembers**
- History survives restarts. Retention is your choice: 1 hour, 1 day, 7 days, 30 days, 90 days or never (default 30 days).
- Tabs: All, Recent, Pinned, Snippets.
- Long-press an entry to cycle it: unpinned, pinned, snippet.
- Pinned items and snippets are never deleted automatically and survive "clear history".
- Anything an app marks as sensitive, such as passwords and one-time codes, is never stored.

**AI assistant**
- A toolbar key opens a panel that works on your selected text, or on the whole field if nothing is selected.
- Actions: fix grammar, rewrite, shorten, expand, professional, casual, polite, summarize, translate, reply, ask AI, and one custom instruction.
- Replace the text in one tap, and restore the original if you change your mind.
- Bring your own key for Claude, OpenAI, OpenRouter, Groq or NVIDIA. Models are loaded live from the provider, and a test button checks your setup.
- Keys are encrypted with the Android Keystore, kept out of backups, and never leave the device except to the provider you chose.
- Nothing is sent until you tap an action. The panel is disabled in password fields.

**Voice typing**
- The mic key uses Groq Whisper for accurate dictation, or your device's own voice input. Pick either in settings.
- A live recording animation and timer, then a preview you review before inserting. Optional AI clean-up of the transcript.
- Works offline by switching to the device's voice input automatically. Audio is deleted from the phone right after transcribing.

**Handwriting**
- A toolbar key turns the keys into a white canvas. Write with a finger or a stylus and the words are read and inserted when you pause.
- Palm rejection while using a stylus. Drawings and shapes are not inserted as text; you can still tap a suggestion to insert one.
- Reading happens on the phone. The language model downloads once, about 20 MB per language.

**Clipboard manager**
- Search, edit, add snippets, delete, and set a custom retention period from Settings.

**Simple appearance**
- Theme colors, day and night, key borders, key style, background image, keyboard height and font size on one short screen. Everything else is under "More appearance options".

**Everything HeliBoard already does**
Suggestions and autocorrect, dictionaries, themes, custom layouts, number row, one-handed mode, emoji, and a privacy-first design with no analytics.

## Status

Under active development. New features are verified by automated builds and unit tests, and still need testing on real devices.

Still to come: a Gboard-style default look and layout presets.

See [docs/MASTER_SPEC.md](docs/MASTER_SPEC.md) for scope, decisions and the roadmap.

## Building

Builds and tests run on GitHub Actions. Every push to `main` publishes the debug APK to the [latest release](../../releases/latest). See [docs/BUILDING.md](docs/BUILDING.md).

## Privacy

No analytics and no telemetry. The network is used only for the AI assistant, Groq voice typing, and the one-time handwriting model download, and only when you use those features.

## License

GPL-3.0, inherited from HeliBoard. See [LICENSE](LICENSE). Thanks to the HeliBoard and AOSP keyboard contributors whose work this builds on.
