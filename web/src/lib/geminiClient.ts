import { Auth } from "firebase/auth";
import {
  ChatMessage,
  DailySessionType,
  EnglishLevel,
  GeneratedInterviewReportDraft,
  GeneratedTopicDraft,
  GeneratedWordDraft,
  MarathiScript,
  TutorTurnResult,
} from "../types/models";

async function callGeminiEndpoint<T>(
  auth: Auth,
  action: string,
  payload: Record<string, unknown>
): Promise<T> {
  const user = auth.currentUser;
  if (!user) {
    throw new Error("Please sign in with Google to use FluentMate AI.");
  }
  const idToken = await user.getIdToken();

  const response = await fetch("/api/gemini", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({ action, payload }),
  });

  const data = (await response.json().catch(() => ({}))) as {
    result?: T;
    error?: string;
  };

  if (!response.ok || data.result === undefined) {
    throw new Error(
      data.error || `Gemini request failed (HTTP ${response.status}).`
    );
  }

  return data.result;
}

export class FluentMateGeminiClient {
  constructor(private readonly auth: Auth) {}

  analyzeAndReply(params: {
    userMessage: string;
    level: EnglishLevel;
    marathiScript: MarathiScript;
    history: ChatMessage[];
    topicContext?: string | null;
    interviewCategory?: string | null;
    isVoiceMode?: boolean;
  }): Promise<TutorTurnResult> {
    return callGeminiEndpoint<TutorTurnResult>(this.auth, "analyzeAndReply", {
      userMessage: params.userMessage,
      level: params.level,
      marathiScript: params.marathiScript,
      history: params.history.slice(-12).map((m) => ({
        role: m.role,
        text: m.text,
      })),
      topicContext: params.topicContext ?? "",
      interviewCategory: params.interviewCategory ?? "",
      isVoiceMode: Boolean(params.isVoiceMode),
    });
  }

  generateDailyTopic(params: {
    level: EnglishLevel;
    dayOfWeekName: string;
    scheduledSessionType: DailySessionType;
    interviewsThisWeek: number;
    weeklyInterviewFrequency: number;
    preferredCategory?: string | null;
    preferredInterviewCategory?: string | null;
    recentTopicTitles: string[];
    dateKey: string;
    allowScheduleAdjustment?: boolean;
  }): Promise<GeneratedTopicDraft> {
    return callGeminiEndpoint<GeneratedTopicDraft>(
      this.auth,
      "generateDailyTopic",
      {
        ...params,
      }
    );
  }

  generateInterviewReport(params: {
    interviewCategory: string;
    level: EnglishLevel;
    marathiScript: MarathiScript;
    transcript: ChatMessage[];
  }): Promise<GeneratedInterviewReportDraft> {
    return callGeminiEndpoint<GeneratedInterviewReportDraft>(
      this.auth,
      "generateInterviewReport",
      {
        interviewCategory: params.interviewCategory,
        level: params.level,
        marathiScript: params.marathiScript,
        transcript: params.transcript.map((m) => ({
          role: m.role,
          text: m.text,
          hasMistake: m.hasMistake,
          correctedSentence: m.correctedSentence ?? null,
        })),
      }
    );
  }

  generateDailyVocabulary(params: {
    level: EnglishLevel;
    existingWords: string[];
    marathiScript: MarathiScript;
  }): Promise<GeneratedWordDraft[]> {
    return callGeminiEndpoint<GeneratedWordDraft[]>(
      this.auth,
      "generateDailyVocabulary",
      params
    );
  }

  enrichVocabularyWord(params: {
    word: string;
    level: EnglishLevel;
    marathiScript: MarathiScript;
  }): Promise<GeneratedWordDraft> {
    return callGeminiEndpoint<GeneratedWordDraft>(
      this.auth,
      "enrichVocabularyWord",
      params
    );
  }
}
