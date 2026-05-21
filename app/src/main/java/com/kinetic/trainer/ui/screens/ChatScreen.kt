package com.kinetic.trainer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kinetic.trainer.data.models.ChatMessage
import com.kinetic.trainer.ui.theme.*
import com.kinetic.trainer.ui.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    clientId: String,
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
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
                Column {
                    // Send-error banner — dismissed automatically when the user retries
                    uiState.sendError?.let { errorMsg ->
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        OutlinedTextField(
                            value = uiState.inputText,
                            onValueChange = {
                                viewModel.clearSendError()
                                viewModel.onInputChange(it)
                            },
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
                            onClick = { viewModel.sendMessage {} },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (uiState.inputText.isBlank()) Surface2 else Lime,
                                    RoundedCornerShape(50)
                                ),
                            enabled = uiState.inputText.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send, "Send",
                                tint = if (uiState.inputText.isBlank()) TextMuted else Background
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Lime)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    ChatBubble(msg, isTrainer = msg.isFromTrainer)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isTrainer: Boolean) {
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
