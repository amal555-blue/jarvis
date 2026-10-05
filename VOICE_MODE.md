# Jarvis 0.3 voice mode

Install the APK from the successful Build APK workflow artifact. The artifact ZIP contains app-debug.apk. This is a debug build; if Android reports a signing conflict with a prior build, uninstall that build first (this clears its saved settings).

1. Unlock Jarvis with your phone credential once per app session. Android lock still blocks commands; unlocking the phone restores the session. Stopping/force-stopping the process requires authentication again.
2. Open AI & voice setup. Keep your compatible AI endpoint, model and API key. Optional Fish Audio API key and voice ID enable Fish replies in hands-free mode. With no Fish key or a service error, Android text-to-speech is used. Provider charges/quotas may apply.
3. Tap Start Jarvis wake word, grant microphone, notifications and display-over-other-apps permissions, then tap again and Enable. First start extracts the bundled offline English model.
4. When the notification says Ready, say “Jarvis”, pause for the animated popup, then speak. Ask questions, say “open WhatsApp”, “search ...”, “time”, or “date”. Say “stop listening” after waking it to end voice mode. The notification also has Stop listening.
5. On Vivo, allow background activity for Jarvis in the phone battery settings if listening stops. It consumes battery while enabled. Restart voice mode after reboot or force-stop.

Offline wake detection responds to any speaker. It does not verify your voice. The old Picovoice enrollment is removed, and its saved profile is not used. Do not rely on a wake word for identity verification. Phone actions require an authenticated session and an unlocked screen. No arbitrary remote phone control, automatic messaging, purchases or deletion is implemented.

Android may block overlays on protected screens, stop background apps, or deny the microphone while another app uses it. Wake detection is not guaranteed every time or in every app. Locked-screen wake only prompts you to unlock; private AI replies and actions are blocked. Listening does not automatically restart after reboot.

Wake and command transcription run locally using Vosk. Questions that are not supported local commands are sent to your configured AI service. If Fish is enabled, reply text is sent to https://api.fish.audio/v1/tts using its s2.1-pro-free model; availability depends on the provider/account. Temporary reply audio is deleted after playback. API keys are encrypted with Android Keystore.

## Third-party model
Vosk small English model: vosk-model-small-en-us-0.15 (Apache License 2.0), downloaded during CI from https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip. Vosk: https://github.com/alphacep/vosk-api . Model listing and license: https://alphacephei.com/vosk/models .

## Validation
GitHub Actions compiles the Android debug APK. Physical Vivo device testing, recognition accuracy, lock transitions, microphone contention, and Fish/AI account requests still require testing on your phone; a successful build does not verify those behaviors.
