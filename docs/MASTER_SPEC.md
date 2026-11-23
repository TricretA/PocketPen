# Personal Keyboard — Master Specification

Living document. Update on every decision, completed item and deferral.
Last updated: 2026-11-23

## 1. Product decision

A personal Android keyboard, built as a **fork of HeliBoard** (GPL-3.0). Not from scratch,
not an ecosystem. Reuse the engine, restyle it Gboard-like, upgrade the clipboard, add AI + voice.

- **Base:** HeliBoard (Kotlin/Java IME, AOSP LatinIME lineage, Compose settings, active — last commit 2026-11-15).
- **Reference only (no code transplant):**
  - **CleverKeys** (GPL-3.0): clipboard UX/design ideas, built-in theme set, dictionary-manager ideas. Different core
    (custom `Keyboard2View`, 1,557 files, ~13k lines in clipboard alone) — its code does not drop into HeliBoard.
  - **FlorisBoard** (Apache-2.0): clipboard concept. Not used as a base.
- **Upstream policy:** HeliBoard's `AI_USAGE.md` forbids AI-generated PRs. We stay a private fork and never PR upstream.
  `upstream` remote is kept for pulling their fixes.
- **Package policy:** Kotlin package `helium314.*` is kept deliberately so upstream merges stay cheap.
  Only `applicationId`, app name, icons and branding change.

## 2. Scope — BUILD (the "important things" only)

| # | Module | Scope | Status |
|---|--------|-------|--------|
| 1 | Core keyboard | QWERTY, number row, long-press symbols, emoji, language switch, comma/period keys, resize, one-handed, landscape, suggestion strip | Mostly inherited from HeliBoard — verify |
| 2 | **Clipboard** (differentiator) | Persistent history, pins, snippets, search, retention (never/1d/7d/30d/90d/custom), duplicate handling, timestamps, sensitive-content handling, tabs: All / Recent / Pinned / Snippets | Partial — see §4 |
| 3 | Layout customization | Height, spacing, number row, bottom row, one-handed side, presets (Standard / Compact / Developer) | Partial — HeliBoard has most knobs |
| 4 | **AI** | Rewrite, fix grammar, shorten, expand, tone (professional/casual/polite), summarize, translate, reply, custom prompt. Selected-text mode first. BYO API key, provider interface | New |
| 5 | **Voice** | Mic key, preview-before-insert, optional AI cleanup | New (HeliBoard has no voice) |
| 6 | Typing intelligence | Autocorrect + undo, autocap, next-word, personal dictionary, double-space period, glide typing | Inherited; glide see risk R1 |
| 7 | Themes | Gboard-like default (light/dark/dynamic), custom colors, a few CleverKeys-inspired presets | Inherited engine; restyle |
| 8 | Privacy | Password-field handling, incognito, clear data, no analytics, encrypted clipboard DB, sensitive-flag respect | Partial |

## 3. Scope — CUT (explicitly not building)

Theme marketplace, GIF/sticker integrations, Emoji Kitchen, Copilot-style search, conversation-mode chatbot,
contact suggestions, clipboard backup UI beyond HeliBoard's existing backup, per-app exclusion lists (maybe later),
multi-provider AI settings sprawl (one provider interface, two implementations max at first).

## 4. Reconnaissance findings (evidence)

HeliBoard clipboard today (`latin/ClipboardHistoryManager.kt`, `latin/database/ClipboardDao.kt`):
- SQLite-backed, cache loaded at start → **already survives keyboard/device restarts**.
- Pinning exists; pinned-first ordering is a setting.
- Dedupe by exact text; moves existing item to top.
- **Retention is a minutes slider capped at 120; above that = never expires.** No 1d/7d/30d/90d presets.
  (`ClipboardDao.clearOldClips`, `Settings.PREF_CLIPBOARD_HISTORY_RETENTION_TIME`.)
  Need to verify pinned items are exempt from expiry.
