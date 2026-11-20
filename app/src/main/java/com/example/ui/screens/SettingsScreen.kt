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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.EnglishLevel
import com.example.data.MarathiScript
import com.example.data.ThemeMode
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.LevelSelectorRow
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    profileState: UiState<UserProfile>,
    onSelectLevel: (EnglishLevel) -> Unit,
    onSelectTheme: (ThemeMode) -> Unit,
    onSelectMarathiScript: (MarathiScript) -> Unit,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onChangeSpeechRate: (Double) -> Unit,
    onChangeWeeklyInterviewFrequency: (Long) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data ?: UserProfile()
    val currentLevel = EnglishLevel.fromRaw(profile.englishLevel)
    val currentTheme = ThemeMode.fromRaw(profile.themeMode)
    val currentScript = MarathiScript.fromRaw(profile.marathiScript)
    val weeklyFreq = profile.weeklyInterviewFrequency.coerceIn(1L, 2L)

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
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Account Profile Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = profile.displayName.firstOrNull()?.uppercase() ?: "F",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = profile.email.ifBlank { "Google Account Connected" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // English Proficiency Level
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("English Proficiency Level", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentLevel.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LevelSelectorRow(
                            selectedLevel = currentLevel,
                            onSelectLevel = onSelectLevel
                        )
                    }
                }

                // Marathi Explanation Style
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Translate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Marathi Explanation Style", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentScript.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MarathiScript.entries.forEach { script ->
                                FilterChip(
                                    selected = currentScript == script,
                                    onClick = { onSelectMarathiScript(script) },
                                    label = { Text(script.label) }
                                )
                            }
                        }
                    }
                }

                // Voice & Audio Preferences
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice & Speech", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-speak AI replies", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Automatically read Gemini's reply aloud in Voice Practice",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = profile.autoSpeakAiReplies,
                                onCheckedChange = onToggleAutoSpeak,
                                modifier = Modifier.testTag("auto_speak_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        val rateLabel = ((profile.speechRate * 100.0).roundToInt() / 100.0)
                        Text(
                            text = "Tutor Speech Speed: ${rateLabel}x",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = profile.speechRate.toFloat().coerceIn(0.6f, 1.4f),
                            onValueChange = { onChangeSpeechRate(it.toDouble()) },
                            valueRange = 0.6f..1.4f,
                            steps = 7
                        )
                    }
                }

                // Weekly Interview Practice Schedule
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkOutline, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Weekly Interview Practice", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (weeklyFreq == 1L) {
                                "1 session/week (Friday) • Other days focus on daily & general conversations."
                            } else {
                                "2 sessions/week (Tuesday & Friday — Default) • Balanced with daily conversations."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = weeklyFreq == 1L,
                                onClick = { onChangeWeeklyInterviewFrequency(1L) },
                                label = { Text("1 session / week") },
                                modifier = Modifier.testTag("interview_freq_1_chip")
                            )
                            FilterChip(
                                selected = weeklyFreq == 2L,
                                onClick = { onChangeWeeklyInterviewFrequency(2L) },
                                label = { Text("2 sessions / week") },
                                modifier = Modifier.testTag("interview_freq_2_chip")
                            )
                        }
                    }
                }

                // Appearance Theme
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Appearance", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = currentTheme == mode,
                                    onClick = { onSelectTheme(mode) },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("theme_chip_${mode.name.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Sign Out
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        }
    }
}
