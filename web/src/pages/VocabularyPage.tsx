import { useMemo, useState } from "react";
import {
  BookmarkPlus,
  Check,
  CheckCircle2,
  Circle,
  Loader2,
  Plus,
  Search,
  Sparkles,
  Trash2,
  Volume2,
  X,
} from "lucide-react";
import {
  GeneratedWordDraft,
  UserProfile,
  VocabularyWord,
} from "../types/models";

interface VocabularyPageProps {
  profile: UserProfile;
  words: VocabularyWord[];
  dailySuggestions: GeneratedWordDraft[];
  isLoadingSuggestions: boolean;
  onGenerateDailyWords: () => Promise<void>;
  onSaveSuggestion: (draft: GeneratedWordDraft) => Promise<void>;
  onAddManualWord: (draft: GeneratedWordDraft) => Promise<void>;
  onAiFillWord: (word: string) => Promise<GeneratedWordDraft>;
  onToggleLearned: (word: VocabularyWord) => Promise<void>;
  onDeleteWord: (word: VocabularyWord) => Promise<void>;
  onSpeakText: (text: string) => void;
}

type VocabFilter = "ALL" | "TO_REVIEW" | "LEARNED";

export function VocabularyPage({
  profile,
  words,
  dailySuggestions,
  isLoadingSuggestions,
  onGenerateDailyWords,
  onSaveSuggestion,
  onAddManualWord,
  onAiFillWord,
  onToggleLearned,
  onDeleteWord,
  onSpeakText,
}: VocabularyPageProps) {
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<VocabFilter>("ALL");
  const [addOpen, setAddOpen] = useState(false);

  const [newWord, setNewWord] = useState("");
  const [newMarathi, setNewMarathi] = useState("");
  const [newExample, setNewExample] = useState("");
  const [newPronunciation, setNewPronunciation] = useState("");
  const [newPos, setNewPos] = useState("word");
  const [isAiFilling, setIsAiFilling] = useState(false);
  const [isSavingModal, setIsSavingModal] = useState(false);

  const savedWordSet = useMemo(
    () => new Set(words.map((w) => w.word.trim().toLowerCase())),
    [words]
  );

  const filteredWords = useMemo(() => {
    const q = search.trim().toLowerCase();
    return words.filter((w) => {
      if (filter === "LEARNED" && !w.learned) return false;
      if (filter === "TO_REVIEW" && w.learned) return false;
      if (!q) return true;
      return (
        w.word.toLowerCase().includes(q) ||
        w.marathiMeaning.toLowerCase().includes(q) ||
        w.exampleSentence.toLowerCase().includes(q)
      );
    });
  }, [words, filter, search]);

  const learnedCount = words.filter((w) => w.learned).length;
  const reviewCount = words.length - learnedCount;

  const handleAiFill = async () => {
    const clean = newWord.trim();
    if (!clean || isAiFilling) return;
    setIsAiFilling(true);
    try {
      const filled = await onAiFillWord(clean);
      setNewWord(filled.word || clean);
      setNewMarathi(filled.marathiMeaning);
      setNewExample(filled.exampleSentence);
      setNewPronunciation(filled.pronunciation);
      setNewPos(filled.partOfSpeech || "word");
    } finally {
      setIsAiFilling(false);
    }
  };

  const handleSaveManual = async () => {
    if (!newWord.trim() || !newMarathi.trim() || isSavingModal) return;
    setIsSavingModal(true);
    try {
      await onAddManualWord({
        word: newWord.trim(),
        marathiMeaning: newMarathi.trim(),
        exampleSentence:
          newExample.trim() || `I use "${newWord.trim()}" in daily English.`,
        pronunciation: newPronunciation.trim(),
        partOfSpeech: newPos.trim() || "word",
      });
      setNewWord("");
      setNewMarathi("");
      setNewExample("");
      setNewPronunciation("");
      setNewPos("word");
      setAddOpen(false);
    } finally {
      setIsSavingModal(false);
    }
  };

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-2xl font-bold text-[var(--fm-text)]">
            Vocabulary Builder
          </h1>
          <p className="text-sm text-[var(--fm-text-muted)]">
            Level {profile.englishLevel} words with Roman Marathi meanings,
            pronunciations &amp; examples.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            disabled={isLoadingSuggestions}
            onClick={() => void onGenerateDailyWords()}
            className="inline-flex min-h-11 items-center gap-2 rounded-2xl border border-[var(--fm-secondary)]/50 bg-[var(--fm-secondary-container)] px-4 py-2 text-xs font-semibold text-[var(--fm-on-secondary-container)] cursor-pointer sm:text-sm"
          >
            {isLoadingSuggestions ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Sparkles className="h-4 w-4" />
            )}
            <span>Daily Words</span>
          </button>
          <button
            type="button"
            onClick={() => setAddOpen(true)}
            className="inline-flex min-h-11 items-center gap-2 rounded-2xl bg-[var(--fm-primary)] px-4 py-2 text-xs font-semibold text-[var(--fm-on-primary)] cursor-pointer sm:text-sm"
          >
            <Plus className="h-4 w-4" />
            <span>Add Word</span>
          </button>
        </div>
      </div>

      {/* AI Daily Word Suggestions */}
      {dailySuggestions.length > 0 && (
        <section className="space-y-3 rounded-3xl border border-[var(--fm-secondary)]/40 bg-[var(--fm-secondary-container)]/25 p-5">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-[var(--fm-secondary)]" />
            <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
              Today&apos;s AI Recommended Words
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
            {dailySuggestions.map((item) => {
              const alreadySaved = savedWordSet.has(
                item.word.trim().toLowerCase()
              );
              return (
                <div
                  key={item.word}
                  className="flex flex-col justify-between rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <div className="font-display text-base font-bold text-[var(--fm-text)]">
                          {item.word}
                        </div>
                        <div className="text-xs text-[var(--fm-text-muted)]">
                          {item.partOfSpeech}
                          {item.pronunciation ? ` • ${item.pronunciation}` : ""}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => onSpeakText(item.word)}
                        aria-label={`Pronounce ${item.word}`}
                        className="flex h-9 w-9 items-center justify-center rounded-full text-[var(--fm-secondary)] hover:bg-[var(--fm-surface-variant)] cursor-pointer"
                      >
                        <Volume2 className="h-4 w-4" />
                      </button>
                    </div>
                    <p className="mt-2 text-xs font-semibold text-[var(--fm-primary)]">
                      Marathi: {item.marathiMeaning}
                    </p>
                    <p className="mt-1.5 text-xs italic text-[var(--fm-text-muted)]">
                      &ldquo;{item.exampleSentence}&rdquo;
                    </p>
                  </div>
                  <button
                    type="button"
                    disabled={alreadySaved}
                    onClick={() => void onSaveSuggestion(item)}
                    className={`mt-3 inline-flex min-h-10 w-full items-center justify-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold cursor-pointer ${
                      alreadySaved
                        ? "bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                        : "bg-[var(--fm-primary)] text-[var(--fm-on-primary)]"
                    }`}
                  >
                    {alreadySaved ? (
                      <>
                        <Check className="h-4 w-4" />
                        <span>Saved</span>
                      </>
                    ) : (
                      <>
                        <BookmarkPlus className="h-4 w-4" />
                        <span>Save to My Words</span>
                      </>
                    )}
                  </button>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* Search + Filter */}
      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="relative flex-1 max-w-md">
          <Search className="pointer-events-none absolute top-1/2 left-3.5 h-4 w-4 -translate-y-1/2 text-[var(--fm-text-muted)]" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search English words or Marathi meanings..."
            className="min-h-11 w-full rounded-2xl border border-[var(--fm-outline)] bg-[var(--fm-surface)] pr-4 pl-10 text-sm text-[var(--fm-text)] focus:border-[var(--fm-primary)] focus:outline-none"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          {(
            [
              { id: "ALL", label: `All (${words.length})` },
              { id: "TO_REVIEW", label: `To Review (${reviewCount})` },
              { id: "LEARNED", label: `Learned (${learnedCount})` },
            ] as const
          ).map((tab) => (
            <button
              key={tab.id}
              type="button"
              onClick={() => setFilter(tab.id)}
              className={`min-h-10 rounded-xl border px-3.5 py-1.5 text-xs font-semibold cursor-pointer ${
                filter === tab.id
                  ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                  : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] text-[var(--fm-text-muted)]"
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Saved Words Grid */}
      {filteredWords.length === 0 ? (
        <div className="rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-10 text-center">
          <p className="font-display text-base font-bold text-[var(--fm-text)]">
            {words.length === 0
              ? "Your vocabulary bank is empty"
              : "No matching words"}
          </p>
          <p className="mt-1 text-xs text-[var(--fm-text-muted)]">
            Tap &ldquo;Daily Words&rdquo; to generate personalized words with
            Marathi meanings, or add your own word.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
          {filteredWords.map((word) => (
            <div
              key={word.id}
              className="flex flex-col justify-between rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <h3 className="font-display text-lg font-bold text-[var(--fm-text)]">
                        {word.word}
                      </h3>
                      <span className="rounded-md bg-[var(--fm-surface-variant)] px-2 py-0.5 text-[11px] font-semibold text-[var(--fm-text-muted)]">
                        {word.partOfSpeech}
                      </span>
                      <span className="rounded-md bg-[var(--fm-primary-container)]/60 px-2 py-0.5 font-mono-code text-[10px] font-bold text-[var(--fm-on-primary-container)]">
                        {word.englishLevel}
                      </span>
                    </div>
                    {word.pronunciation && (
                      <div className="mt-0.5 font-mono-code text-xs text-[var(--fm-text-muted)]">
                        {word.pronunciation}
                      </div>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => onSpeakText(`${word.word}. ${word.exampleSentence}`)}
                    aria-label={`Listen to ${word.word}`}
                    className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-[var(--fm-secondary)] hover:bg-[var(--fm-surface-variant)] cursor-pointer"
                  >
                    <Volume2 className="h-4 w-4" />
                  </button>
                </div>

                <div className="mt-3 rounded-xl bg-[var(--fm-secondary-container)]/50 px-3 py-2 text-xs text-[var(--fm-on-secondary-container)]">
                  <strong>Marathi Meaning:</strong> {word.marathiMeaning}
                </div>

                <p className="mt-2.5 text-sm text-[var(--fm-text)]">
                  &ldquo;{word.exampleSentence}&rdquo;
                </p>
              </div>

              <div className="mt-4 flex items-center justify-between border-t border-[var(--fm-outline-variant)] pt-3">
                <button
                  type="button"
                  onClick={() => void onToggleLearned(word)}
                  className={`inline-flex min-h-10 items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold cursor-pointer ${
                    word.learned
                      ? "bg-[var(--fm-primary-container)] text-[var(--fm-on-primary-container)]"
                      : "border border-[var(--fm-outline)] text-[var(--fm-text)] hover:bg-[var(--fm-surface-variant)]"
                  }`}
                >
                  {word.learned ? (
                    <>
                      <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
                      <span>Learned</span>
                    </>
                  ) : (
                    <>
                      <Circle className="h-4 w-4 text-[var(--fm-text-muted)]" />
                      <span>Mark Learned</span>
                    </>
                  )}
                </button>

                <button
                  type="button"
                  onClick={() => void onDeleteWord(word)}
                  aria-label={`Delete ${word.word}`}
                  className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--fm-error)] hover:bg-[var(--fm-error-container)]/45 cursor-pointer"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Add Word Modal */}
      {addOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-label="Add Vocabulary Word"
        >
          <div className="w-full max-w-md rounded-3xl border border-[var(--fm-outline)] bg-[var(--fm-surface)] p-5 shadow-2xl">
            <div className="flex items-center justify-between">
              <h2 className="font-display text-lg font-bold text-[var(--fm-text)]">
                Add Vocabulary Word
              </h2>
              <button
                type="button"
                onClick={() => setAddOpen(false)}
                className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-[var(--fm-surface-variant)] cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="mt-4 space-y-3">
              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--fm-text-muted)]">
                  English Word
                </label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={newWord}
                    onChange={(e) => setNewWord(e.target.value)}
                    placeholder="e.g. Resilient"
                    className="min-h-11 flex-1 rounded-xl border border-[var(--fm-outline)] bg-[var(--fm-bg)] px-3 text-sm text-[var(--fm-text)]"
                  />
                  <button
                    type="button"
                    disabled={!newWord.trim() || isAiFilling}
                    onClick={() => void handleAiFill()}
                    className="inline-flex min-h-11 items-center gap-1.5 rounded-xl bg-[var(--fm-secondary-container)] px-3 text-xs font-semibold text-[var(--fm-on-secondary-container)] disabled:opacity-50 cursor-pointer"
                  >
                    {isAiFilling ? (
                      <Loader2 className="h-4 w-4 animate-spin" />
                    ) : (
                      <Sparkles className="h-4 w-4" />
                    )}
                    <span>AI Fill</span>
                  </button>
                </div>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--fm-text-muted)]">
                  Marathi Meaning
                </label>
                <input
                  type="text"
                  value={newMarathi}
                  onChange={(e) => setNewMarathi(e.target.value)}
                  placeholder="e.g. Kathin paristhitit punha ubhe rahanara"
                  className="min-h-11 w-full rounded-xl border border-[var(--fm-outline)] bg-[var(--fm-bg)] px-3 text-sm text-[var(--fm-text)]"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="mb-1 block text-xs font-semibold text-[var(--fm-text-muted)]">
                    Part of Speech
                  </label>
                  <input
                    type="text"
                    value={newPos}
                    onChange={(e) => setNewPos(e.target.value)}
                    placeholder="adjective"
                    className="min-h-11 w-full rounded-xl border border-[var(--fm-outline)] bg-[var(--fm-bg)] px-3 text-sm text-[var(--fm-text)]"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-xs font-semibold text-[var(--fm-text-muted)]">
                    Pronunciation
                  </label>
                  <input
                    type="text"
                    value={newPronunciation}
                    onChange={(e) => setNewPronunciation(e.target.value)}
                    placeholder="ri-zil-yuhnt"
                    className="min-h-11 w-full rounded-xl border border-[var(--fm-outline)] bg-[var(--fm-bg)] px-3 text-sm text-[var(--fm-text)]"
                  />
                </div>
              </div>

              <div>
                <label className="mb-1 block text-xs font-semibold text-[var(--fm-text-muted)]">
                  Example Sentence
                </label>
                <textarea
                  rows={2}
                  value={newExample}
                  onChange={(e) => setNewExample(e.target.value)}
                  placeholder="She stayed resilient during her interview preparation."
                  className="w-full rounded-xl border border-[var(--fm-outline)] bg-[var(--fm-bg)] p-3 text-sm text-[var(--fm-text)]"
                />
              </div>
            </div>

            <div className="mt-5 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setAddOpen(false)}
                className="min-h-11 rounded-xl px-4 py-2 text-xs font-semibold text-[var(--fm-text-muted)] hover:bg-[var(--fm-surface-variant)] cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={
                  !newWord.trim() || !newMarathi.trim() || isSavingModal
                }
                onClick={() => void handleSaveManual()}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-[var(--fm-primary)] px-5 py-2 text-xs font-semibold text-[var(--fm-on-primary)] disabled:opacity-50 cursor-pointer"
              >
                {isSavingModal && <Loader2 className="h-4 w-4 animate-spin" />}
                <span>Save Word</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
