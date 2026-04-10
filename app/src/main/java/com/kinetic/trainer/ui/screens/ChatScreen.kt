package com.kinetic.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kinetic.trainer.ui.components.KineticCard
import com.kinetic.trainer.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Note: Real implementation uses Signal Protocol (libsignal-protocol-java)
// Keys stored in EncryptedSharedPreferences, only ciphertext in Firestore.
// This screen shows the UI layer; encryption happens in repository layer.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    clientId: String,
    onBack: () -> Unit
) {
    var messageText by remember { mutableStateOf("") }

    // Placeholder messages — replaced by Firestore E2E ciphertext stream in real impl
    val messages = remember {
        listOf(
            FakeChatMessage("trainer", "Hey! Great session today 💪", System.currentTimeMillis() - 3_600_000),
            FakeChatMessage("client", "Thanks coach! Felt strong on squats 🙌", System.currentTimeMillis() - 3_300_000),
            FakeChatMessage("trainer", "Exactly — you're ready to bump to 90kg next week", System.currentTimeMillis() - 3_000_000),
            FakeChatMessage("client", "Let's do it! Same time Thursday?", System.currentTimeMillis() - 2_700_000),
        )
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Surface2, RoundedCornerShape(50)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                clientId.take(2).uppercase(),
                                color = TextPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Client $clientId", color = TextPrimary, fontWeight = FontWeight.Bold,
                                fontSize = 15.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = Lime,
                                    modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("End-to-end encrypted", color = Lime, fontSize = 10.sp)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { /* open client detail */ }) {
                        Icon(Icons.Default.Person, "Client", tint = TextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface1)
            )
        },
        bottomBar = {
            Surface(color = Surface1, tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Message...", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Lime, unfocusedBorderColor = Surface2,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                            focusedContainerColor = Surface2, unfocusedContainerColor = Surface2
                        ),
                        maxLines = 4
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (messageText.isNotBlank()) messageText = "" },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Lime, RoundedCornerShape(50))
                    ) {
                        Icon(Icons.Default.Send, "Send", tint = Background)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }
    }
}

private data class FakeChatMessage(val sender: String, val text: String, val timestampMs: Long)

@Composable
private fun ChatBubble(message: FakeChatMessage) {
    val isTrainer = message.sender == "trainer"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isTrainer) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isTrainer) Alignment.End else Alignment.Start) {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp, topEnd = 16.dp,
                    bottomStart = if (isTrainer) 16.dp else 4.dp,
                    bottomEnd = if (isTrainer) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isTrainer) Lime else Surface1
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isTrainer) Background else TextPrimary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = SimpleDateFormat("h:mm a", Locale.getDefault())
                    .format(Date(message.timestampMs)),
                color = TextMuted,
                fontSize = 10.sp
            )
        }
    }
}
