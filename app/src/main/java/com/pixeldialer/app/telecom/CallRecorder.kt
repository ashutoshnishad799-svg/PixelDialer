package com.pixeldialer.app.telecom

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecordingMode { VOICE_CALL, MICROPHONE, FAILED }

data class RecordingResult(
    val file: File,
    val mode: RecordingMode,
    val callerLabel: String
)

/**
 * Records the current call to local storage.
 *
 * Honest note on how this actually works (worth keeping in code, not just
 * chat, since it explains the fallback logic below): Android 10+ blocks
 * third-party apps from cleanly capturing the other party's voice —
 * MediaRecorder.AudioSource.VOICE_CALL is restricted to system/privileged
 * apps on most devices. Some OEM skins (older Samsung/Xiaomi builds
 * especially) still allow it for the default dialer; everywhere else it
 * throws or silently fails. So we:
 *   1. Try VOICE_CALL first — works on some devices, best quality when it does.
 *   2. If that fails to even start, fall back to MIC — captures the
 *      earpiece/speaker audio via the microphone. Works everywhere, but
 *      picks up ambient noise and is quieter for the other party's voice,
 *      especially off speakerphone.
 * The active mode is always surfaced in the UI so nothing is silently
 * pretending to be better than it is.
 */
class CallRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var currentMode: RecordingMode = RecordingMode.FAILED

    val isRecording: Boolean
        get() = recorder != null

    fun start(callerLabel: String): RecordingMode {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeLabel = callerLabel.filter { it.isLetterOrDigit() }.ifBlank { "call" }
        val file = File(dir, "${safeLabel}_$timestamp.m4a")

        // Attempt 1: VOICE_CALL source (works on select devices/OEM builds only)
        if (tryStart(file, useVoiceCallSource = true)) {
            outputFile = file
            currentMode = RecordingMode.VOICE_CALL
            return currentMode
        }

        // Attempt 2: microphone fallback (works everywhere, lower fidelity for the other party)
        if (tryStart(file, useVoiceCallSource = false)) {
            outputFile = file
            currentMode = RecordingMode.MICROPHONE
            return currentMode
        }

        currentMode = RecordingMode.FAILED
        return currentMode
    }

    private fun tryStart(file: File, useVoiceCallSource: Boolean): Boolean {
        return try {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(
                if (useVoiceCallSource) MediaRecorder.AudioSource.VOICE_CALL
                else MediaRecorder.AudioSource.MIC
            )
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            true
        } catch (e: Exception) {
            Log.w("CallRecorder", "Recording start failed (voiceCall=$useVoiceCallSource)", e)
            releaseQuietly()
            false
        }
    }

    fun stop(): RecordingResult? {
        val r = recorder ?: return null
        return try {
            r.stop()
            r.release()
            recorder = null
            val file = outputFile ?: return null
            RecordingResult(file, currentMode, "")
        } catch (e: Exception) {
            Log.w("CallRecorder", "Recording stop failed", e)
            releaseQuietly()
            null
        }
    }

    private fun releaseQuietly() {
        try {
            recorder?.release()
        } catch (_: Exception) {
        }
        recorder = null
    }

    companion object {
        fun listRecordings(context: Context): List<File> {
            val dir = File(context.filesDir, "recordings")
            if (!dir.exists()) return emptyList()
            return dir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }

        fun deleteRecording(file: File): Boolean = file.delete()
    }
}
