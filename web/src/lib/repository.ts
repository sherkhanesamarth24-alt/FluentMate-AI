import { Auth } from "firebase/auth";
import {
  collection,
  doc,
  Firestore,
  getDoc,
  getDocs,
  increment,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
  Unsubscribe,
  updateDoc,
  writeBatch,
} from "firebase/firestore";
import {
  ChatMessage,
  ChatSession,
  DailyProgressStat,
  DailySessionType,
  DailyTopicRecord,
  EnglishLevel,
  GeneratedInterviewReportDraft,
  GeneratedTopicDraft,
  GeneratedWordDraft,
  InterviewReport,
  MarathiScript,
  SessionMode,
  ThemeMode,
  TutorTurnResult,
  UserProfile,
  VocabSource,
  VocabularyWord,
  WeeklyDayScheduleItem,
} from "../types/models";

export function formatDateKey(date: Date = new Date()): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

export function todayDateKey(): string {
  return formatDateKey(new Date());
}

export function yesterdayDateKey(): string {
  const d = new Date();
  d.setDate(d.getDate() - 1);
  return formatDateKey(d);
}

function timestampToMs(val: unknown): number | undefined {
  if (val instanceof Timestamp) return val.toMillis();
  if (
    typeof val === "object" &&
    val !== null &&
    "toMillis" in val &&
    typeof (val as { toMillis: () => number }).toMillis === "function"
  ) {
    return (val as { toMillis: () => number }).toMillis();
  }
  return undefined;
}

export function startOfCurrentWeekDate(now: Date = new Date()): Date {
  const d = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const day = d.getDay(); // 0=Sun, 1=Mon
  const diffToMonday = day === 0 ? -6 : 1 - day;
  d.setDate(d.getDate() + diffToMonday);
  return d;
}

export function defaultSessionTypeForJsDay(
  jsDay: number,
  frequency: number
): DailySessionType {
  // 0=Sun, 1=Mon, 2=Tue, 3=Wed, 4=Thu, 5=Fri, 6=Sat
  switch (jsDay) {
    case 2:
      return frequency >= 2 ? "INTERVIEW_PRACTICE" : "DAILY_CONVERSATION";
    case 5:
      return "INTERVIEW_PRACTICE";
    case 6:
      return "GENERAL_CONVERSATION";
    case 0:
      return "REVISION";
    default:
      return "DAILY_CONVERSATION";
  }
}

export function buildWeeklyScheduleItems(
  topics: DailyTopicRecord[],
  frequency: number
): WeeklyDayScheduleItem[] {
  const clampedFreq = Math.min(2, Math.max(1, frequency));
  const todayKey = todayDateKey();
  const monday = startOfCurrentWeekDate();
  const topicByDate = new Map(topics.map((t) => [t.dateKey, t]));

  const days: Array<{ jsDay: number; short: string; full: string }> = [
    { jsDay: 1, short: "Mon", full: "Monday" },
    { jsDay: 2, short: "Tue", full: "Tuesday" },
    { jsDay: 3, short: "Wed", full: "Wednesday" },
    { jsDay: 4, short: "Thu", full: "Thursday" },
    { jsDay: 5, short: "Fri", full: "Friday" },
    { jsDay: 6, short: "Sat", full: "Saturday" },
    { jsDay: 0, short: "Sun", full: "Sunday" },
  ];

  return days.map((d, idx) => {
    const dt = new Date(monday);
    dt.setDate(monday.getDate() + idx);
    const dateKey = formatDateKey(dt);
    const saved = topicByDate.get(dateKey);
    const sessionType =
      saved?.sessionType ?? defaultSessionTypeForJsDay(d.jsDay, clampedFreq);
    return {
      dateKey,
      dayShort: d.short,
      dayFull: d.full,
      sessionType,
      isToday: dateKey === todayKey,
      isCompleted: Boolean(saved?.completed),
    };
  });
}

