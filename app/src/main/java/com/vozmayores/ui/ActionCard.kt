package com.vozmayores.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vozmayores.intent.IntentAction
import com.vozmayores.intent.IntentSource

data class HistoryEntry(
    val intent: IntentAction,
    val phone: String?,
    val simulated: Boolean,
    val message: String,
    val source: IntentSource = IntentSource.NONE,
    val llmRaw: String? = null,
)

@Composable
fun ActionCard(entry: HistoryEntry) {
    val (bg, header) = when (entry.intent) {
        is IntentAction.Call     -> Color(0xFF2E7D32) to "📞  LLAMADA"
        is IntentAction.WhatsApp -> Color(0xFF075E54) to "💬  WHATSAPP"
        is IntentAction.Sms      -> Color(0xFF1565C0) to "✉️  SMS"
        is IntentAction.Alarm    -> Color(0xFF283593) to "⏰  ALARMA"
        is IntentAction.Respond  -> Color(0xFF455A64) to "🗣  RESPUESTA"
        is IntentAction.Open     -> Color(0xFF5D4037) to "🧭  NAVEGAR"
        IntentAction.Back        -> Color(0xFF5D4037) to "↩  VOLVER"
        IntentAction.Unknown     -> Color(0xFFC62828) to "❓  SIN ENTENDER"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = header,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (entry.simulated) "SIMULACIÓN" else "EJECUTADO",
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (val a = entry.intent) {
            is IntentAction.Call -> {
                ContactRow(name = a.contact, phone = entry.phone)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (entry.phone != null) "Marcando…" else "❗ Sin número",
                    color = Color.White,
                    fontSize = 18.sp,
                )
            }

            is IntentAction.WhatsApp -> {
                ContactRow(name = a.contact, phone = entry.phone)
                Spacer(modifier = Modifier.height(12.dp))
                MessageBubble(text = a.message, mine = true, bubbleColor = Color(0xFFDCF8C6))
                if (entry.phone == null) NoContactBanner(a.contact)
            }

            is IntentAction.Sms -> {
                ContactRow(name = a.contact, phone = entry.phone)
                Spacer(modifier = Modifier.height(12.dp))
                MessageBubble(text = a.message, mine = true, bubbleColor = Color(0xFFBBDEFB))
                if (entry.phone == null) NoContactBanner(a.contact)
            }

            is IntentAction.Alarm -> {
                Text(
                    text = "%02d:%02d".format(a.hour, a.minute),
                    color = Color.White,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (!a.label.isNullOrBlank()) {
                    Text(
                        text = a.label,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                    )
                }
            }

            is IntentAction.Respond -> {
                Text(
                    text = a.text,
                    color = Color.White,
                    fontSize = 20.sp,
                )
            }

            is IntentAction.Open -> {
                Text(
                    text = "${a.screen.emoji}  ${a.screen.title}",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            IntentAction.Back -> {
                Text(
                    text = "Inicio",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            IntentAction.Unknown -> {
                Text(
                    text = "No entendí la orden.",
                    color = Color.White,
                    fontSize = 18.sp,
                )
            }
        }

        if (entry.message.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = entry.message,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
            )
        }

        val sourceLabel = when (entry.source) {
            IntentSource.LOCAL -> "🔎 regex"
            IntentSource.LLM -> "🧠 LLM"
            IntentSource.NONE -> null
        }
        if (sourceLabel != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = sourceLabel,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (!entry.llmRaw.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "raw: ${entry.llmRaw.take(300)}",
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ContactRow(name: String, phone: String?) {
    Column {
        Text(
            text = name,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        if (phone != null) {
            Text(
                text = phone,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
        } else {
            Text(
                text = "(sin contacto)",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun MessageBubble(text: String, mine: Boolean, bubbleColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Text(
                text = text,
                color = Color(0xFF212121),
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun NoContactBanner(contact: String) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "❗ No encuentro a $contact en tus contactos.",
        color = Color(0xFFFFF59D),
        fontSize = 13.sp,
    )
}
