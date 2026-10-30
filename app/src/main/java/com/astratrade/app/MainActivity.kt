package com.astratrade.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val captureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val service = Intent(this, ScreenMonitorService::class.java).apply {
                action = ScreenMonitorService.ACTION_START
                putExtra(ScreenMonitorService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenMonitorService.EXTRA_DATA, result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service) else startService(service)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AstraTrader(
                onStartMonitor = {
                    val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                    captureLauncher.launch(manager.createScreenCaptureIntent())
                },
                onStopMonitor = {
                    startService(Intent(this, ScreenMonitorService::class.java).setAction(ScreenMonitorService.ACTION_STOP))
                }
            )
        }
    }
}

@Composable
fun AstraTrader(onStartMonitor: () -> Unit, onStopMonitor: () -> Unit) {
    var item by remember { mutableStateOf("AWM Dark Camo") }
    var buy by remember { mutableStateOf("0.13") }
    var sell by remember { mutableStateOf("0.18") }
    var fee by remember { mutableStateOf("20") }
    var threshold by remember { mutableStateOf("8") }
    var recognized by remember { mutableStateOf("ожидание первого кадра") }
    var monitorState by remember { mutableStateOf("мониторинг выключен") }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(context) {
        while (true) {
            val prefs = context.getSharedPreferences(ScreenMonitorService.PREFS, Context.MODE_PRIVATE)
            monitorState = if (prefs.getBoolean(ScreenMonitorService.KEY_RUNNING, false)) "захват активен" else "мониторинг выключен"
            recognized = prefs.getString(ScreenMonitorService.KEY_TEXT, "ожидание первого кадра") ?: "ожидание первого кадра"
            kotlinx.coroutines.delay(1200)
        }
    }
    val b = buy.toDoubleOrNull() ?: 0.0
    val s = sell.toDoubleOrNull() ?: 0.0
    val net = s * (1 - (fee.toDoubleOrNull() ?: 20.0).coerceIn(0.0, 100.0) / 100)
    val profit = net - b
    val roi = if (b > 0) profit / b * 100 else 0.0
    val pass = b > 0 && roi >= (threshold.toDoubleOrNull() ?: 0.0)
    MaterialTheme(colorScheme = darkColorScheme()) {
        Scaffold { pad ->
            LazyColumn(Modifier.fillMaxSize().padding(pad).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text("ASTRA TRADER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Standoff 2 · live screen monitor")
                }
                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("МОНИТОРИНГ ЭКРАНА", fontWeight = FontWeight.Bold)
                            Text(monitorState)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onStartMonitor, modifier = Modifier.weight(1f)) { Text("НАЧАТЬ ЗАХВАТ") }
                                OutlinedButton(onClick = onStopMonitor, modifier = Modifier.weight(1f)) { Text("СТОП") }
                            }
                            Text("Распознанный текст:", fontWeight = FontWeight.SemiBold)
                            Text(recognized, style = MaterialTheme.typography.bodySmall)
                            Text("Android покажет системный запрос на разрешение захвата. Не закрывай уведомление Astra Trade.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    Card(shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("КАЛЬКУЛЯТОР СДЕЛКИ", fontWeight = FontWeight.Bold)
                            OutlinedTextField(item, { item = it }, label = { Text("Предмет") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PriceField("Покупка G", buy, { buy = it }, Modifier.weight(1f))
                                PriceField("Продажа G", sell, { sell = it }, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PriceField("Комиссия %", fee, { fee = it }, Modifier.weight(1f))
                                PriceField("Мин. ROI %", threshold, { threshold = it }, Modifier.weight(1f))
                            }
                            HorizontalDivider()
                            Line("После комиссии", money(net) + " G")
                            Line("Чистая прибыль", money(profit) + " G")
                            Line("ROI", money(roi) + "%")
                            AssistChip(onClick = {}, label = { Text(if (pass) "ПРОХОДИТ ПОРОГ" else "НЕ ПРОХОДИТ ПОРОГ") })
                        }
                    }
                }
                item { Text("текущий этап: захват экрана + OCR. распознавание цен ещё не превращено в торговые сигналы; покупки и продажи не выполняются.", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

@Composable
fun PriceField(label: String, value: String, onValue: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(value, onValue, label = { Text(label) }, modifier = modifier, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}

@Composable
fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

fun money(value: Double) = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')