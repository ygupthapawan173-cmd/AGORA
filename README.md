# JARVIS AI v0.2.0

An Android-first personal assistant for Telugu + English, built with Kotlin and a futuristic red / black / white interface.

## Included in this release
- Gemini + OpenAI + Local AI provider routing with Auto selection.
- Android Keystore encrypted API key storage for Gemini and OpenAI.
- Chat, speech input, installed Android TTS voice preferences, and the existing `Hey Jarvis` SpeechRecognizer service.
- Camera still / image picker analysis through Gemini.
- Local PDF / DOCX / XLSX / TXT / MD / CSV / JSON / XML document text extraction.
- Agent planning and a separate user-approved set of built-in Android intent actions.
- Local reminders, conversation memory controls, and new Control Center screens.
- Optional Accessibility screen reader and limited one-time Back / Scroll / Swipe / exact-label Click actions, restricted to the user's app allowlist and local-only processing.

## Build

GitHub Actions builds the debug APK on pushes to `jarvis-red-white-ui` and the default branches. It publishes an artifact named `AGORA-debug-apk`.

## AI provider setup

1. Open **Settings → AI Provider Settings**.
2. Choose Gemini, OpenAI, Local AI, or Auto.
3. Paste your own provider key in the corresponding settings dialog when needed. Keys are encrypted at rest using Android Keystore.
4. OpenAI API usage is separate from ChatGPT subscriptions and may incur API charges. Select it only when you intend to use a paid API.
5. Local AI requires the user to run an OpenAI-compatible llama.cpp server in Termux on `http://localhost:8080`; no model is bundled with this APK.

## Accessibility privacy

- The app allowlist starts empty. The user selects launcher apps explicitly.
- Android's Accessibility Settings must be enabled by the user after the app disclosure.
- Screen snapshots are held only in service memory and are not written to disk or sent to any AI provider.
- Each supported action requires a separate preview and approval. The one-time action is bound to the previously selected app and a short time window.
- Banking, wallet, payment-like packages and screens that appear to contain password / OTP / PIN / CVV details are blocked using conservative heuristics. These heuristics are not a guarantee; do not use the feature on sensitive screens.
- v0.2.0 supports Back, Scroll up / down, Swipe left / right / up / down, and exact visible-label Click only. It does not provide unrestricted remote control or arbitrary autonomous tapping.
- Review the relevant app-store accessibility API declaration and disclosure requirements before publishing through Google Play.

## Important limitations
- The wake phrase prototype uses Android `SpeechRecognizer`, not a dedicated offline wake-word engine. The recognition provider and battery behavior depend on the phone.
- Male / female TTS depends on installed voices on the device; the app cannot synthesize voices that are not installed.
- Voice is speech recognition followed by TTS, not full-duplex streaming speech-to-speech.
- Camera understanding is a selected still image, not continuous live video.
- Agent plans are planning output only. Device actions are limited to separate, user-approved local commands.
- The app-side Gemini request counter is not a provider billing cap. There is no provider-side budget enforcement in this app.
- Source implementation and GitHub CI success do not replace testing on the user's Android device.

See [JARVIS_ROADMAP.md](JARVIS_ROADMAP.md) for the implementation checklist and deferred features.
