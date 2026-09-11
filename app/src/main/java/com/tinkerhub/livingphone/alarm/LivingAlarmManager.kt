package com.tinkerhub.livingphone.alarm

import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AlarmState(
    val isRunning: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val progress: Float = 0f,
    val currentStage: String = "Set a timer to test the soul's patience"
)

class LivingAlarmManager(private val engine: PersonalityEngine) {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _alarmState = MutableStateFlow(AlarmState())
    val alarmState: StateFlow<AlarmState> = _alarmState.asStateFlow()

    fun startAlarm(totalSeconds: Int) {
        if (totalSeconds <= 0) return
        stopAlarm()

        timerJob = scope.launch {
            _alarmState.value = AlarmState(
                isRunning = true,
                totalSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                progress = 1f,
                currentStage = "Starting alarm... Preparing judgment"
            )

            // Slight delay (1.2 sec) before the first voice note plays
            delay(1200)
            if (!isActive) return@launch
            engine.triggerEvent(EventType.ALARM_STARTED)
            _alarmState.value = _alarmState.value.copy(
                currentStage = "Stage 1: 'You really think you'll finish?'"
            )

            var twoThirdsTriggered = false
            var final5Triggered = false

            val twoThirdsMarkSeconds = if (totalSeconds <= 15) 7 else (totalSeconds * 2 / 3)

            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                val progress = remaining.toFloat() / totalSeconds.toFloat()
                val elapsed = totalSeconds - remaining

                if (!twoThirdsTriggered && elapsed >= twoThirdsMarkSeconds && remaining > 5) {
                    twoThirdsTriggered = true
                    engine.triggerEvent(EventType.ALARM_TWO_THIRDS)
                    _alarmState.value = _alarmState.value.copy(
                        remainingSeconds = remaining,
                        progress = progress,
                        currentStage = "Stage 2: ⚠️ 2/3 of your time is GONE!"
                    )
                } else if (!final5Triggered && remaining in 1..5) {
                    final5Triggered = true
                    engine.triggerEvent(EventType.ALARM_FINAL_5_SEC)
                    _alarmState.value = _alarmState.value.copy(
                        remainingSeconds = remaining,
                        progress = progress,
                        currentStage = "Stage 3: 🚨 5 SECONDS LEFT! RUN!"
                    )
                } else {
                    _alarmState.value = _alarmState.value.copy(
                        remainingSeconds = remaining,
                        progress = progress
                    )
                }
            }

            if (isActive && remaining == 0) {
                engine.triggerEvent(EventType.ALARM_DONE)
                _alarmState.value = AlarmState(
                    isRunning = false,
                    totalSeconds = totalSeconds,
                    remainingSeconds = 0,
                    progress = 0f,
                    currentStage = "Stage 4: ⏰ TIME IS UP!"
                )
            }
        }
    }

    fun stopAlarm() {
        timerJob?.cancel()
        timerJob = null
        _alarmState.value = AlarmState(
            isRunning = false,
            totalSeconds = 0,
            remainingSeconds = 0,
            progress = 0f,
            currentStage = "Alarm Cancelled"
        )
    }
}
