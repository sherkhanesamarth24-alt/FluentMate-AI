import { KeyboardEvent, useEffect, useRef, useState } from "react";
import {
  Briefcase,
  ClipboardCheck,
  Eraser,
  History,
  Loader2,
  MessageSquarePlus,
  Send,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import { LevelSelector } from "../components/LevelSelector";
import { SmartCorrectionCard } from "../components/SmartCorrectionCard";
import {
  ChatMessage,
  ChatSession,
  ENGLISH_LEVELS,
  EnglishLevel,
  GeneratedWordDraft,
  UserProfile,
} from "../types/models";

interface AiChatPageProps {
  profile: UserProfile;
  sessions: ChatSession[];
  activeSession: ChatSession | null;
  messages: ChatMessage[];
  isSending: boolean;
  isGeneratingReport: boolean;
  onSelectLevel: (level: EnglishLevel) => void;
  onSelectSession: (sessionId: string) => void;
  onCreateNewChat: () => void;
  onSendMessage: (text: string) => Promise<void>;
  onClearChat: (sessionId: string) => Promise<void>;
  onDeleteSession: (sessionId: string) => Promise<void>;
  onSaveWord: (draft: GeneratedWordDraft) => Promise<void>;
  onSpeakText: (text: string) => void;
  onFinishInterview: (session: ChatSession, messages: ChatMessage[]) => Promise<void>;
}

const STARTER_PROMPTS_BY_LEVEL: Record<EnglishLevel, string[]> = {
  LOW: [
    "Hello! I want to practice simple daily English.",
    "I am go to college yesterday and meet my friends.",
    "Can we talk about my daily routine?",
    "Ask me 3 easy questions about my family.",
  ],
  MEDIUM: [
    "I want to improve my English fluency for workplace conversations.",
    "Yesterday I have attended a meeting about our new software project.",
    "Let's discuss my favorite book or movie.",
    "Help me practice describing my hobbies clearly.",
  ],
  HIGH: [
    "Let's practice persuasive executive communication and structured answers.",
    "I'd like to discuss the impact of AI on software engineering careers.",
    "Challenge me with natural idioms and formal alternatives.",
    "Ask me a tough situational question and critique my clarity.",
  ],
};

export function AiChatPage({
  profile,
  sessions,
  activeSession,
  messages,
  isSending,
  isGeneratingReport,
  onSelectLevel,
  onSelectSession,
  onCreateNewChat,
  onSendMessage,
  onClearChat,
  onDeleteSession,
  onSaveWord,
  onSpeakText,
  onFinishInterview,
}: AiChatPageProps) {
  const [input, setInput] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);
  const [showLevelBar, setShowLevelBar] = useState(false);
  const endRef = useRef<HTMLDivElement | null>(null);

  const isInterviewMode =
    activeSession?.mode === "INTERVIEW" ||
    Boolean(activeSession?.interviewCategory);
  const userAnswersCount = messages.filter((m) => m.role === "user").length;

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages.length, isSending]);

  const handleSubmit = async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending) return;
    setInput("");
    await onSendMessage(trimmed);
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      void handleSubmit();
    }
  };

  const chatSessions = sessions.filter((s) => s.mode !== "VOICE");

  return (
    <div className="mx-auto flex h-[calc(100vh-8.5rem)] max-w-5xl flex-col overflow-hidden rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] lg:h-[calc(100vh-3rem)]">
      {/* Header Bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--fm-outline-variant)] px-4 py-3">
        <div className="flex min-w-0 items-center gap-2.5">
          <button
            type="button"
            onClick={() => setHistoryOpen(true)}
            aria-label="Open chat history"
            className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/60 text-[var(--fm-text)] transition hover:bg-[var(--fm-surface-variant)] cursor-pointer"
          >
            <History className="h-4 w-4" />
          </button>
          <div className="min-w-0">
            <div className="flex items-center gap-2">
              <h1 className="truncate font-display text-base font-bold text-[var(--fm-text)]">
                {activeSession?.title || "New English Chat"}
              </h1>
              <button
                type="button"
                onClick={() => setShowLevelBar((v) => !v)}
                className="shrink-0 rounded-full bg-[var(--fm-primary-container)] px-2.5 py-0.5 font-mono-code text-[11px] font-bold text-[var(--fm-on-primary-container)] cursor-pointer"
              >
                {profile.englishLevel}
              </button>
            </div>
            <p className="truncate text-xs text-[var(--fm-text-muted)]">
              {isInterviewMode
                ? `${activeSession?.interviewCategory || "Mock Interview"} • 1 question at a time`
                : `${ENGLISH_LEVELS[profile.englishLevel].label} Tutor • Roman Marathi explanations`}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={onCreateNewChat}
            className="inline-flex min-h-10 items-center gap-1.5 rounded-xl bg-[var(--fm-primary-container)] px-3 py-1.5 text-xs font-semibold text-[var(--fm-on-primary-container)] transition hover:opacity-90 cursor-pointer"
          >
            <MessageSquarePlus className="h-4 w-4" />
            <span>New Chat</span>
          </button>

          {activeSession && messages.length > 0 && (
            <button
              type="button"
              onClick={() => void onClearChat(activeSession.id)}
              title="Clear messages"
              aria-label="Clear chat messages"
              className="flex h-10 w-10 items-center justify-center rounded-xl text-[var(--fm-text-muted)] transition hover:bg-[var(--fm-surface-variant)] hover:text-[var(--fm-text)] cursor-pointer"
            >
              <Eraser className="h-4 w-4" />
            </button>
          )}

          {activeSession && (
            <button
              type="button"
              onClick={() => void onDeleteSession(activeSession.id)}
              title="Delete chat"
              aria-label="Delete chat session"
              className="flex h-10 w-10 items-center justify-center rounded-xl text-[var(--fm-error)] transition hover:bg-[var(--fm-error-container)]/45 cursor-pointer"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {/* Collapsible Level Switcher */}
      {showLevelBar && (
        <div className="border-b border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/40 px-4 py-3">
          <LevelSelector
            selectedLevel={profile.englishLevel}
            onSelectLevel={onSelectLevel}
          />
        </div>
      )}

      {/* Interview Practice Banner */}
      {isInterviewMode && activeSession && (
        <div className="flex flex-wrap items-center justify-between gap-2 border-b border-[var(--fm-primary)]/30 bg-[var(--fm-primary-container)]/30 px-4 py-2.5">
          <div className="flex items-center gap-2 text-xs font-semibold text-[var(--fm-on-primary-container)]">
            <Briefcase className="h-4 w-4 text-[var(--fm-primary)]" />
            <span>
              {activeSession.interviewCategory || "Interview Practice"} •{" "}
              {userAnswersCount}{" "}
              {userAnswersCount === 1 ? "answer" : "answers"} given
            </span>
          </div>
          <button
            type="button"
            disabled={userAnswersCount === 0 || isGeneratingReport}
            onClick={() => void onFinishInterview(activeSession, messages)}
            className="inline-flex min-h-9 items-center gap-1.5 rounded-xl bg-[var(--fm-primary)] px-3 py-1.5 text-xs font-semibold text-[var(--fm-on-primary)] transition hover:opacity-90 disabled:opacity-50 cursor-pointer"
          >
            {isGeneratingReport ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin" />
            ) : (
              <ClipboardCheck className="h-3.5 w-3.5" />
            )}
            <span>
              {isGeneratingReport ? "Generating Report..." : "Finish & Report"}
            </span>
          </button>
        </div>
      )}

      {/* Messages Viewport */}
      <div className="flex-1 space-y-4 overflow-y-auto px-4 py-5">
        {messages.length === 0 ? (
          <div className="mx-auto flex max-w-xl flex-col items-center py-8 text-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--fm-primary-container)] text-[var(--fm-primary)]">
              <Sparkles className="h-7 w-7" />
            </div>
            <h2 className="mt-4 font-display text-xl font-bold text-[var(--fm-text)]">
              Start Practicing English
            </h2>
            <p className="mt-1.5 text-sm text-[var(--fm-text-muted)]">
              Write anything in English! FluentMate AI replies naturally,
              corrects mistakes gently, and explains why in simple Roman
              Marathi.
            </p>
            <div className="mt-6 grid w-full grid-cols-1 gap-2 sm:grid-cols-2">
              {STARTER_PROMPTS_BY_LEVEL[profile.englishLevel].map((prompt) => (
                <button
                  key={prompt}
                  type="button"
                  disabled={isSending}
                  onClick={() => void onSendMessage(prompt)}
                  className="rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/55 p-3.5 text-left text-xs font-medium text-[var(--fm-text)] transition hover:border-[var(--fm-primary)] hover:bg-[var(--fm-primary-container)]/30 cursor-pointer"
                >
                  &ldquo;{prompt}&rdquo;
                </button>
              ))}
            </div>
          </div>
        ) : (
          messages.map((msg) =>
            msg.role === "user" ? (
              <div key={msg.id} className="flex justify-end">
                <div className="max-w-[85%] rounded-2xl rounded-tr-md bg-[var(--fm-primary)] px-4 py-3 text-[15px] leading-relaxed text-[var(--fm-on-primary)] shadow-xs sm:max-w-[75%]">
                  <p className="whitespace-pre-wrap">{msg.text}</p>
                </div>
              </div>
            ) : (
              <div key={msg.id} className="flex justify-start">
                <div className="w-full max-w-[92%] sm:max-w-[82%]">
                  <SmartCorrectionCard
                    message={msg}
                    isInterviewMode={isInterviewMode}
                    onSpeakText={onSpeakText}
                    onSaveSuggestedWord={(draft) => void onSaveWord(draft)}
                  />
                </div>
              </div>
            )
          )
        )}

        {isSending && (
          <div className="flex items-center gap-2.5 rounded-2xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/50 px-4 py-3 text-xs font-medium text-[var(--fm-text-muted)] w-fit">
            <Loader2 className="h-4 w-4 animate-spin text-[var(--fm-primary)]" />
            <span>FluentMate AI is checking grammar &amp; replying...</span>
          </div>
        )}
        <div ref={endRef} />
      </div>

      {/* Composer */}
      <div className="border-t border-[var(--fm-outline-variant)] bg-[var(--fm-bg)]/50 p-3 sm:p-4">
        <div className="flex items-end gap-2">
          <textarea
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            rows={1}
            placeholder={
              isInterviewMode
                ? "Type your interview answer in English... (Enter to send)"
                : "Type in English... (Enter to send, Shift+Enter for new line)"
            }
            className="max-h-36 min-h-12 flex-1 resize-y rounded-2xl border border-[var(--fm-outline)] bg-[var(--fm-surface)] px-4 py-3 text-sm text-[var(--fm-text)] placeholder:text-[var(--fm-text-muted)] focus:border-[var(--fm-primary)] focus:outline-none"
          />
          <button
            type="button"
            onClick={() => void handleSubmit()}
            disabled={!input.trim() || isSending}
            aria-label="Send message"
            className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-[var(--fm-primary)] text-[var(--fm-on-primary)] transition hover:opacity-90 disabled:opacity-45 cursor-pointer"
          >
            {isSending ? (
              <Loader2 className="h-5 w-5 animate-spin" />
            ) : (
              <Send className="h-5 w-5" />
            )}
          </button>
        </div>
      </div>

      {/* Session History Modal */}
      {historyOpen && (
        <div
          className="fixed inset-0 z-50 flex justify-start bg-black/60 backdrop-blur-xs"
          role="dialog"
          aria-modal="true"
          aria-label="Conversation History"
        >
          <div className="flex h-full w-full max-w-sm flex-col border-r border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] shadow-2xl">
            <div className="flex items-center justify-between border-b border-[var(--fm-outline-variant)] px-4 py-3.5">
              <h2 className="font-display text-base font-bold text-[var(--fm-text)]">
                Chat History ({chatSessions.length})
              </h2>
              <button
                type="button"
                onClick={() => setHistoryOpen(false)}
                aria-label="Close history"
                className="flex h-10 w-10 items-center justify-center rounded-full text-[var(--fm-text-muted)] hover:bg-[var(--fm-surface-variant)] cursor-pointer"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="p-3">
              <button
                type="button"
                onClick={() => {
                  onCreateNewChat();
                  setHistoryOpen(false);
                }}
                className="flex min-h-11 w-full items-center justify-center gap-2 rounded-xl bg-[var(--fm-primary)] px-4 py-2 text-sm font-semibold text-[var(--fm-on-primary)] cursor-pointer"
              >
                <MessageSquarePlus className="h-4 w-4" />
                <span>Start New Chat</span>
              </button>
            </div>
            <div className="flex-1 space-y-2 overflow-y-auto px-3 pb-4">
              {chatSessions.length === 0 ? (
                <p className="py-8 text-center text-xs text-[var(--fm-text-muted)]">
                  No saved chats yet. Send a message to start your first
                  conversation.
                </p>
              ) : (
                chatSessions.map((sess) => {
                  const selected = sess.id === activeSession?.id;
                  return (
                    <div
                      key={sess.id}
                      className={`flex items-center gap-2 rounded-2xl border p-3 transition ${
                        selected
                          ? "border-[var(--fm-primary)] bg-[var(--fm-primary-container)]/35"
                          : "border-[var(--fm-outline-variant)] bg-[var(--fm-surface-variant)]/35 hover:border-[var(--fm-outline)]"
                      }`}
                    >
                      <button
                        type="button"
                        onClick={() => {
                          onSelectSession(sess.id);
                          setHistoryOpen(false);
                        }}
                        className="min-w-0 flex-1 text-left cursor-pointer"
                      >
                        <div className="flex items-center gap-1.5">
                          <span className="truncate text-sm font-semibold text-[var(--fm-text)]">
                            {sess.title}
                          </span>
                          <span className="shrink-0 rounded-md bg-[var(--fm-surface)] px-1.5 py-0.5 font-mono-code text-[10px] font-bold text-[var(--fm-primary)]">
                            {sess.englishLevel}
                          </span>
                        </div>
                        <p className="mt-0.5 truncate text-xs text-[var(--fm-text-muted)]">
                          {sess.lastMessagePreview || "No messages"}
                        </p>
                        <div className="mt-1 text-[11px] text-[var(--fm-text-muted)]">
                          {sess.mode} • {sess.messageCount} msgs •{" "}
                          {sess.mistakesCount} tips
                        </div>
                      </button>
                      <button
                        type="button"
                        onClick={() => void onDeleteSession(sess.id)}
                        aria-label={`Delete ${sess.title}`}
                        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-[var(--fm-error)] hover:bg-[var(--fm-error-container)]/50 cursor-pointer"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </div>
                  );
                })
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
