package com.tinkerhub.livingphone.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import java.util.Calendar

class ScreenStateTracker(
    private val context: Context,
    private val engine: PersonalityEngine
) {
    private var screenOffTime = 0L
    private var lastLateNightAlertTime = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffTime = System.currentTimeMillis()
                }
                Intent.ACTION_SCREEN_ON -> {
                    val now = System.currentTimeMillis()

                    // Check for Late Night (11:00 PM to 5:00 AM)
                    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    val isLateNight = currentHour >= 23 || currentHour < 5

                    if (isLateNight && (now - lastLateNightAlertTime > 30 * 60 * 1000L)) {
                        engine.triggerEvent(EventType.LATE_NIGHT)
                        lastLateNightAlertTime = now
                    } else if (screenOffTime > 0) {
                        val idleDuration = now - screenOffTime
                        if (idleDuration > 30 * 60 * 1000L) { // 30 mins
                            engine.triggerEvent(EventType.SCREEN_ON_IDLE_PICKUP)
                        }
                    }
                    screenOffTime = 0L
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        context.registerReceiver(screenReceiver, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
