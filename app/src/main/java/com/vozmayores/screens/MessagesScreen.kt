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
fun MessagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Mensajes", onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Di algo como «mándale un wasap a María diciendo…» y la app hace el resto. Aquí abres las apps de mensajes.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            Spacer(modifier = Modifier.height(12.dp))

            BigActionButton(
                emoji = "💬",
                label = "Abrir WhatsApp",
                bg = Color(0xFF075E54),
                onClick = { LauncherActions.openWhatsApp(context) },
            )

            Spacer(modifier = Modifier.height(10.dp))

            BigActionButton(
                emoji = "✉️",
                label = "Abrir Mensajes",
                subLabel = "SMS",
                bg = Color(0xFF1565C0),
                onClick = { LauncherActions.openMessaging(context) },
            )
        }
    }
}
