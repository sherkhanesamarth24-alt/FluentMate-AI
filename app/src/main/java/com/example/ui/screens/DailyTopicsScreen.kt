package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DailySessionType
import com.example.data.DailyTopicRecord
import com.example.data.EnglishLevel
import com.example.data.InterviewCategory
import com.example.data.InterviewReport
import com.example.data.UiState
import com.example.data.WeeklyDayScheduleItem
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.InterviewReportDialog

private val TOPIC_CATEGORIES = listOf(
    "All",
    "Daily Life",
    "Education",
    "Technology",
    "AI and Machine Learning",
    "Career",
    "Business",
    "Travel",
    "Friends and Family",
    "General Knowledge",
    "Real-life Situations",
    "Job Interviews"
)

@Composable
fun DailyTopicsScreen(
    currentLevel: EnglishLevel,
    topicsState: UiState<List<DailyTopicRecord>>,
    reportsState: UiState<List<InterviewReport>>,
    weeklySchedule: List<WeeklyDayScheduleItem>,
    interviewsThisWeek: Int,
    weeklyInterviewFrequency: Int,
    todayDateKey: String,
    isGeneratingTopic: Boolean,
    onGenerateTopic: (preferredCategory: String?, preferredInterviewCategory: InterviewCategory?) -> Unit,
    onStartTopicInChat: (DailyTopicRecord) -> Unit,
    onStartTopicInVoice: (DailyTopicRecord) -> Unit,
    onStartInterview: (category: InterviewCategory, inVoiceMode: Boolean, fromTopicDateKey: String?) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedInterviewCategory by remember { mutableStateOf(InterviewCategory.HR) }
    var inspectedReport by remember { mutableStateOf<InterviewReport?>(null) }

    val topics = (topicsState as? UiState.Success)?.data.orEmpty()
    val reports = (reportsState as? UiState.Success)?.data.orEmpty()
    val todayTopic = topics.firstOrNull { it.dateKey == todayDateKey }
    val previousTopics = topics.filterNot { it.dateKey == todayDateKey }
        .filter { selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true) }

    val isTodayInterviewScheduled = todayTopic?.sessionType == DailySessionType.INTERVIEW_PRACTICE.name ||
        weeklySchedule.firstOrNull { it.isToday }?.sessionType == DailySessionType.INTERVIEW_PRACTICE

    val quotaAvailable = interviewsThisWeek < weeklyInterviewFrequency

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier.widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Topics & Interview Prep",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Daily conversation + 1–2x/week interview practice • ${currentLevel.badge} (${currentLevel.label})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            onGenerateTopic(
                                selectedCategory.takeIf { it != "All" },
                                if (isTodayInterviewScheduled) selectedInterviewCategory else null
                            )
                        },
                        enabled = !isGeneratingTopic,
                        modifier = Modifier.testTag("refresh_daily_topic_button")
                    ) {
                        if (isGeneratingTopic) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (todayTopic == null) "Generate" else "New Topic")
                    }
                }

                // Weekly Schedule Card (Mon - Sun)
                WeeklyScheduleCard(
                    schedule = weeklySchedule,
                    interviewsThisWeek = interviewsThisWeek,
                    weeklyInterviewFrequency = weeklyInterviewFrequency
                )

                // Category Chips
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(TOPIC_CATEGORIES) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                // Today's Topic Card
                if (todayTopic == null) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = if (isTodayInterviewScheduled) Icons.Default.WorkOutline else Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isTodayInterviewScheduled) {
                                    "Today is Interview Practice Day ($todayDateKey)"
                                } else {
                                    "Ready for Today's Topic ($todayDateKey)?"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isTodayInterviewScheduled) {
                                    "FluentMate AI will run a realistic ${selectedInterviewCategory.title} adapted to your ${currentLevel.label} level."
                                } else {
                                    "FluentMate AI will create a fresh conversation topic avoiding your recently completed topics."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onGenerateTopic(
                                        selectedCategory.takeIf { it != "All" },
                                        if (isTodayInterviewScheduled) selectedInterviewCategory else null
                                    )
                                },
                                enabled = !isGeneratingTopic,
                                modifier = Modifier.testTag("generate_today_topic_button")
                            ) {
                                Text(if (isGeneratingTopic) "Generating..." else "Generate Today's Session")
                            }
                        }
                    }
                } else {
                    TopicCard(
                        topic = todayTopic,
                        isToday = true,
                        onStartChat = { onStartTopicInChat(todayTopic) },
                        onStartVoice = { onStartTopicInVoice(todayTopic) },
                        onMarkCompleted = { onMarkCompleted(todayTopic.dateKey) }
                    )
                }

                // Weekly Interview Practice Module (7 Categories)
                InterviewPracticeSection(
                    selectedCategory = selectedInterviewCategory,
                    onSelectCategory = { selectedInterviewCategory = it },
                    interviewsThisWeek = interviewsThisWeek,
                    weeklyInterviewFrequency = weeklyInterviewFrequency,
                    todayTopic = todayTopic,
                    quotaAvailable = quotaAvailable,
                    onStartChatInterview = {
                        val linkedDateKey = todayTopic?.takeIf {
                            it.sessionType == DailySessionType.INTERVIEW_PRACTICE.name && it.sessionId.isNullOrBlank()
                        }?.dateKey
                        onStartInterview(selectedInterviewCategory, false, linkedDateKey)
                    },
                    onStartVoiceInterview = {
                        val linkedDateKey = todayTopic?.takeIf {
                            it.sessionType == DailySessionType.INTERVIEW_PRACTICE.name && it.sessionId.isNullOrBlank()
                        }?.dateKey
                        onStartInterview(selectedInterviewCategory, true, linkedDateKey)
                    }
                )

                // Past Interview Performance Reports
                if (reports.isNotEmpty()) {
                    Text(
                        text = "Interview Performance Reports (${reports.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        if (reports.isNotEmpty()) {
            items(reports, key = { "report_${it.id}" }) { report ->
                Column(modifier = Modifier.widthIn(max = 860.dp)) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { inspectedReport = report }
                            .testTag("interview_report_card_${report.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Assignment,
                                contentDescription = "Interview Report",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = report.interviewCategory,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${report.englishLevel} • ${report.questionsAnswered} answers • Tap to view full feedback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = report.overallSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.widthIn(max = 860.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Previous Topics (${previousTopics.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp)
                )

                if (previousTopics.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.Topic,
                        title = "No previous topics yet",
                        subtitle = "Topics you generate and complete each day appear here so you can revisit them anytime."
                    )
                }
            }
        }

        items(previousTopics, key = { it.dateKey }) { topic ->
            Column(modifier = Modifier.widthIn(max = 860.dp)) {
                TopicCard(
                    topic = topic,
                    isToday = false,
                    onStartChat = { onStartTopicInChat(topic) },
                    onStartVoice = { onStartTopicInVoice(topic) },
                    onMarkCompleted = { onMarkCompleted(topic.dateKey) }
                )
            }
        }
    }

    inspectedReport?.let { report ->
        InterviewReportDialog(
            report = report,
            onDismiss = { inspectedReport = null },
            onSpeakText = onSpeakText
        )
    }
}

