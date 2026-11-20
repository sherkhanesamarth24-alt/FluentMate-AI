package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FluentMateRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    fun todayDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun yesterdayDateKey(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(cal.time)
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        userDoc(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeChatSessions(userId: String): Flow<List<ChatSession>> =
        userDoc(userId).collection("chatSessions")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatSession::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeMessages(userId: String, sessionId: String): Flow<List<ChatMessage>> =
        userDoc(userId).collection("chatSessions").document(sessionId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ChatMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeDailyTopics(userId: String): Flow<List<DailyTopicRecord>> =
        userDoc(userId).collection("dailyTopics")
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DailyTopicRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeInterviewReports(userId: String): Flow<List<InterviewReport>> =
        userDoc(userId).collection("interviewReports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    InterviewReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeVocabulary(userId: String): Flow<List<VocabularyWord>> =
        userDoc(userId).collection("vocabulary")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VocabularyWord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    fun observeDailyStats(userId: String): Flow<List<DailyProgressStat>> =
        userDoc(userId).collection("dailyStats")
            .orderBy("dateKey", Query.Direction.DESCENDING)
            .limit(14)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    DailyProgressStat::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }

    suspend fun ensureUserProfile(userId: String, displayName: String, email: String): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user mismatch." }
            val docRef = userDoc(userId)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val safeName = displayName.trim().ifBlank { "English Learner" }.take(100)
                val safeEmail = email.trim().take(160)
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "englishLevel" to EnglishLevel.LOW.name,
                    "themeMode" to ThemeMode.DARK.name,
                    "marathiScript" to MarathiScript.ROMAN.name,
                    "autoSpeakAiReplies" to true,
                    "speechRate" to 1.0,
                    "weeklyInterviewFrequency" to 2L,
                    "totalConversations" to 0L,
                    "totalMessagesSent" to 0L,
                    "grammarMistakesDetected" to 0L,
                    "topicsCompletedCount" to 0L,
                    "vocabularyLearnedCount" to 0L,
                    "totalPracticeSeconds" to 0L,
                    "currentStreakDays" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSettings(
        userId: String,
        englishLevel: EnglishLevel? = null,
        themeMode: ThemeMode? = null,
        marathiScript: MarathiScript? = null,
        autoSpeakAiReplies: Boolean? = null,
        speechRate: Double? = null,
        weeklyInterviewFrequency: Long? = null
    ): Result<Unit> {
        val path = "users/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            englishLevel?.let { updates["englishLevel"] = it.name }
            themeMode?.let { updates["themeMode"] = it.name }
            marathiScript?.let { updates["marathiScript"] = it.name }
            autoSpeakAiReplies?.let { updates["autoSpeakAiReplies"] = it }
            speechRate?.let { updates["speechRate"] = it.coerceIn(0.5, 1.5) }
            weeklyInterviewFrequency?.let { updates["weeklyInterviewFrequency"] = it.coerceIn(1L, 2L) }
            userDoc(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createChatSession(
        userId: String,
        title: String,
        level: EnglishLevel,
        mode: SessionMode,
        topicId: String? = null,
        topicTitle: String? = null,
        interviewCategory: String? = null,
        initialAssistantGreeting: String? = null
    ): Result<String> {
        var path = "users/$userId/chatSessions"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val sessionRef = userDoc(userId).collection("chatSessions").document()
            path = sessionRef.path
            val safeTitle = title.trim().ifBlank { "English Practice" }.take(120)
            val preview = (initialAssistantGreeting ?: "Started a new ${mode.name.lowercase()} session").take(240)

            val sessionPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "title" to safeTitle,
                "englishLevel" to level.name,
                "mode" to mode.name,
                "messageCount" to if (initialAssistantGreeting != null) 1L else 0L,
                "mistakesCount" to 0L,
                "lastMessagePreview" to preview,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!topicId.isNullOrBlank()) sessionPayload["topicId"] = topicId.take(128)
            if (!topicTitle.isNullOrBlank()) sessionPayload["topicTitle"] = topicTitle.take(120)
            if (!interviewCategory.isNullOrBlank()) sessionPayload["interviewCategory"] = interviewCategory.take(80)

            val batch = db.batch()
            batch.set(sessionRef, sessionPayload)

            if (!initialAssistantGreeting.isNullOrBlank()) {
                val msgRef = sessionRef.collection("messages").document()
                batch.set(
                    msgRef,
                    mapOf(
                        "userId" to uid,
                        "sessionId" to sessionRef.id,
                        "role" to "assistant",
                        "text" to initialAssistantGreeting.take(4000),
                        "englishLevel" to level.name,
                        "hasMistake" to false,
                        "voiceMessage" to (mode == SessionMode.VOICE),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.update(
                userDoc(userId),
                mapOf(
                    "totalConversations" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(sessionRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTutorTurn(
        userId: String,
        sessionId: String,
        userText: String,
        tutorResult: TutorTurnResult,
        level: EnglishLevel,
        isVoiceMessage: Boolean,
        currentProfile: UserProfile?
    ): Result<Unit> {
        val sessionPath = "users/$userId/chatSessions/$sessionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val sessionRef = userDoc(userId).collection("chatSessions").document(sessionId)
            val userMsgRef = sessionRef.collection("messages").document()
            val aiMsgRef = sessionRef.collection("messages").document()

            val userMsgPayload = mapOf(
                "userId" to uid,
                "sessionId" to sessionId,
                "role" to "user",
                "text" to userText.trim().take(4000),
                "englishLevel" to level.name,
                "hasMistake" to false,
                "voiceMessage" to isVoiceMessage,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val aiMsgPayload = mutableMapOf<String, Any>(
                "userId" to uid,
                "sessionId" to sessionId,
                "role" to "assistant",
                "text" to tutorResult.assistantReply.trim().take(4000),
                "englishLevel" to level.name,
                "hasMistake" to tutorResult.hasMistake,
                "mistakeCategory" to tutorResult.mistakeCategory.take(80),
                "voiceMessage" to isVoiceMessage,
                "createdAt" to FieldValue.serverTimestamp()
            )
            tutorResult.originalSentence?.let { aiMsgPayload["originalSentence"] = it.take(1000) }
            tutorResult.correctedSentence?.let { aiMsgPayload["correctedSentence"] = it.take(1000) }
            tutorResult.marathiExplanation?.let { aiMsgPayload["marathiExplanation"] = it.take(1500) }
            tutorResult.naturalAlternative?.let { aiMsgPayload["naturalAlternative"] = it.take(1000) }
            tutorResult.suggestedVocabulary?.let { vocab ->
                aiMsgPayload["suggestedWord"] = vocab.word.take(80)
                aiMsgPayload["suggestedWordMarathi"] = vocab.marathiMeaning.take(200)
                aiMsgPayload["suggestedWordExample"] = vocab.exampleSentence.take(400)
                aiMsgPayload["suggestedWordPronunciation"] = vocab.pronunciation.take(120)
                aiMsgPayload["suggestedWordPos"] = vocab.partOfSpeech.take(40)
            }

            val mistakeDelta = if (tutorResult.hasMistake) 1L else 0L
            val today = todayDateKey()
            val yesterday = yesterdayDateKey()

            val newStreak = when (currentProfile?.lastPracticeDate) {
                today -> maxOf(1L, currentProfile.currentStreakDays)
                yesterday -> currentProfile.currentStreakDays + 1L
                else -> 1L
            }

            val statRef = userDoc(userId).collection("dailyStats").document(today)
            val statSnap = statRef.get().await()

            val batch = db.batch()
            batch.set(userMsgRef, userMsgPayload)
            batch.set(aiMsgRef, aiMsgPayload)

            val sessionSnap = sessionRef.get().await()
            val prevMsgCount = sessionSnap.getLong("messageCount") ?: 0L
            val sessionMode = sessionSnap.getString("mode") ?: SessionMode.CHAT.name
            val existingTitle = sessionSnap.getString("title") ?: "English Practice"
            val sessionTitleUpdate = if (prevMsgCount <= 1L && sessionMode != SessionMode.INTERVIEW.name) {
                userText.trim().take(42).ifBlank { existingTitle }
            } else {
                existingTitle
            }

            batch.update(
                sessionRef,
                mapOf(
                    "title" to sessionTitleUpdate,
                    "englishLevel" to level.name,
                    "messageCount" to FieldValue.increment(2L),
                    "mistakesCount" to FieldValue.increment(mistakeDelta),
                    "lastMessagePreview" to tutorResult.assistantReply.trim().take(240),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.update(
                userDoc(userId),
                mapOf(
                    "totalMessagesSent" to FieldValue.increment(1L),
                    "grammarMistakesDetected" to FieldValue.increment(mistakeDelta),
                    "currentStreakDays" to newStreak,
                    "lastPracticeDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (statSnap.exists()) {
                batch.update(
                    statRef,
                    mapOf(
                        "messagesCount" to FieldValue.increment(1L),
                        "mistakesCount" to FieldValue.increment(mistakeDelta),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    statRef,
                    mapOf(
                        "userId" to uid,
                        "dateKey" to today,
                        "practiceSeconds" to 0L,
                        "messagesCount" to 1L,
                        "mistakesCount" to mistakeDelta,
                        "wordsLearnedCount" to 0L,
                        "topicsCompletedCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, sessionPath)
            Result.failure(e)
        }
    }

    suspend fun clearSessionMessages(userId: String, sessionId: String): Result<Unit> {
        val path = "users/$userId/chatSessions/$sessionId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val sessionRef = userDoc(userId).collection("chatSessions").document(sessionId)
            val messages = sessionRef.collection("messages").get().await()
            val batch = db.batch()
            for (doc in messages.documents) {
                batch.delete(doc.reference)
            }
            batch.update(
                sessionRef,
                mapOf(
                    "messageCount" to 0L,
                    "mistakesCount" to 0L,
                    "lastMessagePreview" to "Chat cleared",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteSession(userId: String, sessionId: String): Result<Unit> {
        val path = "users/$userId/chatSessions/$sessionId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val sessionRef = userDoc(userId).collection("chatSessions").document(sessionId)
            val messages = sessionRef.collection("messages").get().await()
            val batch = db.batch()
            for (doc in messages.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(sessionRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveDailyTopic(
        userId: String,
        dateKey: String,
        draft: GeneratedTopicDraft,
        level: EnglishLevel
    ): Result<Unit> {
        val path = "users/$userId/dailyTopics/$dateKey"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(userId).collection("dailyTopics").document(dateKey)
            val existing = ref.get().await()
            val payload = mutableMapOf<String, Any>(
                "title" to draft.title.take(120),
                "category" to draft.category.take(60),
                "sessionType" to draft.sessionType.name,
                "englishLevel" to level.name,
                "promptSummary" to draft.promptSummary.take(600),
                "starterQuestion" to draft.starterQuestion.take(500),
                "marathiHint" to draft.marathiHint.take(500),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            draft.interviewCategory?.takeIf { it.isNotBlank() }?.let {
                payload["interviewCategory"] = it.take(80)
            }
            draft.scheduleAdjustedNote?.takeIf { it.isNotBlank() }?.let {
                payload["scheduleAdjustedNote"] = it.take(300)
            }

            if (existing.exists()) {
                ref.update(payload).await()
            } else {
                payload["userId"] = uid
                payload["dateKey"] = dateKey
                payload["completed"] = false
                payload["turnsCompleted"] = 0L
                payload["createdAt"] = FieldValue.serverTimestamp()
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTopicProgress(
        userId: String,
        dateKey: String,
        sessionId: String?,
        markCompleted: Boolean,
        turnsIncrement: Long = 0L
    ): Result<Unit> {
        val path = "users/$userId/dailyTopics/$dateKey"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(userId).collection("dailyTopics").document(dateKey)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.success(Unit)

            val wasCompleted = snap.getBoolean("completed") ?: false
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!sessionId.isNullOrBlank()) updates["sessionId"] = sessionId
            if (turnsIncrement > 0L) updates["turnsCompleted"] = FieldValue.increment(turnsIncrement)
            if (markCompleted) updates["completed"] = true

            val batch = db.batch()
            batch.update(ref, updates)

            if (markCompleted && !wasCompleted) {
                batch.update(
                    userDoc(userId),
                    mapOf(
                        "topicsCompletedCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val today = todayDateKey()
                val statRef = userDoc(userId).collection("dailyStats").document(today)
                val statSnap = statRef.get().await()
                if (statSnap.exists()) {
                    batch.update(
                        statRef,
                        mapOf(
                            "topicsCompletedCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        statRef,
                        mapOf(
                            "userId" to uid,
                            "dateKey" to today,
                            "practiceSeconds" to 0L,
                            "messagesCount" to 0L,
                            "mistakesCount" to 0L,
                            "wordsLearnedCount" to 0L,
                            "topicsCompletedCount" to 1L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveInterviewReport(
        userId: String,
        sessionId: String,
        interviewCategory: String,
        level: EnglishLevel,
        draft: GeneratedInterviewReportDraft,
        questionsAnswered: Long
    ): Result<InterviewReport> {
        var path = "users/$userId/interviewReports"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(userId).collection("interviewReports").document()
            path = docRef.path
            val safeCategory = interviewCategory.trim().ifBlank { "HR Interview" }.take(80)
            val payload = mapOf(
                "userId" to uid,
                "sessionId" to sessionId.take(128),
                "interviewCategory" to safeCategory,
                "englishLevel" to level.name,
                "overallSummary" to draft.overallSummary.trim().ifBlank { "Completed interview practice." }.take(1000),
                "grammarImprovement" to draft.grammarImprovement.trim().ifBlank { "Keep practicing verb tenses." }.take(1500),
                "vocabularyFeedback" to draft.vocabularyFeedback.trim().ifBlank { "Good word choices." }.take(1500),
                "sentenceFormation" to draft.sentenceFormation.trim().ifBlank { "Clear sentence structure." }.take(1500),
                "answerClarity" to draft.answerClarity.trim().ifBlank { "Clear answers." }.take(1500),
                "communicationFeedback" to draft.communicationFeedback.trim().ifBlank { "Confident tone." }.take(1500),
                "suggestedBetterAnswers" to draft.suggestedBetterAnswers.trim().ifBlank { "Use structured examples." }.take(2500),
                "areasToImprove" to draft.areasToImprove.trim().ifBlank { "Practice daily." }.take(1500),
                "marathiCoachNote" to draft.marathiCoachNote.trim().ifBlank { "Chhan prayatna!" }.take(1500),
                "questionsAnswered" to questionsAnswered.coerceAtLeast(0L),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            val saved = docRef.get().await().toObject(
                InterviewReport::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ) ?: InterviewReport(
                id = docRef.id,
                userId = uid,
                sessionId = sessionId,
                interviewCategory = safeCategory,
                englishLevel = level.name,
                overallSummary = draft.overallSummary,
                grammarImprovement = draft.grammarImprovement,
                vocabularyFeedback = draft.vocabularyFeedback,
                sentenceFormation = draft.sentenceFormation,
                answerClarity = draft.answerClarity,
                communicationFeedback = draft.communicationFeedback,
                suggestedBetterAnswers = draft.suggestedBetterAnswers,
                areasToImprove = draft.areasToImprove,
                marathiCoachNote = draft.marathiCoachNote,
                questionsAnswered = questionsAnswered
            )
            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun addVocabularyWord(
        userId: String,
        draft: GeneratedWordDraft,
        level: EnglishLevel,
        source: VocabSource,
        markLearnedImmediately: Boolean = false
    ): Result<Unit> {
        var path = "users/$userId/vocabulary"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val wordText = draft.word.trim().take(80)
            require(wordText.isNotBlank()) { "English word cannot be empty." }
            val marathi = draft.marathiMeaning.trim().ifBlank { "Meaning saved" }.take(200)
            val example = draft.exampleSentence.trim().ifBlank { "Example: $wordText" }.take(400)
            val today = todayDateKey()

            val docRef = userDoc(userId).collection("vocabulary").document()
            path = docRef.path
            val batch = db.batch()
            batch.set(
                docRef,
                mapOf(
                    "userId" to uid,
                    "word" to wordText,
                    "marathiMeaning" to marathi,
                    "exampleSentence" to example,
                    "pronunciation" to draft.pronunciation.trim().take(120),
                    "partOfSpeech" to draft.partOfSpeech.trim().take(40),
                    "englishLevel" to level.name,
                    "dateLearned" to today,
                    "learned" to markLearnedImmediately,
                    "source" to source.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (markLearnedImmediately) {
                batch.update(
                    userDoc(userId),
                    mapOf(
                        "vocabularyLearnedCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val statRef = userDoc(userId).collection("dailyStats").document(today)
                val statSnap = statRef.get().await()
                if (statSnap.exists()) {
                    batch.update(
                        statRef,
                        mapOf(
                            "wordsLearnedCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        statRef,
                        mapOf(
                            "userId" to uid,
                            "dateKey" to today,
                            "practiceSeconds" to 0L,
                            "messagesCount" to 0L,
                            "mistakesCount" to 0L,
                            "wordsLearnedCount" to 1L,
                            "topicsCompletedCount" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleWordLearned(
        userId: String,
        wordId: String,
        newLearned: Boolean
    ): Result<Unit> {
        val path = "users/$userId/vocabulary/$wordId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val ref = userDoc(userId).collection("vocabulary").document(wordId)
            val snap = ref.get().await()
            val prevLearned = snap.getBoolean("learned") ?: false
            if (prevLearned == newLearned) return Result.success(Unit)

            val delta = if (newLearned) 1L else -1L
            val today = todayDateKey()
            val batch = db.batch()
            batch.update(
                ref,
                mapOf(
                    "learned" to newLearned,
                    "dateLearned" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                userDoc(userId),
                mapOf(
                    "vocabularyLearnedCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (newLearned) {
                val statRef = userDoc(userId).collection("dailyStats").document(today)
                val statSnap = statRef.get().await()
                if (statSnap.exists()) {
                    batch.update(
                        statRef,
                        mapOf(
                            "wordsLearnedCount" to FieldValue.increment(1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.set(
                        statRef,
                        mapOf(
                            "userId" to uid,
                            "dateKey" to today,
                            "practiceSeconds" to 0L,
                            "messagesCount" to 0L,
                            "mistakesCount" to 0L,
                            "wordsLearnedCount" to 1L,
                            "topicsCompletedCount" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVocabularyWord(userId: String, word: VocabularyWord): Result<Unit> {
        val path = "users/$userId/vocabulary/${word.id}"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val batch = db.batch()
            batch.delete(userDoc(userId).collection("vocabulary").document(word.id))
            if (word.learned) {
                batch.update(
                    userDoc(userId),
                    mapOf(
                        "vocabularyLearnedCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordPracticeSeconds(userId: String, seconds: Long): Result<Unit> {
        if (seconds <= 0L) return Result.success(Unit)
        val today = todayDateKey()
        val path = "users/$userId/dailyStats/$today"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val statRef = userDoc(userId).collection("dailyStats").document(today)
            val statSnap = statRef.get().await()

            val batch = db.batch()
            batch.update(
                userDoc(userId),
                mapOf(
                    "totalPracticeSeconds" to FieldValue.increment(seconds),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (statSnap.exists()) {
                batch.update(
                    statRef,
                    mapOf(
                        "practiceSeconds" to FieldValue.increment(seconds),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    statRef,
                    mapOf(
                        "userId" to uid,
                        "dateKey" to today,
                        "practiceSeconds" to seconds,
                        "messagesCount" to 0L,
                        "mistakesCount" to 0L,
                        "wordsLearnedCount" to 0L,
                        "topicsCompletedCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
