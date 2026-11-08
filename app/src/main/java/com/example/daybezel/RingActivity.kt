package com.example.daybezel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class RingActivity : ComponentActivity() {
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        val id = intent.getIntExtra("id", -1)
        val a = Store.load(this).find { it.id == id }
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF121110)).padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(a?.name ?: "Будильник", fontSize = 26.sp, color = Color.White)
                    Text(a?.timeText ?: "", fontSize = 72.sp, color = Color(0xFFFF8A1F))
                    Spacer(Modifier.height(48.dp))
                    Button(
                        onClick = { Scheduler.stop(this@RingActivity, id); finish() },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A1F))
                    ) { Text("Стоп", fontSize = 20.sp, color = Color.Black) }
                }
            }
        }
    }
}
