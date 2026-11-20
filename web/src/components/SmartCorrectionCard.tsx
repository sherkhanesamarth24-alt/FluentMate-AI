import {
  BookmarkPlus,
  CheckCircle2,
  Languages,
  Lightbulb,
  Sparkles,
  Volume2,
} from "lucide-react";
import { ChatMessage, GeneratedWordDraft } from "../types/models";

interface SmartCorrectionCardProps {
  message: ChatMessage;
  isInterviewMode?: boolean;
  onSpeakText: (text: string) => void;
  onSaveSuggestedWord?: (draft: GeneratedWordDraft) => void;
}

export function SmartCorrectionCard({
  message,
  isInterviewMode = false,
  onSpeakText,
  onSaveSuggestedWord,
}: SmartCorrectionCardProps) {
  const corrected = message.correctedSentence?.trim() || null;
  const hasMistake = Boolean(message.hasMistake && corrected);
  const naturalAlt =
    message.naturalAlternative?.trim() &&
    message.naturalAlternative.trim() !== corrected
      ? message.naturalAlternative.trim()
      : null;

  return (
    <div className="flex w-full flex-col gap-2.5">
      {hasMistake && corrected && (
        <div className="rounded-2xl border border-[var(--fm-outline)]/60 bg-[var(--fm-surface-variant)]/80 p-4">
          <div className="mb-2 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-[var(--fm-tertiary)]">
              <Lightbulb className="h-4 w-4" />
              <span>
                {message.mistakeCategory && message.mistakeCategory !== "None"
                  ? message.mistakeCategory
                  : "Grammar Tip"}
              </span>
            </div>
            <button
              type="button"
              onClick={() => onSpeakText(corrected)}
              aria-label="Listen to corrected sentence"
              className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--fm-primary)] transition hover:bg-[var(--fm-primary-container)]/50 cursor-pointer"
            >
              <Volume2 className="h-4 w-4" />
            </button>
          </div>

          {message.originalSentence && (
            <div className="mb-2">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-[var(--fm-error)]">
                Original
              </div>
              <p className="text-sm text-[var(--fm-text-muted)] line-through">
                {message.originalSentence}
              </p>
            </div>
          )}

          <div>
            <div className="text-[11px] font-semibold uppercase tracking-wide text-[var(--fm-primary)]">
              Correct English
            </div>
            <p className="text-sm font-semibold text-[var(--fm-text)]">
              {corrected}
            </p>
          </div>

          {message.marathiExplanation && (
            <div className="mt-3 flex items-start gap-2.5 rounded-xl bg-[var(--fm-secondary-container)]/65 p-3">
              <Languages className="mt-0.5 h-4 w-4 shrink-0 text-[var(--fm-secondary)]" />
              <div>
                <div className="text-[11px] font-semibold uppercase tracking-wide text-[var(--fm-secondary)]">
                  Marathi Explanation
                </div>
                <p className="text-sm text-[var(--fm-on-secondary-container)]">
                  {message.marathiExplanation}
                </p>
              </div>
            </div>
          )}

          {!isInterviewMode && naturalAlt && (
            <div className="mt-3 border-t border-[var(--fm-outline-variant)] pt-2.5">
              <div className="text-[11px] font-semibold uppercase tracking-wide text-[var(--fm-secondary)]">
                Natural Alternative
              </div>
              <p className="text-sm text-[var(--fm-text)]">{naturalAlt}</p>
            </div>
          )}
        </div>
      )}

      {isInterviewMode && naturalAlt && (
        <div className="rounded-2xl border border-[var(--fm-primary)]/45 bg-[var(--fm-primary-container)]/35 p-4">
          <div className="mb-1 flex items-center justify-between gap-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-[var(--fm-primary)]">
              <Sparkles className="h-4 w-4" />
              <span>Professional Interview Answer</span>
            </div>
            <button
              type="button"
              onClick={() => onSpeakText(naturalAlt)}
              aria-label="Listen to professional interview answer"
              className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--fm-primary)] transition hover:bg-[var(--fm-primary-container)] cursor-pointer"
            >
              <Volume2 className="h-4 w-4" />
            </button>
          </div>
          <p className="text-sm leading-relaxed text-[var(--fm-text)]">
            {naturalAlt}
          </p>
        </div>
      )}

      {/* Conversational AI Reply Bubble */}
      <div className="rounded-2xl rounded-tl-md border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4 shadow-xs">
        <div className="mb-1.5 flex items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-[var(--fm-primary)]" />
            <span className="text-xs font-semibold text-[var(--fm-primary)]">
              {isInterviewMode ? "AI Interviewer" : "FluentMate AI"}
            </span>
            {message.role === "assistant" &&
              !hasMistake &&
              message.mistakeCategory === "None" && (
                <CheckCircle2
                  className="h-3.5 w-3.5 text-[var(--fm-primary)]"
                  aria-label="Great English"
                />
              )}
          </div>
          <button
            type="button"
            onClick={() => onSpeakText(message.text)}
            aria-label="Speak AI reply"
            className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--fm-secondary)] transition hover:bg-[var(--fm-secondary-container)]/50 cursor-pointer"
          >
            <Volume2 className="h-4 w-4" />
          </button>
        </div>

        <p className="whitespace-pre-wrap text-[15px] leading-relaxed text-[var(--fm-text)]">
          {message.text}
        </p>

        {message.suggestedWord && onSaveSuggestedWord && (
          <div className="mt-3 border-t border-[var(--fm-outline-variant)] pt-3">
            <button
              type="button"
              onClick={() =>
                onSaveSuggestedWord({
                  word: message.suggestedWord!,
                  marathiMeaning:
                    message.suggestedWordMarathi || "Helpful word",
                  exampleSentence: message.suggestedWordExample || message.text,
                  pronunciation: message.suggestedWordPronunciation || "",
                  partOfSpeech: message.suggestedWordPos || "word",
                })
              }
              className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-[var(--fm-primary)]/40 bg-[var(--fm-primary-container)]/50 px-3 py-1.5 text-xs font-semibold text-[var(--fm-on-primary-container)] transition hover:bg-[var(--fm-primary-container)] cursor-pointer"
            >
              <BookmarkPlus className="h-4 w-4" />
              <span>
                Save word: {message.suggestedWord}
                {message.suggestedWordMarathi
                  ? ` (${message.suggestedWordMarathi})`
                  : ""}
              </span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
