import {
  BookOpen,
  Briefcase,
  CalendarCheck,
  Clock,
  Flame,
  Lightbulb,
  MessageSquare,
} from "lucide-react";
import {
  AccuracyRingChart,
  StatKpiCard,
  WeeklyPracticeBarChart,
} from "../components/ProgressCharts";
import {
  ChatSession,
  DailyProgressStat,
  DailyTopicRecord,
  InterviewReport,
  UserProfile,
  VocabularyWord,
} from "../types/models";

interface ProgressPageProps {
  profile: UserProfile;
  dailyStats: DailyProgressStat[];
  sessions: ChatSession[];
  topics: DailyTopicRecord[];
  words: VocabularyWord[];
  reports: InterviewReport[];
}

function formatMins(seconds: number): string {
  const mins = Math.floor(seconds / 60);
  if (mins < 60) return `${mins}m`;
  return `${Math.floor(mins / 60)}h ${mins % 60}m`;
}

export function ProgressPage({
  profile,
  dailyStats,
  sessions,
  topics,
  words,
  reports,
}: ProgressPageProps) {
  const chatSessionsCount = sessions.filter((s) => s.mode !== "VOICE").length;
  const voiceSessionsCount = sessions.filter((s) => s.mode === "VOICE").length;
  const learnedWordsCount = words.filter((w) => w.learned).length;
  const completedTopicsCount = topics.filter((t) => t.completed).length;

  const vocabPct =
    words.length > 0 ? Math.round((learnedWordsCount / words.length) * 100) : 0;
  const topicsPct =
    topics.length > 0
      ? Math.round((completedTopicsCount / topics.length) * 100)
      : 0;

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-[var(--fm-text)]">
          Learning Progress &amp; Analytics
        </h1>
        <p className="text-sm text-[var(--fm-text-muted)]">
          Live metrics computed from your real conversations, topics,
          interviews, and vocabulary bank.
        </p>
      </div>

      {/* KPI Grid */}
      <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatKpiCard
          title="Daily Streak"
          value={`${profile.currentStreakDays}d`}
          subtitle={
            profile.lastPracticeDate
              ? `Last active: ${profile.lastPracticeDate}`
              : "Start practicing today"
          }
          icon={<Flame className="h-4 w-4" />}
          accentBgClass="bg-[var(--fm-tertiary)]/15 text-[var(--fm-tertiary)]"
        />
        <StatKpiCard
          title="Active Time"
          value={formatMins(profile.totalPracticeSeconds)}
          subtitle={`${profile.totalMessagesSent} messages analyzed`}
          icon={<Clock className="h-4 w-4" />}
        />
        <StatKpiCard
          title="Sessions"
          value={String(sessions.length)}
          subtitle={`${chatSessionsCount} Chat • ${voiceSessionsCount} Voice`}
          icon={<MessageSquare className="h-4 w-4" />}
          accentBgClass="bg-[var(--fm-secondary)]/15 text-[var(--fm-secondary)]"
        />
        <StatKpiCard
          title="Grammar Tips"
          value={String(profile.grammarMistakesDetected)}
          subtitle="Mistakes corrected"
          icon={<Lightbulb className="h-4 w-4" />}
          accentBgClass="bg-[var(--fm-tertiary)]/15 text-[var(--fm-tertiary)]"
        />
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <AccuracyRingChart
          totalMessages={profile.totalMessagesSent}
          totalMistakes={profile.grammarMistakesDetected}
        />
        <WeeklyPracticeBarChart stats={dailyStats} />
      </div>

      {/* Mastery Breakdown */}
      <section className="grid grid-cols-1 gap-4 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5 md:grid-cols-3">
        <div className="space-y-2 rounded-2xl bg-[var(--fm-surface-variant)]/45 p-4">
          <div className="flex items-center justify-between text-xs font-bold text-[var(--fm-text)]">
            <span className="flex items-center gap-1.5">
              <BookOpen className="h-4 w-4 text-[var(--fm-secondary)]" />
              Vocabulary Learned
            </span>
            <span>
              {learnedWordsCount} / {words.length}
            </span>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-[var(--fm-outline-variant)]">
            <div
              style={{ width: `${vocabPct}%` }}
              className="h-full rounded-full bg-[var(--fm-secondary)] transition-all"
            />
          </div>
          <p className="text-[11px] text-[var(--fm-text-muted)]">
            {words.length > 0
              ? `${vocabPct}% of saved words mastered`
              : "Save words in Vocabulary to track mastery"}
          </p>
        </div>

        <div className="space-y-2 rounded-2xl bg-[var(--fm-surface-variant)]/45 p-4">
          <div className="flex items-center justify-between text-xs font-bold text-[var(--fm-text)]">
            <span className="flex items-center gap-1.5">
              <CalendarCheck className="h-4 w-4 text-[var(--fm-primary)]" />
              Daily Topics Done
            </span>
            <span>
              {completedTopicsCount} / {topics.length}
            </span>
          </div>
          <div className="h-2.5 overflow-hidden rounded-full bg-[var(--fm-outline-variant)]">
            <div
              style={{ width: `${topicsPct}%` }}
              className="h-full rounded-full bg-[var(--fm-primary)] transition-all"
            />
          </div>
          <p className="text-[11px] text-[var(--fm-text-muted)]">
            {topics.length > 0
              ? `${topicsPct}% of generated daily topics completed`
              : "Generate your first Daily Topic to begin"}
          </p>
        </div>

        <div className="space-y-2 rounded-2xl bg-[var(--fm-surface-variant)]/45 p-4">
          <div className="flex items-center justify-between text-xs font-bold text-[var(--fm-text)]">
            <span className="flex items-center gap-1.5">
              <Briefcase className="h-4 w-4 text-[var(--fm-tertiary)]" />
              Mock Interviews
            </span>
            <span>{reports.length}</span>
          </div>
          <div className="font-display text-2xl font-bold text-[var(--fm-text)]">
            {reports.length}{" "}
            <span className="text-xs font-medium text-[var(--fm-text-muted)]">
              reports generated
            </span>
          </div>
          <p className="text-[11px] text-[var(--fm-text-muted)]">
            Weekly schedule: {profile.weeklyInterviewFrequency}x per week
          </p>
        </div>
      </section>

      {/* Daily Logs */}
      <section className="rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
          Recent Daily Logs
        </h2>
        {dailyStats.length === 0 ? (
          <p className="mt-3 text-xs text-[var(--fm-text-muted)]">
            No daily logs recorded yet. Chat or speak with FluentMate AI to
            create today&apos;s log.
          </p>
        ) : (
          <div className="mt-3 divide-y divide-[var(--fm-outline-variant)]">
            {dailyStats.map((stat) => (
              <div
                key={stat.id}
                className="flex flex-wrap items-center justify-between gap-2 py-3 text-xs"
              >
                <span className="font-mono-code font-bold text-[var(--fm-text)]">
                  {stat.dateKey}
                </span>
                <div className="flex flex-wrap items-center gap-4 text-[var(--fm-text-muted)]">
                  <span>{stat.messagesCount} messages</span>
                  <span>{stat.mistakesCount} tips</span>
                  <span>{Math.floor(stat.practiceSeconds / 60)}m active</span>
                  <span>{stat.wordsLearnedCount} words</span>
                  <span>{stat.topicsCompletedCount} topics</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
