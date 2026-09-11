package com.tinkerhub.livingphone.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tinkerhub.livingphone.personality.EventType
import com.tinkerhub.livingphone.personality.PersonalityEngine
import com.tinkerhub.livingphone.service.LivingPhoneService

class MainActivity : ComponentActivity() {
    private lateinit var engine: PersonalityEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = PersonalityEngine(this)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onStartService = { startLivingService() },
                        onTestAudio = { event -> engine.triggerEvent(event) }
                    )
                }
            }
        }
    }

    private fun startLivingService() {
        val serviceIntent = Intent(this, LivingPhoneService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }
}

@Composable
fun MainScreen(
    onStartService: () -> Unit,
    onTestAudio: (EventType) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "LivingPhone Sentient App", style = MaterialTheme.typography.headlineMedium)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onStartService) {
            Text("Start Phone Soul (Foreground Service)")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }) {
            Text("Ignore Battery Optimizations")
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Test Chamber", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onTestAudio(EventType.CHARGER_CONNECTED) }) { Text("Plug In") }
            Button(onClick = { onTestAudio(EventType.CHARGER_DISCONNECTED) }) { Text("Unplug") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { onTestAudio(EventType.BATTERY_LEVEL_20) }) { Text("Bat 20%") }
            Button(onClick = { onTestAudio(EventType.BATTERY_LEVEL_1) }) { Text("Bat 1%") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            Button(onClick = { onTestAudio(EventType.SHAKE_DETECTED) }) { Text("Shake") }
            Button(onClick = { onTestAudio(EventType.OVERHEATING) }) { Text("Overheat") }
        }
    }
}
