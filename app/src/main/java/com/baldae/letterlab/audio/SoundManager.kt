package com.baldae.letterlab.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class GameSound(val defaultVolume: Float, val varyPitch: Boolean = false) {
    SELECT(0.50f),
    MOVE(0.80f, varyPitch = true),
    INVALID(0.45f),
    UNDO(0.55f),
    WIN(0.90f),
    ACHIEVEMENT(0.75f),
}

/**
 * All sound effects are synthesized at first launch — soft sine chimes with
 * exponential decay — written once to the cache dir and played via SoundPool.
 * No audio assets, no network, tiny APK.
 */
class SoundManager(private val context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds = mutableMapOf<GameSound, Int>()
    private val loaded = mutableSetOf<Int>()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(loaded) { loaded += sampleId }
        }
        scope.launch { synthesizeAll() }
    }

    fun play(sound: GameSound, volume: Float = sound.defaultVolume) {
        if (!enabled) return
        val id = soundIds[sound] ?: return
        if (synchronized(loaded) { id !in loaded }) return
        // A touch of pitch variation keeps repeated move sounds organic.
        val rate = if (sound.varyPitch) 0.94f + random.nextFloat() * 0.12f else 1f
        soundPool.play(id, volume, volume, 1, 0, rate)
    }

    private val random = java.util.Random()

    private fun synthesizeAll() {
        val dir = File(context.cacheDir, "sfx").apply { mkdirs() }
        val recipes: Map<GameSound, () -> ShortArray> = mapOf(
            GameSound.SELECT to { tone(660.0, 0.08, gain = 0.35) },
            GameSound.MOVE to { slide(440.0, 560.0, 0.11, gain = 0.4) },
            GameSound.INVALID to { buzz(150.0, 0.12, gain = 0.4) },
            GameSound.UNDO to { slide(560.0, 420.0, 0.11, gain = 0.35) },
            GameSound.WIN to { arpeggio(listOf(523.25, 659.25, 783.99, 1046.5), 0.14, gain = 0.4) },
            GameSound.ACHIEVEMENT to { arpeggio(listOf(783.99, 1046.5), 0.2, gain = 0.4) },
        )
        for ((sound, render) in recipes) {
            val file = File(dir, "${sound.name.lowercase()}_v2.wav")
            if (!file.exists()) file.writeBytes(wavBytes(render()))
            soundIds[sound] = soundPool.load(file.path, 1)
        }
    }

    // -------------------------------------------------------------- synthesis

    /** A soft bell voice: fundamental plus quiet upper partials for warmth. */
    private fun voice(freq: Double, t: Double): Double =
        (sin(2 * PI * freq * t) +
            0.28 * sin(2 * PI * freq * 2 * t) +
            0.10 * sin(2 * PI * freq * 3 * t)) / 1.38

    private fun tone(freq: Double, seconds: Double, gain: Double): ShortArray =
        render(seconds) { t -> voice(freq, t) * envelope(t, seconds) * gain }

    private fun slide(from: Double, to: Double, seconds: Double, gain: Double): ShortArray =
        render(seconds) { t ->
            val f = from + (to - from) * (t / seconds)
            voice(f, t) * envelope(t, seconds) * gain
        }

    /** A rougher tone (odd harmonics) for the "nope" feedback. */
    private fun buzz(freq: Double, seconds: Double, gain: Double): ShortArray =
        render(seconds) { t ->
            val s = sin(2 * PI * freq * t) + 0.35 * sin(2 * PI * freq * 3 * t)
            s / 1.35 * envelope(t, seconds) * gain
        }

    private fun arpeggio(freqs: List<Double>, noteSeconds: Double, gain: Double): ShortArray {
        val tail = 0.25
        val total = noteSeconds * freqs.size + tail
        return render(total) { t ->
            var sample = 0.0
            for ((i, f) in freqs.withIndex()) {
                val start = i * noteSeconds
                if (t >= start) {
                    val local = t - start
                    sample += voice(f, local) * exp(-6.0 * local) * gain
                }
            }
            sample / 1.5
        }
    }

    private fun envelope(t: Double, seconds: Double): Double {
        val attack = 0.005
        val a = if (t < attack) t / attack else 1.0
        return a * exp(-5.0 * t / seconds)
    }

    private fun render(seconds: Double, wave: (Double) -> Double): ShortArray {
        val n = (SAMPLE_RATE * seconds).toInt()
        return ShortArray(n) { i ->
            val v = wave(i.toDouble() / SAMPLE_RATE).coerceIn(-1.0, 1.0)
            (v * Short.MAX_VALUE).toInt().toShort()
        }
    }

    /** Minimal 16-bit mono PCM WAV container. */
    private fun wavBytes(samples: ShortArray): ByteArray {
        val dataSize = samples.size * 2
        val out = ByteArray(44 + dataSize)
        fun writeInt(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = (value shr 8 and 0xFF).toByte()
            out[offset + 2] = (value shr 16 and 0xFF).toByte()
            out[offset + 3] = (value shr 24 and 0xFF).toByte()
        }
        fun writeShort(offset: Int, value: Int) {
            out[offset] = (value and 0xFF).toByte()
            out[offset + 1] = (value shr 8 and 0xFF).toByte()
        }
        "RIFF".toByteArray().copyInto(out, 0)
        writeInt(4, 36 + dataSize)
        "WAVE".toByteArray().copyInto(out, 8)
        "fmt ".toByteArray().copyInto(out, 12)
        writeInt(16, 16)                    // fmt chunk size
        writeShort(20, 1)                   // PCM
        writeShort(22, 1)                   // mono
        writeInt(24, SAMPLE_RATE)
        writeInt(28, SAMPLE_RATE * 2)       // byte rate
        writeShort(32, 2)                   // block align
        writeShort(34, 16)                  // bits per sample
        "data".toByteArray().copyInto(out, 36)
        writeInt(40, dataSize)
        for ((i, s) in samples.withIndex()) {
            out[44 + i * 2] = (s.toInt() and 0xFF).toByte()
            out[45 + i * 2] = (s.toInt() shr 8 and 0xFF).toByte()
        }
        return out
    }

    private companion object {
        const val SAMPLE_RATE = 44100
    }
}