export function countInterviewsThisWeek(
  topics: DailyTopicRecord[],
  sessions: ChatSession[],
  excludeDateKey?: string
): number {
  const monday = startOfCurrentWeekDate();
  const weekKeys = new Set<string>();
  for (let i = 0; i < 7; i += 1) {
    const dt = new Date(monday);
    dt.setDate(monday.getDate() + i);
    weekKeys.add(formatDateKey(dt));
  }

  const topicDates = new Set(
    topics
      .filter(
        (t) =>
          weekKeys.has(t.dateKey) &&
          t.dateKey !== excludeDateKey &&
          t.sessionType === "INTERVIEW_PRACTICE"
      )
      .map((t) => t.dateKey)
  );

  const standaloneSessions = sessions.filter((s) => {
    const isInterview = s.mode === "INTERVIEW" || Boolean(s.interviewCategory);
    const notLinked = !s.topicId;
    const dateKey = s.createdAtMs
      ? formatDateKey(new Date(s.createdAtMs))
      : null;
    return isInterview && notLinked && (!dateKey || weekKeys.has(dateKey));
  }).length;

  return topicDates.size + standaloneSessions;
}

export class FluentMateRepository {
  constructor(
    private readonly db: Firestore,
    private readonly auth: Auth
  ) {}

  private requireUserId(userId: string): string {
    const uid = this.auth.currentUser?.uid;
    if (!uid || uid !== userId) {
      throw new Error("Please sign in with Google before accessing data.");
    }
    return uid;
  }

  observeUserProfile(
    userId: string,
    onData: (profile: UserProfile | null) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const ref = doc(this.db, "users", userId);
    return onSnapshot(
      ref,
      (snap) => {
        if (!snap.exists()) {
          onData(null);
          return;
        }
        const d = snap.data({ serverTimestamps: "estimate" });
        onData({
          id: snap.id,
          userId: String(d.userId ?? userId),
          displayName: String(d.displayName ?? "Learner"),
          email: String(d.email ?? ""),
          englishLevel: (d.englishLevel as EnglishLevel) ?? "LOW",
          themeMode: (d.themeMode as ThemeMode) ?? "DARK",
          marathiScript: (d.marathiScript as MarathiScript) ?? "ROMAN",
          autoSpeakAiReplies: Boolean(d.autoSpeakAiReplies ?? true),
          speechRate: Number(d.speechRate ?? 1.0),
          weeklyInterviewFrequency: Number(d.weeklyInterviewFrequency ?? 2),
          totalConversations: Number(d.totalConversations ?? 0),
          totalMessagesSent: Number(d.totalMessagesSent ?? 0),
          grammarMistakesDetected: Number(d.grammarMistakesDetected ?? 0),
          topicsCompletedCount: Number(d.topicsCompletedCount ?? 0),
          vocabularyLearnedCount: Number(d.vocabularyLearnedCount ?? 0),
          totalPracticeSeconds: Number(d.totalPracticeSeconds ?? 0),
          currentStreakDays: Number(d.currentStreakDays ?? 0),
          lastPracticeDate:
            typeof d.lastPracticeDate === "string" ? d.lastPracticeDate : null,
        });
      },
      (err) => onError(err)
    );
  }

  observeChatSessions(
    userId: string,
    onData: (sessions: ChatSession[]) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const q = query(
      collection(this.db, "users", userId, "chatSessions"),
      orderBy("updatedAt", "desc")
    );
    return onSnapshot(
      q,
      (snap) => {
        const list: ChatSession[] = snap.docs.map((docSnap) => {
          const d = docSnap.data({ serverTimestamps: "estimate" });
          return {
            id: docSnap.id,
            userId: String(d.userId ?? userId),
            title: String(d.title ?? "English Chat"),
            topicId: typeof d.topicId === "string" ? d.topicId : null,
            topicTitle: typeof d.topicTitle === "string" ? d.topicTitle : null,
            interviewCategory:
              typeof d.interviewCategory === "string"
                ? d.interviewCategory
                : null,
            englishLevel: (d.englishLevel as EnglishLevel) ?? "LOW",
            mode: (d.mode as SessionMode) ?? "CHAT",
            messageCount: Number(d.messageCount ?? 0),
            mistakesCount: Number(d.mistakesCount ?? 0),
            lastMessagePreview: String(d.lastMessagePreview ?? ""),
            createdAtMs: timestampToMs(d.createdAt),
            updatedAtMs: timestampToMs(d.updatedAt),
          };
        });
        onData(list);
      },
      (err) => onError(err)
    );
  }

