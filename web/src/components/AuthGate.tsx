import { useState } from "react";
import { AlertTriangle, CheckCircle2, Sparkles } from "lucide-react";
import { Auth } from "firebase/auth";
import { FirebaseEnvStatus, signInWithGoogle } from "../lib/firebase";

interface AuthGateProps {
  auth: Auth | null;
  envStatus: FirebaseEnvStatus;
}

export function AuthGate({ auth, envStatus }: AuthGateProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleGoogleSignIn = async () => {
    if (!auth || loading) return;
    setLoading(true);
    setError(null);
    try {
      await signInWithGoogle(auth);
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Google Sign-In failed. Check your Firebase Authorized Domains."
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--fm-bg)] px-4 py-10">
      <div className="w-full max-w-lg rounded-3xl border border-[var(--fm-outline-variant)] bg-[var(--fm-surface)] p-6 shadow-xl sm:p-8">
        <div className="flex items-center gap-3">
          <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-[var(--fm-primary)] to-[var(--fm-secondary)] text-white shadow-md">
            <Sparkles className="h-6 w-6" />
          </div>
          <div>
            <h1 className="font-display text-2xl font-bold text-[var(--fm-text)]">
              FluentMate AI
            </h1>
            <p className="text-xs font-medium text-[var(--fm-text-muted)]">
              Personal English &amp; Marathi AI Tutor • Web Edition
            </p>
          </div>
        </div>

        <p className="mt-5 text-sm leading-relaxed text-[var(--fm-text-muted)]">
          Practice real English conversations, get gentle grammar corrections
          explained in simple Roman Marathi, build vocabulary, and run weekly
          AI mock interviews — synced with your FluentMate AI account.
        </p>

        <div className="mt-4 grid grid-cols-1 gap-2 rounded-2xl bg-[var(--fm-surface-variant)]/60 p-4 text-xs text-[var(--fm-text)] sm:grid-cols-2">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
            <span>Low / Medium / High Levels</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
            <span>Roman Marathi Explanations</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
            <span>Voice Practice (Mic &amp; TTS)</span>
          </div>
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-[var(--fm-primary)]" />
            <span>1–2x/Week Mock Interviews</span>
          </div>
        </div>

        {!envStatus.configured ? (
          <div className="mt-6 rounded-2xl border border-[var(--fm-tertiary)]/50 bg-[var(--fm-tertiary-container)]/35 p-4">
            <div className="flex items-center gap-2 font-display text-sm font-bold text-[var(--fm-tertiary)]">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              <span>Verify &amp; Configure Firebase Web App</span>
            </div>
            <p className="mt-2 text-xs leading-relaxed text-[var(--fm-text)]">
              Your Android project uses Firebase project{" "}
              <code className="font-mono-code font-semibold">
                {envStatus.projectId}
              </code>{" "}
              and Firestore database{" "}
              <code className="font-mono-code font-semibold">
                {envStatus.databaseId}
              </code>
              , which currently only contains Android credentials. Register a{" "}
              <strong>Web App</strong> in Firebase Console, then configure these
              environment variables in{" "}
              <code className="font-mono-code">web/.env</code> or Cloudflare
              Pages:
            </p>
            <ul className="mt-2 list-disc space-y-1 pl-5 font-mono-code text-xs text-[var(--fm-text-muted)]">
              {envStatus.missingKeys.map((k) => (
                <li key={k}>{k}</li>
              ))}
            </ul>
          </div>
        ) : (
          <div className="mt-6 space-y-3">
            {error && (
              <div className="rounded-xl border border-[var(--fm-error)]/40 bg-[var(--fm-error-container)] px-4 py-3 text-xs text-[var(--fm-on-error-container)]">
                {error}
              </div>
            )}
            <button
              type="button"
              onClick={handleGoogleSignIn}
              disabled={loading}
              className="flex min-h-12 w-full items-center justify-center gap-3 rounded-2xl bg-[var(--fm-primary)] px-5 py-3 text-sm font-semibold text-[var(--fm-on-primary)] shadow-md transition hover:opacity-95 disabled:opacity-60 cursor-pointer"
            >
              <svg className="h-5 w-5" viewBox="0 0 24 24" aria-hidden="true">
                <path
                  fill="currentColor"
                  d="M21.35 11.1h-9.17v2.73h6.51c-.33 3.81-3.5 5.44-6.5 5.44C8.36 19.27 5 16.25 5 12c0-4.1 3.2-7.27 7.2-7.27c3.09 0 4.9 1.97 4.9 1.97L19 4.72S16.56 2 12.1 2C6.42 2 2.03 6.8 2.03 12c0 5.05 4.13 10 10.22 10c5.35 0 9.25-3.67 9.25-9.09c0-1.15-.15-1.81-.15-1.81"
                />
              </svg>
              <span>
                {loading ? "Signing in with Google..." : "Continue with Google"}
              </span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
