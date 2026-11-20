import { ENGLISH_LEVELS, EnglishLevel } from "../types/models";

interface LevelSelectorProps {
  selectedLevel: EnglishLevel;
  onSelectLevel: (level: EnglishLevel) => void;
}

const ORDERED_LEVELS: EnglishLevel[] = ["LOW", "MEDIUM", "HIGH"];

export function LevelSelector({
  selectedLevel,
  onSelectLevel,
}: LevelSelectorProps) {
  return (
    <div className="grid grid-cols-3 gap-2">
      {ORDERED_LEVELS.map((lvl) => {
        const meta = ENGLISH_LEVELS[lvl];
        const active = selectedLevel === lvl;
        return (
          <button
            key={lvl}
            type="button"
            onClick={() => onSelectLevel(lvl)}
            className={`flex min-h-11 items-center justify-center gap-1.5 rounded-xl border px-2.5 py-2 text-xs font-semibold transition-all cursor-pointer sm:text-sm ${
              active
                ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)] shadow-xs"
                : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/50 text-[var(--fm-text-muted)] hover:border-[var(--fm-outline)] hover:text-[var(--fm-text)]"
            }`}
          >
            <span className="font-mono-code text-[11px] font-bold">
              {meta.badge}
            </span>
            <span className="hidden sm:inline">•</span>
            <span className="truncate">{meta.label}</span>
          </button>
        );
      })}
    </div>
  );
}
