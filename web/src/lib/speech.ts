interface SpeechRecognitionAlternativeLike {
  transcript: string;
}

interface SpeechRecognitionResultLike {
  isFinal: boolean;
  0: SpeechRecognitionAlternativeLike;
}

interface SpeechRecognitionEventLike {
  resultIndex: number;
  results: ArrayLike<SpeechRecognitionResultLike>;
}

interface SpeechRecognitionErrorEventLike {
  error: string;
}

interface SpeechRecognitionInstance {
  lang: string;
  interimResults: boolean;
  continuous: boolean;
  maxAlternatives: number;
  onstart: (() => void) | null;
  onresult: ((event: SpeechRecognitionEventLike) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEventLike) => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
  abort: () => void;
}

type SpeechRecognitionCtor = new () => SpeechRecognitionInstance;

function getSpeechRecognitionCtor(): SpeechRecognitionCtor | null {
  const win = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  };
  return win.SpeechRecognition || win.webkitSpeechRecognition || null;
}

export function isBrowserSpeechRecognitionSupported(): boolean {
  return typeof window !== "undefined" && getSpeechRecognitionCtor() !== null;
}

export function isBrowserTtsSupported(): boolean {
  return typeof window !== "undefined" && "speechSynthesis" in window;
}

export function createBrowserRecognizer(callbacks: {
  onListeningChange: (listening: boolean) => void;
  onPartialResult: (text: string) => void;
  onFinalResult: (text: string) => void;
  onError: (message: string) => void;
}): { start: () => void; stop: () => void } {
  const Ctor = getSpeechRecognitionCtor();
  if (!Ctor) {
    return {
      start: () => {
        callbacks.onError(
          "Speech recognition is not supported in this browser. Please use Chrome, Edge, or Safari, or type your sentence below."
        );
      },
      stop: () => {},
    };
  }

  let activeInstance: SpeechRecognitionInstance | null = null;
  let emittedFinal = false;

  const stop = () => {
    try {
      activeInstance?.stop();
    } catch {
      // Ignore stop error if already inactive
    }
  };

  const start = () => {
    try {
      activeInstance?.abort();
    } catch {
      // Ignore abort errors
    }

    const recognition = new Ctor();
    activeInstance = recognition;
    emittedFinal = false;
    recognition.lang = "en-US";
    recognition.interimResults = true;
    recognition.continuous = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      callbacks.onListeningChange(true);
    };

    recognition.onresult = (event) => {
      let interim = "";
      let finalChunk = "";
      for (let i = event.resultIndex; i < event.results.length; i += 1) {
        const res = event.results[i];
        const text = res[0]?.transcript ?? "";
        if (res.isFinal) {
          finalChunk += text;
        } else {
          interim += text;
        }
      }
      if (interim.trim()) {
        callbacks.onPartialResult(interim.trim());
      }
      if (finalChunk.trim() && !emittedFinal) {
        emittedFinal = true;
        callbacks.onPartialResult("");
        callbacks.onFinalResult(finalChunk.trim());
      }
    };

    recognition.onerror = (event) => {
      callbacks.onListeningChange(false);
      const code = event.error;
      const msg =
        code === "not-allowed" || code === "service-not-allowed"
          ? "Microphone permission was denied. Please allow microphone access in your browser settings."
          : code === "no-speech"
            ? "No speech detected. Tap the microphone and speak clearly."
            : code === "audio-capture"
              ? "No microphone was found on this device."
              : code === "network"
                ? "Speech recognition requires an active internet connection."
                : `Speech recognition error: ${code}`;
      callbacks.onError(msg);
    };

    recognition.onend = () => {
      callbacks.onListeningChange(false);
    };

    try {
      recognition.start();
    } catch (err) {
      callbacks.onListeningChange(false);
      callbacks.onError(
        err instanceof Error
          ? err.message
          : "Could not start microphone recognition."
      );
    }
  };

  return { start, stop };
}

export function speakEnglishText(
  text: string,
  rate: number,
  callbacks?: {
    onStart?: () => void;
    onEnd?: () => void;
    onError?: (message: string) => void;
  }
): void {
  const clean = text.trim();
  if (!clean) return;
  if (!isBrowserTtsSupported()) {
    callbacks?.onError?.("Text-to-speech is not supported in this browser.");
    return;
  }

  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(clean);
  utterance.lang = "en-US";
  utterance.rate = Math.min(1.5, Math.max(0.5, rate));

  const voices = window.speechSynthesis.getVoices();
  const preferredVoice =
    voices.find((v) => v.lang.startsWith("en-IN")) ||
    voices.find((v) => v.lang.startsWith("en-US")) ||
    voices.find((v) => v.lang.startsWith("en"));
  if (preferredVoice) {
    utterance.voice = preferredVoice;
  }

  utterance.onstart = () => callbacks?.onStart?.();
  utterance.onend = () => callbacks?.onEnd?.();
  utterance.onerror = () => {
    callbacks?.onEnd?.();
    callbacks?.onError?.("Audio playback failed in this browser.");
  };

  window.speechSynthesis.speak(utterance);
}

export function stopSpeakingEnglish(): void {
  if (isBrowserTtsSupported()) {
    window.speechSynthesis.cancel();
  }
}
