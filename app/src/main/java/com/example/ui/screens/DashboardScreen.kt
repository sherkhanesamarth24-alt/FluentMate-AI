package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DailyProgressStat
import com.example.data.DailyTopicRecord
import com.example.data.EnglishLevel
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VocabularyWord
import com.example.ui.components.AccuracyRingChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LevelSelectorRow
import com.example.ui.components.StatKpiCard
import com.example.ui.components.WeeklyPracticeBarChart

@Composable
fun DashboardScreen(
    profileState: UiState<UserProfile>,
    topicsState: UiState<List<DailyTopicRecord>>,
    vocabularyState: UiState<List<VocabularyWord>>,
    statsState: UiState<List<DailyProgressStat>>,
    todayDateKey: String,
    isGeneratingTopic: Boolean,
    onSelectLevel: (EnglishLevel) -> Unit,
    onGenerateTodayTopic: () -> Unit,
    onStartTopicChat: (DailyTopicRecord) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToVoice: () -> Unit,
    onNavigateToVocabulary: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (profileState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        EmptyStateCard(
                            icon = Icons.Default.AutoAwesome,
                            title = "Could not load dashboard",
                            subtitle = profileState.message
                        )
                    }

                    is UiState.Success -> {
                        val profile = profileState.data
                        val level = EnglishLevel.fromRaw(profile.englishLevel)
                        val topics = (topicsState as? UiState.Success)?.data.orEmpty()
                        val todayTopic = topics.firstOrNull { it.dateKey == todayDateKey }
                        val words = (vocabularyState as? UiState.Success)?.data.orEmpty()
                        val stats = (statsState as? UiState.Success)?.data.orEmpty()

                        // Hero Greeting & Level Switcher
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    )
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Namaste, ${profile.displayName.substringBefore(" ")}!",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Ready for today's English & Marathi coaching?",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = "Streak",
                                                    tint = MaterialTheme.colorScheme.tertiary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${profile.currentStreakDays}d",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = "English Level • ${level.description}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LevelSelectorRow(
                                        selectedLevel = level,
                                        onSelectLevel = onSelectLevel
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Button(
                                            onClick = onNavigateToChat,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("dashboard_open_chat_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("AI Chat")
                                        }
                                        FilledTonalButton(
                                            onClick = onNavigateToVoice,
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("dashboard_open_voice_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Voice Practice")
                                        }
                                    }
                                }
                            }
                        }

                        // Today's Daily Topic Card
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Topic,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Today's Topic ($todayDateKey)",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (todayTopic?.completed == true) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Completed",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Completed",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (todayTopic == null) {
                                    Text(
                                        text = "Generate today's personalized conversation topic adapted to your ${level.label} level.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = onGenerateTodayTopic,
                                        enabled = !isGeneratingTopic,
                                        modifier = Modifier.testTag("dashboard_generate_topic_btn")
                                    ) {
                                        if (isGeneratingTopic) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generating Topic...")
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Generate Today's Topic")
                                        }
                                    }
                                } else {
                                    val sessionTypeLabel = com.example.data.DailySessionType.fromRaw(todayTopic.sessionType).shortLabel
                                    Text(
                                        text = "$sessionTypeLabel • ${todayTopic.category} • ${todayTopic.englishLevel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = todayTopic.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = todayTopic.starterQuestion,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { onStartTopicChat(todayTopic) },
                                            modifier = Modifier.testTag("dashboard_start_topic_btn")
                                        ) {
                                            Text(if (todayTopic.sessionId.isNullOrBlank()) "Practice Today's Topic" else "Continue Topic")
                                        }
                                        OutlinedButton(
                                            onClick = onNavigateToVocabulary
                                        ) {
                                            Text("Daily Vocab")
                                        }
                                    }
                                }
                            }
                        }

                        // Real Activity KPI Grid (2x3)
                        val practiceMins = (profile.totalPracticeSeconds / 60L)
                        val learnedCount = words.count { it.learned }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatKpiCard(
                                title = "Conversations",
                                value = "${profile.totalConversations}",
                                subtitle = "${profile.totalMessagesSent} msgs analyzed",
                                icon = Icons.AutoMirrored.Filled.Chat,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatKpiCard(
                                title = "Topics Done",
                                value = "${profile.topicsCompletedCount}",
                                subtitle = "${topics.size} total unlocked",
                                icon = Icons.Default.Topic,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatKpiCard(
                                title = "Practice Time",
                                value = "${practiceMins}m",
                                subtitle = "${profile.currentStreakDays} day streak",
                                icon = Icons.Default.Schedule,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            StatKpiCard(
                                title = "Grammar Tips",
                                value = "${profile.grammarMistakesDetected}",
                                subtitle = "Real mistakes fixed",
                                icon = Icons.Default.Spellcheck,
                                accentColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatKpiCard(
                                title = "Vocabulary",
                                value = "$learnedCount",
                                subtitle = "${words.size} saved words",
                                icon = Icons.Default.Book,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            StatKpiCard(
                                title = "Current Level",
                                value = level.badge,
                                subtitle = level.label,
                                icon = Icons.Default.AutoAwesome,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AccuracyRingChart(
                            totalMessages = profile.totalMessagesSent,
                            totalMistakes = profile.grammarMistakesDetected
                        )

                        WeeklyPracticeBarChart(stats = stats)
                    }
                }
            }
        }
    }
}
