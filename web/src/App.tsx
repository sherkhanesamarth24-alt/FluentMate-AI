import { User, onAuthStateChanged } from "firebase/auth";
import { useCallback, useEffect, useMemo, useState } from "react";
import {
  BarChart3,
  BookOpen,
  CalendarDays,
  Home,
  Loader2,
  Menu,
  MessageSquare,
  Mic,
  Settings,
  Sparkles,
  X,
} from "lucide-react";
import { AuthGate } from "./components/AuthGate";
import { InterviewReportModal } from "./components/InterviewReportModal";
import {
  getFirebaseEnvStatus,
  getFirebaseServices,
  signOutUser,
} from "./lib/firebase";
import { FluentMateGeminiClient } from "./lib/geminiClient";
import {
  FluentMateRepository,
  countInterviewsThisWeek,
  defaultSessionTypeForJsDay,
  todayDateKey,
} from "./lib/repository";
import { speakEnglishText, stopSpeakingEnglish } from "./lib/speech";
import { AiChatPage } from "./pages/AiChatPage";
import { DailyTopicsPage } from "./pages/DailyTopicsPage";
import { DashboardPage } from "./pages/DashboardPage";
import { ProgressPage } from "./pages/ProgressPage";
import { SettingsPage } from "./pages/SettingsPage";
import { VocabularyPage } from "./pages/VocabularyPage";
import { VoicePracticePage } from "./pages/VoicePracticePage";
import {
  ChatMessage,
  ChatSession,
  DailyProgressStat,
  DailyTopicRecord,
  EnglishLevel,
  GeneratedWordDraft,
  InterviewReport,
  MarathiScript,
  ThemeMode,
  UserProfile,
  VocabSource,
  VocabularyWord,
} from "./types/models";

type AppTab =
  | "dashboard"
  | "chat"
  | "voice"
  | "topics"
  | "vocabulary"
  | "progress"
  | "settings";

const DEFAULT_PROFILE_FALLBACK = (user: User): UserProfile => ({
  id: user.uid,
  userId: user.uid,
  displayName: user.displayName || "Learner",
  email: user.email || "",
  englishLevel: "LOW",
  themeMode: "DARK",
  marathiScript: "ROMAN",
  autoSpeakAiReplies: true,
  speechRate: 1.0,
  weeklyInterviewFrequency: 2,
  totalConversations: 0,
  totalMessagesSent: 0,
  grammarMistakesDetected: 0,
  topicsCompletedCount: 0,
  vocabularyLearnedCount: 0,
  totalPracticeSeconds: 0,
  currentStreakDays: 0,
  lastPracticeDate: null,
});

