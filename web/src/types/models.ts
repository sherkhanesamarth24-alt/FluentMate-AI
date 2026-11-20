export type EnglishLevel = "LOW" | "MEDIUM" | "HIGH";
export type ThemeMode = "SYSTEM" | "LIGHT" | "DARK";
export type MarathiScript = "ROMAN" | "ROMAN_AND_DEVANAGARI";
export type SessionMode = "CHAT" | "VOICE" | "TOPIC" | "INTERVIEW";
export type DailySessionType =
  | "DAILY_CONVERSATION"
  | "INTERVIEW_PRACTICE"
  | "GENERAL_CONVERSATION"
  | "REVISION";
export type VocabSource = "DAILY" | "CHAT" | "MANUAL";

export const ENGLISH_LEVELS: Record<
  EnglishLevel,
  { badge: EnglishLevel; label: string; description: string }
> = {
  LOW: {
    badge: "LOW",
    label: "Beginner",
    description:
      "Simple English, short sentences, basic daily conversations, easy vocabulary",
  },
  MEDIUM: {
    badge: "MEDIUM",
    label: "Intermediate",
    description:
      "Natural conversations, richer vocabulary, longer sentences, grammar polish",
  },
  HIGH: {
    badge: "HIGH",
    label: "Advanced",
    description:
      "Professional English, advanced vocabulary, complex discussions, interview prep",
  },
};

export const MARATHI_SCRIPTS: Record<
  MarathiScript,
  { label: string; description: string }
> = {
  ROMAN: {
    label: "Roman Marathi",
    description: "Marathi written in simple English letters (Recommended)",
  },
  ROMAN_AND_DEVANAGARI: {
    label: "Roman + Marathi",
    description: "Simple Roman Marathi with Devanagari context",
  },
};

export const DAILY_SESSION_TYPE_LABELS: Record<
  DailySessionType,
  { label: string; shortLabel: string }
> = {
  DAILY_CONVERSATION: { label: "Daily Conversation", shortLabel: "Daily" },
  INTERVIEW_PRACTICE: { label: "Interview Practice", shortLabel: "Interview" },
  GENERAL_CONVERSATION: { label: "General Conversation", shortLabel: "General" },
  REVISION: { label: "Revision and Free Conversation", shortLabel: "Revision" },
};

export interface InterviewCategoryMeta {
  id: string;
  title: string;
  subtitle: string;
}

export const INTERVIEW_CATEGORIES: InterviewCategoryMeta[] = [
  {
    id: "HR",
    title: "HR Interview",
    subtitle: "Strengths, teamwork, conflict resolution, and culture fit",
  },
  {
    id: "SELF_INTRODUCTION",
    title: "Self Introduction",
    subtitle: "Confident personal pitch, background, and career goals",
  },
  {
    id: "TECHNICAL",
    title: "Technical Interview",
    subtitle: "Problem solving, system thinking, and project walkthroughs",
  },
  {
    id: "COLLEGE_PLACEMENT",
    title: "College Placement Interview",
    subtitle: "Campus hiring questions, academics, and fresher readiness",
  },
  {
    id: "INTERNSHIP",
    title: "Internship Interview",
    subtitle: "Learning mindset, college projects, and availability",
  },
  {
    id: "AI_ML_JOB",
    title: "AI/ML Job Interview",
    subtitle: "Machine learning concepts, LLMs, evaluation, and real projects",
  },
  {
    id: "COMMUNICATION_SKILLS",
    title: "Communication Skills Interview",
    subtitle: "Clarity, active listening, persuasion, and fluency",
  },
];

export const TOPIC_CATEGORIES = [
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
  "Job Interviews",
] as const;

export interface WeeklyDayScheduleItem {
  dateKey: string;
  dayShort: string;
  dayFull: string;
  sessionType: DailySessionType;
  isToday: boolean;
  isCompleted: boolean;
}

