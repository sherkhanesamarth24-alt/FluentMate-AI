package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.EnglishLevel
import com.example.data.GeneratedWordDraft
import com.example.data.SessionMode
import com.example.data.UiState
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LevelSelectorRow
import com.example.ui.components.SmartCorrectionCard
import com.example.ui.components.TutorAvatarOrb
import com.example.voice.SpeechRecognitionState
import com.example.voice.VoiceEngine

@Composable
fun VoicePracticeScreen(
    voiceEngine: VoiceEngine,
    currentLevel: EnglishLevel,
    autoSpeak: Boolean,
    speechRate: Float,
    activeSession: ChatSession?,
    messagesState: UiState<List<ChatMessage>>,
    isProcessingVoice: Boolean,
    isGeneratingReport: Boolean,
    onSelectLevel: (EnglishLevel) -> Unit,
    onStartNewVoiceSession: () -> Unit,
    onSendSpokenText: (String, (String) -> Unit) -> Unit,
    onFinishInterview: (String) -> Unit,
    onSaveVocabularyWord: (GeneratedWordDraft) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recognitionState by voiceEngine.recognitionState.collectAsStateWithLifecycle()
    val rmsDb by voiceEngine.rmsDb.collectAsStateWithLifecycle()
    val isSpeaking by voiceEngine.isSpeaking.collectAsStateWithLifecycle()
    val ttsError by voiceEngine.ttsError.collectAsStateWithLifecycle()
    val lastSpokenText by voiceEngine.lastSpokenText.collectAsStateWithLifecycle()

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            voiceEngine.startListening { spoken ->
                onSendSpokenText(spoken) { aiReply ->
                    if (autoSpeak) {
                        voiceEngine.speak(aiReply, speechRate)
                    }
                }
            }
        }
    }

    var manualVoiceDraft by remember { mutableStateOf("") }
    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    val listState = rememberLazyListState()
    val isInterviewMode = activeSession?.mode == SessionMode.INTERVIEW.name ||
        !activeSession?.interviewCategory.isNullOrBlank()
    val userAnswerCount = messages.count { it.role == "user" }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val isListening = recognitionState is SpeechRecognitionState.Listening ||
        recognitionState is SpeechRecognitionState.Partial

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Level selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Voice Practice Studio",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Speak in English • Real-time Marathi correction & spoken reply",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = {
                        voiceEngine.stopSpeaking()
                        voiceEngine.stopListening()
                        onStartNewVoiceSession()
                    },
                    modifier = Modifier.testTag("new_voice_session_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Session")
                }
            }

            LevelSelectorRow(selectedLevel = currentLevel, onSelectLevel = onSelectLevel)

            activeSession?.takeIf { isInterviewMode }?.let { session ->
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
                                    text = session.interviewCategory ?: "Voice Interview",
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
                            enabled = userAnswerCount > 0 && !isGeneratingReport && !isProcessingVoice,
                            modifier = Modifier.testTag("voice_finish_interview_button")
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

            // Real service/error checks
            if (!voiceEngine.isRecognitionAvailable) {
                ErrorBanner(
                    message = "Speech recognition service is not installed on this device/emulator. Voice playback (TTS) works, or you can type below to practice.",
                    onDismiss = { voiceEngine.clearRecognitionError() }
                )
            }

            val recError = recognitionState as? SpeechRecognitionState.Error
            if (recError != null) {
                ErrorBanner(
                    message = recError.message,
                    onDismiss = { voiceEngine.clearRecognitionError() }
                )
            }

            ttsError?.takeIf { it.isNotBlank() }?.let { errText ->
                ErrorBanner(
                    message = errText,
                    onDismiss = { }
                )
            }

            // Voice Orb & Controls Card
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        TutorAvatarOrb(
                            isActive = isListening || isSpeaking || isProcessingVoice,
                            rmsLevel = rmsDb,
                            modifier = Modifier.size(116.dp)
                        )
                        FilledIconButton(
                            onClick = {
                                if (isListening) {
                                    voiceEngine.stopListening()
                                } else if (!hasMicPermission) {
                                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    voiceEngine.startListening { spoken ->
                                        onSendSpokenText(spoken) { aiReply ->
                                            if (autoSpeak) {
                                                voiceEngine.speak(aiReply, speechRate)
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessingVoice,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isListening) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            ),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(64.dp)
                                .testTag("voice_mic_button")
                        ) {
                            if (isProcessingVoice) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(26.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Stop recording" else "Start speaking",
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val statusText = when {
                        isProcessingVoice -> "Analyzing your English & preparing Marathi coaching..."
                        recognitionState is SpeechRecognitionState.Partial ->
                            "\"${(recognitionState as SpeechRecognitionState.Partial).text}\""
                        isListening -> "Listening... Speak clearly in English"
                        isSpeaking -> "FluentMate AI is speaking..."
                        !hasMicPermission -> "Tap microphone to grant Microphone permission"
                        else -> "Tap the microphone and speak in English"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio Playback Controls (Play / Pause-Stop / Replay)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val lastAssistantText = messages.lastOrNull { it.role == "assistant" }?.text ?: lastSpokenText
                        FilledTonalButton(
                            onClick = {
                                if (isSpeaking) {
                                    voiceEngine.stopSpeaking()
                                } else if (!lastAssistantText.isNullOrBlank()) {
                                    voiceEngine.speak(lastAssistantText, speechRate)
                                }
                            },
                            enabled = !lastAssistantText.isNullOrBlank(),
                            modifier = Modifier.testTag("voice_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isSpeaking) "Pause AI speech" else "Play AI speech",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSpeaking) "Stop Audio" else "Play Reply")
                        }

                        Button(
                            onClick = {
                                if (!lastAssistantText.isNullOrBlank()) {
                                    voiceEngine.speak(lastAssistantText, speechRate)
                                }
                            },
                            enabled = !lastAssistantText.isNullOrBlank(),
                            colors = ButtonDefaults.filledTonalButtonColors(),
                            modifier = Modifier.testTag("voice_replay_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Replay AI speech",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Replay")
                        }
                    }
                }
            }

            // Transcript & Correction History
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Try saying aloud:",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"Hello, today I woke up at 7 AM and I am go to college.\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            if (msg.role == "user") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "You spoke: \"${msg.text}\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            } else {
                                SmartCorrectionCard(
                                    message = msg,
                                    onSpeakText = { text -> voiceEngine.speak(text, speechRate) },
                                    onSaveSuggestedWord = onSaveVocabularyWord,
                                    isInterviewMode = isInterviewMode
                                )
                            }
                        }
                    }
                }
            }

            // Fallback typed input when SpeechRecognizer isn't available or user is in a quiet room
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualVoiceDraft,
                    onValueChange = { manualVoiceDraft = it },
                    placeholder = { Text("Or type a spoken sentence to hear AI reply...") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("voice_fallback_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        val txt = manualVoiceDraft.trim()
                        if (txt.isNotBlank() && !isProcessingVoice) {
                            manualVoiceDraft = ""
                            onSendSpokenText(txt) { reply ->
                                if (autoSpeak) voiceEngine.speak(reply, speechRate)
                            }
                        }
                    },
                    enabled = manualVoiceDraft.isNotBlank() && !isProcessingVoice,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send spoken text")
                }
            }
        }
    }
}
