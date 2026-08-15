package com.vozmayores.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozmayores.actions.LauncherActions

@Composable
fun AlarmsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Alarmas", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Di algo como «pon una alarma a las siete y media». Aquí abres el despertador del móvil para ver y quitar las alarmas.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            BigActionButton(
                emoji = "⏰",
                label = "Abrir despertador",
                subLabel = "Ver todas las alarmas del móvil",
                bg = Color(0xFF283593),
                onClick = { LauncherActions.openSystemAlarms(context) },
            )
        }
    }
}
