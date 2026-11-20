import { ClipboardList, Languages, Volume2, X } from "lucide-react";
import { InterviewReport } from "../types/models";

interface InterviewReportModalProps {
  report: InterviewReport;
  onClose: () => void;
  onSpeakText?: (text: string) => void;
}

export function InterviewReportModal({
  report,
  onClose,
  onSpeakText,
}: InterviewReportModalProps) {
  const sections = [
    { title: "Grammar Improvement", body: report.grammarImprovement },
    { title: "Vocabulary Feedback", body: report.vocabularyFeedback },
    { title: "Sentence Formation", body: report.sentenceFormation },
    { title: "Answer Clarity", body: report.answerClarity },
    { title: "Communication Feedback", body: report.communicationFeedback },
    { title: "Suggested Better Answers", body: report.suggestedBetterAnswers },
    { title: "Areas to Improve", body: report.areasToImprove },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4 backdrop-blur-xs"
      role="dialog"
      aria-modal="true"
      aria-label="Interview Performance Report"
    >
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-3xl border border-[var(--fm-outline)] bg-[var(--fm-surface)] shadow-2xl">
        <div className="flex items-center justify-between gap-3 border-b border-[var(--fm-outline-variant)] px-5 py-4">
          <div className="flex min-w-0 items-center gap-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[var(--fm-primary-container)] text-[var(--fm-primary)]">
              <ClipboardList className="h-5 w-5" />
            </div>
            <div className="min-w-0">
              <h2 className="truncate font-display text-lg font-bold text-[var(--fm-text)]">
                Interview Performance Report
              </h2>
              <p className="truncate text-xs font-medium text-[var(--fm-secondary)]">
                {report.interviewCategory} • {report.englishLevel} •{" "}
                {report.questionsAnswered} answers
              </p>
            </div>
          </div>
          <div className="flex items-center gap-1">
            {onSpeakText && (
              <button
                type="button"
                onClick={() => onSpeakText(report.overallSummary)}
                aria-label="Listen to summary"
                className="flex h-11 w-11 items-center justify-center rounded-full text-[var(--fm-primary)] transition hover:bg-[var(--fm-surface-variant)] cursor-pointer"
              >
                <Volume2 className="h-5 w-5" />
              </button>
            )}
            <button
              type="button"
              onClick={onClose}
              aria-label="Close report"
              className="flex h-11 w-11 items-center justify-center rounded-full text-[var(--fm-text-muted)] transition hover:bg-[var(--fm-surface-variant)] cursor-pointer"
            >
              <X className="h-5 w-5" />
            </button>
          </div>
        </div>

        <div className="flex-1 space-y-3 overflow-y-auto p-5">
          <div className="rounded-2xl bg-[var(--fm-primary-container)]/45 p-4">
            <div className="text-xs font-bold uppercase tracking-wide text-[var(--fm-primary)]">
              Overall Summary
            </div>
            <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-[var(--fm-text)]">
              {report.overallSummary}
            </p>
          </div>

          {sections.map((sec) => (
            <div
              key={sec.title}
              className="rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/45 p-3.5"
            >
              <div className="text-xs font-bold text-[var(--fm-primary)]">
                {sec.title}
              </div>
              <p className="mt-1 whitespace-pre-wrap text-sm leading-relaxed text-[var(--fm-text)]">
                {sec.body}
              </p>
            </div>
          ))}

          <div className="flex items-start gap-2.5 rounded-2xl bg-[var(--fm-secondary-container)]/60 p-4">
            <Languages className="mt-0.5 h-5 w-5 shrink-0 text-[var(--fm-secondary)]" />
            <div>
              <div className="text-xs font-bold text-[var(--fm-secondary)]">
                Marathi Coach Note
              </div>
              <p className="mt-1 whitespace-pre-wrap text-sm text-[var(--fm-on-secondary-container)]">
                {report.marathiCoachNote}
              </p>
            </div>
          </div>
        </div>

        <div className="flex justify-end border-t border-[var(--fm-outline-variant)] px-5 py-3">
          <button
            type="button"
            onClick={onClose}
            className="min-h-11 rounded-xl bg-[var(--fm-primary)] px-5 py-2 text-sm font-semibold text-[var(--fm-on-primary)] transition hover:opacity-90 cursor-pointer"
          >
            Done
          </button>
        </div>
      </div>
    </div>
  );
}
