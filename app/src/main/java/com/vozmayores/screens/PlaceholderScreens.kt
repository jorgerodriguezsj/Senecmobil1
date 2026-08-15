package com.vozmayores.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MessagesScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = "Mensajes",
    body = "Aquí verás las últimas conversaciones. Pronto.",
    onBack = onBack,
)

@Composable
fun AlarmsScreen(onBack: () -> Unit) = PlaceholderScreen(
    title = "Alarmas",
    body = "Aquí verás las alarmas activas y podrás quitarlas con la voz. Pronto.",
    onBack = onBack,
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        ScreenHeader(title = "Ayuda", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text(
                "Puedes decir cosas como:",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            listOf(
                "«Llama a Pepe»",
                "«Manda un wasap a María diciendo llego tarde»",
                "«Pon una alarma a las ocho y media»",
                "«Abre contactos» / «vuelve al inicio»",
                "«Muéstrame las alarmas»",
            ).forEach {
                Text(
                    text = it,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 3.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, body: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = title, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = body,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "🚧",
                fontSize = 42.sp,
            )
        }
    }
}