- Handles images/files (content URIs copied to files dir, size limit pref).
- **Missing vs. spec:** snippets (manual entries), tabs/categories, search, edit entry, `IS_SENSITIVE` clip flag handling,
  DB encryption, multi-select.

CleverKeys clipboard features worth re-implementing (design only): tabs, tags, edit-in-place, search,
swipe-to-delete, `IS_SENSITIVE` skip (`ClipboardHistoryService.kt:1019`), per-entry private flag, source package.

HeliBoard other:
- Settings UI is Jetpack Compose + Material3 (`settings/screens/*`) — good, easy to regroup.
- Themes: Material/Holo/Rounded styles; light, dark, darker, black, dynamic + 10 colour themes; custom colours via picker.
- No voice input code. No AI code. minSdk 21, targetSdk/compileSdk 37, NDK 28.0.13004108, 4 ABIs.
- Permissions: READ/WRITE_USER_DICTIONARY, READ_CONTACTS, VIBRATE, RECEIVE_BOOT_COMPLETED. **No INTERNET permission** — adding AI means adding it; see §6.

## 5. Risks

- **R1 — Glide typing:** HeliBoard's gesture typing needs Google's proprietary `libjni_latinimegoogle.so`, which cannot be bundled.
  Users load it themselves. Decision pending: (a) accept user-supplied lib, (b) drop glide, (c) later evaluate an open engine.
- **R2 — INTERNET permission:** a keyboard with network access is a trust-sensitive surface. Mitigation: AI/voice network code
  lives in a separate, clearly gated module; never sends text unless the user triggers an AI action; password fields hard-blocked.
- **R3 — Upstream drift:** large UI rewrites make merging upstream painful. Mitigation: keep package names; isolate new features in new packages
  (`helium314.keyboard.ai`, `.voice`, `.clipboardplus`).
- **R4 — Build environment:** first full build is heavy (NDK + Compose). Tracked in §7.

## 6. Architecture for new modules

- `ai/` — `AiProvider` interface (`suspend fun transform(prompt, text): Flow<String>`), implementations behind a key stored in
  EncryptedSharedPreferences. Actions are prompt templates. Surfaced as a toolbar key + panel replacing the keyboard area.
- `voice/` — Phase 1: launch system `RecognizerIntent`/`SpeechRecognizer` from a toolbar key, show preview strip, commit on confirm.
  Phase 2: pluggable cloud STT.
- `clipboard` upgrades — extend existing `ClipboardDao` schema via DB migration (add `kind`, `tags/category`, `sensitive`), keep file-store.

## 7. Roadmap / status

- [x] Recon: HeliBoard, CleverKeys, FlorisBoard cloned and assessed
- [x] Fork created from HeliBoard full history (`upstream` remote retained)
- [x] Baseline build of unmodified fork (27 MB debug APK; Maven Central 429s need retry — see `docs/BUILDING.md`)
- [x] Rebrand: applicationId `com.personal.keyboard`, name "MyKeys" (working name, change freely), provider authorities, APK name. Icon still HeliBoard's.
- [x] Clipboard v2 step 1: retention presets (1h/1d/7d/30d/90d/never), default 30 days (was 10 min), pinned exempt (already was), `IS_SENSITIVE` clips no longer stored
- [x] Clipboard v2 step 2a: tabs (All/Recent/Pinned/Snippets), snippets (DB v4 `KIND` column, protected from expiry/clear, removable by swipe only in Snippets tab), long-press cycles unpinned -> pinned -> snippet. 5 unit tests.
- [ ] Clipboard v2 step 2b: search, edit entry, add-snippet-by-typing, custom retention period, DB encryption
  - UI is untested on a device/emulator (none available in the build container): verify tab row height and long-press cycle on a phone.
- [ ] Gboard-style default look + layout presets
- [x] AI panel (selected-text actions): Claude, OpenAI, OpenRouter, Groq, NVIDIA; toolbar key + settings screen; written without a local build, verified by CI only
- [ ] Voice key + preview
- [ ] Privacy pass, device testing, release APK
