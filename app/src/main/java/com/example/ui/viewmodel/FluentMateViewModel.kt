package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FluentMateAiService
import com.example.data.ChatMessage
import com.example.data.ChatSession
import com.example.data.DailyProgressStat
import com.example.data.DailySessionType
import com.example.data.DailyTopicRecord
import com.example.data.EnglishLevel
import com.example.data.FluentMateRepository
import com.example.data.GeneratedWordDraft
import com.example.data.InterviewCategory
import com.example.data.InterviewReport
import com.example.data.MarathiScript
import com.example.data.SessionMode
import com.example.data.ThemeMode
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.VocabSource
import com.example.data.VocabularyWord
import com.example.data.WeeklyDayScheduleItem
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class FluentMateViewModel(
    private val repository: FluentMateRepository,
    private val aiService: FluentMateAiService,
    private val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String
) : ViewModel() {

    private val _selectedChatSessionId = MutableStateFlow<String?>(null)
    val selectedChatSessionId: StateFlow<String?> = _selectedChatSessionId.asStateFlow()

    private val _selectedVoiceSessionId = MutableStateFlow<String?>(null)
    val selectedVoiceSessionId: StateFlow<String?> = _selectedVoiceSessionId.asStateFlow()

    private val _isSendingChat = MutableStateFlow(false)
    val isSendingChat: StateFlow<Boolean> = _isSendingChat.asStateFlow()

    private val _isSendingVoice = MutableStateFlow(false)
    val isSendingVoice: StateFlow<Boolean> = _isSendingVoice.asStateFlow()

    private val _isGeneratingTopic = MutableStateFlow(false)
    val isGeneratingTopic: StateFlow<Boolean> = _isGeneratingTopic.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _latestGeneratedReport = MutableStateFlow<InterviewReport?>(null)
    val latestGeneratedReport: StateFlow<InterviewReport?> = _latestGeneratedReport.asStateFlow()

    private val _isGeneratingVocab = MutableStateFlow(false)
    val isGeneratingVocab: StateFlow<Boolean> = _isGeneratingVocab.asStateFlow()

    private val _isEnrichingWord = MutableStateFlow(false)
    val isEnrichingWord: StateFlow<Boolean> = _isEnrichingWord.asStateFlow()

    private val _dailyWordSuggestions = MutableStateFlow<List<GeneratedWordDraft>>(emptyList())
    val dailyWordSuggestions: StateFlow<List<GeneratedWordDraft>> = _dailyWordSuggestions.asStateFlow()

    private val _bannerError = MutableStateFlow<String?>(null)
    val bannerError: StateFlow<String?> = _bannerError.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val userProfileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            UiState.Success(profile ?: UserProfile(userId = currentUserId))
        }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing user profile", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val chatSessionsState: StateFlow<UiState<List<ChatSession>>> = repository.observeChatSessions(currentUserId)
        .map<List<ChatSession>, UiState<List<ChatSession>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing chat sessions", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load chat sessions"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val activeChatMessages: StateFlow<UiState<List<ChatMessage>>> = _selectedChatSessionId
        .flatMapLatest { sessionId ->
            if (sessionId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, sessionId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        Log.w(TAG, "Error observing chat messages", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load messages"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val activeVoiceMessages: StateFlow<UiState<List<ChatMessage>>> = _selectedVoiceSessionId
        .flatMapLatest { sessionId ->
            if (sessionId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(currentUserId, sessionId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = unwrapFirestoreError(error)
                        if (firestoreError == null && error is CancellationException) throw error
                        val root = firestoreError ?: error
                        Log.w(TAG, "Error observing voice messages", root)
                        emit(UiState.Error(root.localizedMessage ?: "Failed to load voice transcript"))
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    val dailyTopicsState: StateFlow<UiState<List<DailyTopicRecord>>> = repository.observeDailyTopics(currentUserId)
        .map<List<DailyTopicRecord>, UiState<List<DailyTopicRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing daily topics", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load daily topics"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val interviewReportsState: StateFlow<UiState<List<InterviewReport>>> = repository.observeInterviewReports(currentUserId)
        .map<List<InterviewReport>, UiState<List<InterviewReport>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing interview reports", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load interview reports"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val vocabularyState: StateFlow<UiState<List<VocabularyWord>>> = repository.observeVocabulary(currentUserId)
        .map<List<VocabularyWord>, UiState<List<VocabularyWord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing vocabulary", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load vocabulary"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val dailyStatsState: StateFlow<UiState<List<DailyProgressStat>>> = repository.observeDailyStats(currentUserId)
        .map<List<DailyProgressStat>, UiState<List<DailyProgressStat>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = unwrapFirestoreError(error)
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing daily stats", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load progress statistics"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val result = repository.ensureUserProfile(currentUserId, initialDisplayName, initialEmail)
            result.onFailure { err ->
                _bannerError.value = err.localizedMessage ?: "Could not initialize user profile."
            }
        }
        // Track real active app usage time and persist every 30 seconds
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                repository.recordPracticeSeconds(currentUserId, 30L)
            }
        }
    }

    fun currentProfile(): UserProfile? =
        (userProfileState.value as? UiState.Success)?.data

    fun currentLevel(): EnglishLevel =
        EnglishLevel.fromRaw(currentProfile()?.englishLevel)

    fun currentThemeMode(): ThemeMode =
        ThemeMode.fromRaw(currentProfile()?.themeMode)

    fun currentMarathiScript(): MarathiScript =
        MarathiScript.fromRaw(currentProfile()?.marathiScript)

    fun currentWeeklyInterviewFrequency(): Int =
        (currentProfile()?.weeklyInterviewFrequency ?: 2L).toInt().coerceIn(1, 2)

    fun todayDateKey(): String = repository.todayDateKey()

    fun dismissBannerError() {
        _bannerError.value = null
    }

    fun dismissStatusMessage() {
        _statusMessage.value = null
    }

    fun dismissLatestReport() {
        _latestGeneratedReport.value = null
    }

    fun updateEnglishLevel(level: EnglishLevel) {
        viewModelScope.launch {
            repository.updateSettings(currentUserId, englishLevel = level)
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to update level." }
                .onSuccess { _statusMessage.value = "Level set to ${level.badge} (${level.label})" }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateSettings(currentUserId, themeMode = mode)
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to update theme." }
        }
    }

    fun updateMarathiScript(script: MarathiScript) {
        viewModelScope.launch {
            repository.updateSettings(currentUserId, marathiScript = script)
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to update Marathi script." }
        }
    }

    fun updateVoiceSettings(autoSpeak: Boolean? = null, speechRate: Double? = null) {
        viewModelScope.launch {
            repository.updateSettings(
                userId = currentUserId,
                autoSpeakAiReplies = autoSpeak,
                speechRate = speechRate
            ).onFailure { _bannerError.value = it.localizedMessage ?: "Failed to update voice settings." }
        }
    }

    fun updateWeeklyInterviewFrequency(frequency: Long) {
        val clamped = frequency.coerceIn(1L, 2L)
        viewModelScope.launch {
            repository.updateSettings(
                userId = currentUserId,
                weeklyInterviewFrequency = clamped
            )
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to update weekly interview schedule." }
                .onSuccess {
                    _statusMessage.value = if (clamped == 1L) {
                        "Interview Practice set to 1 session/week (Friday)."
                    } else {
                        "Interview Practice set to 2 sessions/week (Tue & Fri)."
                    }
                }
        }
    }

    fun getWeeklyScheduleItems(
        topics: List<DailyTopicRecord> = (dailyTopicsState.value as? UiState.Success)?.data.orEmpty(),
        frequency: Int = currentWeeklyInterviewFrequency()
    ): List<WeeklyDayScheduleItem> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayKey = todayDateKey()
        val topicByDate = topics.associateBy { it.dateKey }
        val weekCal = startOfCurrentWeekCalendar()
        val clampedFreq = frequency.coerceIn(1, 2)

        val dayDefs = listOf(
            Triple(Calendar.MONDAY, "Mon", "Monday"),
            Triple(Calendar.TUESDAY, "Tue", "Tuesday"),
            Triple(Calendar.WEDNESDAY, "Wed", "Wednesday"),
            Triple(Calendar.THURSDAY, "Thu", "Thursday"),
            Triple(Calendar.FRIDAY, "Fri", "Friday"),
            Triple(Calendar.SATURDAY, "Sat", "Saturday"),
            Triple(Calendar.SUNDAY, "Sun", "Sunday")
        )

        return dayDefs.mapIndexed { index, (calDay, shortName, fullName) ->
            val cal = (weekCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, index) }
            val dateKey = sdf.format(cal.time)
            val defaultType = defaultSessionTypeForDay(calDay, clampedFreq)
            val savedTopic = topicByDate[dateKey]
            val actualType = savedTopic?.let { DailySessionType.fromRaw(it.sessionType) } ?: defaultType
            WeeklyDayScheduleItem(
                dayShort = shortName,
                dayFull = fullName,
                sessionType = actualType,
                isToday = dateKey == todayKey,
                isCompleted = savedTopic?.completed == true
            )
        }
    }

    fun interviewsThisWeekCount(
        topics: List<DailyTopicRecord> = (dailyTopicsState.value as? UiState.Success)?.data.orEmpty(),
        sessions: List<ChatSession> = (chatSessionsState.value as? UiState.Success)?.data.orEmpty(),
        excludeDateKey: String? = null
    ): Int {
        val weekKeys = currentWeekDateKeys()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val interviewTopicKeys = topics
            .filter {
                it.dateKey in weekKeys &&
                    it.dateKey != excludeDateKey &&
                    it.sessionType == DailySessionType.INTERVIEW_PRACTICE.name
            }
            .map { it.dateKey }
            .toSet()

        val standaloneInterviewSessions = sessions.count { session ->
            val isInterview = session.mode == SessionMode.INTERVIEW.name || !session.interviewCategory.isNullOrBlank()
            val notLinkedToTopic = session.topicId.isNullOrBlank()
            val sessionDateKey = session.createdAt?.toDate()?.let { sdf.format(it) }
            isInterview && notLinkedToTopic && (sessionDateKey == null || sessionDateKey in weekKeys)
        }

        return interviewTopicKeys.size + standaloneInterviewSessions
    }

    fun todayDefaultSessionType(): DailySessionType {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return defaultSessionTypeForDay(day, currentWeeklyInterviewFrequency())
    }

    private fun startOfCurrentWeekCalendar(): Calendar {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return cal
    }

    private fun currentWeekDateKeys(): Set<String> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = startOfCurrentWeekCalendar()
        return (0..6).map { offset ->
            val cal = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            sdf.format(cal.time)
        }.toSet()
    }

    private fun defaultSessionTypeForDay(calendarDayOfWeek: Int, frequency: Int): DailySessionType =
        when (calendarDayOfWeek) {
            Calendar.TUESDAY -> if (frequency >= 2) DailySessionType.INTERVIEW_PRACTICE else DailySessionType.DAILY_CONVERSATION
            Calendar.FRIDAY -> DailySessionType.INTERVIEW_PRACTICE
            Calendar.SATURDAY -> DailySessionType.GENERAL_CONVERSATION
            Calendar.SUNDAY -> DailySessionType.REVISION
            else -> DailySessionType.DAILY_CONVERSATION
        }

    fun selectChatSession(sessionId: String?) {
        _selectedChatSessionId.value = sessionId
    }

    fun startNewChat() {
        viewModelScope.launch {
            val level = currentLevel()
            val result = repository.createChatSession(
                userId = currentUserId,
                title = "English Chat (${level.badge})",
                level = level,
                mode = SessionMode.CHAT
            )
            result.onSuccess { sessionId ->
                _selectedChatSessionId.value = sessionId
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Failed to start new chat."
            }
        }
    }

    fun clearActiveChat() {
        val sessionId = _selectedChatSessionId.value ?: return
        viewModelScope.launch {
            repository.clearSessionMessages(currentUserId, sessionId)
                .onSuccess { _statusMessage.value = "Chat cleared." }
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to clear chat." }
        }
    }

    fun deleteChatSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(currentUserId, sessionId)
                .onSuccess {
                    if (_selectedChatSessionId.value == sessionId) {
                        _selectedChatSessionId.value = null
                    }
                    if (_selectedVoiceSessionId.value == sessionId) {
                        _selectedVoiceSessionId.value = null
                    }
                }
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to delete session." }
        }
    }

    fun sendChatMessage(rawText: String) {
        val trimmed = rawText.trim()
        if (trimmed.isBlank() || _isSendingChat.value) return

        viewModelScope.launch {
            _isSendingChat.value = true
            _bannerError.value = null
            try {
                val level = currentLevel()
                val script = currentMarathiScript()
                var sessionId = _selectedChatSessionId.value

                if (sessionId.isNullOrBlank()) {
                    val created = repository.createChatSession(
                        userId = currentUserId,
                        title = trimmed.take(40),
                        level = level,
                        mode = SessionMode.CHAT
                    )
                    sessionId = created.getOrThrow()
                    _selectedChatSessionId.value = sessionId
                }

                val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
                val activeSession = sessions.firstOrNull { it.id == sessionId }
                val history = (activeChatMessages.value as? UiState.Success)?.data.orEmpty()
                val resolvedInterviewCategory = if (
                    activeSession != null && (
                        activeSession.mode == SessionMode.INTERVIEW.name ||
                            !activeSession.interviewCategory.isNullOrBlank()
                        )
                ) {
                    activeSession.interviewCategory ?: InterviewCategory.HR.title
                } else null

                val tutorResult = aiService.analyzeAndReply(
                    userMessage = trimmed,
                    level = level,
                    marathiScript = script,
                    history = history,
                    topicContext = activeSession?.topicTitle,
                    interviewCategory = resolvedInterviewCategory,
                    isVoiceMode = false
                )

                repository.saveTutorTurn(
                    userId = currentUserId,
                    sessionId = sessionId,
                    userText = trimmed,
                    tutorResult = tutorResult,
                    level = level,
                    isVoiceMessage = false,
                    currentProfile = currentProfile()
                ).getOrThrow()

                activeSession?.topicId?.takeIf { it.isNotBlank() }?.let { topicDateKey ->
                    repository.updateTopicProgress(
                        userId = currentUserId,
                        dateKey = topicDateKey,
                        sessionId = sessionId,
                        markCompleted = false,
                        turnsIncrement = 1L
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "sendChatMessage failed", e)
                _bannerError.value = e.localizedMessage ?: "Could not send message. Please try again."
            } finally {
                _isSendingChat.value = false
            }
        }
    }

    fun sendVoiceMessage(
        spokenText: String,
        onAiReplyReady: (String) -> Unit
    ) {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank() || _isSendingVoice.value) return

        viewModelScope.launch {
            _isSendingVoice.value = true
            _bannerError.value = null
            try {
                val level = currentLevel()
                val script = currentMarathiScript()
                var sessionId = _selectedVoiceSessionId.value

                if (sessionId.isNullOrBlank()) {
                    val created = repository.createChatSession(
                        userId = currentUserId,
                        title = "Voice Practice (${level.badge})",
                        level = level,
                        mode = SessionMode.VOICE
                    )
                    sessionId = created.getOrThrow()
                    _selectedVoiceSessionId.value = sessionId
                }

                val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
                val activeSession = sessions.firstOrNull { it.id == sessionId }
                val history = (activeVoiceMessages.value as? UiState.Success)?.data.orEmpty()
                val resolvedInterviewCategory = if (
                    activeSession != null && (
                        activeSession.mode == SessionMode.INTERVIEW.name ||
                            !activeSession.interviewCategory.isNullOrBlank()
                        )
                ) {
                    activeSession.interviewCategory ?: InterviewCategory.HR.title
                } else null

                val tutorResult = aiService.analyzeAndReply(
                    userMessage = trimmed,
                    level = level,
                    marathiScript = script,
                    history = history,
                    topicContext = activeSession?.topicTitle,
                    interviewCategory = resolvedInterviewCategory,
                    isVoiceMode = true
                )

                repository.saveTutorTurn(
                    userId = currentUserId,
                    sessionId = sessionId,
                    userText = trimmed,
                    tutorResult = tutorResult,
                    level = level,
                    isVoiceMessage = true,
                    currentProfile = currentProfile()
                ).getOrThrow()

                activeSession?.topicId?.takeIf { it.isNotBlank() }?.let { topicDateKey ->
                    repository.updateTopicProgress(
                        userId = currentUserId,
                        dateKey = topicDateKey,
                        sessionId = sessionId,
                        markCompleted = false,
                        turnsIncrement = 1L
                    )
                }

                onAiReplyReady(tutorResult.assistantReply)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "sendVoiceMessage failed", e)
                _bannerError.value = e.localizedMessage ?: "Voice AI analysis failed."
            } finally {
                _isSendingVoice.value = false
            }
        }
    }

    fun startNewVoiceSession() {
        viewModelScope.launch {
            val level = currentLevel()
            repository.createChatSession(
                userId = currentUserId,
                title = "Voice Practice (${level.badge})",
                level = level,
                mode = SessionMode.VOICE
            ).onSuccess { id ->
                _selectedVoiceSessionId.value = id
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Failed to start voice session."
            }
        }
    }

    fun generateOrRefreshDailyTopic(
        preferredCategory: String? = null,
        preferredInterviewCategory: InterviewCategory? = null,
        allowScheduleAdjustment: Boolean = true
    ) {
        if (_isGeneratingTopic.value) return
        viewModelScope.launch {
            _isGeneratingTopic.value = true
            _bannerError.value = null
            try {
                val level = currentLevel()
                val dateKey = repository.todayDateKey()
                val dayName = SimpleDateFormat("EEEE", Locale.US).format(Calendar.getInstance().time)
                val existingTopics = (dailyTopicsState.value as? UiState.Success)?.data.orEmpty()
                val recentTitles = existingTopics.map { it.title }.take(12)
                val scheduledType = todayDefaultSessionType()
                val freq = currentWeeklyInterviewFrequency()
                val interviewsCount = interviewsThisWeekCount(
                    topics = existingTopics,
                    excludeDateKey = dateKey
                )

                val draft = aiService.generateDailyTopic(
                    level = level,
                    dayOfWeekName = dayName,
                    scheduledSessionType = scheduledType,
                    interviewsThisWeek = interviewsCount,
                    weeklyInterviewFrequency = freq,
                    preferredCategory = preferredCategory,
                    preferredInterviewCategory = preferredInterviewCategory,
                    recentTopicTitles = recentTitles,
                    dateKey = dateKey,
                    allowScheduleAdjustment = allowScheduleAdjustment
                )
                repository.saveDailyTopic(currentUserId, dateKey, draft, level).getOrThrow()
                _statusMessage.value = if (draft.sessionType == DailySessionType.INTERVIEW_PRACTICE) {
                    "Interview Practice ready: ${draft.interviewCategory ?: draft.title}"
                } else {
                    "Today's topic is ready: ${draft.title}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateOrRefreshDailyTopic failed", e)
                _bannerError.value = e.localizedMessage ?: "Could not generate daily topic."
            } finally {
                _isGeneratingTopic.value = false
            }
        }
    }

    fun startTopicConversation(
        topic: DailyTopicRecord,
        inVoiceMode: Boolean,
        onNavigate: () -> Unit
    ) {
        viewModelScope.launch {
            val existingSessionId = topic.sessionId
            if (!existingSessionId.isNullOrBlank()) {
                if (inVoiceMode) _selectedVoiceSessionId.value = existingSessionId
                else _selectedChatSessionId.value = existingSessionId
                onNavigate()
                return@launch
            }

            val level = currentLevel()
            val isInterviewTopic = topic.sessionType == DailySessionType.INTERVIEW_PRACTICE.name
            val interviewCat = if (isInterviewTopic) {
                topic.interviewCategory?.takeIf { it.isNotBlank() } ?: InterviewCategory.HR.title
            } else null

            val greeting = if (isInterviewTopic) {
                "Welcome to your $interviewCat (${level.label} level). I will ask one question at a time and wait for your answer.\n\nQuestion 1: ${topic.starterQuestion}"
            } else {
                "${topic.promptSummary}\n\n${topic.starterQuestion}"
            }

            val result = repository.createChatSession(
                userId = currentUserId,
                title = if (isInterviewTopic) "$interviewCat (${level.badge})" else topic.title,
                level = level,
                mode = when {
                    isInterviewTopic -> SessionMode.INTERVIEW
                    inVoiceMode -> SessionMode.VOICE
                    else -> SessionMode.TOPIC
                },
                topicId = topic.dateKey,
                topicTitle = "${topic.title} (${topic.category})",
                interviewCategory = interviewCat,
                initialAssistantGreeting = greeting
            )
            result.onSuccess { sessionId ->
                repository.updateTopicProgress(
                    userId = currentUserId,
                    dateKey = topic.dateKey,
                    sessionId = sessionId,
                    markCompleted = false
                )
                if (inVoiceMode) _selectedVoiceSessionId.value = sessionId
                else _selectedChatSessionId.value = sessionId
                onNavigate()
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Failed to start topic conversation."
            }
        }
    }

    fun startInterviewSession(
        category: InterviewCategory,
        inVoiceMode: Boolean,
        fromTopicDateKey: String? = null,
        onNavigate: () -> Unit
    ) {
        viewModelScope.launch {
            val freq = currentWeeklyInterviewFrequency()
            val topics = (dailyTopicsState.value as? UiState.Success)?.data.orEmpty()
            val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
            val usedThisWeek = interviewsThisWeekCount(
                topics = topics,
                sessions = sessions,
                excludeDateKey = fromTopicDateKey
            )

            if (fromTopicDateKey == null && usedThisWeek >= freq) {
                _bannerError.value = "Weekly interview limit reached ($usedThisWeek/$freq this week). Interview practice runs 1–2 times per week. You can change weekly frequency in Settings."
                return@launch
            }

            val level = currentLevel()
            val openingQuestion = buildOpeningInterviewQuestion(category, level)
            val greeting = "Welcome to your ${category.title} (${level.label} level). I will ask one question at a time and help polish your grammar and professional delivery.\n\nQuestion 1: $openingQuestion"

            val result = repository.createChatSession(
                userId = currentUserId,
                title = "${category.title} (${level.badge})",
                level = level,
                mode = SessionMode.INTERVIEW,
                topicId = fromTopicDateKey,
                topicTitle = category.title,
                interviewCategory = category.title,
                initialAssistantGreeting = greeting
            )
            result.onSuccess { sessionId ->
                if (!fromTopicDateKey.isNullOrBlank()) {
                    repository.updateTopicProgress(
                        userId = currentUserId,
                        dateKey = fromTopicDateKey,
                        sessionId = sessionId,
                        markCompleted = false
                    )
                }
                if (inVoiceMode) _selectedVoiceSessionId.value = sessionId
                else _selectedChatSessionId.value = sessionId
                onNavigate()
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Failed to start interview session."
            }
        }
    }

    fun finishAndGenerateInterviewReport(sessionId: String, isVoiceSession: Boolean) {
        if (_isGeneratingReport.value) return
        viewModelScope.launch {
            _isGeneratingReport.value = true
            _bannerError.value = null
            try {
                val sessions = (chatSessionsState.value as? UiState.Success)?.data.orEmpty()
                val activeSession = sessions.firstOrNull { it.id == sessionId }
                val messages = if (isVoiceSession) {
                    (activeVoiceMessages.value as? UiState.Success)?.data.orEmpty()
                } else {
                    (activeChatMessages.value as? UiState.Success)?.data.orEmpty()
                }

                val questionsAnswered = messages.count { it.role == "user" }.toLong()
                if (questionsAnswered <= 0L) {
                    _bannerError.value = "Please answer at least one interview question before generating your report."
                    return@launch
                }

                val level = currentLevel()
                val script = currentMarathiScript()
                val category = activeSession?.interviewCategory?.takeIf { it.isNotBlank() }
                    ?: activeSession?.topicTitle?.takeIf { it.isNotBlank() }
                    ?: InterviewCategory.HR.title

                val draft = aiService.generateInterviewReport(
                    interviewCategory = category,
                    level = level,
                    marathiScript = script,
                    transcript = messages
                )

                val savedReport = repository.saveInterviewReport(
                    userId = currentUserId,
                    sessionId = sessionId,
                    interviewCategory = category,
                    level = level,
                    draft = draft,
                    questionsAnswered = questionsAnswered
                ).getOrThrow()

                activeSession?.topicId?.takeIf { it.isNotBlank() }?.let { topicDateKey ->
                    repository.updateTopicProgress(
                        userId = currentUserId,
                        dateKey = topicDateKey,
                        sessionId = sessionId,
                        markCompleted = true
                    )
                }

                _latestGeneratedReport.value = savedReport
                _statusMessage.value = "Interview Performance Report generated!"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "finishAndGenerateInterviewReport failed", e)
                _bannerError.value = e.localizedMessage ?: "Could not generate interview report."
            } finally {
                _isGeneratingReport.value = false
            }
        }
    }

    private fun buildOpeningInterviewQuestion(category: InterviewCategory, level: EnglishLevel): String {
        return when (category) {
            InterviewCategory.HR -> when (level) {
                EnglishLevel.LOW -> "Can you tell me about your strengths and why you want this job?"
                EnglishLevel.MEDIUM -> "What motivates you to do your best work, and how do you handle deadlines?"
                EnglishLevel.HIGH -> "Tell me about a challenging workplace or team situation you navigated and what you learned from it."
            }
            InterviewCategory.SELF_INTRODUCTION -> when (level) {
                EnglishLevel.LOW -> "Please introduce yourself in simple English — your name, education, and hobbies."
                EnglishLevel.MEDIUM -> "Tell me about yourself, your academic background, and your key skills."
                EnglishLevel.HIGH -> "Walk me through your professional profile, key achievements, and what brings you to this role."
            }
            InterviewCategory.TECHNICAL -> when (level) {
                EnglishLevel.LOW -> "Which programming language or technical subject do you enjoy most, and why?"
                EnglishLevel.MEDIUM -> "Tell me about a technical project you built and one problem you solved in it."
                EnglishLevel.HIGH -> "Walk me through the architecture of a challenging technical project you designed, including trade-offs you made."
            }
            InterviewCategory.COLLEGE_PLACEMENT -> when (level) {
                EnglishLevel.LOW -> "What is your college major, and which subject is your favorite?"
                EnglishLevel.MEDIUM -> "Why should our company hire you as a fresher from your college?"
                EnglishLevel.HIGH -> "How have your academic projects and campus experiences prepared you to contribute from day one?"
            }
            InterviewCategory.INTERNSHIP -> when (level) {
                EnglishLevel.LOW -> "Why do you want to do an internship, and what do you hope to learn?"
                EnglishLevel.MEDIUM -> "Tell me about a skill you practiced recently that makes you a good fit for this internship."
                EnglishLevel.HIGH -> "What specific skills do you want to deepen during this internship, and how will you add value to the team?"
            }
            InterviewCategory.AI_ML_JOB -> when (level) {
                EnglishLevel.LOW -> "What interests you about Artificial Intelligence and Machine Learning?"
                EnglishLevel.MEDIUM -> "Explain a machine learning or AI project you worked on and what data or model you used."
                EnglishLevel.HIGH -> "How do you evaluate an AI or ML model in production to balance accuracy, latency, and reliability?"
            }
            InterviewCategory.COMMUNICATION_SKILLS -> when (level) {
                EnglishLevel.LOW -> "How do you explain an idea clearly to a friend or classmate?"
                EnglishLevel.MEDIUM -> "Tell me about a time when clear communication helped you solve a misunderstanding."
                EnglishLevel.HIGH -> "How do you adapt your communication style when presenting complex ideas to non-technical stakeholders?"
            }
        }
    }

    fun markTopicCompleted(dateKey: String) {
        viewModelScope.launch {
            repository.updateTopicProgress(
                userId = currentUserId,
                dateKey = dateKey,
                sessionId = null,
                markCompleted = true
            ).onSuccess {
                _statusMessage.value = "Topic marked as completed!"
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Failed to mark topic completed."
            }
        }
    }

    fun generateDailyVocabulary() {
        if (_isGeneratingVocab.value) return
        viewModelScope.launch {
            _isGeneratingVocab.value = true
            _bannerError.value = null
            try {
                val level = currentLevel()
                val script = currentMarathiScript()
                val existingWords = (vocabularyState.value as? UiState.Success)?.data
                    ?.map { it.word }
                    .orEmpty()
                val generated = aiService.generateDailyVocabulary(level, existingWords, script)
                _dailyWordSuggestions.value = generated
                if (generated.isEmpty()) {
                    _bannerError.value = "No vocabulary words generated. Please try again."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateDailyVocabulary failed", e)
                _bannerError.value = e.localizedMessage ?: "Failed to generate daily vocabulary."
            } finally {
                _isGeneratingVocab.value = false
            }
        }
    }

    fun saveSuggestedWord(draft: GeneratedWordDraft, source: VocabSource) {
        viewModelScope.launch {
            val existing = (vocabularyState.value as? UiState.Success)?.data.orEmpty()
            if (existing.any { it.word.equals(draft.word, ignoreCase = true) }) {
                _statusMessage.value = "\"${draft.word}\" is already in your vocabulary."
                return@launch
            }
            repository.addVocabularyWord(
                userId = currentUserId,
                draft = draft,
                level = currentLevel(),
                source = source,
                markLearnedImmediately = false
            ).onSuccess {
                _dailyWordSuggestions.value = _dailyWordSuggestions.value.filterNot {
                    it.word.equals(draft.word, ignoreCase = true)
                }
                _statusMessage.value = "Saved \"${draft.word}\" to Vocabulary!"
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Failed to save word."
            }
        }
    }

    fun addManualVocabularyWord(
        word: String,
        marathiMeaning: String,
        exampleSentence: String,
        pronunciation: String,
        partOfSpeech: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val draft = GeneratedWordDraft(
                word = word.trim(),
                marathiMeaning = marathiMeaning.trim(),
                exampleSentence = exampleSentence.trim(),
                pronunciation = pronunciation.trim(),
                partOfSpeech = partOfSpeech.trim().ifBlank { "word" }
            )
            repository.addVocabularyWord(
                userId = currentUserId,
                draft = draft,
                level = currentLevel(),
                source = VocabSource.MANUAL,
                markLearnedImmediately = false
            ).onSuccess {
                _statusMessage.value = "Added \"${draft.word}\""
                onDone()
            }.onFailure {
                _bannerError.value = it.localizedMessage ?: "Could not add word."
            }
        }
    }

    fun autoFillVocabularyWord(
        word: String,
        onFilled: (GeneratedWordDraft) -> Unit
    ) {
        if (word.isBlank() || _isEnrichingWord.value) return
        viewModelScope.launch {
            _isEnrichingWord.value = true
            try {
                val draft = aiService.enrichVocabularyWord(
                    word = word.trim(),
                    level = currentLevel(),
                    marathiScript = currentMarathiScript()
                )
                onFilled(draft)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerError.value = e.localizedMessage ?: "Could not auto-fill word."
            } finally {
                _isEnrichingWord.value = false
            }
        }
    }

    fun toggleWordLearned(word: VocabularyWord) {
        viewModelScope.launch {
            repository.toggleWordLearned(currentUserId, word.id, !word.learned)
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to update word." }
        }
    }

    fun deleteVocabularyWord(word: VocabularyWord) {
        viewModelScope.launch {
            repository.deleteVocabularyWord(currentUserId, word)
                .onFailure { _bannerError.value = it.localizedMessage ?: "Failed to delete word." }
        }
    }

    private fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    companion object {
        private const val TAG = "FluentMateVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
