package com.example.nexus.data.firebase

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Android MediaRecorder for voice message recording.
 * Outputs .m4a (AAC) files to externalCacheDir.
 */
@Singleton
class VoiceRecorderHelper @Inject constructor() {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startTimeMs: Long = 0L

    /**
     * Start recording a voice message. Returns the output File.
     */
    fun startRecording(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.externalCacheDir ?: context.cacheDir
        val file = File(storageDir, "NEXUS_VOICE_${timeStamp}.m4a")

        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }).apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }

        currentFile = file
        startTimeMs = System.currentTimeMillis()
        return file
    }

    /**
     * Stop recording and return the recorded file Uri + duration in seconds.
     */
    fun stopRecording(): Pair<Uri, Long>? {
        return try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
            val file = currentFile ?: return null
            val durationSec = ((System.currentTimeMillis() - startTimeMs) / 1000).coerceAtLeast(1)
            currentFile = null
            startTimeMs = 0L
            Pair(Uri.fromFile(file), durationSec)
        } catch (_: Exception) {
            recorder?.release()
            recorder = null
            currentFile = null
            startTimeMs = 0L
            null
        }
    }

    /**
     * Cancel recording and delete the file.
     */
    fun cancelRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (_: Exception) {
            try { recorder?.release() } catch (_: Exception) {}
        }
        recorder = null
        currentFile?.delete()
        currentFile = null
        startTimeMs = 0L
    }

    /**
     * Return the maximum amplitude sampled since last call (0..32767).
     * Returns 0 if not recording.
     */
    fun getMaxAmplitude(): Int {
        return try {
            recorder?.maxAmplitude ?: 0
        } catch (_: Exception) {
            0
        }
    }

    val isRecording: Boolean
        get() = recorder != null
}
