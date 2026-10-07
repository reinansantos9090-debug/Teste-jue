package com.example.testejuerpg.game3d

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

class OfflineAudioBus {
    private val sampleRate = 22050
    private val tracks = linkedMapOf<String, AudioTrack>()
    private var enabled = true

    init {
        listOf(
            "hit" to 180f, "hurt" to 110f, "pickup" to 420f, "craft" to 520f,
            "level" to 660f, "boss" to 78f, "rift" to 250f, "story" to 360f
        ).forEach { pair -> tracks[pair.first] = buildTone(pair.first, pair.second) }
    }

    fun setEnabled(value: Boolean) { enabled = value }

    fun play(id: String) {
        if (!enabled) return
        val track = tracks[id] ?: return
        try { track.stop(); track.play() } catch (_: Throwable) {}
    }

    fun release() {
        tracks.values.forEach { try { it.release() } catch (_: Throwable) {} }
        tracks.clear()
    }

    private fun buildTone(id: String, frequency: Float): AudioTrack {
        val durationMs = if (id == "boss") 260 else 95
        val samples = sampleRate * durationMs / 1000
        val pcm = ShortArray(samples)
        for (i in pcm.indices) {
            val envelope = 1f - i.toFloat() / pcm.size
            val harmonic = if (id == "boss") {
                0.72f * sin(2.0 * PI * frequency * i / sampleRate).toFloat() +
                    0.28f * sin(2.0 * PI * frequency * 2.0 * i / sampleRate).toFloat()
            } else {
                sin(2.0 * PI * frequency * i / sampleRate).toFloat()
            }
            pcm[i] = (harmonic * envelope * 0.22f * Short.MAX_VALUE).toInt().toShort()
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        return AudioTrack(
            attributes, format, pcm.size * 2, AudioTrack.MODE_STATIC,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        ).also { track -> track.write(pcm, 0, pcm.size) }
    }
}
