# JARVIS AI — Ultimate Roadmap

Target: Android-first personal assistant with Telugu + English, optional paid cloud services, and local/offline fallbacks. The existing Red + Black + White interface is retained.

## Product principles
- Free-first defaults; paid APIs are opt-in and clearly identified.
- Never commit API keys. Store user-entered credentials locally with Android Keystore where practical.
- Ask for confirmation before calls, messages, purchases, file deletion, or other sensitive actions.
- Show whether a feature is local, cloud-backed, or unavailable on the device.
- Do not claim a task succeeded until Android or the provider confirms it.

## Current implementation status
- [x] Gemini text API client with locally encrypted API key.
- [x] App-side daily request counter (not a provider billing cap).
- [x] Chat UI and basic local conversation memory.
- [x] Android speech recognition input and chat reply TTS.
- [x] Wake phrase prototype using Android SpeechRecognizer.
- [x] Wake-phrase commands can use Gemini when a key is configured; built-in phone intents remain local.
- [x] Male/female preference and Telugu/English/auto language selection, using voices installed by the device TTS engine.
- [x] Image picker + Gemini image understanding.
- [x] Text-file picker and summarization prompt.
- [x] Safe dialer/SMS composer/app-launch intents with user review.
- [x] External web search intent.
- [ ] Reliable dedicated offline wake-word engine (Sherpa-ONNX or equivalent).
- [ ] True streaming, low-latency full-duplex voice conversation.
- [ ] Native Gemini Live audio session.
- [ ] Real-time camera/video understanding.
- [ ] PDF/DOCX/XLSX parsing and searchable file library.
- [ ] Provider abstraction and optional OpenRouter/other API providers.
- [ ] Model picker, per-provider cost estimates, budget alerts, and hard opt-in for paid models.
- [ ] Multi-step agent planner with permission scopes, task preview, execution log, and cancellation.
- [ ] Android automation via supported intents/Accessibility only after explicit user opt-in.
- [ ] Local on-device LLM and offline speech models packaged/downloaded separately.
- [ ] Persistent chat history UI, export, delete, and memory controls.
- [ ] Automated unit/UI tests and real-device voice QA matrix.

## Delivery phases
1. **Voice reliability:** verify TTS voices installed on target phone, robust recognizer lifecycle, stop/resume, Telugu/English handling, error states, and battery-aware foreground service.
2. **Cloud intelligence:** Gemini model selection, conversation history, tool calling, grounding/search, retries, cancellation, and API usage/cost reporting.
3. **Multimodal:** camera capture, image/OCR, PDF and Office file import, document summarization.
4. **Agent workflows:** plan/preview/approve/execute, scoped Android intents, action logs, undo where supported.
5. **Offline mode:** downloadable speech recognition and local model packs with clear storage/RAM requirements.
6. **Hardening:** privacy controls, security review, tests, accessibility, battery/network handling, release signing.

## Voice requirements and limitations
- Android SpeechRecognizer is not a guaranteed always-on offline wake-word engine. OEM battery controls, network availability, speech-service installation, and microphone restrictions affect it.
- Male/female TTS depends on the voices installed in Android's selected TTS engine. The app preference chooses a matching installed voice where detectable; it cannot synthesize a missing voice.
- Continuous streaming speech-to-speech requires a separate real-time audio integration and should not be represented as complete by basic SpeechRecognizer + TextToSpeech.
