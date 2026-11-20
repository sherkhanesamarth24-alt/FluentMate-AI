package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.ChatSession
import com.example.data.DailyProgressStat
import com.example.data.DailyTopicRecord
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VocabularyWord
import com.example.ui.components.AccuracyRingChart
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.StatKpiCard
import com.example.ui.components.WeeklyPracticeBarChart

@Composable
fun ProgressScreen(
    profileState: UiState<UserProfile>,
    statsState: UiState<List<DailyProgressStat>>,
    sessionsState: UiState<List<ChatSession>>,
    topicsState: UiState<List<DailyTopicRecord>>,
    vocabularyState: UiState<List<VocabularyWord>>,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val stats = (statsState as? UiState.Success)?.data.orEmpty()
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val topics = (topicsState as? UiState.Success)?.data.orEmpty()
    val words = (vocabularyState as? UiState.Success)?.data.orEmpty()

    val totalMinutes = profile.totalPracticeSeconds / 60L
    val learnedWords = words.count { it.learned }
    val completedTopics = topics.count { it.completed }
    val voiceSessions = sessions.count { it.mode == "VOICE" }
    val chatSessions = sessions.count { it.mode != "VOICE" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Learning Progress",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Every metric below is calculated live from your real saved sessions, topics, and vocabulary.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatKpiCard(
                        title = "Daily Streak",
                        value = "${profile.currentStreakDays} days",
                        subtitle = profile.lastPracticeDate?.let { "Last: $it" } ?: "Start today",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        title = "Practice Time",
                        value = "${totalMinutes} min",
                        subtitle = "${profile.totalPracticeSeconds} sec total",
                        icon = Icons.Default.Schedule,
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatKpiCard(
                        title = "Sessions",
                        value = "${sessions.size}",
                        subtitle = "$chatSessions chat • $voiceSessions voice",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatKpiCard(
                        title = "Grammar Tips",
                        value = "${profile.grammarMistakesDetected}",
                        subtitle = "Out of ${profile.totalMessagesSent} msgs",
                        icon = Icons.Default.Spellcheck,
                        accentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }

                AccuracyRingChart(
                    totalMessages = profile.totalMessagesSent,
                    totalMistakes = profile.grammarMistakesDetected
                )

                WeeklyPracticeBarChart(stats = stats)

                // Topic & Vocabulary Mastery Progress Bars
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Mastery Breakdown",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Vocabulary Learned", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "$learnedWords / ${words.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (words.isEmpty()) 0f else (learnedWords.toFloat() / words.size.toFloat())
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Daily Topics Completed", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "$completedTopics / ${topics.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = {
                                    if (topics.isEmpty()) 0f else (completedTopics.toFloat() / topics.size.toFloat())
                                },
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                        }
                    }
                }

                if (stats.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Recent Daily Logs",
                                style = MaterialTheme.typography.titleMedium
                            )
                            stats.take(7).forEach { day ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = day.dateKey,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = "${day.messagesCount} msgs • ${day.mistakesCount} tips • ${day.practiceSeconds / 60}m",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    EmptyStateCard(
                        icon = Icons.Default.CheckCircle,
                        title = "Start practicing today",
                        subtitle = "Your daily logs will appear here as soon as you chat, speak, or learn words."
                    )
                }
            }
        }
    }
}
