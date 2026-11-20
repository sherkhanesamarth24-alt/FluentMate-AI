package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.EnglishLevel
import com.example.data.GeneratedWordDraft
import com.example.data.SessionMode
import com.example.data.UiState
import com.example.ui.components.LevelSelectorRow
import com.example.ui.components.SmartCorrectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    currentLevel: EnglishLevel,
    sessionsState: UiState<List<ChatSession>>,
    selectedSessionId: String?,
    messagesState: UiState<List<ChatMessage>>,
    isSending: Boolean,
    isGeneratingReport: Boolean,
    onSelectLevel: (EnglishLevel) -> Unit,
    onSelectSession: (String?) -> Unit,
    onNewChat: () -> Unit,
    onClearChat: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    onFinishInterview: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onSaveVocabularyWord: (GeneratedWordDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val activeSession = sessions.firstOrNull { it.id == selectedSessionId }
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val isInterviewMode = activeSession?.mode == SessionMode.INTERVIEW.name ||
        !activeSession?.interviewCategory.isNullOrBlank()
    val userAnswerCount = messages.count { it.role == "user" }

    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 880.dp)
                .fillMaxSize()
        ) {
            // Chat Toolbar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeSession?.title ?: "New English Chat",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            activeSession?.topicTitle?.takeIf { it.isNotBlank() }?.let { topicTitle ->
                                Text(
                                    text = "Topic: $topicTitle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showHistorySheet = true },
                                modifier = Modifier.testTag("chat_history_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Chat history"
                                )
                            }
                            IconButton(
                                onClick = onNewChat,
                                modifier = Modifier.testTag("new_chat_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddComment,
                                    contentDescription = "New chat"
                                )
                            }
                            if (selectedSessionId != null && messages.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearChat,
                                    modifier = Modifier.testTag("clear_chat_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear chat",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LevelSelectorRow(
                        selectedLevel = currentLevel,
                        onSelectLevel = onSelectLevel
                    )

                    activeSession?.takeIf { isInterviewMode }?.let { session ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WorkOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = session.interviewCategory ?: "Interview Practice",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Text(
                                            text = "1 question at a time • $userAnswerCount answer(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                                FilledTonalButton(
                                    onClick = { onFinishInterview(session.id) },
                                    enabled = userAnswerCount > 0 && !isGeneratingReport && !isSending,
                                    modifier = Modifier.testTag("chat_finish_interview_button")
                                ) {
                                    if (isGeneratingReport) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.AssignmentTurnedIn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isGeneratingReport) "Scoring..." else "Finish & Report")
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (messagesState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    is UiState.Error -> {
                        Text(
                            text = messagesState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(20.dp)
                        )
                    }

                    is UiState.Success -> {
                        if (messages.isEmpty()) {
                            ChatEmptyState(
                                level = currentLevel,
                                onPickStarter = { starter ->
                                    onSendMessage(starter)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    if (msg.role == "user") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(
                                                    topStart = 18.dp,
                                                    topEnd = 6.dp,
                                                    bottomStart = 18.dp,
                                                    bottomEnd = 18.dp
                                                ),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.widthIn(max = 520.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        SmartCorrectionCard(
                                            message = msg,
                                            onSpeakText = onSpeakText,
                                            onSaveSuggestedWord = onSaveVocabularyWord,
                                            isInterviewMode = isInterviewMode
                                        )
                                    }
                                }

                                if (isSending) {
                                    item {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "FluentMate AI is checking your English & replying...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Input Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                if (isInterviewMode) {
                                    "Type your interview answer in English..."
                                } else {
                                    "Type in English (e.g., \"I am go to college yesterday.\")"
                                }
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                val text = inputText.trim()
                                if (text.isNotBlank() && !isSending) {
                                    inputText = ""
                                    onSendMessage(text)
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    event.key == Key.Enter &&
                                    !event.isShiftPressed
                                ) {
                                    val text = inputText.trim()
                                    if (text.isNotBlank() && !isSending) {
                                        inputText = ""
                                        onSendMessage(text)
                                    }
                                    true
                                } else {
                                    false
                                }
                            }
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledIconButton(
                        onClick = {
                            val text = inputText.trim()
                            if (text.isNotBlank() && !isSending) {
                                inputText = ""
                                onSendMessage(text)
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("chat_send_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(onDismissRequest = { showHistorySheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat History",
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(
                        onClick = {
                            showHistorySheet = false
                            onNewChat()
                        }
                    ) {
                        Text("+ New Chat")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (sessions.isEmpty()) {
                    Text(
                        text = "No conversations saved yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sessions, key = { it.id }) { session ->
                            val isSelected = session.id == selectedSessionId
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                    }
                                ),
                                border = if (isSelected) {
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                } else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSession(session.id)
                                        showHistorySheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${session.mode} • ${session.englishLevel} • ${session.messageCount} msgs",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (session.lastMessagePreview.isNotBlank()) {
                                            Text(
                                                text = session.lastMessagePreview,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    IconButton(onClick = { onDeleteSession(session.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete chat session",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(
    level: EnglishLevel,
    onPickStarter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val starters = when (level) {
        EnglishLevel.LOW -> listOf(
            "Hi! I want to practice simple daily English.",
            "I am go to college yesterday.",
            "Can we talk about my morning routine?"
        )
        EnglishLevel.MEDIUM -> listOf(
            "Let's discuss how technology helps students learn faster.",
            "I want to improve my English tenses and fluency.",
            "Tell me about a good habit I can build this month."
        )
        EnglishLevel.HIGH -> listOf(
            "Let's practice answering 'Tell me about yourself' for a job interview.",
            "How can I make my workplace emails sound more professional?",
            "Let's debate the impact of AI on software engineering."
        )
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Chat with FluentMate AI",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Type any English sentence. I will reply naturally, correct genuine mistakes, and explain why in simple Roman Marathi.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 460.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        starters.forEach { prompt ->
            AssistChip(
                onClick = { onPickStarter(prompt) },
                label = { Text(prompt) },
                modifier = Modifier.padding(vertical = 3.dp)
            )
        }
    }
}
