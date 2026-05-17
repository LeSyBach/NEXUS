# NEXUS

This project includes realtime calling using WebRTC and Firebase Realtime Database signaling.

## Setup (WebRTC)
1) Ensure Firebase Realtime Database is enabled and your app is configured.
2) Confirm permissions in `app/src/main/AndroidManifest.xml` for microphone/camera.
3) Add a TURN server for reliable connectivity in mobile networks.

## Push notifications (Calls/Messages)
Firebase Cloud Functions are located in `functions/`.

Quick steps:
- Install Firebase CLI.
- Deploy functions: `firebase deploy --only functions`.

See `functions/README.md` for details.

## Run
Use Android Studio or the Gradle wrapper.

```cmd
D:\AndroidStudioProjects\NEXUS\gradlew.bat test
```

## Manual test checklist
- Start a call from device A to device B.
- Accept on device B.
- Confirm audio connects and timer runs.
- End the call from either side.
