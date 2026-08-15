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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