@Composable
private fun WeeklyScheduleCard(
    schedule: List<WeeklyDayScheduleItem>,
    interviewsThisWeek: Int,
    weeklyInterviewFrequency: Int
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Weekly Learning Plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$interviewsThisWeek/$weeklyInterviewFrequency interviews this week",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(schedule, key = { it.dayShort }) { day ->
                    val isInterview = day.sessionType == DailySessionType.INTERVIEW_PRACTICE
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = when {
                            day.isToday -> MaterialTheme.colorScheme.primaryContainer
                            isInterview -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        },
                        border = if (day.isToday) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else null,
                        modifier = Modifier.width(96.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (day.isToday) "${day.dayShort} • Today" else day.dayShort,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (day.isToday) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1
                                )
                                if (day.isCompleted) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Done",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = day.sessionType.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isInterview) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InterviewPracticeSection(
    selectedCategory: InterviewCategory,
    onSelectCategory: (InterviewCategory) -> Unit,
    interviewsThisWeek: Int,
    weeklyInterviewFrequency: Int,
    todayTopic: DailyTopicRecord?,
    quotaAvailable: Boolean,
    onStartChatInterview: () -> Unit,
    onStartVoiceInterview: () -> Unit
) {
    val canLaunchFromTodayTopic = todayTopic?.sessionType == DailySessionType.INTERVIEW_PRACTICE.name &&
        todayTopic.sessionId.isNullOrBlank()
    val canStart = quotaAvailable || canLaunchFromTodayTopic

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Weekly Interview Practice",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "$interviewsThisWeek / $weeklyInterviewFrequency used",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Scheduled 1–2x/week (default Tue & Fri). Choose an interview track — AI asks one question at a time, corrects mistakes in Roman Marathi, suggests professional answers, and builds your Performance Report.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(InterviewCategory.entries, key = { it.name }) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { onSelectCategory(cat) },
                        label = { Text(cat.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ),
                        modifier = Modifier.testTag("interview_cat_${cat.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = selectedCategory.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartChatInterview,
                    enabled = canStart,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_interview_chat_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat Interview")
                }
                OutlinedButton(
                    onClick = onStartVoiceInterview,
                    enabled = canStart,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("start_interview_voice_button")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice Interview")
                }
            }

            if (!canStart) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You've reached this week's limit ($weeklyInterviewFrequency/week). Focus on Daily Conversations for the rest of the week, or adjust weekly frequency in Settings.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun TopicCard(
    topic: DailyTopicRecord,
    isToday: Boolean,
    onStartChat: () -> Unit,
    onStartVoice: () -> Unit,
    onMarkCompleted: () -> Unit
) {
    val sessionType = DailySessionType.fromRaw(topic.sessionType)
    val isInterview = sessionType == DailySessionType.INTERVIEW_PRACTICE

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isToday) 1.5.dp else 1.dp,
            color = when {
                isToday && isInterview -> MaterialTheme.colorScheme.tertiary
                isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isInterview) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = "${sessionType.shortLabel} • ${topic.category} • ${topic.englishLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isInterview) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                if (topic.completed) {
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
                } else {
                    Text(
                        text = topic.dateKey,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!topic.scheduleAdjustedNote.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Schedule Note: ${topic.scheduleAdjustedNote}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = topic.promptSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isInterview) "Interviewer Opening Question" else "AI Starter Question",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = topic.starterQuestion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (topic.marathiHint.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Marathi Hint: ${topic.marathiHint}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartChat,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat")
                }
                OutlinedButton(
                    onClick = onStartVoice,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Voice")
                }
                if (!topic.completed) {
                    OutlinedButton(onClick = onMarkCompleted) {
                        Text("Done")
                    }
                }
            }
        }
    }
}
