package com.baldae.letterlab.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.round
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Ambient background music: a 16-second synthesized pad loop
 * (Am – F – C – G, soft sine stacks with slow crossfades), played gaplessly
 * from a static AudioTrack. Fully offline, ~700 KB of RAM, no assets.
 *
 * Every frequency is quantized to a multiple of 1/LOOP_SECONDS Hz so each
 * oscillator completes an integer number of cycles per loop — the wraparound
 * is mathematically seamless.
 */
class MusicManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var enabled: Boolean = false

    @Volatile
    private var inForeground: Boolean = false

    init {
        scope.launch {
            val pcm = synthesizeLoop()
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
            audioTrack.write(pcm, 0, pcm.size)
            audioTrack.setLoopPoints(0, pcm.size, -1)
            audioTrack.setVolume(VOLUME)
            track = audioTrack
            applyState()
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        applyState()
    }

    fun onForeground() {
        inForeground = true
        applyState()
    }

    fun onBackground() {
        inForeground = false
        applyState()
    }

    private fun applyState() {
        val t = track ?: return
        try {
            if (enabled && inForeground) {
                if (t.playState != AudioTrack.PLAYSTATE_PLAYING) t.play()
            } else {
                if (t.playState == AudioTrack.PLAYSTATE_PLAYING) t.pause()
            }
        } catch (_: IllegalStateException) {
            // Track was released or not ready; harmless.
        }
    }

    // ---------------------------------------------------------------- synth

    private fun synthesizeLoop(): ShortArray {
        // Chord roots in a calm A-minor progression, as sine stacks.
        val chords = listOf(
            listOf(110.00, 164.81, 220.00, 261.63), // A minor
            listOf(87.31, 130.81, 174.61, 220.00),  // F major
            listOf(130.81, 196.00, 261.63, 329.63), // C major
            listOf(98.00, 146.83, 196.00, 246.94),  // G major
        ).map { chord -> chord.map(::loopQuantize) }

        val total = SAMPLE_RATE * LOOP_SECONDS
        val segment = LOOP_SECONDS.toDouble() / chords.size
        val fade = 1.2 // seconds of crossfade between chords
        val tremoloHz = loopQuantize(0.1875) // 3 cycles per loop

        return ShortArray(total) { n ->
            val t = n.toDouble() / SAMPLE_RATE
            var sample = 0.0
            for ((index, chord) in chords.withIndex()) {
                val gain = chordWindow(t, index * segment, segment, fade)
                if (gain <= 0.0) continue
                var voice = 0.0
                for ((vIndex, freq) in chord.withIndex()) {
                    // Upper voices slightly quieter for a rounder blend.
                    voice += sin(2 * PI * freq * t) * (1.0 - vIndex * 0.15)
                }
                sample += voice / chord.size * gain
            }
            val tremolo = 0.92 + 0.08 * sin(2 * PI * tremoloHz * t)
            ((sample * tremolo * 0.55).coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /**
     * Raised-cosine window for one chord, periodic over the loop.
     * Ramp-in over [0, fade], full until [length], ramp-out over
     * [length, length+fade]. Adjacent windows' ramps are complementary
     * (they sum to exactly 1), so crossfades neither clip nor dip.
     */
    private fun chordWindow(t: Double, start: Double, length: Double, fade: Double): Double {
        val rel = (t - start).mod(LOOP_SECONDS.toDouble())
        return when {
            rel < fade -> 0.5 * (1 - kotlin.math.cos(PI * rel / fade))
            rel < length -> 1.0
            rel < length + fade -> 0.5 * (1 + kotlin.math.cos(PI * (rel - length) / fade))
            else -> 0.0
        }
    }

    /** Snaps a frequency to a whole number of cycles per loop. */
    private fun loopQuantize(freq: Double): Double =
        round(freq * LOOP_SECONDS) / LOOP_SECONDS

    private companion object {
        const val SAMPLE_RATE = 22050
        const val LOOP_SECONDS = 16
        const val VOLUME = 0.30f
    }
}
