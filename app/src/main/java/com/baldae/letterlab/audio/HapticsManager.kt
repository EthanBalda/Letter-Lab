package com.baldae.letterlab.audio

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Thin wrapper over the system vibrator with a settings-controlled gate. */
class HapticsManager(context: Context) {

    @Volatile
    var enabled: Boolean = true

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /** Selecting a letter. */
    fun tick() = oneShot(10, 70)

    /** A move landed. */
    fun move() = oneShot(18, 130)

    /** Illegal tap. */
    fun invalid() = pattern(longArrayOf(0, 25, 40, 25), intArrayOf(0, 160, 0, 160))

    /** Level solved. */
    fun win() = pattern(longArrayOf(0, 25, 60, 35, 60, 60), intArrayOf(0, 120, 0, 160, 0, 220))

    private fun oneShot(millis: Long, amplitude: Int) {
        if (!enabled) return
        vibrator?.takeIf { it.hasVibrator() }
            ?.vibrate(VibrationEffect.createOneShot(millis, amplitude))
    }

    private fun pattern(timings: LongArray, amplitudes: IntArray) {
        if (!enabled) return
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        if (v.hasAmplitudeControl()) {
            v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        }
    }
}
