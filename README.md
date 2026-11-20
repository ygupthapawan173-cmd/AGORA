# AGORA — API-key-free hybrid Android assistant (Phase 1 starter)

This project replaces the uploaded VoiceAgent prototype's hard-coded Claude API dependency with an API-key-free foundation.

## What is included now
- Native Kotlin Android app with dark neon AGORA dashboard.
- User-started microphone foreground service and visible notification.
- `Hey Agora` wake phrase recognition using Android SpeechRecognizer. It prefers Android's on-device recognizer when available and asks for offline preference. If the device has no on-device recognizer, Android's default recognition provider may use a network service.
- Android TextToSpeech response.
- Deterministic local commands: tell time, open Settings, launch installed apps, open dialer (user confirms call), open SMS composer (user reviews and sends), camera, and web search.
- No user API key, no embedded cloud key, no Claude dependency.

## Important honest limitations
1. This first build is a runnable foundation, not the final AGORA intelligence system.
2. The wake phrase is currently recognized through Android SpeechRecognizer. It is NOT yet Sherpa-ONNX dedicated keyword spotting. For robust offline wake-word detection, integrate the Sherpa-ONNX KWS engine and a compatible English keyword model in Phase 2.
3. SpeechRecognizer's on-device mode exists only if the phone's speech service and language pack support it. Otherwise the system recognizer may use the network. `EXTRA_PREFER_OFFLINE` is a preference, not a guarantee.
4. Open-ended generative AI is not included yet. No-API-key offline reasoning requires downloading a local GGUF model and integrating a local runtime such as llama.cpp. Performance depends on RAM, chipset, and model size.
5. Online research currently opens Android web search. It does not call a third-party AI API.
6. Self-update, local LLM runtime, Sherpa KWS, multilingual ASR/TTS model management, and guarded accessibility automation are Phase 2+ work.
7. Calls and messages are only opened in the dialer/composer; the user must confirm. The app does not silently send messages or place calls.

## Build
- JDK 17
- Android SDK platform 35
- Android Gradle Plugin 8.10.1 / Gradle 8.11.1

### GitHub Actions
Push this folder to a GitHub repository. The workflow at `.github/workflows/android.yml` builds a debug APK and uploads it as an Actions artifact.

### Local Gradle
From the project root:

```bash
gradle --version
gradle assembleDebug
```

Install output:
`app/build/outputs/apk/debug/app-debug.apk`

## First run
1. Install AGORA.
2. Open it and tap START HEY AGORA while the app is visible.
3. Grant microphone permission and notification permission.
4. Keep the ongoing AGORA notification visible while wake mode is enabled.
5. Say "Hey Agora". After the assistant replies "Cheppandi", say a supported command.
6. Tap STOP LISTENING to release the microphone.

Android 14+ restricts microphone foreground services: start the service from the visible activity after microphone permission is granted. The app intentionally does not auto-start microphone capture at boot.

## Phase 2 architecture
- Sherpa-ONNX KWS for local `HEY AGORA` detection with AudioRecord + KeywordSpotter.
- A small local GGUF model via llama.cpp for offline natural-language reasoning.
- Android on-device ASR/TTS with explicit language-pack checks; Sherpa models where needed.
- Safe action planner with allowlisted intents, confirmation gates, and audit log.
- Signed update manifest + APK signature verification + PackageInstaller user approval.

References:
- Android microphone foreground service: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android speech recognizer: https://developer.android.com/reference/android/speech/SpeechRecognizer
- Sherpa-ONNX KWS: https://k2-fsa.github.io/sherpa/onnx/kws/
- llama.cpp Android: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md

## Optional local AI brain (no API key)
The app contains `LocalModelClient`, which sends unknown voice prompts to a llama.cpp OpenAI-compatible server on `http://localhost:8080`. This is loopback-only in the app code; the Android network security exception is limited to the `localhost` host.

In Termux, build/run a llama.cpp server and bind it only to loopback. Example outline (exact build options can vary by device):

```bash
pkg update -y && pkg upgrade -y
pkg install -y git cmake clang make
cd ~
git clone https://github.com/ggml-org/llama.cpp
cd llama.cpp
cmake -B build -DCMAKE_BUILD_TYPE=Release
cmake --build build --config Release -j2
```

Download a small instruction-tuned GGUF model that your phone can handle, then run:

```bash
./build/bin/llama-server -m ~/models/your-small-model.gguf --host 127.0.0.1 --port 8080 -c 2048
```

Keep the server bound to `127.0.0.1`; do not expose it to your Wi-Fi/LAN. The app's unknown-command fallback will try this local endpoint. Without the model/server, built-in commands still work and unknown prompts receive a clear fallback message. Model weights are not bundled in this source ZIP.
