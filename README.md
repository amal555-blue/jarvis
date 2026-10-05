# JARVIS for Amal — Android prototype 0.1
Source project, not an APK. Requires Android 8.0 or later. Intended for Vivo Y83/Y19; not compiled or device-tested in the authoring environment.

## Get an APK using only your phone
1. Create a GitHub repository. Extract this ZIP and upload ALL contents, including the `.github` folder (you may need desktop-site mode or a Git client). Keep `app`, `.github`, and the Gradle files at the repository root.
2. Open repository → Actions → Build APK → Run workflow.
3. When successful, download the Jarvis-debug-APK artifact and extract app-debug.apk.
4. Transfer/install the APK, allowing installation from that source when Android asks. A debug APK is for personal testing, not Play Store release.
Alternative: open this folder in Android Studio on your friend's computer and build an APK. Requires JDK 17, Gradle 8.9, Android SDK 35, and internet access to download dependencies.

## Use
Set up a phone screen lock/biometric first. Unlock JARVIS using Android authentication. Tap Speak and grant microphone access, or type a command.
Commands: `open WhatsApp`, `open YouTube`, `alarm 06:30`, `time`, `date`, `settings`, `search black holes`, `lock`.
App launching matches the complete launcher name. Alarm opens the installed clock app for review. Search opens the browser. Speech recognition may send audio to your configured Android recognition provider; text-to-speech requires a configured voice engine.
To enable the floating bubble, grant Display over other apps and tap the enable button again. Tap J to reopen the locked assistant. Long-press J or use its notification Stop action to remove it. The assistant locks whenever it leaves the foreground.

## What is present
System biometric/credential gate; tap-to-talk recognition; spoken responses; typed commands; supported Android app/clock/browser intents; foreground launch bubble with visible notification. No face images are collected or stored.

## What is still missing
Always-listening 'Jarvis' wake word, real-time cloud AI, camera understanding, memory, arbitrary UI automation, notification access, messages/calls and service integrations. This is a deterministic command prototype, not the complete requested assistant. No background microphone is used. No AI API key is required in this version.
Android chooses the authentication modality: face-only unlock cannot be promised on Vivo phones. Fingerprint or phone credentials may be offered. It fails closed if no compatible authentication exists.

## Test checklist for the first APK
- Build workflow completes; install and launch on both phones.
- Cancel unlock: command controls stay hidden. Successful unlock shows controls.
- Background and reopen: authentication required again.
- Deny microphone: typed commands remain usable; Speech does not crash.
- Speak 'time', open a installed app, and open alarm screen without skipping review.
- Grant/revoke overlay permission; bubble opens locked UI; long press and notification stop remove it.
- Check recognition and TTS with network available and unavailable.
- Vivo battery management may stop the bubble; check app battery settings if needed.

## Validation performed
XML parsed and ZIP integrity verified. No Android SDK/Gradle compiler or devices were available, so compilation, runtime behavior, biometric support and APK installation remain unverified. Dependency versions are pinned; automated build downloads them from official repositories.
