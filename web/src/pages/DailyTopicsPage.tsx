import { useState } from "react";
import {
  Briefcase,
  CalendarDays,
  CheckCircle2,
  ClipboardList,
  Loader2,
  MessageSquare,
  Mic,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import {
  buildWeeklyScheduleItems,
  countInterviewsThisWeek,
  todayDateKey,
} from "../lib/repository";
import {
  ChatSession,
  DAILY_SESSION_TYPE_LABELS,
  DailyTopicRecord,
  INTERVIEW_CATEGORIES,
  InterviewReport,
  TOPIC_CATEGORIES,
  UserProfile,
} from "../types/models";

interface DailyTopicsPageProps {
  profile: UserProfile;
  topics: DailyTopicRecord[];
  sessions: ChatSession[];
  reports: InterviewReport[];
  isGeneratingTopic: boolean;
  onGenerateTopic: (preferredCategory: string | null) => Promise<void>;
  onStartTopicInChat: (topic: DailyTopicRecord) => void;
  onStartTopicInVoice: (topic: DailyTopicRecord) => void;
  onMarkTopicCompleted: (dateKey: string) => void;
  onStartInterview: (categoryTitle: string, voiceMode: boolean) => Promise<void>;
  onResumeInterviewSession: (session: ChatSession) => void;
  onOpenReport: (report: InterviewReport) => void;
}

export function DailyTopicsPage({
  profile,
  topics,
  sessions,
  reports,
  isGeneratingTopic,
  onGenerateTopic,
  onStartTopicInChat,
  onStartTopicInVoice,
  onMarkTopicCompleted,
  onStartInterview,
  onResumeInterviewSession,
  onOpenReport,
}: DailyTopicsPageProps) {
  const [selectedCategory, setSelectedCategory] = useState<string>("All");
  const [startingCategory, setStartingCategory] = useState<string | null>(null);

  const todayKey = todayDateKey();
  const todayTopic = topics.find((t) => t.dateKey === todayKey) ?? null;
  const pastTopics = topics.filter((t) => t.dateKey !== todayKey);

  const weeklySchedule = buildWeeklyScheduleItems(
    topics,
    profile.weeklyInterviewFrequency
  );
  const weeklyFreq = Math.min(2, Math.max(1, profile.weeklyInterviewFrequency));
  const usedInterviews = countInterviewsThisWeek(topics, sessions);
  const quotaReached = usedInterviews >= weeklyFreq;

  const latestInterviewSession = sessions.find(
    (s) => s.mode === "INTERVIEW" || Boolean(s.interviewCategory)
  );

  const handleStartInterviewClick = async (
    title: string,
    voiceMode: boolean
  ) => {
    setStartingCategory(`${title}-${voiceMode ? "v" : "c"}`);
    try {
      await onStartInterview(title, voiceMode);
    } finally {
      setStartingCategory(null);
    }
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[var(--fm-text)]">
            Daily Topics &amp; Weekly Interviews
          </h1>
          <p className="text-sm text-[var(--fm-text-muted)]">
            Structured daily speaking prompts with 1–2 balanced mock interviews
            each week.
          </p>
        </div>
        <button
          type="button"
          disabled={isGeneratingTopic}
          onClick={() =>
            void onGenerateTopic(
              selectedCategory === "All" ? null : selectedCategory
            )
          }
          className="inline-flex min-h-11 items-center gap-2 rounded-2xl bg-[var(--fm-primary)] px-4 py-2 text-sm font-semibold text-[var(--fm-on-primary)] transition hover:opacity-90 disabled:opacity-55 cursor-pointer"
        >
          {isGeneratingTopic ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Sparkles className="h-4 w-4" />
          )}
          <span>
            {todayTopic ? "Regenerate Today's Topic" : "Generate Today's Topic"}
          </span>
        </button>
      </div>

      {/* Weekly Plan Strip */}
      <section className="rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <CalendarDays className="h-5 w-5 text-[var(--fm-primary)]" />
            <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
              Weekly Learning Plan
            </h2>
          </div>
          <span className="rounded-full bg-[var(--fm-secondary-container)] px-3 py-1 text-xs font-bold text-[var(--fm-on-secondary-container)]">
            Interviews this week: {usedInterviews} / {weeklyFreq}
          </span>
        </div>

        <div className="grid grid-cols-2 gap-2 sm:grid-cols-4 lg:grid-cols-7">
          {weeklySchedule.map((item) => {
            const isInterview = item.sessionType === "INTERVIEW_PRACTICE";
            return (
              <div
                key={item.dateKey}
                className={`rounded-2xl border p-3 ${
                  item.isToday
                    ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)]/35"
                    : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/45"
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-display text-xs font-bold text-[var(--fm-text)]">
                    {item.dayShort}
                  </span>
                  {item.isCompleted ? (
                    <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
                  ) : item.isToday ? (
                    <span className="rounded bg-[var(--fm-primary)] px-1.5 py-0.5 text-[9px] font-bold uppercase text-[var(--fm-on-primary)]">
                      Today
                    </span>
                  ) : null}
                </div>
                <div
                  className={`mt-2 text-xs font-semibold ${
                    isInterview
                      ? "text-[var(--fm-tertiary)]"
                      : "text-[var(--fm-text-muted)]"
                  }`}
                >
                  {DAILY_SESSION_TYPE_LABELS[item.sessionType].shortLabel}
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Topic Category Filter Pills */}
      <div className="flex flex-wrap gap-2">
        {TOPIC_CATEGORIES.map((cat) => {
          const active = selectedCategory === cat;
          return (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`min-h-10 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition cursor-pointer ${
                active
                  ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                  : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] text-[var(--fm-text-muted)] hover:border-[var(--fm-outline)]"
              }`}
            >
              {cat}
            </button>
          );
        })}
      </div>

      {/* Today's Topic Details */}
      {todayTopic && (
        <section className="rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5 sm:p-6">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full bg-[var(--fm-primary-container)] px-3 py-1 text-xs font-bold text-[var(--fm-on-primary-container)]">
                {DAILY_SESSION_TYPE_LABELS[todayTopic.sessionType].label}
              </span>
              <span className="rounded-full bg-[var(--fm-surface-variant)] px-3 py-1 text-xs font-semibold text-[var(--fm-text-muted)]">
                {todayTopic.category}
              </span>
              {todayTopic.completed && (
                <span className="inline-flex items-center gap-1 rounded-full bg-[var(--fm-primary)]/15 px-3 py-1 text-xs font-bold text-[var(--fm-primary)]">
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  Completed
                </span>
              )}
            </div>
            <button
              type="button"
              disabled={isGeneratingTopic}
              onClick={() =>
                void onGenerateTopic(
                  selectedCategory === "All" ? null : selectedCategory
                )
              }
              className="inline-flex min-h-10 items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold text-[var(--fm-text-muted)] hover:bg-[var(--fm-surface-variant)] cursor-pointer"
            >
              <RefreshCw
                className={`h-4 w-4 ${isGeneratingTopic ? "animate-spin" : ""}`}
              />
              <span>Swap Topic</span>
            </button>
          </div>

          <h2 className="mt-3 font-display text-xl font-bold text-[var(--fm-text)]">
            {todayTopic.title}
          </h2>
          <p className="mt-1 text-sm text-[var(--fm-text-muted)]">
            {todayTopic.promptSummary}
          </p>

          {todayTopic.scheduleAdjustedNote && (
            <div className="mt-3 rounded-xl bg-[var(--fm-tertiary-container)]/40 px-3.5 py-2 text-xs text-[var(--fm-text)]">
              {todayTopic.scheduleAdjustedNote}
            </div>
          )}

          <div className="mt-4 rounded-2xl bg-[var(--fm-surface-variant)]/70 p-4">
            <div className="text-xs font-bold uppercase tracking-wide text-[var(--fm-primary)]">
              First Question
            </div>
            <p className="mt-1 text-sm font-semibold text-[var(--fm-text)]">
              &ldquo;{todayTopic.starterQuestion}&rdquo;
            </p>
            <p className="mt-2 text-xs text-[var(--fm-secondary)]">
              <strong>Marathi Hint:</strong> {todayTopic.marathiHint}
            </p>
          </div>

          <div className="mt-4 flex flex-wrap gap-2.5">
            <button
              type="button"
              onClick={() => onStartTopicInChat(todayTopic)}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-[var(--fm-primary)] px-4 py-2 text-xs font-semibold text-[var(--fm-on-primary)] cursor-pointer sm:text-sm"
            >
              <MessageSquare className="h-4 w-4" />
              <span>Start in Chat</span>
            </button>
            <button
              type="button"
              onClick={() => onStartTopicInVoice(todayTopic)}
              className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-[var(--fm-secondary)]/50 bg-[var(--fm-secondary-container)]/60 px-4 py-2 text-xs font-semibold text-[var(--fm-on-secondary-container)] cursor-pointer sm:text-sm"
            >
              <Mic className="h-4 w-4" />
              <span>Practice in Voice</span>
            </button>
            {!todayTopic.completed && (
              <button
                type="button"
                onClick={() => onMarkTopicCompleted(todayTopic.dateKey)}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-[var(--fm-outline)] px-4 py-2 text-xs font-semibold text-[var(--fm-text)] cursor-pointer sm:text-sm"
              >
                <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
                <span>Mark Completed</span>
              </button>
            )}
          </div>
        </section>
      )}

      {/* Weekly Interview Practice Section */}
      <section className="space-y-4 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5 sm:p-6">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <div className="flex items-center gap-2">
              <Briefcase className="h-5 w-5 text-[var(--fm-tertiary)]" />
              <h2 className="font-display text-lg font-bold text-[var(--fm-text)]">
                Weekly Interview Practice
              </h2>
            </div>
            <p className="mt-1 text-xs text-[var(--fm-text-muted)]">
              AI acts as a real interviewer asking 1 question at a time,
              providing Roman Marathi grammar notes, stronger professional
              answers, and a full Performance Report.
            </p>
          </div>
          {latestInterviewSession && (
            <button
              type="button"
              onClick={() => onResumeInterviewSession(latestInterviewSession)}
              className="inline-flex min-h-10 items-center gap-1.5 rounded-xl border border-[var(--fm-primary)]/40 bg-[var(--fm-primary-container)]/40 px-3.5 py-2 text-xs font-semibold text-[var(--fm-on-primary-container)] cursor-pointer"
            >
              <span>Resume Last Interview</span>
            </button>
          )}
        </div>

        {quotaReached && (
          <div className="rounded-2xl border border-[var(--fm-tertiary)]/50 bg-[var(--fm-tertiary-container)]/40 p-3.5 text-xs text-[var(--fm-text)]">
            You&apos;ve completed your weekly target of{" "}
            <strong>
              {usedInterviews}/{weeklyFreq}
            </strong>{" "}
            mock interview sessions this week (scheduled on Tue &amp; Fri to
            keep daily practice balanced). You can adjust weekly frequency (1 or
            2/week) in Settings or resume your existing interview session.
          </div>
        )}

        <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
          {INTERVIEW_CATEGORIES.map((cat) => {
            const busyChat = startingCategory === `${cat.title}-c`;
            const busyVoice = startingCategory === `${cat.title}-v`;
            return (
              <div
                key={cat.id}
                className="flex flex-col justify-between rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/45 p-4"
              >
                <div>
                  <h3 className="font-display text-sm font-bold text-[var(--fm-text)]">
                    {cat.title}
                  </h3>
                  <p className="mt-1 text-xs leading-relaxed text-[var(--fm-text-muted)]">
                    {cat.subtitle}
                  </p>
                </div>
                <div className="mt-4 flex gap-2">
                  <button
                    type="button"
                    disabled={quotaReached || startingCategory !== null}
                    onClick={() =>
                      void handleStartInterviewClick(cat.title, false)
                    }
                    className="inline-flex min-h-10 flex-1 items-center justify-center gap-1.5 rounded-xl bg-[var(--fm-primary)] px-3 py-2 text-xs font-semibold text-[var(--fm-on-primary)] disabled:opacity-45 cursor-pointer"
                  >
                    {busyChat ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <MessageSquare className="h-3.5 w-3.5" />
                    )}
                    <span>Chat</span>
                  </button>
                  <button
                    type="button"
                    disabled={quotaReached || startingCategory !== null}
                    onClick={() =>
                      void handleStartInterviewClick(cat.title, true)
                    }
                    className="inline-flex min-h-10 flex-1 items-center justify-center gap-1.5 rounded-xl border border-[var(--fm-secondary)]/50 bg-[var(--fm-secondary-container)]/60 px-3 py-2 text-xs font-semibold text-[var(--fm-on-secondary-container)] disabled:opacity-45 cursor-pointer"
                  >
                    {busyVoice ? (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    ) : (
                      <Mic className="h-3.5 w-3.5" />
                    )}
                    <span>Voice</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Past Interview Reports */}
      {reports.length > 0 && (
        <section className="space-y-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
          <div className="flex items-center gap-2">
            <ClipboardList className="h-5 w-5 text-[var(--fm-primary)]" />
            <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
              Interview Performance Reports ({reports.length})
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            {reports.map((report) => (
              <button
                key={report.id}
                type="button"
                onClick={() => onOpenReport(report)}
                className="rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/45 p-4 text-left transition hover:border-[var(--fm-primary)] cursor-pointer"
              >
                <div className="flex items-center justify-between gap-2">
                  <span className="font-display text-sm font-bold text-[var(--fm-text)]">
                    {report.interviewCategory}
                  </span>
                  <span className="rounded-md bg-[var(--fm-primary-container)] px-2 py-0.5 font-mono-code text-[10px] font-bold text-[var(--fm-on-primary-container)]">
                    {report.englishLevel} • {report.questionsAnswered} ans
                  </span>
                </div>
                <p className="mt-1.5 line-clamp-2 text-xs text-[var(--fm-text-muted)]">
                  {report.overallSummary}
                </p>
              </button>
            ))}
          </div>
        </section>
      )}

      {/* Past Topics */}
      {pastTopics.length > 0 && (
        <section className="space-y-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
          <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
            Previous Topics ({pastTopics.length})
          </h2>
          <div className="space-y-2.5">
            {pastTopics.slice(0, 10).map((t) => (
              <div
                key={t.dateKey}
                className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/35 p-3.5"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 text-[11px] text-[var(--fm-text-muted)]">
                    <span className="font-mono-code">{t.dateKey}</span>
                    <span>•</span>
                    <span>{t.category}</span>
                    {t.completed && (
                      <CheckCircle2 className="h-3.5 w-3.5 text-[var(--fm-primary)]" />
                    )}
                  </div>
                  <div className="truncate font-display text-sm font-bold text-[var(--fm-text)]">
                    {t.title}
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <button
                    type="button"
                    onClick={() => onStartTopicInChat(t)}
                    className="min-h-9 rounded-xl border border-[var(--fm-outline)] px-3 py-1.5 text-xs font-semibold text-[var(--fm-text)] hover:bg-[var(--fm-surface)] cursor-pointer"
                  >
                    Chat
                  </button>
                  <button
                    type="button"
                    onClick={() => onStartTopicInVoice(t)}
                    className="min-h-9 rounded-xl border border-[var(--fm-outline)] px-3 py-1.5 text-xs font-semibold text-[var(--fm-text)] hover:bg-[var(--fm-surface)] cursor-pointer"
                  >
                    Voice
                  </button>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
