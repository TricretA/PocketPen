# TriKey

A personal Android keyboard with a clipboard that remembers and AI writing tools built in.

TriKey is a fork of [HeliBoard](https://github.com/Helium314/HeliBoard), which is itself based on the Android Open Source Project keyboard. It keeps that proven typing engine and adds the things a daily driver should have.

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

**Everything HeliBoard already does**
Suggestions and autocorrect, dictionaries, themes, custom layouts, number row, one-handed mode, emoji, and a privacy-first design with no analytics.

## Status

Under active development. Clipboard tabs and the AI panel are new and have been verified by automated builds and unit tests, not yet on a range of devices.

Planned: clipboard search and entry editing, a Gboard-style default look, layout presets, and voice dictation with preview before inserting.

See [docs/MASTER_SPEC.md](docs/MASTER_SPEC.md) for scope, decisions and the roadmap.

## Building

Builds and tests run on GitHub Actions. Every push to `main` produces a debug APK as a workflow artifact. See [docs/BUILDING.md](docs/BUILDING.md).

## Privacy

No analytics and no telemetry. The only network access is the AI assistant, and only when you tap an action.

## License

GPL-3.0, inherited from HeliBoard. See [LICENSE](LICENSE). Thanks to the HeliBoard and AOSP keyboard contributors whose work this builds on.
