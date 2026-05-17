# Plan: Realtime Call Feature (Voice/Video)

## Goals
- Add realtime calling (voice/video) to the app.
- Integrate signaling, media transport, UI, and background behavior.
- Ensure stable audio routing and call lifecycle handling.

## Checklist
- [ ] Decide SDK (Agora / Twilio / WebRTC) and obtain credentials.
- [ ] Confirm signaling backend (Firebase/Firestore/Socket) and data model.
- [ ] Define call states and transitions (idle, ringing, connected, ended, missed).
- [ ] Add permissions and foreground service for ongoing calls.
- [ ] Integrate SDK client initialization and token handling.
- [ ] Implement outgoing call flow (create call, notify callee, join channel).
- [ ] Implement incoming call flow (receive signal, show UI, accept/decline).
- [ ] Wire UI screens to call state updates.
- [ ] Handle audio routing (speaker, earpiece, Bluetooth) and mute.
- [ ] Add cleanup logic (hang up, disconnect, timeout).
- [ ] Test 1:1 call end-to-end on two devices.

## Implementation Outline
1) **Dependencies**
   - Add SDK library to Gradle.
   - Add token/signaling client (if required).

2) **Permissions and Services**
   - Microphone, camera (if video), and foreground service permissions.
   - Foreground service notification for active call.

3) **Signaling**
   - Create `calls` collection or signaling channel.
   - Store call metadata (callerId, calleeId, type, status, timestamps).
   - Listen for incoming calls and updates.

4) **Call Engine**
   - Initialize SDK engine and join channel.
   - Handle token creation/refresh.
   - Publish local audio/video and subscribe to remote stream.

5) **UI and State**
   - Update existing call screens to reflect realtime status.
   - Bind UI controls (mute, speaker, end call) to SDK.

6) **Lifecycle and Cleanup**
   - Handle app background/foreground.
   - End call and release resources.
   - Mark missed/ended calls in backend.

## Open Questions
- Which SDK to use?
- Which signaling backend to use?
- Voice only or voice + video for v1?
- 1:1 only or group calls later?

