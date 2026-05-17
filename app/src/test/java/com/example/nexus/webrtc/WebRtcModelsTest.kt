package com.example.nexus.webrtc

import com.example.nexus.data.webrtc.IceCandidateData
import com.example.nexus.data.webrtc.SessionDescriptionData
import com.example.nexus.data.webrtc.toIceCandidate
import com.example.nexus.data.webrtc.toSessionDescription
import org.junit.Assert.assertEquals
import org.junit.Test
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class WebRtcModelsTest {
    @Test
    fun sessionDescriptionRoundTrip() {
        val original = SessionDescriptionData(type = "offer", sdp = "v=0", senderId = "u1")
        val converted = original.toSessionDescription()
        assertEquals(SessionDescription.Type.OFFER, converted.type)
        assertEquals("v=0", converted.description)
    }

    @Test
    fun iceCandidateRoundTrip() {
        val original = IceCandidateData(sdpMid = "audio", sdpMLineIndex = 0, sdp = "candidate:1", senderId = "u1")
        val converted: IceCandidate = original.toIceCandidate()
        assertEquals("audio", converted.sdpMid)
        assertEquals(0, converted.sdpMLineIndex)
        assertEquals("candidate:1", converted.sdp)
    }
}