  observeMessages(
    userId: string,
    sessionId: string,
    onData: (messages: ChatMessage[]) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const q = query(
      collection(
        this.db,
        "users",
        userId,
        "chatSessions",
        sessionId,
        "messages"
      ),
      orderBy("createdAt", "asc")
    );
    return onSnapshot(
      q,
      (snap) => {
        const list: ChatMessage[] = snap.docs.map((docSnap) => {
          const d = docSnap.data({ serverTimestamps: "estimate" });
          return {
            id: docSnap.id,
            userId: String(d.userId ?? userId),
            sessionId: String(d.sessionId ?? sessionId),
            role: d.role === "assistant" ? "assistant" : "user",
            text: String(d.text ?? ""),
            englishLevel: (d.englishLevel as EnglishLevel) ?? "LOW",
            hasMistake: Boolean(d.hasMistake ?? false),
            originalSentence:
              typeof d.originalSentence === "string"
                ? d.originalSentence
                : null,
            correctedSentence:
              typeof d.correctedSentence === "string"
                ? d.correctedSentence
                : null,
            marathiExplanation:
              typeof d.marathiExplanation === "string"
                ? d.marathiExplanation
                : null,
            naturalAlternative:
              typeof d.naturalAlternative === "string"
                ? d.naturalAlternative
                : null,
            mistakeCategory:
              typeof d.mistakeCategory === "string" ? d.mistakeCategory : null,
            suggestedWord:
              typeof d.suggestedWord === "string" ? d.suggestedWord : null,
            suggestedWordMarathi:
              typeof d.suggestedWordMarathi === "string"
                ? d.suggestedWordMarathi
                : null,
            suggestedWordExample:
              typeof d.suggestedWordExample === "string"
                ? d.suggestedWordExample
                : null,
            suggestedWordPronunciation:
              typeof d.suggestedWordPronunciation === "string"
                ? d.suggestedWordPronunciation
                : null,
            suggestedWordPos:
              typeof d.suggestedWordPos === "string"
                ? d.suggestedWordPos
                : null,
            voiceMessage: Boolean(d.voiceMessage ?? false),
            createdAtMs: timestampToMs(d.createdAt),
          };
        });
        onData(list);
      },
      (err) => onError(err)
    );
  }

  observeDailyTopics(
    userId: string,
    onData: (topics: DailyTopicRecord[]) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const q = query(
      collection(this.db, "users", userId, "dailyTopics"),
      orderBy("dateKey", "desc")
    );
    return onSnapshot(
      q,
      (snap) => {
        const list: DailyTopicRecord[] = snap.docs.map((docSnap) => {
          const d = docSnap.data({ serverTimestamps: "estimate" });
          return {
            id: docSnap.id,
            userId: String(d.userId ?? userId),
            dateKey: String(d.dateKey ?? docSnap.id),
            title: String(d.title ?? ""),
            category: String(d.category ?? "Daily Life"),
            sessionType:
              (d.sessionType as DailySessionType) ?? "DAILY_CONVERSATION",
            interviewCategory:
              typeof d.interviewCategory === "string"
                ? d.interviewCategory
                : null,
            scheduleAdjustedNote:
              typeof d.scheduleAdjustedNote === "string"
                ? d.scheduleAdjustedNote
                : null,
            englishLevel: (d.englishLevel as EnglishLevel) ?? "LOW",
            promptSummary: String(d.promptSummary ?? ""),
            starterQuestion: String(d.starterQuestion ?? ""),
            marathiHint: String(d.marathiHint ?? ""),
            completed: Boolean(d.completed ?? false),
            sessionId: typeof d.sessionId === "string" ? d.sessionId : null,
            turnsCompleted: Number(d.turnsCompleted ?? 0),
          };
        });
        onData(list);
      },
      (err) => onError(err)
    );
  }

  observeInterviewReports(
    userId: string,
    onData: (reports: InterviewReport[]) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const q = query(
      collection(this.db, "users", userId, "interviewReports"),
      orderBy("createdAt", "desc")
    );
    return onSnapshot(
      q,
      (snap) => {
        const list: InterviewReport[] = snap.docs.map((docSnap) => {
          const d = docSnap.data({ serverTimestamps: "estimate" });
          return {
            id: docSnap.id,
            userId: String(d.userId ?? userId),
            sessionId: String(d.sessionId ?? ""),
            interviewCategory: String(d.interviewCategory ?? "HR Interview"),
            englishLevel: (d.englishLevel as EnglishLevel) ?? "LOW",
            overallSummary: String(d.overallSummary ?? ""),
            grammarImprovement: String(d.grammarImprovement ?? ""),
            vocabularyFeedback: String(d.vocabularyFeedback ?? ""),
            sentenceFormation: String(d.sentenceFormation ?? ""),
            answerClarity: String(d.answerClarity ?? ""),
            communicationFeedback: String(d.communicationFeedback ?? ""),
            suggestedBetterAnswers: String(d.suggestedBetterAnswers ?? ""),
            areasToImprove: String(d.areasToImprove ?? ""),
            marathiCoachNote: String(d.marathiCoachNote ?? ""),
            questionsAnswered: Number(d.questionsAnswered ?? 0),
            createdAtMs: timestampToMs(d.createdAt),
          };
        });
        onData(list);
      },
      (err) => onError(err)
    );
  }

