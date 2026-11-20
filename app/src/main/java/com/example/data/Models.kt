package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class EnglishLevel(
    val label: String,
    val badge: String,
    val description: String
) {
    LOW(
        label = "Beginner",
        badge = "LOW",
        description = "Simple English, short sentences, basic daily conversations, easy vocabulary"
    ),
    MEDIUM(
        label = "Intermediate",
        badge = "MEDIUM",
        description = "Natural conversations, richer vocabulary, longer sentences, grammar polish"
    ),
    HIGH(
        label = "Advanced",
        badge = "HIGH",
        description = "Professional English, advanced vocabulary, complex discussions, interview prep"
    );

    companion object {
        fun fromRaw(raw: String?): EnglishLevel =
            entries.firstOrNull { it.name == raw } ?: LOW
    }
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: DARK
    }
}

enum class MarathiScript(val label: String, val description: String) {
    ROMAN("Roman Marathi", "Marathi written in simple English letters (Recommended)"),
    ROMAN_AND_DEVANAGARI("Roman + Marathi", "Simple Roman Marathi with Devanagari context");

    companion object {
        fun fromRaw(raw: String?): MarathiScript =
            entries.firstOrNull { it.name == raw } ?: ROMAN
    }
}

enum class SessionMode {
    CHAT,
    VOICE,
    TOPIC,
    INTERVIEW;

    companion object {
        fun fromRaw(raw: String?): SessionMode =
            entries.firstOrNull { it.name == raw } ?: CHAT
    }
}

enum class DailySessionType(val label: String, val shortLabel: String) {
    DAILY_CONVERSATION("Daily Conversation", "Daily"),
    INTERVIEW_PRACTICE("Interview Practice", "Interview"),
    GENERAL_CONVERSATION("General Conversation", "General"),
    REVISION("Revision and Free Conversation", "Revision");

    companion object {
        fun fromRaw(raw: String?): DailySessionType =
            entries.firstOrNull { it.name == raw } ?: DAILY_CONVERSATION
    }
}

enum class InterviewCategory(val title: String, val subtitle: String) {
    HR("HR Interview", "Strengths, teamwork, conflict resolution, and culture fit"),
    SELF_INTRODUCTION("Self Introduction", "Confident personal pitch, background, and career goals"),
    TECHNICAL("Technical Interview", "Problem solving, system thinking, and project walkthroughs"),
    COLLEGE_PLACEMENT("College Placement Interview", "Campus hiring questions, academics, and fresher readiness"),
    INTERNSHIP("Internship Interview", "Learning mindset, college projects, and availability"),
    AI_ML_JOB("AI/ML Job Interview", "Machine learning concepts, LLMs, evaluation, and real projects"),
    COMMUNICATION_SKILLS("Communication Skills Interview", "Clarity, active listening, persuasion, and fluency");

    companion object {
        fun fromTitle(raw: String?): InterviewCategory =
            entries.firstOrNull { it.title.equals(raw, ignoreCase = true) || it.name == raw }
                ?: HR
    }
}

data class WeeklyDayScheduleItem(
    val dayShort: String,
    val dayFull: String,
    val sessionType: DailySessionType,
    val isToday: Boolean,
    val isCompleted: Boolean
)

enum class VocabSource {
    DAILY,
    CHAT,
    MANUAL;

