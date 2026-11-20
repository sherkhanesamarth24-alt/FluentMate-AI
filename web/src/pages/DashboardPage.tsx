import {
  ArrowRight,
  BookOpen,
  CalendarCheck,
  CheckCircle2,
  Clock,
  Flame,
  GraduationCap,
  Lightbulb,
  MessageSquare,
  Mic,
  RefreshCw,
  Sparkles,
} from "lucide-react";
import { LevelSelector } from "../components/LevelSelector";
import {
  AccuracyRingChart,
  StatKpiCard,
  WeeklyPracticeBarChart,
} from "../components/ProgressCharts";
import {
  buildWeeklyScheduleItems,
  todayDateKey,
} from "../lib/repository";
import {
  DAILY_SESSION_TYPE_LABELS,
  DailyProgressStat,
  DailyTopicRecord,
  ENGLISH_LEVELS,
  EnglishLevel,
  UserProfile,
} from "../types/models";

interface DashboardPageProps {
  profile: UserProfile;
  dailyStats: DailyProgressStat[];
  topics: DailyTopicRecord[];
  isGeneratingTopic: boolean;
  onSelectLevel: (level: EnglishLevel) => void;
  onOpenChat: () => void;
  onOpenVoice: () => void;
  onOpenTopics: () => void;
  onGenerateTodayTopic: () => void;
  onPracticeTopicInChat: (topic: DailyTopicRecord) => void;
  onPracticeTopicInVoice: (topic: DailyTopicRecord) => void;
  onMarkTopicDone: (dateKey: string) => void;
}

function formatPracticeDuration(totalSeconds: number): string {
  if (totalSeconds < 60) return `${totalSeconds}s`;
  const mins = Math.floor(totalSeconds / 60);
  if (mins < 60) return `${mins}m`;
  const hrs = Math.floor(mins / 60);
  const rem = mins % 60;
  return `${hrs}h ${rem}m`;
}

