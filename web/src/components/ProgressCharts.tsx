import { ReactNode } from "react";
import { Lightbulb } from "lucide-react";
import { DailyProgressStat } from "../types/models";

interface StatKpiCardProps {
  title: string;
  value: string;
  subtitle: string;
  icon: ReactNode;
  accentBgClass?: string;
}

export function StatKpiCard({
  title,
  value,
  subtitle,
  icon,
  accentBgClass = "bg-[var(--fm-primary)]/15 text-[var(--fm-primary)]",
}: StatKpiCardProps) {
  return (
    <div className="rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4">
      <div className="flex items-center justify-between gap-2">
        <span className="truncate text-xs font-semibold text-[var(--fm-text-muted)]">
          {title}
        </span>
        <div
          className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full ${accentBgClass}`}
        >
          {icon}
        </div>
      </div>
      <div className="mt-2 font-display text-2xl font-bold text-[var(--fm-text)]">
        {value}
      </div>
      <div className="mt-0.5 truncate text-xs text-[var(--fm-text-muted)]">
        {subtitle}
      </div>
    </div>
  );
}

export function AccuracyRingChart({
  totalMessages,
  totalMistakes,
}: {
  totalMessages: number;
  totalMistakes: number;
}) {
  const cleanMessages = Math.max(0, totalMessages - totalMistakes);
  const accuracyPct =
    totalMessages > 0
      ? Math.min(100, Math.max(0, Math.round((cleanMessages / totalMessages) * 100)))
      : 0;

  const radius = 38;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset =
    totalMessages > 0
      ? circumference - (accuracyPct / 100) * circumference
      : circumference;

  return (
    <div className="flex items-center gap-5 rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4">
      <div className="relative flex h-24 w-24 shrink-0 items-center justify-center">
        <svg className="h-24 w-24 -rotate-90" viewBox="0 0 96 96">
          <circle
            cx="48"
            cy="48"
            r={radius}
            fill="none"
            stroke="var(--fm-surface-variant)"
            strokeWidth="10"
          />
          {totalMessages > 0 && (
            <circle
              cx="48"
              cy="48"
              r={radius}
              fill="none"
              stroke="url(#accuracyGrad)"
              strokeWidth="10"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={strokeDashoffset}
            />
          )}
          <defs>
            <linearGradient id="accuracyGrad" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="var(--fm-primary)" />
              <stop offset="100%" stopColor="var(--fm-tertiary)" />
            </linearGradient>
          </defs>
        </svg>
        <span className="absolute font-display text-base font-bold text-[var(--fm-text)]">
          {totalMessages > 0 ? `${accuracyPct}%` : "--"}
        </span>
      </div>
      <div className="min-w-0 flex-1">
        <h3 className="font-display text-base font-semibold text-[var(--fm-text)]">
          Sentence Accuracy
        </h3>
        <p className="mt-1 text-sm text-[var(--fm-text-muted)]">
          {totalMessages > 0
            ? `${cleanMessages} clean sentences out of ${totalMessages} analyzed (${totalMistakes} grammar tips).`
            : "Send your first message to start tracking real accuracy."}
        </p>
      </div>
    </div>
  );
}

export function WeeklyPracticeBarChart({
  stats,
}: {
  stats: DailyProgressStat[];
}) {
  const ordered = [...stats].slice(0, 7).reverse();

  if (ordered.length === 0) {
    return (
      <div className="flex flex-col items-center rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/45 p-6 text-center">
        <Lightbulb className="h-8 w-8 text-[var(--fm-primary)]" />
        <h3 className="mt-2 font-display text-base font-semibold">
          No practice activity yet
        </h3>
        <p className="mt-1 max-w-md text-xs text-[var(--fm-text-muted)]">
          Send a message in AI Chat or practice with Voice to see your real
          daily progress chart here.
        </p>
      </div>
    );
  }

  const maxMsgs = Math.max(1, ...ordered.map((s) => s.messagesCount));
  const totalMsgs = ordered.reduce((acc, s) => acc + s.messagesCount, 0);

  return (
    <div className="rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4">
      <div className="mb-4 flex items-center justify-between gap-2">
        <div>
          <h3 className="font-display text-base font-semibold text-[var(--fm-text)]">
            Weekly Activity
          </h3>
          <p className="text-xs text-[var(--fm-text-muted)]">
            Real messages &amp; practice minutes per day
          </p>
        </div>
        <span className="text-xs font-bold text-[var(--fm-primary)]">
          {totalMsgs} msgs
        </span>
      </div>

      <div className="grid h-36 grid-cols-7 items-end gap-2">
        {ordered.map((stat) => {
          const pct =
            stat.messagesCount === 0
              ? 6
              : Math.max(10, Math.round((stat.messagesCount / maxMsgs) * 100));
          return (
            <div
              key={stat.dateKey}
              className="flex h-full flex-col items-center justify-end gap-1.5"
            >
              <div className="relative flex h-28 w-full max-w-8 items-end overflow-hidden rounded-lg bg-[var(--fm-surface-variant)]">
                <div
                  style={{ height: `${pct}%` }}
                  className="w-full rounded-lg bg-gradient-to-t from-[var(--fm-secondary)] to-[var(--fm-primary)] transition-all"
                  title={`${stat.dateKey}: ${stat.messagesCount} msgs, ${Math.floor(stat.practiceSeconds / 60)}m`}
                />
              </div>
              <span className="font-mono-code text-[10px] text-[var(--fm-text-muted)]">
                {stat.dateKey.slice(-5)}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
