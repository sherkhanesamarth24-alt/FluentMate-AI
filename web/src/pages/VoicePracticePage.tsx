import { useEffect, useRef, useState } from "react";
import {
  AlertCircle,
  Briefcase,
  ClipboardCheck,
  History,
  Loader2,
  Mic,
  MicOff,
  Plus,
  RotateCcw,
  Send,
  Square,
  Volume2,
  X,
} from "lucide-react";
import { LevelSelector } from "../components/LevelSelector";
import { SmartCorrectionCard } from "../components/SmartCorrectionCard";
import {
  createBrowserRecognizer,
  isBrowserSpeechRecognitionSupported,
  isBrowserTtsSupported,
} from "../lib/speech";
import {
  ChatMessage,
  ChatSession,
  EnglishLevel,
  GeneratedWordDraft,
  UserProfile,
} from "../types/models";

interface VoicePracticePageProps {
  profile: UserProfile;
  sessions: ChatSession[];
  activeSession: ChatSession | null;
  messages: ChatMessage[];
  isSending: boolean;
  isSpeaking: boolean;
  isGeneratingReport: boolean;
  onSelectLevel: (level: EnglishLevel) => void;
  onSelectSession: (sessionId: string) => void;
  onCreateNewVoiceSession: () => void;
  onSendVoiceMessage: (text: string) => Promise<void>;
  onSpeakText: (text: string) => void;
  onStopSpeaking: () => void;
  onSaveWord: (draft: GeneratedWordDraft) => Promise<void>;
  onFinishInterview: (session: ChatSession, messages: ChatMessage[]) => Promise<void>;
}

