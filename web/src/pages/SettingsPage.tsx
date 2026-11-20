import {
  Briefcase,
  Languages,
  LogOut,
  Monitor,
  Moon,
  Sun,
  Volume2,
} from "lucide-react";
import { LevelSelector } from "../components/LevelSelector";
import {
  EnglishLevel,
  MARATHI_SCRIPTS,
  MarathiScript,
  ThemeMode,
  UserProfile,
} from "../types/models";

interface SettingsPageProps {
  profile: UserProfile;
  userEmail: string;
  onUpdateSettings: (
    updates: Partial<{
      englishLevel: EnglishLevel;
      themeMode: ThemeMode;
      marathiScript: MarathiScript;
      autoSpeakAiReplies: boolean;
      speechRate: number;
      weeklyInterviewFrequency: number;
    }>
  ) => Promise<void>;
  onTestVoice: (text: string) => void;
  onSignOut: () => Promise<void>;
}

export function SettingsPage({
  profile,
  userEmail,
  onUpdateSettings,
  onTestVoice,
  onSignOut,
}: SettingsPageProps) {
  const initial =
    profile.displayName.trim().charAt(0).toUpperCase() || "F";

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="font-display text-2xl font-bold text-[var(--fm-text)]">
          Settings
        </h1>
        <p className="text-sm text-[var(--fm-text-muted)]">
          Customize your English level, Marathi explanation style, voice
          playback, interview frequency, and theme.
        </p>
      </div>

      {/* Account */}
      <section className="flex flex-wrap items-center justify-between gap-4 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <div className="flex items-center gap-3.5">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[var(--fm-primary)] font-display text-lg font-bold text-[var(--fm-on-primary)]">
            {initial}
          </div>
          <div>
            <div className="font-display text-base font-bold text-[var(--fm-text)]">
              {profile.displayName}
            </div>
            <div className="text-xs text-[var(--fm-text-muted)]">
              {userEmail || profile.email}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={() => void onSignOut()}
          className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-[var(--fm-error)]/45 px-4 py-2 text-xs font-semibold text-[var(--fm-error)] transition hover:bg-[var(--fm-error-container)]/40 cursor-pointer"
        >
          <LogOut className="h-4 w-4" />
          <span>Sign Out</span>
        </button>
      </section>

      {/* English Level */}
      <section className="space-y-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
          English Proficiency Level
        </h2>
        <LevelSelector
          selectedLevel={profile.englishLevel}
          onSelectLevel={(englishLevel) =>
            void onUpdateSettings({ englishLevel })
          }
        />
      </section>

      {/* Marathi Explanation Script */}
      <section className="space-y-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <div className="flex items-center gap-2">
          <Languages className="h-5 w-5 text-[var(--fm-secondary)]" />
          <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
            Marathi Explanation Style
          </h2>
        </div>
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
          {(["ROMAN", "ROMAN_AND_DEVANAGARI"] as MarathiScript[]).map(
            (script) => {
              const active = profile.marathiScript === script;
              return (
                <button
                  key={script}
                  type="button"
                  onClick={() => void onUpdateSettings({ marathiScript: script })}
                  className={`rounded-2xl border p-4 text-left transition cursor-pointer ${
                    active
                      ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)]/35"
                      : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40"
                  }`}
                >
                  <div className="font-display text-sm font-bold text-[var(--fm-text)]">
                    {MARATHI_SCRIPTS[script].label}
                  </div>
                  <p className="mt-1 text-xs text-[var(--fm-text-muted)]">
                    {MARATHI_SCRIPTS[script].description}
                  </p>
                </button>
              );
            }
          )}
        </div>
      </section>

      {/* Voice Settings */}
      <section className="space-y-4 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Volume2 className="h-5 w-5 text-[var(--fm-primary)]" />
            <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
              Voice &amp; Speech Playback
            </h2>
          </div>
          <button
            type="button"
            onClick={() =>
              onTestVoice(
                "Hello! FluentMate AI is ready to help you speak English confidently."
              )
            }
            className="min-h-9 rounded-xl bg-[var(--fm-secondary-container)] px-3 py-1.5 text-xs font-semibold text-[var(--fm-on-secondary-container)] cursor-pointer"
          >
            Test Voice
          </button>
        </div>

        <label className="flex cursor-pointer items-center justify-between gap-3 rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40 p-4">
          <div>
            <div className="text-sm font-semibold text-[var(--fm-text)]">
              Auto-speak AI replies in Voice mode
            </div>
            <div className="text-xs text-[var(--fm-text-muted)]">
              Automatically read FluentMate AI responses aloud after you speak.
            </div>
          </div>
          <input
            type="checkbox"
            checked={profile.autoSpeakAiReplies}
            onChange={(e) =>
              void onUpdateSettings({ autoSpeakAiReplies: e.target.checked })
            }
            className="h-5 w-5 accent-[var(--fm-primary)] cursor-pointer"
          />
        </label>

        <div className="rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40 p-4">
          <div className="flex items-center justify-between text-sm font-semibold text-[var(--fm-text)]">
            <span>Speech Speed</span>
            <span className="font-mono-code text-xs text-[var(--fm-primary)]">
              {profile.speechRate.toFixed(2)}x
            </span>
          </div>
          <input
            type="range"
            min={0.6}
            max={1.4}
            step={0.05}
            value={profile.speechRate}
            onChange={(e) =>
              void onUpdateSettings({ speechRate: Number(e.target.value) })
            }
            className="mt-2 w-full accent-[var(--fm-primary)] cursor-pointer"
          />
          <div className="mt-1 flex justify-between text-[11px] text-[var(--fm-text-muted)]">
            <span>Slower (0.6x)</span>
            <span>Normal (1.0x)</span>
            <span>Faster (1.4x)</span>
          </div>
        </div>
      </section>

      {/* Weekly Interview Frequency */}
      <section className="space-y-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <div className="flex items-center gap-2">
          <Briefcase className="h-5 w-5 text-[var(--fm-tertiary)]" />
          <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
            Weekly Interview Practice Frequency
          </h2>
        </div>
        <p className="text-xs text-[var(--fm-text-muted)]">
          Keep mock interviews balanced at 1 or 2 sessions per week (default 2:
          Tuesday &amp; Friday).
        </p>
        <div className="grid grid-cols-2 gap-3">
          {[1, 2].map((freq) => {
            const active = profile.weeklyInterviewFrequency === freq;
            return (
              <button
                key={freq}
                type="button"
                onClick={() =>
                  void onUpdateSettings({ weeklyInterviewFrequency: freq })
                }
                className={`min-h-12 rounded-2xl border p-3.5 text-left transition cursor-pointer ${
                  active
                    ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)]/35"
                    : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40"
                }`}
              >
                <div className="font-display text-sm font-bold text-[var(--fm-text)]">
                  {freq} {freq === 1 ? "session" : "sessions"} / week
                </div>
                <div className="text-xs text-[var(--fm-text-muted)]">
                  {freq === 1 ? "Fridays only" : "Tuesdays & Fridays (Default)"}
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Theme */}
      <section className="space-y-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-5">
        <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
          Appearance Theme
        </h2>
        <div className="grid grid-cols-3 gap-3">
          {(
            [
              { mode: "SYSTEM", label: "System", Icon: Monitor },
              { mode: "LIGHT", label: "Light", Icon: Sun },
              { mode: "DARK", label: "Dark", Icon: Moon },
            ] as const
          ).map(({ mode, label, Icon }) => {
            const active = profile.themeMode === mode;
            return (
              <button
                key={mode}
                type="button"
                onClick={() => void onUpdateSettings({ themeMode: mode })}
                className={`flex min-h-12 flex-col items-center justify-center gap-1.5 rounded-2xl border p-3 text-xs font-semibold transition cursor-pointer ${
                  active
                    ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                    : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40 text-[var(--fm-text-muted)]"
                }`}
              >
                <Icon className="h-4 w-4" />
                <span>{label}</span>
              </button>
            );
          })}
        </div>
      </section>
    </div>
  );
}
