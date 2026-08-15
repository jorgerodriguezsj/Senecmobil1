package com.vozmayores.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozmayores.nav.Screen
import com.vozmayores.ui.ActionCard
import com.vozmayores.ui.HistoryEntry
import com.vozmayores.ui.VozColors

@Composable
fun HomeScreen(
    onOpen: (Screen) -> Unit,
    simulate: Boolean,
    onSimulateChange: (Boolean) -> Unit,
    vadEnabled: Boolean,
    onVadChange: (Boolean) -> Unit,
    settingsEnabled: Boolean,
    probeEnabled: Boolean,
    onProbeClick: () -> Unit,
    status: String,
    transcript: String,
    lastEntry: HistoryEntry?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Voz",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = VozColors.KnobRedDark,
        )
        Text(
            text = "Toca una app o habla",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTile(Screen.Contacts.emoji, Screen.Contacts.title, Modifier.weight(1f)) {
                onOpen(Screen.Contacts)
            }
            AppTile(Screen.Messages.emoji, Screen.Messages.title, Modifier.weight(1f)) {
                onOpen(Screen.Messages)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AppTile(Screen.Alarms.emoji, Screen.Alarms.title, Modifier.weight(1f)) {
                onOpen(Screen.Alarms)
            }
            AppTile(Screen.Help.emoji, Screen.Help.title, Modifier.weight(1f)) {
                onOpen(Screen.Help)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SettingsRow(
            simulate = simulate,
            onSimulateChange = onSimulateChange,
            vadEnabled = vadEnabled,
            onVadChange = onVadChange,
            enabled = settingsEnabled,
            probeEnabled = probeEnabled,
            onProbeClick = onProbeClick,
        )

        if (status.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = status,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (transcript.isNotBlank()) {
            Text(
                text = "«$transcript»",
                modifier = Modifier.padding(top = 10.dp, start = 8.dp, end = 8.dp),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        if (lastEntry != null) {
            Spacer(modifier = Modifier.height(10.dp))
            ActionCard(lastEntry)
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun SettingsRow(
    simulate: Boolean,
    onSimulateChange: (Boolean) -> Unit,
    vadEnabled: Boolean,
    onVadChange: (Boolean) -> Unit,
    enabled: Boolean,
    probeEnabled: Boolean,
    onProbeClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ToggleRow(
            title = "Modo simulación",
            subtitle = if (simulate) "Anuncia sin ejecutar" else "Ejecuta las acciones",
            checked = simulate,
            onCheckedChange = onSimulateChange,
            enabled = enabled,
        )
        Spacer(modifier = Modifier.height(6.dp))
        ToggleRow(
            title = "Soltar al callar",
            subtitle = if (vadEnabled) "La app suelta sola cuando te callas"
            else "Suelta el botón tú a mano",
            checked = vadEnabled,
            onCheckedChange = onVadChange,
            enabled = enabled,
        )
        OutlinedButton(
            enabled = probeEnabled,
            onClick = onProbeClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("Probar todo", fontSize = 13.sp)
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun AppTile(
    emoji: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji, fontSize = 42.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
