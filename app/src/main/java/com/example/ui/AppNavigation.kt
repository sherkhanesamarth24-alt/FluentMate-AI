package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ai.FluentMateAiService
import com.example.auth.AuthScreen
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.EnglishLevel
import com.example.data.FluentMateRepository
import com.example.data.ThemeMode
import com.example.data.UiState
import com.example.data.VocabSource
import com.example.ui.components.ErrorBanner
import com.example.ui.components.InterviewReportDialog
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.DailyTopicsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VocabularyScreen
import com.example.ui.screens.VoicePracticeScreen
import com.example.ui.theme.FluentMateTheme
import com.example.ui.viewmodel.FluentMateViewModel
import com.example.voice.VoiceEngine
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
    const val DASHBOARD = "dashboard"
    const val AI_CHAT = "ai_chat"
    const val VOICE_PRACTICE = "voice_practice"
    const val DAILY_TOPICS = "daily_topics"
    const val VOCABULARY = "vocabulary"
    const val PROGRESS = "progress"
    const val SETTINGS = "settings"
}

private data class NavDestination(
    val route: String,
    val label: String,
    val shortLabel: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val showInBottomBar: Boolean
)

private val ALL_DESTINATIONS = listOf(
    NavDestination(Routes.DASHBOARD, "Dashboard", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, true),
    NavDestination(Routes.AI_CHAT, "AI Chat", "Chat", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat, true),
    NavDestination(Routes.VOICE_PRACTICE, "Voice Practice", "Voice", Icons.Filled.Mic, Icons.Outlined.Mic, true),
    NavDestination(Routes.DAILY_TOPICS, "Daily Topics", "Topics", Icons.Filled.Topic, Icons.Outlined.Topic, true),
    NavDestination(Routes.VOCABULARY, "Vocabulary", "Vocab", Icons.Filled.Book, Icons.Outlined.Book, true),
    NavDestination(Routes.PROGRESS, "Progress", "Progress", Icons.Filled.BarChart, Icons.Outlined.BarChart, false),
    NavDestination(Routes.SETTINGS, "Settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, false)
)

@Composable
fun FluentMateApp(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val user = currentUser
    if (user == null) {
        FluentMateTheme(themeMode = ThemeMode.DARK) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        val uid = user.uid
        key(uid) {
            AuthenticatedAppShell(
                currentUserId = uid,
                displayName = user.displayName ?: "English Learner",
                email = user.email ?: ""
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedAppShell(
    currentUserId: String,
    displayName: String,
    email: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: FluentMateViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repository = FluentMateRepository(db)
                val aiService = FluentMateAiService()
                FluentMateViewModel(
                    repository = repository,
                    aiService = aiService,
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    initialEmail = email
                )
            }
        }
    )

    val voiceEngine = remember(currentUserId) { VoiceEngine(context) }
    DisposableEffect(currentUserId) {
        onDispose {
            voiceEngine.release()
        }
    }

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.chatSessionsState.collectAsStateWithLifecycle()
    val selectedChatSessionId by viewModel.selectedChatSessionId.collectAsStateWithLifecycle()
    val selectedVoiceSessionId by viewModel.selectedVoiceSessionId.collectAsStateWithLifecycle()
    val activeChatMessages by viewModel.activeChatMessages.collectAsStateWithLifecycle()
    val activeVoiceMessages by viewModel.activeVoiceMessages.collectAsStateWithLifecycle()
    val topicsState by viewModel.dailyTopicsState.collectAsStateWithLifecycle()
    val interviewReportsState by viewModel.interviewReportsState.collectAsStateWithLifecycle()
    val vocabularyState by viewModel.vocabularyState.collectAsStateWithLifecycle()
    val statsState by viewModel.dailyStatsState.collectAsStateWithLifecycle()

    val isSendingChat by viewModel.isSendingChat.collectAsStateWithLifecycle()
    val isSendingVoice by viewModel.isSendingVoice.collectAsStateWithLifecycle()
    val isGeneratingTopic by viewModel.isGeneratingTopic.collectAsStateWithLifecycle()
    val isGeneratingReport by viewModel.isGeneratingReport.collectAsStateWithLifecycle()
    val latestGeneratedReport by viewModel.latestGeneratedReport.collectAsStateWithLifecycle()
    val isGeneratingVocab by viewModel.isGeneratingVocab.collectAsStateWithLifecycle()
    val isEnrichingWord by viewModel.isEnrichingWord.collectAsStateWithLifecycle()
    val dailyWordSuggestions by viewModel.dailyWordSuggestions.collectAsStateWithLifecycle()
    val bannerError by viewModel.bannerError.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val profile = (profileState as? UiState.Success)?.data
    val themeMode = ThemeMode.fromRaw(profile?.themeMode)
    val currentLevel = EnglishLevel.fromRaw(profile?.englishLevel)
    val autoSpeak = profile?.autoSpeakAiReplies ?: true
    val speechRate = (profile?.speechRate ?: 1.0).toFloat()
    val weeklyInterviewFreq = (profile?.weeklyInterviewFrequency ?: 2L).toInt().coerceIn(1, 2)
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val topics = (topicsState as? UiState.Success)?.data.orEmpty()
    val activeVoiceSession = sessions.firstOrNull { it.id == selectedVoiceSessionId }
    val weeklySchedule = remember(topics, weeklyInterviewFreq) {
        viewModel.getWeeklyScheduleItems(topics, weeklyInterviewFreq)
    }
    val interviewsThisWeek = remember(topics, sessions) {
        viewModel.interviewsThisWeekCount(topics, sessions)
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Routes.DASHBOARD
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissStatusMessage()
        }
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    FluentMateTheme(themeMode = themeMode) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 720.dp

            val mainScaffold: @Composable () -> Unit = {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                if (!isWideScreen) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("drawer_menu_button")
                                    ) {
                                        Icon(Icons.Default.Menu, contentDescription = "Open menu")
                                    }
                                }
                            },
                            title = {
                                val title = ALL_DESTINATIONS.firstOrNull { it.route == currentRoute }?.label
                                    ?: "FluentMate AI"
                                Text(
                                    text = title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            actions = {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = currentLevel.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                if (!isWideScreen) {
                                    IconButton(
                                        onClick = { navigateTo(Routes.PROGRESS) },
                                        modifier = Modifier.testTag("topbar_progress_button")
                                    ) {
                                        Icon(Icons.Outlined.BarChart, contentDescription = "Progress")
                                    }
                                    IconButton(
                                        onClick = { navigateTo(Routes.SETTINGS) },
                                        modifier = Modifier.testTag("topbar_settings_button")
                                    ) {
                                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            NavigationBar {
                                ALL_DESTINATIONS.filter { it.showInBottomBar }.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.shortLabel, maxLines = 1) },
                                        modifier = Modifier.testTag("bottom_nav_${dest.route}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (isWideScreen) {
                            NavigationRail(
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Spacer(modifier = Modifier.height(8.dp))
                                ALL_DESTINATIONS.forEach { dest ->
                                    val selected = currentRoute == dest.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTo(dest.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                                contentDescription = dest.label
                                            )
                                        },
                                        label = { Text(dest.shortLabel) },
                                        modifier = Modifier.testTag("rail_nav_${dest.route}")
                                    )
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            bannerError?.takeIf { it.isNotBlank() }?.let { errMsg ->
                                ErrorBanner(
                                    message = errMsg,
                                    onDismiss = viewModel::dismissBannerError,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                NavHost(
                                    navController = navController,
                                    startDestination = Routes.DASHBOARD
                                ) {
                                    composable(Routes.DASHBOARD) {
                                        DashboardScreen(
                                            profileState = profileState,
                                            topicsState = topicsState,
                                            vocabularyState = vocabularyState,
                                            statsState = statsState,
                                            todayDateKey = viewModel.todayDateKey(),
                                            isGeneratingTopic = isGeneratingTopic,
                                            onSelectLevel = viewModel::updateEnglishLevel,
                                            onGenerateTodayTopic = { viewModel.generateOrRefreshDailyTopic() },
                                            onStartTopicChat = { topic ->
                                                viewModel.startTopicConversation(topic, inVoiceMode = false) {
                                                    navigateTo(Routes.AI_CHAT)
                                                }
                                            },
                                            onNavigateToChat = { navigateTo(Routes.AI_CHAT) },
                                            onNavigateToVoice = { navigateTo(Routes.VOICE_PRACTICE) },
                                            onNavigateToVocabulary = { navigateTo(Routes.VOCABULARY) }
                                        )
                                    }

                                    composable(Routes.AI_CHAT) {
                                        AiChatScreen(
                                            currentLevel = currentLevel,
                                            sessionsState = sessionsState,
                                            selectedSessionId = selectedChatSessionId,
                                            messagesState = activeChatMessages,
                                            isSending = isSendingChat,
                                            isGeneratingReport = isGeneratingReport,
                                            onSelectLevel = viewModel::updateEnglishLevel,
                                            onSelectSession = viewModel::selectChatSession,
                                            onNewChat = viewModel::startNewChat,
                                            onClearChat = viewModel::clearActiveChat,
                                            onDeleteSession = viewModel::deleteChatSession,
                                            onSendMessage = viewModel::sendChatMessage,
                                            onFinishInterview = { sessionId ->
                                                viewModel.finishAndGenerateInterviewReport(sessionId, isVoiceSession = false)
                                            },
                                            onSpeakText = { text -> voiceEngine.speak(text, speechRate) },
                                            onSaveVocabularyWord = { draft ->
                                                viewModel.saveSuggestedWord(draft, VocabSource.CHAT)
                                            }
                                        )
                                    }

                                    composable(Routes.VOICE_PRACTICE) {
                                        VoicePracticeScreen(
                                            voiceEngine = voiceEngine,
                                            currentLevel = currentLevel,
                                            autoSpeak = autoSpeak,
                                            speechRate = speechRate,
                                            activeSession = activeVoiceSession,
                                            messagesState = activeVoiceMessages,
                                            isProcessingVoice = isSendingVoice,
                                            isGeneratingReport = isGeneratingReport,
                                            onSelectLevel = viewModel::updateEnglishLevel,
                                            onStartNewVoiceSession = viewModel::startNewVoiceSession,
                                            onSendSpokenText = viewModel::sendVoiceMessage,
                                            onFinishInterview = { sessionId ->
                                                viewModel.finishAndGenerateInterviewReport(sessionId, isVoiceSession = true)
                                            },
                                            onSaveVocabularyWord = { draft ->
                                                viewModel.saveSuggestedWord(draft, VocabSource.CHAT)
                                            }
                                        )
                                    }

                                    composable(Routes.DAILY_TOPICS) {
                                        DailyTopicsScreen(
                                            currentLevel = currentLevel,
                                            topicsState = topicsState,
                                            reportsState = interviewReportsState,
                                            weeklySchedule = weeklySchedule,
                                            interviewsThisWeek = interviewsThisWeek,
                                            weeklyInterviewFrequency = weeklyInterviewFreq,
                                            todayDateKey = viewModel.todayDateKey(),
                                            isGeneratingTopic = isGeneratingTopic,
                                            onGenerateTopic = { prefCat, prefInterviewCat ->
                                                viewModel.generateOrRefreshDailyTopic(
                                                    preferredCategory = prefCat,
                                                    preferredInterviewCategory = prefInterviewCat
                                                )
                                            },
                                            onStartTopicInChat = { topic ->
                                                viewModel.startTopicConversation(topic, inVoiceMode = false) {
                                                    navigateTo(Routes.AI_CHAT)
                                                }
                                            },
                                            onStartTopicInVoice = { topic ->
                                                viewModel.startTopicConversation(topic, inVoiceMode = true) {
                                                    navigateTo(Routes.VOICE_PRACTICE)
                                                }
                                            },
                                            onStartInterview = { category, inVoiceMode, fromTopicDateKey ->
                                                viewModel.startInterviewSession(
                                                    category = category,
                                                    inVoiceMode = inVoiceMode,
                                                    fromTopicDateKey = fromTopicDateKey
                                                ) {
                                                    navigateTo(if (inVoiceMode) Routes.VOICE_PRACTICE else Routes.AI_CHAT)
                                                }
                                            },
                                            onMarkCompleted = viewModel::markTopicCompleted,
                                            onSpeakText = { text -> voiceEngine.speak(text, speechRate) }
                                        )
                                    }

                                    composable(Routes.VOCABULARY) {
                                        VocabularyScreen(
                                            currentLevel = currentLevel,
                                            vocabularyState = vocabularyState,
                                            dailySuggestions = dailyWordSuggestions,
                                            isGeneratingDailyVocab = isGeneratingVocab,
                                            isEnrichingWord = isEnrichingWord,
                                            onGenerateDailyVocab = viewModel::generateDailyVocabulary,
                                            onSaveSuggestedWord = viewModel::saveSuggestedWord,
                                            onAddManualWord = viewModel::addManualVocabularyWord,
                                            onAutoFillWord = viewModel::autoFillVocabularyWord,
                                            onToggleLearned = viewModel::toggleWordLearned,
                                            onDeleteWord = viewModel::deleteVocabularyWord,
                                            onSpeakWord = { text -> voiceEngine.speak(text, speechRate) }
                                        )
                                    }

                                    composable(Routes.PROGRESS) {
                                        ProgressScreen(
                                            profileState = profileState,
                                            statsState = statsState,
                                            sessionsState = sessionsState,
                                            topicsState = topicsState,
                                            vocabularyState = vocabularyState
                                        )
                                    }

                                    composable(Routes.SETTINGS) {
                                        SettingsScreen(
                                            profileState = profileState,
                                            onSelectLevel = viewModel::updateEnglishLevel,
                                            onSelectTheme = viewModel::updateThemeMode,
                                            onSelectMarathiScript = viewModel::updateMarathiScript,
                                            onToggleAutoSpeak = { enabled ->
                                                viewModel.updateVoiceSettings(autoSpeak = enabled)
                                            },
                                            onChangeSpeechRate = { rate ->
                                                viewModel.updateVoiceSettings(speechRate = rate)
                                            },
                                            onChangeWeeklyInterviewFrequency = viewModel::updateWeeklyInterviewFrequency,
                                            onSignOut = {
                                                voiceEngine.release()
                                                signOut(credentialManager, scope)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isWideScreen) {
                mainScaffold()
            } else {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "FluentMate AI",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${currentLevel.badge} • ${currentLevel.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            ALL_DESTINATIONS.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationDrawerItem(
                                    selected = selected,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        navigateTo(dest.route)
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                ) {
                    mainScaffold()
                }
            }

            latestGeneratedReport?.let { report ->
                InterviewReportDialog(
                    report = report,
                    onDismiss = viewModel::dismissLatestReport,
                    onSpeakText = { text -> voiceEngine.speak(text, speechRate) }
                )
            }
        }
    }
}
