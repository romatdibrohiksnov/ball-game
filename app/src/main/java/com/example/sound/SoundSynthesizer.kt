package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

/**
 * Ultra-high-performance, zero-allocation procedural audio synthesizer.
 * Pre-allocates and reuses static AudioTracks to eliminate all GC pauses and thread overhead.
 */
class SoundSynthesizer {

    private val sampleRate = 44100
    private var isMuted = false

    // Pentatonic scale frequencies in Hz (C5, D5, E5, G5, A5, C6, D6, E6, G6, A6, C7)
    private val pentatonicScale = floatArrayOf(
        523.25f, 587.33f, 659.25f, 783.99f, 880.00f,
        1046.50f, 1174.66f, 1318.51f, 1567.98f, 1760.00f, 2093.00f
    )

    private val audioTracks = arrayOfNulls<AudioTrack>(pentatonicScale.size)
    private var lastPlayTimeMs = 0L

    init {
        try {
            val durationSec = 0.065f // 65ms crisp percussive glass plink
            val numSamples = (sampleRate * durationSec).toInt()

            for (i in pentatonicScale.indices) {
                val freq = pentatonicScale[i]
                val buffer = ShortArray(numSamples)
                for (s in 0 until numSamples) {
                    val t = s.toFloat() / sampleRate
                    // Rapid exponential decay for crystal glass tap
                    val envelope = kotlin.math.exp(-s * 55.0 / sampleRate).toFloat()
                    val sampleVal = (sin(2.0 * Math.PI * freq * t) * 0.78 +
                            sin(2.0 * Math.PI * freq * 2.0 * t) * 0.22).toFloat()
                    val finalVal = (sampleVal * envelope * 0.6f * Short.MAX_VALUE).toInt()
                    buffer[s] = finalVal.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                audioTracks[i] = track
            }
        } catch (_: Exception) {
            // Audio hardware fallback
        }
    }

    /**
     * Plays the bounce chime using zero allocations and zero coroutine spawns.
     */
    fun playBounceChime(ballCount: Int) {
        if (isMuted) return
        val now = System.currentTimeMillis()
        // Throttle slightly to prevent sound saturation
        if (now - lastPlayTimeMs < 45) return
        lastPlayTimeMs = now

        try {
            val index = (kotlin.math.log2(ballCount.coerceAtLeast(1).toDouble()).toInt()) % pentatonicScale.size
            val track = audioTracks[index] ?: return
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: Exception) {
            // Audio track transient state fallback
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun isMuted(): Boolean = isMuted

    fun release() {
        for (i in audioTracks.indices) {
            try {
                audioTracks[i]?.stop()
                audioTracks[i]?.release()
                audioTracks[i] = null
            } catch (_: Exception) {}
        }
    }
}
