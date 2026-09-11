package com.tinkerhub.livingphone.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine

class ScreenStateTracker(
    private val context: Context,
    private val engine: PersonalityEngine
) {
    private var screenOffTime = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffTime = System.currentTimeMillis()
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (screenOffTime > 0) {
                        val idleDuration = System.currentTimeMillis() - screenOffTime
                        if (idleDuration > 30 * 60 * 1000) { // 30 mins
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
