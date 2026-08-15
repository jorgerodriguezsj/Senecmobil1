package com.vozmayores.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozmayores.actions.ContactResolver
import com.vozmayores.actions.ContactRow
import com.vozmayores.intent.LocalMatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen(
    contactResolver: ContactResolver,
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    onWhatsApp: (String) -> Unit,
) {
    var contacts by remember { mutableStateOf<List<ContactRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        contacts = withContext(Dispatchers.IO) {
            runCatching { contactResolver.getAllContacts() }.getOrElse { emptyList() }
        }
        loading = false
    }

    val filtered = remember(contacts, query) {
        if (query.isBlank()) contacts
        else {
            val q = LocalMatcher.stripAccents(query.trim().lowercase())
            contacts.filter { row ->
                LocalMatcher.stripAccents(row.name.lowercase()).contains(q)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(title = "Contactos", onBack = onBack)

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar…", fontSize = 14.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        when {
            loading -> CenterMessage("Cargando…")
            contacts.isEmpty() -> CenterMessage("No tengo permiso o no hay contactos.")
            filtered.isEmpty() -> CenterMessage("Ningún contacto con «$query».")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
            ) {
                items(items = filtered, key = { it.name }) { row ->
                    ContactRowItem(
                        row = row,
                        onCall = { onCall(row.name) },
                        onWhatsApp = { onWhatsApp(row.name) },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ContactRowItem(
    row: ContactRow,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name = row.name)
        Spacer(modifier = Modifier.size(width = 12.dp, height = 0.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.phone,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ActionBadge(emoji = "📞", bg = Color(0xFF2E7D32), onClick = onCall)
        Spacer(modifier = Modifier.size(width = 6.dp, height = 0.dp))
        ActionBadge(emoji = "💬", bg = Color(0xFF075E54), onClick = onWhatsApp)
    }
}

@Composable
private fun Avatar(name: String) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondary),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun ActionBadge(emoji: String, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 22.sp)
    }
}

@Composable
private fun CenterMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
