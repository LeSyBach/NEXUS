package com.example.nexus.data.firebase

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val progress: Float = 0f
)

@Singleton
class AudioPlayerHelper @Inject constructor() {

    private var player: MediaPlayer? = null
    private var _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    fun load(context: Context, uri: Uri) {
        release()
        player = MediaPlayer().apply {
            setDataSource(context, uri)
            prepare()
            _state.value = PlaybackState(
                isPlaying = false,
                currentPositionMs = 0L,
                durationMs = duration.toLong(),
                progress = 0f
            )
        }
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            stopProgressTracking()
            _state.value = _state.value.copy(isPlaying = false)
        } else {
            p.start()
            startProgressTracking()
            _state.value = _state.value.copy(isPlaying = true)
            p.setOnCompletionListener {
                stopProgressTracking()
                _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0L, progress = 0f)
            }
        }
    }

    fun seekTo(fraction: Float) {
        val p = player ?: return
        val targetMs = (fraction * p.duration).toInt()
        p.seekTo(targetMs)
        _state.value = _state.value.copy(
            currentPositionMs = targetMs.toLong(),
            progress = fraction
        )
    }

    fun stop() {
        player?.let {
            if (it.isPlaying) it.pause()
            it.seekTo(0)
        }
        stopProgressTracking()
        _state.value = _state.value.copy(isPlaying = false, currentPositionMs = 0L, progress = 0f)
    }

    fun release() {
        stopProgressTracking()
        player?.release()
        player = null
        _state.value = PlaybackState()
    }

    private fun startProgressTracking() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (true) {
                val p = player ?: break
                if (!p.isPlaying) break
                val dur = p.duration.toLong()
                val pos = p.currentPosition.toLong()
                _state.value = _state.value.copy(
                    currentPositionMs = pos,
                    durationMs = dur,
                    progress = if (dur > 0) pos.toFloat() / dur else 0f
                )
                delay(100)
            }
        }
    }

    private fun stopProgressTracking() {
        progressJob?.cancel()
        progressJob = null
    }
}
