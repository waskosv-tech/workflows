package com.example.daybezel

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

private val Orange = Color(0xFFFF8A1F)
private val Pill = Color(0xFFF6D3AC)

class MainActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { App() } }
    }
}

fun nowMin(): Float {
    val c = Calendar.getInstance()
    return c.get(Calendar.HOUR_OF_DAY) * 60f + c.get(Calendar.MINUTE) + c.get(Calendar.SECOND) / 60f
}

fun fmt(min: Float): String {
    val t = min.toInt(); val h = t / 60; val m = t % 60
    return if (h > 0) "$h год $m хв" else "$m хв"
}

@Composable
fun App() {
    val ctx = LocalContext.current
    var alarms by remember { mutableStateOf(Store.load(ctx)) }
    var now by remember { mutableStateOf(nowMin()) }

    val askNotif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        while (true) { now = nowMin(); alarms = Store.load(ctx); delay(1000) }
    }

    fun update(l: List<Alarm>) { alarms = l; Store.save(ctx, l) }
    fun delta(a: Alarm) = (a.minuteOfDay - now + 1440f) % 1440f
    val next = alarms.filter { it.enabled }.minByOrNull { delta(it) }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF121110))
            .statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Daybezel", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val c = center
                val r = size.minDimension / 2 * 0.74f
                fun pos(min: Float, rad: Float): Offset {
                    val a = min / 1440f * 2f * PI.toFloat()
                    return Offset(c.x + rad * sin(a), c.y - rad * cos(a))
                }
                drawCircle(Color(0xFF3A342E), r * 1.12f, c, style = Stroke(2f))
                drawCircle(Color(0xFF221F1C), r, c, style = Stroke(r * 0.16f))
                drawCircle(Color(0xFF161412), r * 0.8f, c)

                val paint = android.graphics.Paint().apply {
                    color = 0xFF6F675F.toInt(); textSize = r * 0.1f
                    textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
                }
                for (h in 0 until 24 step 3) {
                    val p = pos(h * 60f, r * 1.27f)
                    drawContext.canvas.nativeCanvas.drawText("%02d".format(h), p.x, p.y + paint.textSize / 3, paint)
                }
                if (next != null) {
                    drawArc(
                        Orange, now / 1440f * 360f - 90f, delta(next) / 1440f * 360f, false,
                        Offset(c.x - r, c.y - r), Size(2 * r, 2 * r),
                        style = Stroke(r * 0.05f, cap = StrokeCap.Round)
                    )
                }
                alarms.filter { it.enabled }.forEach { a ->
                    val p = pos(a.minuteOfDay.toFloat(), r)
                    rotate(a.minuteOfDay / 1440f * 360f, p) {
                        drawRoundRect(
                            if (a.id == next?.id) Orange else Pill,
                            Offset(p.x - r * 0.07f, p.y - r * 0.1f),
                            Size(r * 0.14f, r * 0.2f), CornerRadius(r * 0.07f)
                        )
                    }
                }
                drawCircle(Color.White, r * 0.04f, pos(now, r))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (next != null) {
                    Text("НАСТУПНИЙ", fontSize = 11.sp, color = Color(0xFF8A8178), letterSpacing = 2.sp)
                    Text(next.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text(next.timeText, fontSize = 44.sp, color = Color.White)
                    Text("через ${fmt(delta(next))}", fontSize = 13.sp, color = Color(0xFFCFC7BD))
                    Text("${next.rings} дзвінки · кожні ${next.intervalMin} хв", fontSize = 11.sp, color = Color(0xFF8A8178))
                } else Text("Немає будильників", color = Color.White)
            }
        }

        alarms.sortedBy { it.minuteOfDay }.forEach { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(a.timeText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold,
                    color = if (a.enabled) Color.White else Color(0xFF555555))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.name, color = if (a.enabled) Color.White else Color(0xFF555555))
                    TextButton(
                        onClick = { update(alarms.map { if (it.id == a.id) it.copy(repeatDaily = !a.repeatDaily) else it }) },
                        contentPadding = PaddingValues(0.dp)
                    ) { Text(if (a.repeatDaily) "Щодня" else "Один раз", fontSize = 12.sp, color = Color(0xFF8A8178)) }
                }
                Switch(checked = a.enabled, onCheckedChange = { on ->
                    val upd = a.copy(enabled = on)
                    if (on) Scheduler.schedule(ctx, upd) else Scheduler.cancel(ctx, upd)
                    update(alarms.map { if (it.id == a.id) upd else it })
                })
                TextButton(onClick = {
                    Scheduler.cancel(ctx, a); update(alarms.filter { it.id != a.id })
                }) { Text("✕", color = Color(0xFF8A8178)) }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                TimePickerDialog(ctx, { _, h, m ->
                    val a = Alarm((alarms.maxOfOrNull { it.id } ?: 0) + 1, h, m, "Будильник")
                    Scheduler.schedule(ctx, a)
                    update(alarms + a)
                }, 7, 0, true).show()
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange)
        ) { Text("＋ Додати будильник", color = Color.Black) }
        Spacer(Modifier.height(24.dp))
    }
}
