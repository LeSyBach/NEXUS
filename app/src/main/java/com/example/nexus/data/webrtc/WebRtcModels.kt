package com.example.nexus.data.webrtc

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

/**
 * Serializable WebRTC session description for Firebase signaling.
 */
data class SessionDescriptionData(
    val type: String = "",
    val sdp: String = "",
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Serializable ICE candidate for Firebase signaling.
 */
data class IceCandidateData(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = "",
    val senderId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

fun SessionDescription.toData(senderId: String): SessionDescriptionData {
    return SessionDescriptionData(type.canonicalForm(), description ?: "", senderId)
}

fun SessionDescriptionData.toSessionDescription(): SessionDescription {
    val sdpType = SessionDescription.Type.fromCanonicalForm(type)
    return SessionDescription(sdpType, sdp)
}

fun IceCandidate.toData(senderId: String): IceCandidateData {
    return IceCandidateData(sdpMid ?: "", sdpMLineIndex, sdp ?: "", senderId)
}

fun IceCandidateData.toIceCandidate(): IceCandidate {
    return IceCandidate(sdpMid, sdpMLineIndex, sdp)
}