export function App() {
  const envStatus = useMemo(() => getFirebaseEnvStatus(), []);
  const services = useMemo(() => getFirebaseServices(), []);
  const repo = useMemo(
    () =>
      services ? new FluentMateRepository(services.db, services.auth) : null,
    [services]
  );
  const gemini = useMemo(
    () => (services ? new FluentMateGeminiClient(services.auth) : null),
    [services]
  );

  const [authReady, setAuthReady] = useState(!services);
  const [user, setUser] = useState<User | null>(null);
  const [activeTab, setActiveTab] = useState<AppTab>("dashboard");
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [activeChatSessionId, setActiveChatSessionId] = useState<string | null>(
    null
  );
  const [activeVoiceSessionId, setActiveVoiceSessionId] = useState<
    string | null
  >(null);
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
  const [voiceMessages, setVoiceMessages] = useState<ChatMessage[]>([]);
  const [topics, setTopics] = useState<DailyTopicRecord[]>([]);
  const [reports, setReports] = useState<InterviewReport[]>([]);
  const [words, setWords] = useState<VocabularyWord[]>([]);
  const [dailyStats, setDailyStats] = useState<DailyProgressStat[]>([]);
  const [dailySuggestions, setDailySuggestions] = useState<
    GeneratedWordDraft[]
  >([]);

  const [isSendingChat, setIsSendingChat] = useState(false);
  const [isSendingVoice, setIsSendingVoice] = useState(false);
  const [isGeneratingTopic, setIsGeneratingTopic] = useState(false);
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false);
  const [isSpeaking, setIsSpeaking] = useState(false);

  const [activeReport, setActiveReport] = useState<InterviewReport | null>(
    null
  );
  const [bannerError, setBannerError] = useState<string | null>(null);
  const [bannerToast, setBannerToast] = useState<string | null>(null);

  const showToast = useCallback((msg: string) => {
    setBannerToast(msg);
    window.setTimeout(() => {
      setBannerToast((prev) => (prev === msg ? null : prev));
    }, 3200);
  }, []);

  // Theme application
  const effectiveThemeMode: ThemeMode = profile?.themeMode ?? "DARK";
  useEffect(() => {
    const root = document.documentElement;
    const apply = (dark: boolean) => {
      root.classList.toggle("dark", dark);
    };

    if (effectiveThemeMode === "DARK") {
      apply(true);
      return;
    }
    if (effectiveThemeMode === "LIGHT") {
      apply(false);
      return;
    }

    const mq = window.matchMedia("(prefers-color-scheme: dark)");
    apply(mq.matches);
    const onChange = (e: MediaQueryListEvent) => apply(e.matches);
    mq.addEventListener("change", onChange);
    return () => mq.removeEventListener("change", onChange);
  }, [effectiveThemeMode]);

  // Auth listener
  useEffect(() => {
    if (!services) return;
    const unsub = onAuthStateChanged(services.auth, (nextUser) => {
      setUser(nextUser);
      setAuthReady(true);
    });
    return () => unsub();
  }, [services]);

  // Core Firestore listeners
  useEffect(() => {
    if (!user || !repo) {
      setProfile(null);
      setSessions([]);
      setTopics([]);
      setReports([]);
      setWords([]);
      setDailyStats([]);
      setActiveChatSessionId(null);
      setActiveVoiceSessionId(null);
      return;
    }

    let cancelled = false;
    void repo
      .ensureUserProfile(
        user.uid,
        user.displayName || "Learner",
        user.email || ""
      )
      .catch((err: unknown) => {
        if (!cancelled) {
          setBannerError(
            err instanceof Error
              ? err.message
              : "Failed to initialize user profile."
          );
        }
      });

    const onErr = (err: Error) => {
      if (!cancelled) setBannerError(err.message);
    };

    const unsubs = [
      repo.observeUserProfile(user.uid, setProfile, onErr),
      repo.observeChatSessions(user.uid, setSessions, onErr),
      repo.observeDailyTopics(user.uid, setTopics, onErr),
      repo.observeInterviewReports(user.uid, setReports, onErr),
      repo.observeVocabulary(user.uid, setWords, onErr),
      repo.observeDailyStats(user.uid, setDailyStats, onErr),
    ];

    return () => {
      cancelled = true;
      unsubs.forEach((u) => u());
    };
  }, [user, repo]);

  // Active Chat Messages Listener
  useEffect(() => {
    if (!user || !repo || !activeChatSessionId) {
      setChatMessages([]);
      return;
    }
    return repo.observeMessages(user.uid, activeChatSessionId, setChatMessages, (err) =>
      setBannerError(err.message)
    );
  }, [user, repo, activeChatSessionId]);

  // Active Voice Messages Listener
  useEffect(() => {
    if (!user || !repo || !activeVoiceSessionId) {
      setVoiceMessages([]);
      return;
    }
    return repo.observeMessages(
      user.uid,
      activeVoiceSessionId,
      setVoiceMessages,
      (err) => setBannerError(err.message)
    );
  }, [user, repo, activeVoiceSessionId]);

  // Keep active session selections synced if sessions load or get deleted
  useEffect(() => {
    if (sessions.length === 0) {
      setActiveChatSessionId(null);
      setActiveVoiceSessionId(null);
      return;
    }
    if (
      activeChatSessionId &&
      !sessions.some((s) => s.id === activeChatSessionId)
    ) {
      setActiveChatSessionId(null);
    }
    if (
      activeVoiceSessionId &&
      !sessions.some((s) => s.id === activeVoiceSessionId)
    ) {
      setActiveVoiceSessionId(null);
    }
  }, [sessions, activeChatSessionId, activeVoiceSessionId]);

  // 30-second active practice timer
  useEffect(() => {
    if (!user || !repo) return;
    const id = window.setInterval(() => {
      if (document.visibilityState === "visible") {
        void repo.recordPracticeSeconds(user.uid, 30).catch(() => {});
      }
    }, 30000);
    return () => window.clearInterval(id);
  }, [user, repo]);

  const currentProfile = useMemo(() => {
    if (profile) return profile;
    return user ? DEFAULT_PROFILE_FALLBACK(user) : null;
  }, [profile, user]);

  const activeChatSession = useMemo(
    () => sessions.find((s) => s.id === activeChatSessionId) ?? null,
    [sessions, activeChatSessionId]
  );

  const activeVoiceSession = useMemo(
    () => sessions.find((s) => s.id === activeVoiceSessionId) ?? null,
    [sessions, activeVoiceSessionId]
  );

  const handleSpeakText = useCallback(
    (text: string) => {
      const rate = currentProfile?.speechRate ?? 1.0;
      speakEnglishText(text, rate, {
        onStart: () => setIsSpeaking(true),
        onEnd: () => setIsSpeaking(false),
        onError: (msg) => {
          setIsSpeaking(false);
          setBannerError(msg);
        },
      });
    },
    [currentProfile?.speechRate]
  );

  const handleStopSpeaking = useCallback(() => {
    stopSpeakingEnglish();
    setIsSpeaking(false);
  }, []);

  const handleUpdateSettings = useCallback(
    async (
      updates: Partial<{
        englishLevel: EnglishLevel;
        themeMode: ThemeMode;
        marathiScript: MarathiScript;
        autoSpeakAiReplies: boolean;
        speechRate: number;
        weeklyInterviewFrequency: number;
      }>
    ) => {
      if (!user || !repo) return;
      try {
        await repo.updateSettings(user.uid, updates);
      } catch (err) {
        setBannerError(
          err instanceof Error ? err.message : "Failed to update settings."
        );
      }
    },
    [user, repo]
  );

  const handleSendChatMessage = useCallback(
    async (text: string) => {
      if (!user || !repo || !gemini || !currentProfile) return;
      setIsSendingChat(true);
      setBannerError(null);
      try {
        let sessionId = activeChatSessionId;
        let sessionMeta = activeChatSession;
        if (!sessionId) {
          sessionId = await repo.createChatSession({
            userId: user.uid,
            title: text.slice(0, 42),
            level: currentProfile.englishLevel,
            mode: "CHAT",
          });
          setActiveChatSessionId(sessionId);
        }

        const result = await gemini.analyzeAndReply({
          userMessage: text,
          level: currentProfile.englishLevel,
          marathiScript: currentProfile.marathiScript,
          history: chatMessages,
          topicContext: sessionMeta?.topicTitle ?? null,
          interviewCategory: sessionMeta?.interviewCategory ?? null,
          isVoiceMode: false,
        });

        await repo.saveTutorTurn({
          userId: user.uid,
          sessionId,
          userText: text,
          tutorResult: result,
          level: currentProfile.englishLevel,
          isVoiceMessage: false,
          currentProfile,
        });

        if (sessionMeta?.topicId) {
          const userTurnsSoFar =
            chatMessages.filter((m) => m.role === "user").length + 1;
          await repo.updateTopicProgress({
            userId: user.uid,
            dateKey: sessionMeta.topicId,
            sessionId,
            markCompleted: userTurnsSoFar >= 2,
            turnsIncrement: 1,
          });
        }
      } catch (err) {
        setBannerError(
          err instanceof Error ? err.message : "Failed to get AI response."
        );
      } finally {
        setIsSendingChat(false);
      }
    },
    [
      user,
      repo,
      gemini,
      currentProfile,
      activeChatSessionId,
      activeChatSession,
      chatMessages,
    ]
  );

  const handleSendVoiceMessage = useCallback(
    async (text: string) => {
      if (!user || !repo || !gemini || !currentProfile) return;
      setIsSendingVoice(true);
      setBannerError(null);
      try {
        let sessionId = activeVoiceSessionId;
        const sessionMeta = activeVoiceSession;
        if (!sessionId) {
          sessionId = await repo.createChatSession({
            userId: user.uid,
            title: `Voice: ${text.slice(0, 34)}`,
            level: currentProfile.englishLevel,
            mode: "VOICE",
          });
          setActiveVoiceSessionId(sessionId);
        }

        const result = await gemini.analyzeAndReply({
          userMessage: text,
          level: currentProfile.englishLevel,
          marathiScript: currentProfile.marathiScript,
          history: voiceMessages,
          topicContext: sessionMeta?.topicTitle ?? null,
          interviewCategory: sessionMeta?.interviewCategory ?? null,
          isVoiceMode: true,
        });

        await repo.saveTutorTurn({
          userId: user.uid,
          sessionId,
          userText: text,
          tutorResult: result,
          level: currentProfile.englishLevel,
          isVoiceMessage: true,
          currentProfile,
        });

        if (sessionMeta?.topicId) {
          const userTurnsSoFar =
            voiceMessages.filter((m) => m.role === "user").length + 1;
          await repo.updateTopicProgress({
            userId: user.uid,
            dateKey: sessionMeta.topicId,
            sessionId,
            markCompleted: userTurnsSoFar >= 2,
            turnsIncrement: 1,
          });
        }

        if (currentProfile.autoSpeakAiReplies) {
          handleSpeakText(result.assistantReply);
        }
      } catch (err) {
        setBannerError(
          err instanceof Error
            ? err.message
            : "Failed to analyze spoken English."
        );
      } finally {
        setIsSendingVoice(false);
      }
    },
    [
      user,
      repo,
      gemini,
      currentProfile,
      activeVoiceSessionId,
      activeVoiceSession,
      voiceMessages,
      handleSpeakText,
    ]
  );

  const handleGenerateTopic = useCallback(
    async (preferredCategory: string | null) => {
      if (!user || !repo || !gemini || !currentProfile) return;
      setIsGeneratingTopic(true);
      setBannerError(null);
      try {
        const now = new Date();
        const dateKey = todayDateKey();
        const dayNames = [
          "Sunday",
          "Monday",
          "Tuesday",
          "Wednesday",
          "Thursday",
          "Friday",
          "Saturday",
        ];
        const dayOfWeekName = dayNames[now.getDay()] ?? "Monday";
        const freq = Math.min(
          2,
          Math.max(1, currentProfile.weeklyInterviewFrequency)
        );
        const scheduledType = defaultSessionTypeForJsDay(now.getDay(), freq);
        const interviewsThisWeek = countInterviewsThisWeek(
          topics,
          sessions,
          dateKey
        );

        const draft = await gemini.generateDailyTopic({
          level: currentProfile.englishLevel,
          dayOfWeekName,
          scheduledSessionType: scheduledType,
          interviewsThisWeek,
          weeklyInterviewFrequency: freq,
          preferredCategory,
          recentTopicTitles: topics.slice(0, 8).map((t) => t.title),
          dateKey,
          allowScheduleAdjustment: true,
        });

        await repo.saveDailyTopic(
          user.uid,
          dateKey,
          draft,
          currentProfile.englishLevel
        );
        showToast("Today's topic is ready!");
      } catch (err) {
        setBannerError(
          err instanceof Error ? err.message : "Could not generate daily topic."
        );
      } finally {
        setIsGeneratingTopic(false);
      }
    },
    [user, repo, gemini, currentProfile, topics, sessions, showToast]
  );

  const handleStartTopic = useCallback(
    async (topic: DailyTopicRecord, voiceMode: boolean) => {
      if (!user || !repo || !currentProfile) return;
      setBannerError(null);
      try {
        const isInterview = topic.sessionType === "INTERVIEW_PRACTICE";
        const sessionId = await repo.createChatSession({
          userId: user.uid,
          title: topic.title,
          level: currentProfile.englishLevel,
          mode: isInterview ? "INTERVIEW" : voiceMode ? "VOICE" : "TOPIC",
          topicId: topic.dateKey,
          topicTitle: topic.title,
          interviewCategory: topic.interviewCategory,
          initialAssistantGreeting: topic.starterQuestion,
          isVoiceInitial: voiceMode,
        });

        await repo.updateTopicProgress({
          userId: user.uid,
          dateKey: topic.dateKey,
          sessionId,
          markCompleted: false,
        });

        if (voiceMode) {
          setActiveVoiceSessionId(sessionId);
          setActiveTab("voice");
          if (currentProfile.autoSpeakAiReplies) {
            handleSpeakText(topic.starterQuestion);
          }
        } else {
          setActiveChatSessionId(sessionId);
          setActiveTab("chat");
        }
      } catch (err) {
        setBannerError(
          err instanceof Error ? err.message : "Failed to start topic session."
        );
      }
    },
    [user, repo, currentProfile, handleSpeakText]
  );

  const handleStartInterview = useCallback(
    async (categoryTitle: string, voiceMode: boolean) => {
      if (!user || !repo || !gemini || !currentProfile) return;
      const weeklyFreq = Math.min(
        2,
        Math.max(1, currentProfile.weeklyInterviewFrequency)
      );
      const used = countInterviewsThisWeek(topics, sessions);
      if (used >= weeklyFreq) {
        setBannerError(
          `Weekly interview limit (${used}/${weeklyFreq}) reached. Update frequency in Settings or resume an existing interview.`
        );
        return;
      }

      setBannerError(null);
      try {
        const opening = await gemini.analyzeAndReply({
          userMessage:
            "Start our mock interview now. Greet me warmly as the interviewer and ask Question #1.",
          level: currentProfile.englishLevel,
          marathiScript: currentProfile.marathiScript,
          history: [],
          interviewCategory: categoryTitle,
          isVoiceMode: voiceMode,
        });

        const sessionId = await repo.createChatSession({
          userId: user.uid,
          title: categoryTitle,
          level: currentProfile.englishLevel,
          mode: "INTERVIEW",
          interviewCategory: categoryTitle,
          initialAssistantGreeting: opening.assistantReply,
          isVoiceInitial: voiceMode,
        });

        if (voiceMode) {
          setActiveVoiceSessionId(sessionId);
          setActiveTab("voice");
          if (currentProfile.autoSpeakAiReplies) {
            handleSpeakText(opening.assistantReply);
          }
        } else {
          setActiveChatSessionId(sessionId);
          setActiveTab("chat");
        }
      } catch (err) {
        setBannerError(
          err instanceof Error ? err.message : "Failed to start interview."
        );
      }
    },
    [user, repo, gemini, currentProfile, topics, sessions, handleSpeakText]
  );

  const handleFinishInterview = useCallback(
    async (session: ChatSession, sessionMessages: ChatMessage[]) => {
      if (!user || !repo || !gemini || !currentProfile) return;
      const userAnswers = sessionMessages.filter(
        (m) => m.role === "user"
      ).length;
      if (userAnswers === 0) return;

      setIsGeneratingReport(true);
      setBannerError(null);
      try {
        const category = session.interviewCategory || session.title;
        const draft = await gemini.generateInterviewReport({
          interviewCategory: category,
          level: currentProfile.englishLevel,
          marathiScript: currentProfile.marathiScript,
          transcript: sessionMessages,
        });

        const saved = await repo.saveInterviewReport({
          userId: user.uid,
          sessionId: session.id,
          interviewCategory: category,
          level: currentProfile.englishLevel,
          draft,
          questionsAnswered: userAnswers,
        });
        setActiveReport(saved);
      } catch (err) {
        setBannerError(
          err instanceof Error
            ? err.message
            : "Failed to generate interview report."
        );
      } finally {
        setIsGeneratingReport(false);
      }
    },
    [user, repo, gemini, currentProfile]
  );

  const handleSaveWord = useCallback(
    async (draft: GeneratedWordDraft, source: VocabSource) => {
      if (!user || !repo || !currentProfile) return;
      try {
        await repo.addVocabularyWord({
          userId: user.uid,
          draft,
          level: currentProfile.englishLevel,
          source,
        });
        showToast(`Saved "${draft.word}" to Vocabulary!`);
      } catch (err) {
        setBannerError(
          err instanceof Error ? err.message : "Failed to save word."
        );
      }
    },
    [user, repo, currentProfile, showToast]
  );

  const handleGenerateDailyWords = useCallback(async () => {
    if (!gemini || !currentProfile) return;
    setIsLoadingSuggestions(true);
    setBannerError(null);
    try {
      const list = await gemini.generateDailyVocabulary({
        level: currentProfile.englishLevel,
        existingWords: words.map((w) => w.word),
        marathiScript: currentProfile.marathiScript,
      });
      setDailySuggestions(list);
    } catch (err) {
      setBannerError(
        err instanceof Error ? err.message : "Failed to generate daily words."
      );
    } finally {
      setIsLoadingSuggestions(false);
    }
  }, [gemini, currentProfile, words]);

  const handleAiFillWord = useCallback(
    async (wordText: string): Promise<GeneratedWordDraft> => {
      if (!gemini || !currentProfile) {
        throw new Error("Sign in required.");
      }
      return gemini.enrichVocabularyWord({
        word: wordText,
        level: currentProfile.englishLevel,
        marathiScript: currentProfile.marathiScript,
      });
    },
    [gemini, currentProfile]
  );

  if (!authReady) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[var(--fm-bg)]">
        <div className="flex items-center gap-3 text-sm font-medium text-[var(--fm-text-muted)]">
          <Loader2 className="h-5 w-5 animate-spin text-[var(--fm-primary)]" />
          <span>Loading FluentMate AI...</span>
        </div>
      </div>
    );
  }

  if (!user || !currentProfile || !services || !repo) {
    return <AuthGate auth={services?.auth ?? null} envStatus={envStatus} />;
  }

  const navItems: Array<{
    id: AppTab;
    label: string;
    shortLabel: string;
    Icon: typeof Home;
  }> = [
    { id: "dashboard", label: "Dashboard", shortLabel: "Home", Icon: Home },
    { id: "chat", label: "AI Chat", shortLabel: "Chat", Icon: MessageSquare },
    { id: "voice", label: "Voice Practice", shortLabel: "Voice", Icon: Mic },
    {
      id: "topics",
      label: "Daily Topics & Interviews",
      shortLabel: "Topics",
      Icon: CalendarDays,
    },
    { id: "vocabulary", label: "Vocabulary", shortLabel: "Vocab", Icon: BookOpen },
    { id: "progress", label: "Progress", shortLabel: "Progress", Icon: BarChart3 },
    { id: "settings", label: "Settings", shortLabel: "Settings", Icon: Settings },
  ];

  const primaryBottomItems = navItems.slice(0, 5);

  return (
    <div className="flex min-h-screen bg-[var(--fm-bg)] text-[var(--fm-text)]">
      {/* Desktop Sidebar */}
      <aside className="hidden w-64 shrink-0 flex-col border-r border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4 lg:flex">
        <div className="flex items-center gap-3 px-2 py-2">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-[var(--fm-primary)] to-[var(--fm-secondary)] text-white shadow-sm">
            <Sparkles className="h-5 w-5" />
          </div>
          <div>
            <div className="font-display text-base font-bold leading-tight text-[var(--fm-text)]">
              FluentMate AI
            </div>
            <div className="text-[11px] text-[var(--fm-text-muted)]">
              English &amp; Marathi Tutor
            </div>
          </div>
        </div>

        <nav className="mt-6 flex-1 space-y-1">
          {navItems.map(({ id, label, Icon }) => {
            const active = activeTab === id;
            return (
              <button
                key={id}
                type="button"
                onClick={() => setActiveTab(id)}
                className={`flex min-h-11 w-full items-center gap-3 rounded-2xl px-3.5 py-2.5 text-sm font-semibold transition cursor-pointer ${
                  active
                    ? "bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                    : "text-[var(--fm-text-muted)] hover:bg-[var(--fm-surface-variant)] hover:text-[var(--fm-text)]"
                }`}
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span className="truncate">{label}</span>
              </button>
            );
          })}
        </nav>

        <div className="mt-4 rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/50 p-3">
          <div className="truncate text-xs font-bold text-[var(--fm-text)]">
            {currentProfile.displayName}
          </div>
          <div className="mt-0.5 flex items-center justify-between text-[11px] text-[var(--fm-text-muted)]">
            <span>Level {currentProfile.englishLevel}</span>
            <span>{currentProfile.currentStreakDays}d streak</span>
          </div>
        </div>
      </aside>

      {/* Main Content Shell */}
      <div className="flex min-w-0 flex-1 flex-col">
        {/* Mobile Top Bar */}
        <header className="sticky top-0 z-30 flex items-center justify-between border-b border-[var(--fm-outline-variant)] bg-[var(--fm-surface)]/95 px-4 py-2.5 backdrop-blur-sm lg:hidden">
          <div className="flex items-center gap-2.5">
            <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-[var(--fm-primary)] to-[var(--fm-secondary)] text-white">
              <Sparkles className="h-4 w-4" />
            </div>
            <span className="font-display text-base font-bold text-[var(--fm-text)]">
              FluentMate AI
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="rounded-full bg-[var(--fm-primary-container)] px-2.5 py-1 font-mono-code text-[11px] font-bold text-[var(--fm-on-primary-container)]">
              {currentProfile.englishLevel}
            </span>
            <button
              type="button"
              onClick={() => setMobileMenuOpen(true)}
              aria-label="Open menu"
              className="flex h-10 w-10 items-center justify-center rounded-xl border border-[var(--fm-outline-variant)] text-[var(--fm-text)] cursor-pointer"
            >
              <Menu className="h-5 w-5" />
            </button>
          </div>
        </header>

        {/* Alerts & Toasts */}
        {bannerError && (
          <div className="mx-4 mt-3 flex items-center justify-between gap-3 rounded-2xl border border-[var(--fm-error)]/45 bg-[var(--fm-error-container)] px-4 py-3 text-xs text-[var(--fm-on-error-container)] lg:mx-6">
            <span>{bannerError}</span>
            <button
              type="button"
              onClick={() => setBannerError(null)}
              aria-label="Dismiss error"
              className="shrink-0 font-bold cursor-pointer"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
        )}

        {bannerToast && (
          <div className="mx-4 mt-3 rounded-2xl border border-[var(--fm-primary)]/45 bg-[var(--fm-primary-container)] px-4 py-2.5 text-xs font-semibold text-[var(--fm-on-primary-container)] lg:mx-6">
            {bannerToast}
          </div>
        )}

        {/* Active Page */}
        <main className="flex-1 px-4 py-5 pb-24 lg:px-6 lg:py-6 lg:pb-6">
          {activeTab === "dashboard" && (
            <DashboardPage
              profile={currentProfile}
              dailyStats={dailyStats}
              topics={topics}
              isGeneratingTopic={isGeneratingTopic}
              onSelectLevel={(englishLevel) =>
                void handleUpdateSettings({ englishLevel })
              }
              onOpenChat={() => setActiveTab("chat")}
              onOpenVoice={() => setActiveTab("voice")}
              onOpenTopics={() => setActiveTab("topics")}
              onGenerateTodayTopic={() => void handleGenerateTopic(null)}
              onPracticeTopicInChat={(topic) =>
                void handleStartTopic(topic, false)
              }
              onPracticeTopicInVoice={(topic) =>
                void handleStartTopic(topic, true)
              }
              onMarkTopicDone={(dateKey) =>
                void repo.updateTopicProgress({
                  userId: user.uid,
                  dateKey,
                  markCompleted: true,
                })
              }
            />
          )}

          {activeTab === "chat" && (
            <AiChatPage
              profile={currentProfile}
              sessions={sessions}
              activeSession={activeChatSession}
              messages={chatMessages}
              isSending={isSendingChat}
              isGeneratingReport={isGeneratingReport}
              onSelectLevel={(englishLevel) =>
                void handleUpdateSettings({ englishLevel })
              }
              onSelectSession={(id) => setActiveChatSessionId(id)}
              onCreateNewChat={() => setActiveChatSessionId(null)}
              onSendMessage={handleSendChatMessage}
              onClearChat={async (id) => {
                await repo.clearSessionMessages(user.uid, id);
                showToast("Chat messages cleared.");
              }}
              onDeleteSession={async (id) => {
                await repo.deleteSession(user.uid, id);
                if (activeChatSessionId === id) setActiveChatSessionId(null);
              }}
              onSaveWord={(draft) => handleSaveWord(draft, "CHAT")}
              onSpeakText={handleSpeakText}
              onFinishInterview={handleFinishInterview}
            />
          )}

          {activeTab === "voice" && (
            <VoicePracticePage
              profile={currentProfile}
              sessions={sessions}
              activeSession={activeVoiceSession}
              messages={voiceMessages}
              isSending={isSendingVoice}
              isSpeaking={isSpeaking}
              isGeneratingReport={isGeneratingReport}
              onSelectLevel={(englishLevel) =>
                void handleUpdateSettings({ englishLevel })
              }
              onSelectSession={(id) => setActiveVoiceSessionId(id)}
              onCreateNewVoiceSession={() => setActiveVoiceSessionId(null)}
              onSendVoiceMessage={handleSendVoiceMessage}
              onSpeakText={handleSpeakText}
              onStopSpeaking={handleStopSpeaking}
              onSaveWord={(draft) => handleSaveWord(draft, "CHAT")}
              onFinishInterview={handleFinishInterview}
            />
          )}

          {activeTab === "topics" && (
            <DailyTopicsPage
              profile={currentProfile}
              topics={topics}
              sessions={sessions}
              reports={reports}
              isGeneratingTopic={isGeneratingTopic}
              onGenerateTopic={handleGenerateTopic}
              onStartTopicInChat={(topic) =>
                void handleStartTopic(topic, false)
              }
              onStartTopicInVoice={(topic) =>
                void handleStartTopic(topic, true)
              }
              onMarkTopicCompleted={(dateKey) =>
                void repo.updateTopicProgress({
                  userId: user.uid,
                  dateKey,
                  markCompleted: true,
                })
              }
              onStartInterview={handleStartInterview}
              onResumeInterviewSession={(sess) => {
                if (sess.mode === "VOICE") {
                  setActiveVoiceSessionId(sess.id);
                  setActiveTab("voice");
                } else {
                  setActiveChatSessionId(sess.id);
                  setActiveTab("chat");
                }
              }}
              onOpenReport={(report) => setActiveReport(report)}
            />
          )}

          {activeTab === "vocabulary" && (
            <VocabularyPage
              profile={currentProfile}
              words={words}
              dailySuggestions={dailySuggestions}
              isLoadingSuggestions={isLoadingSuggestions}
              onGenerateDailyWords={handleGenerateDailyWords}
              onSaveSuggestion={(draft) => handleSaveWord(draft, "DAILY")}
              onAddManualWord={(draft) => handleSaveWord(draft, "MANUAL")}
              onAiFillWord={handleAiFillWord}
              onToggleLearned={(word) =>
                repo.toggleWordLearned(user.uid, word.id, !word.learned)
              }
              onDeleteWord={(word) =>
                repo.deleteVocabularyWord(user.uid, word)
              }
              onSpeakText={handleSpeakText}
            />
          )}

          {activeTab === "progress" && (
            <ProgressPage
              profile={currentProfile}
              dailyStats={dailyStats}
              sessions={sessions}
              topics={topics}
              words={words}
              reports={reports}
            />
          )}

          {activeTab === "settings" && (
            <SettingsPage
              profile={currentProfile}
              userEmail={user.email || ""}
              onUpdateSettings={handleUpdateSettings}
              onTestVoice={handleSpeakText}
              onSignOut={() => signOutUser(services.auth)}
            />
          )}
        </main>
      </div>

      {/* Mobile Bottom Navigation */}
      <nav
        aria-label="Primary Navigation"
        className="fixed inset-x-0 bottom-0 z-30 grid grid-cols-5 border-t border-[var(--fm-outline-variant)] bg-[var(--fm-surface)]/95 backdrop-blur-sm lg:hidden"
      >
        {primaryBottomItems.map(({ id, shortLabel, Icon }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              type="button"
              onClick={() => setActiveTab(id)}
              className={`flex min-h-14 flex-col items-center justify-center gap-0.5 text-[11px] font-semibold cursor-pointer ${
                active
                  ? "text-[var(--fm-primary)]"
                  : "text-[var(--fm-text-muted)]"
              }`}
            >
              <Icon className="h-5 w-5" />
              <span>{shortLabel}</span>
            </button>
          );
        })}
      </nav>

      {/* Mobile Drawer for Progress & Settings */}
      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-xs lg:hidden"
          role="dialog"
          aria-modal="true"
          aria-label="Navigation Menu"
        >
          <div
            className="flex-1"
            onClick={() => setMobileMenuOpen(false)}
            aria-hidden="true"
          />
          <div className="flex h-full w-72 flex-col border-l border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--fm-outline-variant)] pb-3">
              <span className="font-display text-base font-bold">
                FluentMate AI
              </span>
              <button
                type="button"
                onClick={() => setMobileMenuOpen(false)}
                className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-[var(--fm-surface-variant)] cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="mt-4 space-y-1.5">
              {navItems.map(({ id, label, Icon }) => {
                const active = activeTab === id;
                return (
                  <button
                    key={id}
                    type="button"
                    onClick={() => {
                      setActiveTab(id);
                      setMobileMenuOpen(false);
                    }}
                    className={`flex min-h-11 w-full items-center gap-3 rounded-2xl px-3.5 py-2 text-sm font-semibold cursor-pointer ${
                      active
                        ? "bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                        : "text-[var(--fm-text)] hover:bg-[var(--fm-surface-variant)]"
                    }`}
                  >
                    <Icon className="h-4 w-4" />
                    <span>{label}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Global Interview Report Modal */}
      {activeReport && (
        <InterviewReportModal
          report={activeReport}
          onClose={() => setActiveReport(null)}
          onSpeakText={handleSpeakText}
        />
      )}
    </div>
  );
}

export default App;