export interface UserProfile {
  id: string;
  userId: string;
  displayName: string;
  email: string;
  englishLevel: EnglishLevel;
  themeMode: ThemeMode;
  marathiScript: MarathiScript;
  autoSpeakAiReplies: boolean;
  speechRate: number;
  weeklyInterviewFrequency: number;
  totalConversations: number;
  totalMessagesSent: number;
  grammarMistakesDetected: number;
  topicsCompletedCount: number;
  vocabularyLearnedCount: number;
  totalPracticeSeconds: number;
  currentStreakDays: number;
  lastPracticeDate?: string | null;
}

export interface ChatSession {
  id: string;
  userId: string;
  title: string;
  topicId?: string | null;
  topicTitle?: string | null;
  interviewCategory?: string | null;
  englishLevel: EnglishLevel;
  mode: SessionMode;
  messageCount: number;
  mistakesCount: number;
  lastMessagePreview: string;
  createdAtMs?: number;
  updatedAtMs?: number;
}

export interface ChatMessage {
  id: string;
  userId: string;
  sessionId: string;
  role: "user" | "assistant";
  text: string;
  englishLevel: EnglishLevel;
  hasMistake: boolean;
  originalSentence?: string | null;
  correctedSentence?: string | null;
  marathiExplanation?: string | null;
  naturalAlternative?: string | null;
  mistakeCategory?: string | null;
  suggestedWord?: string | null;
  suggestedWordMarathi?: string | null;
  suggestedWordExample?: string | null;
  suggestedWordPronunciation?: string | null;
  suggestedWordPos?: string | null;
  voiceMessage: boolean;
  createdAtMs?: number;
}

export interface DailyTopicRecord {
  id: string;
  userId: string;
  dateKey: string;
  title: string;
  category: string;
  sessionType: DailySessionType;
  interviewCategory?: string | null;
  scheduleAdjustedNote?: string | null;
  englishLevel: EnglishLevel;
  promptSummary: string;
  starterQuestion: string;
  marathiHint: string;
  completed: boolean;
  sessionId?: string | null;
  turnsCompleted: number;
}

export interface InterviewReport {
  id: string;
  userId: string;
  sessionId: string;
  interviewCategory: string;
  englishLevel: EnglishLevel;
  overallSummary: string;
  grammarImprovement: string;
  vocabularyFeedback: string;
  sentenceFormation: string;
  answerClarity: string;
  communicationFeedback: string;
  suggestedBetterAnswers: string;
  areasToImprove: string;
  marathiCoachNote: string;
  questionsAnswered: number;
  createdAtMs?: number;
}

export interface VocabularyWord {
  id: string;
  userId: string;
  word: string;
  marathiMeaning: string;
  exampleSentence: string;
  pronunciation: string;
  partOfSpeech: string;
  englishLevel: EnglishLevel;
  dateLearned: string;
  learned: boolean;
  source: VocabSource;
}

export interface DailyProgressStat {
  id: string;
  userId: string;
  dateKey: string;
  practiceSeconds: number;
  messagesCount: number;
  mistakesCount: number;
  wordsLearnedCount: number;
  topicsCompletedCount: number;
}

export interface GeneratedWordDraft {
  word: string;
  marathiMeaning: string;
  exampleSentence: string;
  pronunciation: string;
  partOfSpeech: string;
}

export interface TutorTurnResult {
  hasMistake: boolean;
  mistakeCategory: string;
  originalSentence: string | null;
  correctedSentence: string | null;
  marathiExplanation: string | null;
  naturalAlternative: string | null;
  assistantReply: string;
  suggestedVocabulary: GeneratedWordDraft | null;
}

export interface GeneratedTopicDraft {
  title: string;
  category: string;
  sessionType: DailySessionType;
  interviewCategory: string | null;
  scheduleAdjustedNote: string | null;
  promptSummary: string;
  starterQuestion: string;
  marathiHint: string;
}

export interface GeneratedInterviewReportDraft {
  overallSummary: string;
  grammarImprovement: string;
  vocabularyFeedback: string;
  sentenceFormation: string;
  answerClarity: string;
  communicationFeedback: string;
  suggestedBetterAnswers: string;
  areasToImprove: string;
  marathiCoachNote: string;
}