  observeVocabulary(
    userId: string,
    onData: (words: VocabularyWord[]) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const q = query(
      collection(this.db, "users", userId, "vocabulary"),
      orderBy("createdAt", "desc")
    );
    return onSnapshot(
      q,
      (snap) => {
        const list: VocabularyWord[] = snap.docs.map((docSnap) => {
          const d = docSnap.data({ serverTimestamps: "estimate" });
          return {
            id: docSnap.id,
            userId: String(d.userId ?? userId),
            word: String(d.word ?? ""),
            marathiMeaning: String(d.marathiMeaning ?? ""),
            exampleSentence: String(d.exampleSentence ?? ""),
            pronunciation: String(d.pronunciation ?? ""),
            partOfSpeech: String(d.partOfSpeech ?? "word"),
            englishLevel: (d.englishLevel as EnglishLevel) ?? "LOW",
            dateLearned: String(d.dateLearned ?? ""),
            learned: Boolean(d.learned ?? false),
            source: (d.source as VocabSource) ?? "MANUAL",
          };
        });
        onData(list);
      },
      (err) => onError(err)
    );
  }

  observeDailyStats(
    userId: string,
    onData: (stats: DailyProgressStat[]) => void,
    onError: (err: Error) => void
  ): Unsubscribe {
    const q = query(
      collection(this.db, "users", userId, "dailyStats"),
      orderBy("dateKey", "desc"),
      limit(14)
    );
    return onSnapshot(
      q,
      (snap) => {
        const list: DailyProgressStat[] = snap.docs.map((docSnap) => {
          const d = docSnap.data({ serverTimestamps: "estimate" });
          return {
            id: docSnap.id,
            userId: String(d.userId ?? userId),
            dateKey: String(d.dateKey ?? docSnap.id),
            practiceSeconds: Number(d.practiceSeconds ?? 0),
            messagesCount: Number(d.messagesCount ?? 0),
            mistakesCount: Number(d.mistakesCount ?? 0),
            wordsLearnedCount: Number(d.wordsLearnedCount ?? 0),
            topicsCompletedCount: Number(d.topicsCompletedCount ?? 0),
          };
        });
        onData(list);
      },
      (err) => onError(err)
    );
  }

