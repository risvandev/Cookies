package com.tinkerhub.livingphone.personality

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

enum class EventType {
    CHARGER_CONNECTED,
    CHARGER_DISCONNECTED,
    BATTERY_LEVEL_20,
    BATTERY_LEVEL_10,
    BATTERY_LEVEL_5,
    BATTERY_LEVEL_1,
    OVERHEATING,
    SHAKE_DETECTED,
    SCREEN_ON_IDLE_PICKUP,
    SCREEN_OFF_IGNORED
}

class PersonalityEngine(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun triggerEvent(event: EventType) {
        Log.d("LivingPhone", "Event triggered: $event")
        
        // Define audio file names corresponding to events
        val audioFileName = when (event) {
            EventType.CHARGER_CONNECTED -> "voice_charger_connected"
            EventType.CHARGER_DISCONNECTED -> "voice_charger_disconnected"
            EventType.BATTERY_LEVEL_20 -> "voice_battery_20"
            EventType.BATTERY_LEVEL_10 -> "voice_battery_10"
            EventType.BATTERY_LEVEL_5 -> "voice_battery_5"
            EventType.BATTERY_LEVEL_1 -> "voice_battery_1"
            EventType.OVERHEATING -> "voice_overheating"
            EventType.SHAKE_DETECTED -> "voice_shake"
            EventType.SCREEN_ON_IDLE_PICKUP -> "voice_pickup"
            EventType.SCREEN_OFF_IGNORED -> "voice_ignored"
        }

        playAudio(audioFileName)
        triggerHaptic(event)
    }

    private fun playAudio(fileName: String) {
        // Try to find the resource ID dynamically so compilation doesn't fail if file is missing
        val resId = context.resources.getIdentifier(fileName, "raw", context.packageName)
        if (resId != 0) {
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer.create(context, resId)
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e("LivingPhone", "Failed to play audio $fileName", e)
            }
        } else {
            Log.w("LivingPhone", "Audio file not found in res/raw: $fileName")
        }
    }

    private fun triggerHaptic(event: EventType) {
        if (!vibrator.hasVibrator()) return

        val pattern = when (event) {
            EventType.SHAKE_DETECTED, EventType.OVERHEATING -> longArrayOf(0, 100, 50, 100, 50, 100)
            EventType.BATTERY_LEVEL_1 -> longArrayOf(0, 500, 200, 500)
            else -> longArrayOf(0, 150)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, -1)
        }
    }
}