    companion object {
        fun fromRaw(raw: String?): VocabSource =
            entries.firstOrNull { it.name == raw } ?: MANUAL
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Learner",
    val email: String = "",
    val englishLevel: String = EnglishLevel.LOW.name,
    val themeMode: String = ThemeMode.DARK.name,
    val marathiScript: String = MarathiScript.ROMAN.name,
    val autoSpeakAiReplies: Boolean = true,
    val speechRate: Double = 1.0,
    val weeklyInterviewFrequency: Long = 2L,
    val totalConversations: Long = 0L,
    val totalMessagesSent: Long = 0L,
    val grammarMistakesDetected: Long = 0L,
    val topicsCompletedCount: Long = 0L,
    val vocabularyLearnedCount: Long = 0L,
    val totalPracticeSeconds: Long = 0L,
    val currentStreakDays: Long = 0L,
    val lastPracticeDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Chat",
    val topicId: String? = null,
    val topicTitle: String? = null,
    val interviewCategory: String? = null,
    val englishLevel: String = EnglishLevel.LOW.name,
    val mode: String = SessionMode.CHAT.name,
    val messageCount: Long = 0L,
    val mistakesCount: Long = 0L,
    val lastMessagePreview: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val role: String = "user",
    val text: String = "",
    val englishLevel: String = EnglishLevel.LOW.name,
    val hasMistake: Boolean = false,
    val originalSentence: String? = null,
    val correctedSentence: String? = null,
    val marathiExplanation: String? = null,
    val naturalAlternative: String? = null,
    val mistakeCategory: String? = null,
    val suggestedWord: String? = null,
    val suggestedWordMarathi: String? = null,
    val suggestedWordExample: String? = null,
    val suggestedWordPronunciation: String? = null,
    val suggestedWordPos: String? = null,
    val voiceMessage: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyTopicRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val title: String = "",
    val category: String = "",
    val sessionType: String = DailySessionType.DAILY_CONVERSATION.name,
    val interviewCategory: String? = null,
    val scheduleAdjustedNote: String? = null,
    val englishLevel: String = EnglishLevel.LOW.name,
    val promptSummary: String = "",
    val starterQuestion: String = "",
    val marathiHint: String = "",
    val completed: Boolean = false,
    val sessionId: String? = null,
    val turnsCompleted: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class InterviewReport(
    @DocumentId val id: String = "",
    val userId: String = "",
    val sessionId: String = "",
    val interviewCategory: String = "",
    val englishLevel: String = EnglishLevel.LOW.name,
    val overallSummary: String = "",
    val grammarImprovement: String = "",
    val vocabularyFeedback: String = "",
    val sentenceFormation: String = "",
    val answerClarity: String = "",
    val communicationFeedback: String = "",
    val suggestedBetterAnswers: String = "",
    val areasToImprove: String = "",
    val marathiCoachNote: String = "",
    val questionsAnswered: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class VocabularyWord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val word: String = "",
    val marathiMeaning: String = "",
    val exampleSentence: String = "",
    val pronunciation: String = "",
    val partOfSpeech: String = "",
    val englishLevel: String = EnglishLevel.LOW.name,
    val dateLearned: String = "",
    val learned: Boolean = false,
    val source: String = VocabSource.MANUAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyProgressStat(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val practiceSeconds: Long = 0L,
    val messagesCount: Long = 0L,
    val mistakesCount: Long = 0L,
    val wordsLearnedCount: Long = 0L,
    val topicsCompletedCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GeneratedWordDraft(
    val word: String,
    val marathiMeaning: String,
    val exampleSentence: String,
    val pronunciation: String,
    val partOfSpeech: String
)

data class TutorTurnResult(
    val hasMistake: Boolean,
    val mistakeCategory: String,
    val originalSentence: String?,
    val correctedSentence: String?,
    val marathiExplanation: String?,
    val naturalAlternative: String?,
    val assistantReply: String,
    val suggestedVocabulary: GeneratedWordDraft?
)

data class GeneratedTopicDraft(
    val title: String,
    val category: String,
    val sessionType: DailySessionType,
    val interviewCategory: String?,
    val scheduleAdjustedNote: String?,
    val promptSummary: String,
    val starterQuestion: String,
    val marathiHint: String
)

data class GeneratedInterviewReportDraft(
    val overallSummary: String,
    val grammarImprovement: String,
    val vocabularyFeedback: String,
    val sentenceFormation: String,
    val answerClarity: String,
    val communicationFeedback: String,
    val suggestedBetterAnswers: String,
    val areasToImprove: String,
    val marathiCoachNote: String
)