export function VoicePracticePage({
  profile,
  sessions,
  activeSession,
  messages,
  isSending,
  isSpeaking,
  isGeneratingReport,
  onSelectLevel,
  onSelectSession,
  onCreateNewVoiceSession,
  onSendVoiceMessage,
  onSpeakText,
  onStopSpeaking,
  onSaveWord,
  onFinishInterview,
}: VoicePracticePageProps) {
  const [isListening, setIsListening] = useState(false);
  const [partialText, setPartialText] = useState("");
  const [micError, setMicError] = useState<string | null>(null);
  const [manualInput, setManualInput] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);
  const recognizerRef = useRef<{ start: () => void; stop: () => void } | null>(
    null
  );
  const endRef = useRef<HTMLDivElement | null>(null);
  const sendRef = useRef(onSendVoiceMessage);
  sendRef.current = onSendVoiceMessage;

  const speechSupported = isBrowserSpeechRecognitionSupported();
  const ttsSupported = isBrowserTtsSupported();
  const isInterviewMode =
    activeSession?.mode === "INTERVIEW" ||
    Boolean(activeSession?.interviewCategory);
  const userAnswersCount = messages.filter((m) => m.role === "user").length;

  const lastAiReply = [...messages]
    .reverse()
    .find((m) => m.role === "assistant")?.text;

  useEffect(() => {
    recognizerRef.current = createBrowserRecognizer({
      onListeningChange: (listening) => setIsListening(listening),
      onPartialResult: (text) => setPartialText(text),
      onFinalResult: (finalText) => {
        setMicError(null);
        void sendRef.current(finalText);
      },
      onError: (msg) => setMicError(msg),
    });

    return () => {
      recognizerRef.current?.stop();
    };
  }, []);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length, isSending]);

  const handleToggleMic = () => {
    if (isSending) return;
    setMicError(null);
    if (isListening) {
      recognizerRef.current?.stop();
    } else {
      onStopSpeaking();
      setPartialText("");
      recognizerRef.current?.start();
    }
  };

  const handleManualSubmit = async () => {
    const trimmed = manualInput.trim();
    if (!trimmed || isSending) return;
    setManualInput("");
    await onSendVoiceMessage(trimmed);
  };

  const voiceSessions = sessions.filter(
    (s) => s.mode === "VOICE" || s.mode === "INTERVIEW"
  );

  return (
    <div className="mx-auto flex max-w-5xl flex-col gap-4">
      {/* Top Bar & Level */}
      <div className="flex flex-wrap items-center justify-between gap-3 rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-4">
        <div className="flex min-w-0 items-center gap-2.5">
          <button
            type="button"
            onClick={() => setHistoryOpen(true)}
            aria-label="Open voice session history"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/60 text-[var(--fm-text)] cursor-pointer"
          >
            <History className="h-4 w-4" />
          </button>
          <div className="min-w-0">
            <h1 className="truncate font-display text-lg font-bold text-[var(--fm-text)]">
              {activeSession?.title || "Voice English Practice"}
            </h1>
            <p className="text-xs text-[var(--fm-text-muted)]">
              Speak naturally in English • Real Web Speech &amp; TTS
            </p>
          </div>
        </div>
        <button
          type="button"
          onClick={onCreateNewVoiceSession}
          className="inline-flex min-h-10 items-center gap-1.5 rounded-xl bg-[var(--fm-secondary-container)] px-3.5 py-2 text-xs font-semibold text-[var(--fm-on-secondary-container)] cursor-pointer"
        >
          <Plus className="h-4 w-4" />
          <span>New Voice Session</span>
        </button>
      </div>

      <LevelSelector
        selectedLevel={profile.englishLevel}
        onSelectLevel={onSelectLevel}
      />

      {isInterviewMode && activeSession && (
        <div className="flex flex-wrap items-center justify-between gap-2 rounded-2xl border border-[var(--fm-primary)]/40 bg-[var(--fm-primary-container)]/35 px-4 py-3">
          <div className="flex items-center gap-2 text-xs font-semibold text-[var(--fm-on-primary-container)]">
            <Briefcase className="h-4 w-4 text-[var(--fm-primary)]" />
            <span>
              Voice Interview: {activeSession.interviewCategory || "Practice"} •{" "}
              {userAnswersCount} answers
            </span>
          </div>
          <button
            type="button"
            disabled={userAnswersCount === 0 || isGeneratingReport}
            onClick={() => void onFinishInterview(activeSession, messages)}
            className="inline-flex min-h-10 items-center gap-1.5 rounded-xl bg-[var(--fm-primary)] px-3.5 py-1.5 text-xs font-semibold text-[var(--fm-on-primary)] disabled:opacity-50 cursor-pointer"
          >
            {isGeneratingReport ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <ClipboardCheck className="h-4 w-4" />
            )}
            <span>
              {isGeneratingReport ? "Generating Report..." : "Finish & Report"}
            </span>
          </button>
        </div>
      )}

      {!speechSupported && (
        <div className="flex items-start gap-2.5 rounded-2xl border border-[var(--fm-tertiary)]/50 bg-[var(--fm-tertiary-container)]/40 p-4 text-xs text-[var(--fm-text)]">
          <AlertCircle className="mt-0.5 h-4 w-4 shrink-0 text-[var(--fm-tertiary)]" />
          <div>
            <strong>Speech Recognition Unavailable in This Browser:</strong>{" "}
            Your current browser does not expose the Web Speech Recognition
            API. Use Google Chrome, Microsoft Edge, or Safari for microphone
            voice capture, or type your sentence in the box below to practice
            with TTS playback.
          </div>
        </div>
      )}

      {/* Voice Orb & Controls */}
      <section className="flex flex-col items-center rounded-3xl border border-[var(--fm-outline-variant)] bg-gradient-to-b from-[var(--fm-surface)] to-[var(--fm-surface-variant)]/45 p-6 text-center">
        <div
          className={`flex h-28 w-28 items-center justify-center rounded-full border-2 transition-all ${
            isListening
              ? "animate-orb border-[var(--fm-error)] bg-[var(--fm-error-container)]/50 text-[var(--fm-error)] shadow-lg"
              : isSpeaking
                ? "animate-orb border-[var(--fm-secondary)] bg-[var(--fm-secondary-container)]/60 text-[var(--fm-secondary)] shadow-lg"
                : isSending
                  ? "border-[var(--fm-tertiary)] bg-[var(--fm-tertiary-container)]/50 text-[var(--fm-tertiary)]"
                  : "border-[var(--fm-primary)]/50 bg-[var(--fm-primary-container)]/50 text-[var(--fm-primary)]"
          }`}
        >
          {isSending ? (
            <Loader2 className="h-10 w-10 animate-spin" />
          ) : isListening ? (
            <Mic className="h-10 w-10" />
          ) : isSpeaking ? (
            <Volume2 className="h-10 w-10" />
          ) : (
            <Mic className="h-10 w-10" />
          )}
        </div>

        <p className="mt-4 font-display text-base font-bold text-[var(--fm-text)]">
          {isListening
            ? "Listening... speak clearly in English"
            : isSending
              ? "Analyzing your spoken English..."
              : isSpeaking
                ? "Speaking FluentMate AI response..."
                : "Tap the microphone and speak in English"}
        </p>

        {partialText && (
          <div className="mt-3 max-w-lg rounded-2xl border border-[var(--fm-primary)]/40 bg-[var(--fm-surface)] px-4 py-2.5 text-sm italic text-[var(--fm-text)]">
            &ldquo;{partialText}&rdquo;
          </div>
        )}

        {micError && (
          <div className="mt-3 max-w-lg rounded-xl border border-[var(--fm-error)]/40 bg-[var(--fm-error-container)] px-4 py-2.5 text-xs text-[var(--fm-on-error-container)]">
            {micError}
          </div>
        )}

        <div className="mt-5 flex flex-wrap items-center justify-center gap-3">
          <button
            type="button"
            onClick={handleToggleMic}
            disabled={isSending}
            className={`inline-flex min-h-12 items-center gap-2 rounded-2xl px-6 py-3 text-sm font-semibold shadow-md transition disabled:opacity-55 cursor-pointer ${
              isListening
                ? "bg-[var(--fm-error)] text-white"
                : "bg-[var(--fm-primary)] text-[var(--fm-on-primary)]"
            }`}
          >
            {isListening ? (
              <>
                <MicOff className="h-5 w-5" />
                <span>Stop Listening</span>
              </>
            ) : (
              <>
                <Mic className="h-5 w-5" />
                <span>Start Speaking</span>
              </>
            )}
          </button>

          {ttsSupported && lastAiReply && (
            <button
              type="button"
              onClick={() => onSpeakText(lastAiReply)}
              className="inline-flex min-h-12 items-center gap-2 rounded-2xl border border-[var(--fm-outline)] bg-[var(--fm-surface)] px-4 py-3 text-sm font-semibold text-[var(--fm-text)] transition hover:bg-[var(--fm-surface-variant)] cursor-pointer"
            >
              <RotateCcw className="h-4 w-4 text-[var(--fm-secondary)]" />
              <span>Replay AI</span>
            </button>
          )}

          {isSpeaking && (
            <button
              type="button"
              onClick={onStopSpeaking}
              className="inline-flex min-h-12 items-center gap-2 rounded-2xl border border-[var(--fm-error)]/50 bg-[var(--fm-error-container)]/50 px-4 py-3 text-sm font-semibold text-[var(--fm-error)] cursor-pointer"
            >
              <Square className="h-4 w-4" />
              <span>Stop Audio</span>
            </button>
          )}
        </div>
      </section>

      {/* Transcript + Fallback Input */}
      <section className="flex flex-col overflow-hidden rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)]">
        <div className="border-b border-[var(--fm-outline-variant)] px-4 py-3">
          <h2 className="font-display text-sm font-bold text-[var(--fm-text)]">
            Voice Transcript &amp; Grammar Feedback
          </h2>
        </div>

        <div className="max-h-[420px] space-y-4 overflow-y-auto p-4">
          {messages.length === 0 ? (
            <p className="py-8 text-center text-xs text-[var(--fm-text-muted)]">
              Your spoken sentences, grammar corrections, Marathi explanations,
              and AI replies will appear here.
            </p>
          ) : (
            messages.map((msg) =>
              msg.role === "user" ? (
                <div key={msg.id} className="flex justify-end">
                  <div className="max-w-[85%] rounded-2xl rounded-tr-md bg-[var(--fm-secondary)] px-4 py-3 text-sm text-white">
                    <div className="mb-0.5 text-[10px] font-bold uppercase tracking-wide opacity-80">
                      You Spoke
                    </div>
                    <p>{msg.text}</p>
                  </div>
                </div>
              ) : (
                <div key={msg.id} className="max-w-[92%] sm:max-w-[85%]">
                  <SmartCorrectionCard
                    message={msg}
                    isInterviewMode={isInterviewMode}
                    onSpeakText={onSpeakText}
                    onSaveSuggestedWord={(draft) => void onSaveWord(draft)}
                  />
                </div>
              )
            )
          )}
          <div ref={endRef} />
        </div>

        <div className="flex items-center gap-2 border-t border-[var(--fm-outline-variant)] bg-[var(--fm-bg)]/50 p-3">
          <input
            type="text"
            value={manualInput}
            onChange={(e) => setManualInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                void handleManualSubmit();
              }
            }}
            placeholder="Or type your spoken sentence here if mic is unavailable..."
            className="min-h-11 flex-1 rounded-xl border border-[var(--fm-outline)] bg-[var(--fm-surface)] px-3.5 text-sm text-[var(--fm-text)] focus:border-[var(--fm-primary)] focus:outline-none"
          />
          <button
            type="button"
            disabled={!manualInput.trim() || isSending}
            onClick={() => void handleManualSubmit()}
            aria-label="Send sentence"
            className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[var(--fm-primary)] text-[var(--fm-on-primary)] disabled:opacity-45 cursor-pointer"
          >
            <Send className="h-4 w-4" />
          </button>
        </div>
      </section>

      {historyOpen && (
        <div
          className="fixed inset-0 z-50 flex justify-start bg-black/60 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-label="Voice Sessions"
        >
          <div className="flex h-full w-full max-w-sm flex-col border-r border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--fm-outline-variant)] px-4 py-3.5">
              <h2 className="font-display text-base font-bold">
                Voice Sessions ({voiceSessions.length})
              </h2>
              <button
                type="button"
                onClick={() => setHistoryOpen(false)}
                className="flex h-10 w-10 items-center justify-center rounded-full hover:bg-[var(--fm-surface-variant)] cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="flex-1 space-y-2 overflow-y-auto p-3">
              {voiceSessions.length === 0 ? (
                <p className="py-8 text-center text-xs text-[var(--fm-text-muted)]">
                  No voice sessions yet.
                </p>
              ) : (
                voiceSessions.map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => {
                      onSelectSession(s.id);
                      setHistoryOpen(false);
                    }}
                    className={`w-full rounded-2xl border p-3 text-left cursor-pointer ${
                      s.id === activeSession?.id
                        ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)]/35"
                        : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="truncate text-sm font-semibold">
                        {s.title}
                      </span>
                      <span className="font-mono-code text-[10px] font-bold text-[var(--fm-primary)]">
                        {s.englishLevel}
                      </span>
                    </div>
                    <p className="mt-0.5 truncate text-xs text-[var(--fm-text-muted)]">
                      {s.lastMessagePreview || "Voice session"}
                    </p>
                  </button>
                ))
              )}
            </div>
          </div>
          <div
            className="flex-1"
            onClick={() => setHistoryOpen(false)}
            aria-hidden="true"
          />
        </div>
      )}
    </div>
  );
}