export function DashboardPage({
  profile,
  dailyStats,
  topics,
  isGeneratingTopic,
  onSelectLevel,
  onOpenChat,
  onOpenVoice,
  onOpenTopics,
  onGenerateTodayTopic,
  onPracticeTopicInChat,
  onPracticeTopicInVoice,
  onMarkTopicDone,
}: DashboardPageProps) {
  const firstName =
    profile.displayName.trim().split(/\s+/)[0] || "Learner";
  const todayKey = todayDateKey();
  const todayTopic = topics.find((t) => t.dateKey === todayKey) ?? null;
  const weeklyPlan = buildWeeklyScheduleItems(
    topics,
    profile.weeklyInterviewFrequency
  );
  const todaySchedule = weeklyPlan.find((d) => d.isToday);
  const scheduledType =
    todayTopic?.sessionType ??
    todaySchedule?.sessionType ??
    "DAILY_CONVERSATION";
  const scheduledLabel = DAILY_SESSION_TYPE_LABELS[scheduledType].label;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Welcome Banner */}
      <section className="flex flex-col justify-between gap-4 rounded-3xl border border-[var(--fm-outline-variant)] bg-gradient-to-br from-[var(--fm-surface)] via-[var(--fm-surface)] to-[var(--fm-primary-container)]/35 p-5 sm:flex-row sm:items-center sm:p-6">
        <div>
          <div className="inline-flex items-center gap-2 rounded-full bg-[var(--fm-primary-container)]/70 px-3 py-1 text-xs font-semibold text-[var(--fm-on-primary-container)]">
            <Sparkles className="h-3.5 w-3.5" />
            <span>FluentMate AI Tutor</span>
          </div>
          <h1 className="mt-2.5 font-display text-2xl font-bold text-[var(--fm-text)] sm:text-3xl">
            Namaste, {firstName}!
          </h1>
          <p className="mt-1 text-sm text-[var(--fm-text-muted)]">
            Speak &amp; type in English. Get gentle corrections explained in
            simple Roman Marathi.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2 rounded-2xl border border-[var(--fm-tertiary)]/40 bg-[var(--fm-tertiary-container)]/50 px-4 py-2.5">
            <Flame className="h-5 w-5 text-[var(--fm-tertiary)]" />
            <div>
              <div className="font-display text-lg font-bold leading-none text-[var(--fm-text)]">
                {profile.currentStreakDays}{" "}
                {profile.currentStreakDays === 1 ? "Day" : "Days"}
              </div>
              <div className="text-[11px] font-medium text-[var(--fm-text-muted)]">
                Practice Streak
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Level Selector + Quick Actions */}
      <section className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <div className="rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5 lg:col-span-7">
          <div className="mb-3 flex items-center justify-between gap-2">
            <div>
              <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
                Your English Level
              </h2>
              <p className="text-xs text-[var(--fm-text-muted)]">
                {ENGLISH_LEVELS[profile.englishLevel].description}
              </p>
            </div>
          </div>
          <LevelSelector
            selectedLevel={profile.englishLevel}
            onSelectLevel={onSelectLevel}
          />
        </div>

        <div className="grid grid-cols-2 gap-3 lg:col-span-5">
          <button
            type="button"
            onClick={onOpenChat}
            className="flex flex-col justify-between rounded-3xl border border-[var(--fm-primary)]/40 bg-[var(--fm-primary)] p-5 text-left text-[var(--fm-on-primary)] shadow-sm transition hover:opacity-95 cursor-pointer"
          >
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-white/20">
              <MessageSquare className="h-5 w-5" />
            </div>
            <div className="mt-4">
              <div className="font-display text-base font-bold">AI Chat</div>
              <div className="mt-0.5 flex items-center gap-1 text-xs opacity-90">
                <span>Type &amp; learn</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </div>
            </div>
          </button>

          <button
            type="button"
            onClick={onOpenVoice}
            className="flex flex-col justify-between rounded-3xl border border-[var(--fm-secondary)]/40 bg-[var(--fm-secondary-container)] p-5 text-left text-[var(--fm-on-secondary-container)] shadow-sm transition hover:opacity-95 cursor-pointer"
          >
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[var(--fm-secondary)]/20 text-[var(--fm-secondary)]">
              <Mic className="h-5 w-5" />
            </div>
            <div className="mt-4">
              <div className="font-display text-base font-bold">
                Voice Tutor
              </div>
              <div className="mt-0.5 flex items-center gap-1 text-xs opacity-85">
                <span>Speak English</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </div>
            </div>
          </button>
        </div>
      </section>

      {/* Today's Session Card */}
      <section className="rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5 sm:p-6">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex items-center gap-2.5">
            <span className="rounded-full bg-[var(--fm-secondary-container)] px-3 py-1 text-xs font-bold text-[var(--fm-on-secondary-container)]">
              Today: {scheduledLabel}
            </span>
            {todayTopic?.completed && (
              <span className="inline-flex items-center gap-1 rounded-full bg-[var(--fm-primary-container)] px-3 py-1 text-xs font-bold text-[var(--fm-on-primary-container)]">
                <CheckCircle2 className="h-3.5 w-3.5" />
                Completed
              </span>
            )}
          </div>
          <button
            type="button"
            onClick={onOpenTopics}
            className="inline-flex min-h-10 items-center gap-1.5 text-xs font-semibold text-[var(--fm-primary)] hover:underline cursor-pointer"
          >
            <span>Weekly Schedule &amp; Interviews</span>
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>

        {todayTopic ? (
          <div className="mt-4 space-y-3">
            <div>
              <div className="text-xs font-semibold text-[var(--fm-text-muted)]">
                {todayTopic.category}
                {todayTopic.interviewCategory
                  ? ` • ${todayTopic.interviewCategory}`
                  : ""}
              </div>
              <h2 className="mt-0.5 font-display text-xl font-bold text-[var(--fm-text)]">
                {todayTopic.title}
              </h2>
              <p className="mt-1 text-sm text-[var(--fm-text-muted)]">
                {todayTopic.promptSummary}
              </p>
            </div>

            {todayTopic.scheduleAdjustedNote && (
              <div className="rounded-xl bg-[var(--fm-tertiary-container)]/40 px-3 py-2 text-xs text-[var(--fm-text)]">
                <strong>Schedule Note:</strong>{" "}
                {todayTopic.scheduleAdjustedNote}
              </div>
            )}

            <div className="rounded-2xl bg-[var(--fm-surface-variant)]/70 p-4">
              <div className="text-xs font-semibold uppercase tracking-wide text-[var(--fm-primary)]">
                Starter Prompt
              </div>
              <p className="mt-1 text-sm font-medium text-[var(--fm-text)]">
                &ldquo;{todayTopic.starterQuestion}&rdquo;
              </p>
              {todayTopic.marathiHint && (
                <p className="mt-2 text-xs text-[var(--fm-secondary)]">
                  <strong>Hint (Marathi):</strong> {todayTopic.marathiHint}
                </p>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-2.5 pt-1">
              <button
                type="button"
                onClick={() => onPracticeTopicInChat(todayTopic)}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-[var(--fm-primary)] px-4 py-2 text-xs font-semibold text-[var(--fm-on-primary)] transition hover:opacity-90 cursor-pointer sm:text-sm"
              >
                <MessageSquare className="h-4 w-4" />
                <span>Practice in Chat</span>
              </button>
              <button
                type="button"
                onClick={() => onPracticeTopicInVoice(todayTopic)}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-[var(--fm-secondary)]/50 bg-[var(--fm-secondary-container)]/60 px-4 py-2 text-xs font-semibold text-[var(--fm-on-secondary-container)] transition hover:bg-[var(--fm-secondary-container)] cursor-pointer sm:text-sm"
              >
                <Mic className="h-4 w-4" />
                <span>Practice in Voice</span>
              </button>
              {!todayTopic.completed && (
                <button
                  type="button"
                  onClick={() => onMarkTopicDone(todayTopic.dateKey)}
                  className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-[var(--fm-outline)] px-4 py-2 text-xs font-semibold text-[var(--fm-text)] transition hover:bg-[var(--fm-surface-variant)] cursor-pointer sm:text-sm"
                >
                  <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
                  <span>Mark Done</span>
                </button>
              )}
              <button
                type="button"
                onClick={onGenerateTodayTopic}
                disabled={isGeneratingTopic}
                className="inline-flex min-h-11 items-center gap-1.5 rounded-xl px-3 py-2 text-xs font-semibold text-[var(--fm-text-muted)] transition hover:bg-[var(--fm-surface-variant)] disabled:opacity-50 cursor-pointer"
              >
                <RefreshCw
                  className={`h-4 w-4 ${isGeneratingTopic ? "animate-spin" : ""}`}
                />
                <span>New Topic</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="mt-4 flex flex-col items-start justify-between gap-4 rounded-2xl bg-[var(--fm-surface-variant)]/55 p-4 sm:flex-row sm:items-center">
            <div>
              <h3 className="font-display text-base font-bold text-[var(--fm-text)]">
                Ready for today&apos;s {scheduledLabel}?
              </h3>
              <p className="mt-1 text-xs text-[var(--fm-text-muted)]">
                Generate a personalized{" "}
                {ENGLISH_LEVELS[profile.englishLevel].label} topic with a
                Roman Marathi speaking hint.
              </p>
            </div>
            <button
              type="button"
              onClick={onGenerateTodayTopic}
              disabled={isGeneratingTopic}
              className="inline-flex min-h-11 shrink-0 items-center gap-2 rounded-xl bg-[var(--fm-primary)] px-4 py-2 text-sm font-semibold text-[var(--fm-on-primary)] transition hover:opacity-90 disabled:opacity-60 cursor-pointer"
            >
              <Sparkles
                className={`h-4 w-4 ${isGeneratingTopic ? "animate-pulse" : ""}`}
              />
              <span>
                {isGeneratingTopic
                  ? "Generating..."
                  : "Generate Today's Topic"}
              </span>
            </button>
          </div>
        )}
      </section>

      {/* Real Firestore KPI Grid */}
      <section className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
        <StatKpiCard
          title="Conversations"
          value={String(profile.totalConversations)}
          subtitle="Total sessions"
          icon={<MessageSquare className="h-4 w-4" />}
        />
        <StatKpiCard
          title="Topics Done"
          value={String(profile.topicsCompletedCount)}
          subtitle="Daily goals"
          icon={<CalendarCheck className="h-4 w-4" />}
          accentBgClass="bg-[var(--fm-secondary)]/15 text-[var(--fm-secondary)]"
        />
        <StatKpiCard
          title="Practice Time"
          value={formatPracticeDuration(profile.totalPracticeSeconds)}
          subtitle="Active learning"
          icon={<Clock className="h-4 w-4" />}
        />
        <StatKpiCard
          title="Grammar Tips"
          value={String(profile.grammarMistakesDetected)}
          subtitle="Corrections learned"
          icon={<Lightbulb className="h-4 w-4" />}
          accentBgClass="bg-[var(--fm-tertiary)]/15 text-[var(--fm-tertiary)]"
        />
        <StatKpiCard
          title="Vocabulary"
          value={String(profile.vocabularyLearnedCount)}
          subtitle="Words mastered"
          icon={<BookOpen className="h-4 w-4" />}
          accentBgClass="bg-[var(--fm-secondary)]/15 text-[var(--fm-secondary)]"
        />
        <StatKpiCard
          title="Current Level"
          value={profile.englishLevel}
          subtitle={ENGLISH_LEVELS[profile.englishLevel].label}
          icon={<GraduationCap className="h-4 w-4" />}
        />
      </section>

      {/* Real Charts */}
      <section className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <AccuracyRingChart
          totalMessages={profile.totalMessagesSent}
          totalMistakes={profile.grammarMistakesDetected}
        />
        <WeeklyPracticeBarChart stats={dailyStats} />
      </section>
    </div>
  );
}
