package com.tinkerhub.livingphone.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import java.util.Calendar

class ScreenStateTracker(
    private val context: Context,
    private val engine: PersonalityEngine
) {
    private var screenOffTime = 0L
    private var lastLateNightAlertTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var ignoredRunnable: Runnable? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock(timeoutMs: Long) {
        try {
            if (wakeLock == null) {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LivingPhone:ScreenOffIgnored")
            }
            if (wakeLock?.isHeld != true) {
                wakeLock?.acquire(timeoutMs)
            }
        } catch (e: Exception) {
            // Ignore if permission or lock fails
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOffTime = System.currentTimeMillis()

                    // Cancel any previous pending ignored runnable
                    ignoredRunnable?.let { handler.removeCallbacks(it) }

                    // Acquire wakelock so CPU stays awake for 10s timer
                    acquireWakeLock(15_000L)

                    ignoredRunnable = Runnable {
                        engine.triggerEvent(EventType.SCREEN_OFF_IGNORED)
                        releaseWakeLock()
                    }
                    handler.postDelayed(ignoredRunnable!!, 10_000L)
                }
                Intent.ACTION_SCREEN_ON -> {
                    // User picked up/woke screen before or after 10s -> cancel pending ignored trigger
                    ignoredRunnable?.let { handler.removeCallbacks(it) }
                    ignoredRunnable = null
                    releaseWakeLock()

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
        ignoredRunnable?.let { handler.removeCallbacks(it) }
        ignoredRunnable = null
        releaseWakeLock()
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
