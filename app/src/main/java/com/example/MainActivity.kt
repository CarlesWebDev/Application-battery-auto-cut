package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.BatteryManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DashboardScreen
import com.example.ui.MainViewModel
import com.example.ui.UIEvent
import com.example.ui.theme.MyApplicationTheme
import java.util.*

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private var viewModelReference: MainViewModel? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    // Broadcast receiver for live hardware battery changes
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 50)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val percent = if (scale > 0) (level * 100) / scale else level

            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 3980) // mV
            val temperatureTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 285)
            val temperature = temperatureTenths / 10.0 // tenths -> Celsius

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val pluggedType = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC (Quick Charge)"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB (Slow)"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Baterai"
            }

            val healthVal = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
            val health = when (healthVal) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Sehat"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlalu Panas 🔥"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Mati (Dead)"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Kelebihan Tegangan"
                else -> "Sehat"
            }

            // Push status directly to StateFlow state machine
            viewModelReference?.updateBatteryStatus(
                level = percent,
                voltage = voltage,
                temperature = temperature,
                isCharging = isCharging,
                pluggedType = pluggedType,
                health = health
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        setContent {
            MyApplicationTheme {
                // Instantiate MainViewModel
                val mainVm: MainViewModel = viewModel()
                viewModelReference = mainVm

                // Listen to central single-source-of-truth actions (alarm, TTS, toast messages)
                LaunchedEffect(key1 = true) {
                    mainVm.uiEvents.collect { event ->
                        when (event) {
                            is UIEvent.TriggerAlarm -> {
                                // Sound standard notification alarm
                                try {
                                    val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                    val ringtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                                    ringtone?.play()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                // Speak via TTS Synthesizer
                                if (event.isTtsEnabled && ttsInitialized) {
                                    textToSpeech?.speak(
                                        "Perhatian! Pengisian daya diputus otomatis karena batre telah mencapai limit ${event.level} persen.",
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "autocut_alert_tts"
                                    )
                                }
                            }
                            is UIEvent.ShowToast -> {
                                Toast.makeText(applicationContext, event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                DashboardScreen(
                    viewModel = mainVm,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Register receiver with the dynamic filter
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        registerReceiver(batteryReceiver, filter)
    }

    override fun onStop() {
        super.onStop()
        // Unregister to prevent battery draining background leakages
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("id", "ID")) // Set Indonesian language locale
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to default Locale if ID is missing (like English)
                textToSpeech?.setLanguage(Locale.getDefault())
            }
            ttsInitialized = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up TTS
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
