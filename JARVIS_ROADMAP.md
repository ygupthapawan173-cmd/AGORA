# JARVIS AI v0.2.0 — Product Roadmap & Implementation Status

Target: Android-first personal assistant with Telugu + English, a red / black / white futuristic interface, user-controlled automation, and free-first optional paid cloud providers.

## Product principles
- Free-first by default. Gemini, OpenAI and Local AI are selectable; OpenAI API usage can incur separate charges.
- Keys are stored encrypted at rest with Android Keystore. A mobile app cannot guarantee that a key is secret from a sufficiently privileged device owner or reverse engineer; never commit keys to source control.
- No calls, SMS sending, purchases, deletion, or app UI action is silently executed by JARVIS.
- Accessibility screen content stays local and is not sent to Gemini, OpenAI, or the local model. The allowlist starts empty.
- Features are not considered device-verified until tested on the target Android phone.

## v0.2.0 source implementation
- [x] Futuristic red / black / white home dashboard and new Home / Chat / Voice / Agent / Settings navigation.
- [x] Multi-provider router: Gemini, OpenAI, Local AI, and Auto selection.
- [x] OpenAI Chat Completions client with Android Keystore encrypted key storage (model configured as `gpt-6.2-astra`).
- [x] Gemini text and image clients retained (model `gemini-3.5-flash-lite`).
- [x] Optional local llama.cpp OpenAI-compatible server at `http://localhost:8080`.
- [x] Conversation memory viewer, clear action, and user-triggered export.
- [x] Voice input language preference applied to direct speech recognition; wake-command locale respects Telugu / English / device-default selection.
- [x] Existing Android SpeechRecognizer wake phrase prototype and TTS reply retained.
- [x] Camera snapshot via installed camera app, image picker, and Gemini image analysis.
- [x] Local PDF / DOCX / XLSX / TXT / MD / CSV / JSON / XML text extraction. Extracted document text is handed to the selected AI only after the user explicitly chooses a file and sends it in Chat.
- [x] Agent task planning via selected provider; plans are previews only. Device commands are a separate allowlisted, user-approved action path.
- [x] Local reminders using AlarmManager `setAndAllowWhileIdle` and notification receiver; Android may defer delivery to save battery.
- [x] Accessibility service with Selected Apps Only allowlist, empty by default; no cloud screen sharing; volatile in-memory screen snapshot; one-time approved Back / Scroll / Swipe / exact visible-label Click; 30-second app and screen-snapshot binding; sensitive-screen heuristics; metadata-only audit log; Pause / Resume.
- [x] Accessibility package filtering updates from the selected app list. Banking / wallet / payment-like package names are excluded by conservative package-name checks.
- [x] API, voice, accessibility, memory, agent, reminder and control-center settings surfaces.

## Not complete / requires a later phase
- [ ] True low-latency full-duplex audio, streaming transcription, and native Gemini Live session.
- [ ] Dedicated offline wake-word model (the current wake phrase prototype still uses Android SpeechRecognizer; system fallback may use network).
- [ ] Bundled downloadable on-device LLM and offline Telugu speech model. Local AI currently requires a separate llama.cpp server started by the user in Termux.
- [ ] Continuous live-camera/video understanding. v0.2.0 supports a user-captured camera still or picked image.
- [ ] Arbitrary autonomous multi-step device actions. Agent plans are informational; supported device commands require a separate preview and approval.
- [ ] Swipe-coordinate gestures, cross-app workflows, app-specific integrations, and action rollback.
- [ ] Persistent reminder list/edit/delete UI and exact-alarm scheduling.
- [ ] Accessibility service tested against a device/app QA matrix; custom canvas apps may expose no readable node text.
- [ ] Automated Kotlin unit tests, UI tests, provider integration tests, and real-device battery / permission QA.
- [ ] Provider-side budget caps. Gemini's existing 20-request/day counter is an app-side convenience limit only, not a billing limit; OpenAI usage is billed separately by the provider.

## Release verification checklist
1. GitHub Actions `assembleDebug` passes.
2. Install APK on the target phone and verify startup, all navigation screens, Gemini / OpenAI / local-provider settings.
3. Verify Telugu and English voice input, installed TTS voices, wake phrase lifecycle, and stop / resume behavior.
4. Verify PDF / DOCX / XLSX / TXT / CSV extraction with sample files.
5. Verify reminders when notifications are allowed and when battery saver is active.
6. Enable Accessibility only after reading the in-app disclosure. Select a non-sensitive test app, confirm the app picker allowlist, screen text remaining local, and each approved action being bound to the same app and time window.
7. Review the Accessibility audit log. It must never contain screen text, typed content, passwords, OTPs, or document content.