  async ensureUserProfile(
    userId: string,
    displayName: string,
    email: string
  ): Promise<void> {
    const uid = this.requireUserId(userId);
    const ref = doc(this.db, "users", uid);
    const snap = await getDoc(ref);
    if (!snap.exists()) {
      await setDoc(ref, {
        userId: uid,
        displayName: displayName.trim().slice(0, 100) || "English Learner",
        email: email.trim().slice(0, 160),
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
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
  }

  async updateSettings(
    userId: string,
    updates: Partial<{
      englishLevel: EnglishLevel;
      themeMode: ThemeMode;
      marathiScript: MarathiScript;
      autoSpeakAiReplies: boolean;
      speechRate: number;
      weeklyInterviewFrequency: number;
    }>
  ): Promise<void> {
    const uid = this.requireUserId(userId);
    const payload: Record<string, unknown> = {
      updatedAt: serverTimestamp(),
    };
    if (updates.englishLevel) payload.englishLevel = updates.englishLevel;
    if (updates.themeMode) payload.themeMode = updates.themeMode;
    if (updates.marathiScript) payload.marathiScript = updates.marathiScript;
    if (updates.autoSpeakAiReplies !== undefined) {
      payload.autoSpeakAiReplies = updates.autoSpeakAiReplies;
    }
    if (updates.speechRate !== undefined) {
      payload.speechRate = Math.min(1.5, Math.max(0.5, updates.speechRate));
    }
    if (updates.weeklyInterviewFrequency !== undefined) {
      payload.weeklyInterviewFrequency = Math.min(
        2,
        Math.max(1, Math.round(updates.weeklyInterviewFrequency))
      );
    }
    await updateDoc(doc(this.db, "users", uid), payload);
  }

  async createChatSession(params: {
    userId: string;
    title: string;
    level: EnglishLevel;
    mode: SessionMode;
    topicId?: string | null;
    topicTitle?: string | null;
    interviewCategory?: string | null;
    initialAssistantGreeting?: string | null;
    isVoiceInitial?: boolean;
  }): Promise<string> {
    const uid = this.requireUserId(params.userId);
    const sessionRef = doc(collection(this.db, "users", uid, "chatSessions"));
    const safeTitle = params.title.trim().slice(0, 120) || "English Practice";
    const preview = (
      params.initialAssistantGreeting ||
      `Started a new ${params.mode.toLowerCase()} session`
    ).slice(0, 240);

    const sessionPayload: Record<string, unknown> = {
      userId: uid,
      title: safeTitle,
      englishLevel: params.level,
      mode: params.mode,
      messageCount: params.initialAssistantGreeting ? 1 : 0,
      mistakesCount: 0,
      lastMessagePreview: preview,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    };
    if (params.topicId?.trim()) {
      sessionPayload.topicId = params.topicId.trim().slice(0, 128);
    }
    if (params.topicTitle?.trim()) {
      sessionPayload.topicTitle = params.topicTitle.trim().slice(0, 120);
    }
    if (params.interviewCategory?.trim()) {
      sessionPayload.interviewCategory = params.interviewCategory
        .trim()
        .slice(0, 80);
    }

    const batch = writeBatch(this.db);
    batch.set(sessionRef, sessionPayload);

    if (params.initialAssistantGreeting?.trim()) {
      const msgRef = doc(
        collection(this.db, "users", uid, "chatSessions", sessionRef.id, "messages")
      );
      batch.set(msgRef, {
        userId: uid,
        sessionId: sessionRef.id,
        role: "assistant",
        text: params.initialAssistantGreeting.trim().slice(0, 4000),
        englishLevel: params.level,
        hasMistake: false,
        voiceMessage: Boolean(params.isVoiceInitial || params.mode === "VOICE"),
        createdAt: serverTimestamp(),
      });
    }

    batch.update(doc(this.db, "users", uid), {
      totalConversations: increment(1),
      updatedAt: serverTimestamp(),
    });

    await batch.commit();
    return sessionRef.id;
  }

  async saveTutorTurn(params: {
    userId: string;
    sessionId: string;
    userText: string;
    tutorResult: TutorTurnResult;
    level: EnglishLevel;
    isVoiceMessage: boolean;
    currentProfile: UserProfile | null;
  }): Promise<void> {
    const uid = this.requireUserId(params.userId);
    const sessionRef = doc(
      this.db,
      "users",
      uid,
      "chatSessions",
      params.sessionId
    );
    const userMsgRef = doc(collection(sessionRef, "messages"));
    const aiMsgRef = doc(collection(sessionRef, "messages"));

    const today = todayDateKey();
    const yesterday = yesterdayDateKey();
    const lastDate = params.currentProfile?.lastPracticeDate;
    const prevStreak = params.currentProfile?.currentStreakDays ?? 0;
    const newStreak =
      lastDate === today
        ? Math.max(1, prevStreak)
        : lastDate === yesterday
          ? prevStreak + 1
          : 1;

    const statRef = doc(this.db, "users", uid, "dailyStats", today);
    const [statSnap, sessionSnap] = await Promise.all([
      getDoc(statRef),
      getDoc(sessionRef),
    ]);

    const prevMsgCount = Number(sessionSnap.data()?.messageCount ?? 0);
    const sessionMode = String(sessionSnap.data()?.mode ?? "CHAT");
    const existingTitle = String(
      sessionSnap.data()?.title ?? "English Practice"
    );
    const updatedTitle =
      prevMsgCount <= 1 && sessionMode !== "INTERVIEW"
        ? params.userText.trim().slice(0, 42) || existingTitle
        : existingTitle;

    const mistakeDelta = params.tutorResult.hasMistake ? 1 : 0;

    const aiMsgPayload: Record<string, unknown> = {
      userId: uid,
      sessionId: params.sessionId,
      role: "assistant",
      text: params.tutorResult.assistantReply.trim().slice(0, 4000),
      englishLevel: params.level,
      hasMistake: params.tutorResult.hasMistake,
      mistakeCategory: params.tutorResult.mistakeCategory.slice(0, 80),
      voiceMessage: params.isVoiceMessage,
      createdAt: serverTimestamp(),
    };
    if (params.tutorResult.originalSentence) {
      aiMsgPayload.originalSentence = params.tutorResult.originalSentence.slice(
        0,
        1000
      );
    }
    if (params.tutorResult.correctedSentence) {
      aiMsgPayload.correctedSentence =
        params.tutorResult.correctedSentence.slice(0, 1000);
    }
    if (params.tutorResult.marathiExplanation) {
      aiMsgPayload.marathiExplanation =
        params.tutorResult.marathiExplanation.slice(0, 1500);
    }
    if (params.tutorResult.naturalAlternative) {
      aiMsgPayload.naturalAlternative =
        params.tutorResult.naturalAlternative.slice(0, 1000);
    }
    if (params.tutorResult.suggestedVocabulary) {
      const v = params.tutorResult.suggestedVocabulary;
      aiMsgPayload.suggestedWord = v.word.slice(0, 80);
      aiMsgPayload.suggestedWordMarathi = v.marathiMeaning.slice(0, 200);
      aiMsgPayload.suggestedWordExample = v.exampleSentence.slice(0, 400);
      aiMsgPayload.suggestedWordPronunciation = v.pronunciation.slice(0, 120);
      aiMsgPayload.suggestedWordPos = v.partOfSpeech.slice(0, 40);
    }

    const batch = writeBatch(this.db);
    batch.set(userMsgRef, {
      userId: uid,
      sessionId: params.sessionId,
      role: "user",
      text: params.userText.trim().slice(0, 4000),
      englishLevel: params.level,
      hasMistake: false,
      voiceMessage: params.isVoiceMessage,
      createdAt: serverTimestamp(),
    });
    batch.set(aiMsgRef, aiMsgPayload);

    batch.update(sessionRef, {
      title: updatedTitle,
      englishLevel: params.level,
      messageCount: increment(2),
      mistakesCount: increment(mistakeDelta),
      lastMessagePreview: params.tutorResult.assistantReply.trim().slice(0, 240),
      updatedAt: serverTimestamp(),
    });

    batch.update(doc(this.db, "users", uid), {
      totalMessagesSent: increment(1),
      grammarMistakesDetected: increment(mistakeDelta),
      currentStreakDays: newStreak,
      lastPracticeDate: today,
      updatedAt: serverTimestamp(),
    });

    if (statSnap.exists()) {
      batch.update(statRef, {
        messagesCount: increment(1),
        mistakesCount: increment(mistakeDelta),
        updatedAt: serverTimestamp(),
      });
    } else {
      batch.set(statRef, {
        userId: uid,
        dateKey: today,
        practiceSeconds: 0,
        messagesCount: 1,
        mistakesCount: mistakeDelta,
        wordsLearnedCount: 0,
        topicsCompletedCount: 0,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }

    await batch.commit();
  }

  async clearSessionMessages(userId: string, sessionId: string): Promise<void> {
    const uid = this.requireUserId(userId);
    const sessionRef = doc(this.db, "users", uid, "chatSessions", sessionId);
    const msgsSnap = await getDocs(collection(sessionRef, "messages"));
    const batch = writeBatch(this.db);
    msgsSnap.docs.forEach((d) => batch.delete(d.ref));
    batch.update(sessionRef, {
      messageCount: 0,
      mistakesCount: 0,
      lastMessagePreview: "Chat cleared",
      updatedAt: serverTimestamp(),
    });
    await batch.commit();
  }

  async deleteSession(userId: string, sessionId: string): Promise<void> {
    const uid = this.requireUserId(userId);
    const sessionRef = doc(this.db, "users", uid, "chatSessions", sessionId);
    const msgsSnap = await getDocs(collection(sessionRef, "messages"));
    const batch = writeBatch(this.db);
    msgsSnap.docs.forEach((d) => batch.delete(d.ref));
    batch.delete(sessionRef);
    await batch.commit();
  }

  async saveDailyTopic(
    userId: string,
    dateKey: string,
    draft: GeneratedTopicDraft,
    level: EnglishLevel
  ): Promise<void> {
    const uid = this.requireUserId(userId);
    const ref = doc(this.db, "users", uid, "dailyTopics", dateKey);
    const existing = await getDoc(ref);

    const payload: Record<string, unknown> = {
      title: draft.title.slice(0, 120),
      category: draft.category.slice(0, 60),
      sessionType: draft.sessionType,
      englishLevel: level,
      promptSummary: draft.promptSummary.slice(0, 600),
      starterQuestion: draft.starterQuestion.slice(0, 500),
      marathiHint: draft.marathiHint.slice(0, 500),
      updatedAt: serverTimestamp(),
    };
    if (draft.interviewCategory?.trim()) {
      payload.interviewCategory = draft.interviewCategory.trim().slice(0, 80);
    }
    if (draft.scheduleAdjustedNote?.trim()) {
      payload.scheduleAdjustedNote = draft.scheduleAdjustedNote
        .trim()
        .slice(0, 300);
    }

    if (existing.exists()) {
      await updateDoc(ref, payload);
    } else {
      await setDoc(ref, {
        ...payload,
        userId: uid,
        dateKey,
        completed: false,
        turnsCompleted: 0,
        createdAt: serverTimestamp(),
      });
    }
  }

  async updateTopicProgress(params: {
    userId: string;
    dateKey: string;
    sessionId?: string | null;
    markCompleted: boolean;
    turnsIncrement?: number;
  }): Promise<void> {
    const uid = this.requireUserId(params.userId);
    const ref = doc(this.db, "users", uid, "dailyTopics", params.dateKey);
    const snap = await getDoc(ref);
    if (!snap.exists()) return;

    const wasCompleted = Boolean(snap.data()?.completed);
    const updates: Record<string, unknown> = {
      updatedAt: serverTimestamp(),
    };
    if (params.sessionId?.trim()) updates.sessionId = params.sessionId.trim();
    if (params.turnsIncrement && params.turnsIncrement > 0) {
      updates.turnsCompleted = increment(params.turnsIncrement);
    }
    if (params.markCompleted) updates.completed = true;

    const batch = writeBatch(this.db);
    batch.update(ref, updates);

    if (params.markCompleted && !wasCompleted) {
      batch.update(doc(this.db, "users", uid), {
        topicsCompletedCount: increment(1),
        updatedAt: serverTimestamp(),
      });
      const today = todayDateKey();
      const statRef = doc(this.db, "users", uid, "dailyStats", today);
      const statSnap = await getDoc(statRef);
      if (statSnap.exists()) {
        batch.update(statRef, {
          topicsCompletedCount: increment(1),
          updatedAt: serverTimestamp(),
        });
      } else {
        batch.set(statRef, {
          userId: uid,
          dateKey: today,
          practiceSeconds: 0,
          messagesCount: 0,
          mistakesCount: 0,
          wordsLearnedCount: 0,
          topicsCompletedCount: 1,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    }

    await batch.commit();
  }

  async saveInterviewReport(params: {
    userId: string;
    sessionId: string;
    interviewCategory: string;
    level: EnglishLevel;
    draft: GeneratedInterviewReportDraft;
    questionsAnswered: number;
  }): Promise<InterviewReport> {
    const uid = this.requireUserId(params.userId);
    const reportRef = doc(collection(this.db, "users", uid, "interviewReports"));
    const safeCategory =
      params.interviewCategory.trim().slice(0, 80) || "HR Interview";

    const payload = {
      userId: uid,
      sessionId: params.sessionId.slice(0, 128),
      interviewCategory: safeCategory,
      englishLevel: params.level,
      overallSummary:
        params.draft.overallSummary.trim().slice(0, 1000) ||
        "Completed interview practice.",
      grammarImprovement:
        params.draft.grammarImprovement.trim().slice(0, 1500) ||
        "Keep practicing verb tenses.",
      vocabularyFeedback:
        params.draft.vocabularyFeedback.trim().slice(0, 1500) ||
        "Good word choices.",
      sentenceFormation:
        params.draft.sentenceFormation.trim().slice(0, 1500) ||
        "Clear sentence structure.",
      answerClarity:
        params.draft.answerClarity.trim().slice(0, 1500) || "Clear answers.",
      communicationFeedback:
        params.draft.communicationFeedback.trim().slice(0, 1500) ||
        "Confident tone.",
      suggestedBetterAnswers:
        params.draft.suggestedBetterAnswers.trim().slice(0, 2500) ||
        "Use structured examples.",
      areasToImprove:
        params.draft.areasToImprove.trim().slice(0, 1500) || "Practice daily.",
      marathiCoachNote:
        params.draft.marathiCoachNote.trim().slice(0, 1500) || "Chhan prayatna!",
      questionsAnswered: Math.max(0, Math.round(params.questionsAnswered)),
      createdAt: serverTimestamp(),
    };

    await setDoc(reportRef, payload);
    return {
      id: reportRef.id,
      userId: uid,
      sessionId: payload.sessionId,
      interviewCategory: payload.interviewCategory,
      englishLevel: params.level,
      overallSummary: payload.overallSummary,
      grammarImprovement: payload.grammarImprovement,
      vocabularyFeedback: payload.vocabularyFeedback,
      sentenceFormation: payload.sentenceFormation,
      answerClarity: payload.answerClarity,
      communicationFeedback: payload.communicationFeedback,
      suggestedBetterAnswers: payload.suggestedBetterAnswers,
      areasToImprove: payload.areasToImprove,
      marathiCoachNote: payload.marathiCoachNote,
      questionsAnswered: payload.questionsAnswered,
      createdAtMs: Date.now(),
    };
  }

  async addVocabularyWord(params: {
    userId: string;
    draft: GeneratedWordDraft;
    level: EnglishLevel;
    source: VocabSource;
  }): Promise<void> {
    const uid = this.requireUserId(params.userId);
    const wordText = params.draft.word.trim().slice(0, 80);
    if (!wordText) throw new Error("English word cannot be empty.");

    const wordRef = doc(collection(this.db, "users", uid, "vocabulary"));
    await setDoc(wordRef, {
      userId: uid,
      word: wordText,
      marathiMeaning:
        params.draft.marathiMeaning.trim().slice(0, 200) || "Meaning saved",
      exampleSentence:
        params.draft.exampleSentence.trim().slice(0, 400) ||
        `Example: ${wordText}`,
      pronunciation: params.draft.pronunciation.trim().slice(0, 120),
      partOfSpeech: params.draft.partOfSpeech.trim().slice(0, 40) || "word",
      englishLevel: params.level,
      dateLearned: todayDateKey(),
      learned: false,
      source: params.source,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp(),
    });
  }

  async toggleWordLearned(
    userId: string,
    wordId: string,
    newLearned: boolean
  ): Promise<void> {
    const uid = this.requireUserId(userId);
    const ref = doc(this.db, "users", uid, "vocabulary", wordId);
    const snap = await getDoc(ref);
    const prevLearned = Boolean(snap.data()?.learned);
    if (prevLearned === newLearned) return;

    const delta = newLearned ? 1 : -1;
    const today = todayDateKey();
    const batch = writeBatch(this.db);

    batch.update(ref, {
      learned: newLearned,
      dateLearned: today,
      updatedAt: serverTimestamp(),
    });
    batch.update(doc(this.db, "users", uid), {
      vocabularyLearnedCount: increment(delta),
      updatedAt: serverTimestamp(),
    });

    if (newLearned) {
      const statRef = doc(this.db, "users", uid, "dailyStats", today);
      const statSnap = await getDoc(statRef);
      if (statSnap.exists()) {
        batch.update(statRef, {
          wordsLearnedCount: increment(1),
          updatedAt: serverTimestamp(),
        });
      } else {
        batch.set(statRef, {
          userId: uid,
          dateKey: today,
          practiceSeconds: 0,
          messagesCount: 0,
          mistakesCount: 0,
          wordsLearnedCount: 1,
          topicsCompletedCount: 0,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
      }
    }

    await batch.commit();
  }

  async deleteVocabularyWord(
    userId: string,
    word: VocabularyWord
  ): Promise<void> {
    const uid = this.requireUserId(userId);
    const batch = writeBatch(this.db);
    batch.delete(doc(this.db, "users", uid, "vocabulary", word.id));
    if (word.learned) {
      batch.update(doc(this.db, "users", uid), {
        vocabularyLearnedCount: increment(-1),
        updatedAt: serverTimestamp(),
      });
    }
    await batch.commit();
  }

  async recordPracticeSeconds(userId: string, seconds: number): Promise<void> {
    if (seconds <= 0) return;
    const uid = this.requireUserId(userId);
    const today = todayDateKey();
    const statRef = doc(this.db, "users", uid, "dailyStats", today);
    const statSnap = await getDoc(statRef);

    const batch = writeBatch(this.db);
    batch.update(doc(this.db, "users", uid), {
      totalPracticeSeconds: increment(seconds),
      updatedAt: serverTimestamp(),
    });
    if (statSnap.exists()) {
      batch.update(statRef, {
        practiceSeconds: increment(seconds),
        updatedAt: serverTimestamp(),
      });
    } else {
      batch.set(statRef, {
        userId: uid,
        dateKey: today,
        practiceSeconds: seconds,
        messagesCount: 0,
        mistakesCount: 0,
        wordsLearnedCount: 0,
        topicsCompletedCount: 0,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
    }
    await batch.commit();
  }
}
