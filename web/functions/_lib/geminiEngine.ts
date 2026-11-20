import { GoogleGenAI, Type } from "@google/genai";

export type EnglishLevel = "LOW" | "MEDIUM" | "HIGH";
export type MarathiScript = "ROMAN" | "ROMAN_AND_DEVANAGARI";
export type DailySessionType =
  | "DAILY_CONVERSATION"
  | "INTERVIEW_PRACTICE"
  | "GENERAL_CONVERSATION"
  | "REVISION";

export interface HistoryTurnInput {
  role: "user" | "assistant";
  text: string;
  hasMistake?: boolean;
  correctedSentence?: string | null;
}

export interface GeminiRequestBody {
  action:
    | "analyzeAndReply"
    | "generateDailyTopic"
    | "generateInterviewReport"
    | "generateDailyVocabulary"
    | "enrichVocabularyWord";
  payload: Record<string, unknown>;
}

const DEFAULT_MODEL = "gemini-2.5-flash";

const LEVEL_DESCRIPTIONS: Record<EnglishLevel, string> = {
  LOW: "Simple English, short sentences, basic daily conversations, easy vocabulary",
  MEDIUM: "Natural conversations, richer vocabulary, longer sentences, grammar polish",
  HIGH: "Professional English, advanced vocabulary, complex discussions, interview prep",
};

const INTERVIEW_CATEGORIES = [
  "HR Interview",
  "Self Introduction",
  "Technical Interview",
  "College Placement Interview",
  "Internship Interview",
  "AI/ML Job Interview",
  "Communication Skills Interview",
];

const SYSTEM_PROMPT = `You are FluentMate AI, a friendly and patient English speaking and writing tutor.

Your goal is to help the learner become confident and fluent in English.

The learner communicates in English and prefers simple Roman Marathi explanations for mistakes (Marathi written in English letters).

Reply naturally in English.

Whenever the user sends a message, carefully analyze it for:
- Grammar
- Spelling
- Tenses
- Prepositions
- Vocabulary
- Sentence structure
- Natural English usage

When genuine mistakes occur:
1. Set hasMistake = true.
2. Put the user's mistaken sentence in originalSentence.
3. Put the corrected English sentence in correctedSentence.
4. Explain the mistake clearly in simple Roman Marathi in marathiExplanation (for example: "Kalchi goshta aahe, mhanun past tense vapraycha. Go cha past form Went aahe.").
5. Give a natural or professional alternative in naturalAlternative if useful (always provide a polished professional interview answer when in Interview Practice mode).
6. Continue the conversation warmly in assistantReply with a relevant follow-up question.

Important rules:
- Do NOT mark correct sentences as wrong. If the sentence is grammatically valid and natural, set hasMistake = false.
- Distinguish genuine mistakes from optional improvements.
- Do NOT overcorrect minor stylistic choices.
- Never embarrass or scold the learner. Be encouraging and supportive.
- Adapt your English vocabulary and sentence length to Low, Medium, or High levels.
- Focus on practical English communication, vocabulary, grammar, and confidence.
- Make every interaction educational and enjoyable.`;

const tutorResponseSchema = {
  type: Type.OBJECT,
  properties: {
    hasMistake: {
      type: Type.BOOLEAN,
      description:
        "True ONLY if the learner's English message has a genuine grammar, spelling, tense, preposition, vocabulary, or unnatural phrasing mistake. False if the sentence is already correct and natural.",
    },
    mistakeCategory: {
      type: Type.STRING,
      description:
        "One of: None, Grammar, Tense, Preposition, Spelling, Vocabulary, Sentence Structure, Natural Usage",
    },
    originalSentence: {
      type: Type.STRING,
      nullable: true,
      description:
        "The exact part or sentence from the user that had a mistake, or empty string if hasMistake is false.",
    },
    correctedSentence: {
      type: Type.STRING,
      nullable: true,
      description:
        "The corrected English sentence, or empty string if hasMistake is false.",
    },
    marathiExplanation: {
      type: Type.STRING,
      nullable: true,
      description:
        "Simple, encouraging explanation of the mistake in Marathi (Roman Marathi by default), or empty string if hasMistake is false.",
    },
    naturalAlternative: {
      type: Type.STRING,
      nullable: true,
      description:
        "In Interview mode: a more professional English answer for the interview. In regular chat: a natural native-sounding alternative if helpful.",
    },
    assistantReply: {
      type: Type.STRING,
      description:
        "Friendly, natural conversational English reply (or next single interview question when acting as an interviewer).",
    },
    suggestedVocabulary: {
      type: Type.OBJECT,
      nullable: true,
      description:
        "Optional single helpful vocabulary word to learn from this turn, or null if not needed.",
      properties: {
        word: { type: Type.STRING },
        marathiMeaning: { type: Type.STRING },
        exampleSentence: { type: Type.STRING },
        pronunciation: { type: Type.STRING },
        partOfSpeech: { type: Type.STRING },
      },
      required: ["word", "marathiMeaning", "exampleSentence", "pronunciation", "partOfSpeech"],
    },
  },
  required: ["hasMistake", "mistakeCategory", "assistantReply"],
};

const dailyTopicSchema = {
  type: Type.OBJECT,
  properties: {
    title: { type: Type.STRING },
    category: { type: Type.STRING },
    sessionType: {
      type: Type.STRING,
      description:
        "One of: DAILY_CONVERSATION, INTERVIEW_PRACTICE, GENERAL_CONVERSATION, REVISION",
    },
    interviewCategory: {
      type: Type.STRING,
      nullable: true,
    },
    scheduleAdjustedNote: {
      type: Type.STRING,
      nullable: true,
    },
    promptSummary: { type: Type.STRING },
    starterQuestion: { type: Type.STRING },
    marathiHint: { type: Type.STRING },
  },
  required: [
    "title",
    "category",
    "sessionType",
    "promptSummary",
    "starterQuestion",
    "marathiHint",
  ],
};

const interviewReportSchema = {
  type: Type.OBJECT,
  properties: {
    overallSummary: { type: Type.STRING },
    grammarImprovement: { type: Type.STRING },
    vocabularyFeedback: { type: Type.STRING },
    sentenceFormation: { type: Type.STRING },
    answerClarity: { type: Type.STRING },
    communicationFeedback: { type: Type.STRING },
    suggestedBetterAnswers: { type: Type.STRING },
    areasToImprove: { type: Type.STRING },
    marathiCoachNote: { type: Type.STRING },
  },
  required: [
    "overallSummary",
    "grammarImprovement",
    "vocabularyFeedback",
    "sentenceFormation",
    "answerClarity",
    "communicationFeedback",
    "suggestedBetterAnswers",
    "areasToImprove",
    "marathiCoachNote",
  ],
};

const dailyVocabSchema = {
  type: Type.OBJECT,
  properties: {
    words: {
      type: Type.ARRAY,
      items: {
        type: Type.OBJECT,
        properties: {
          word: { type: Type.STRING },
          marathiMeaning: { type: Type.STRING },
          exampleSentence: { type: Type.STRING },
          pronunciation: { type: Type.STRING },
          partOfSpeech: { type: Type.STRING },
        },
        required: [
          "word",
          "marathiMeaning",
          "exampleSentence",
          "pronunciation",
          "partOfSpeech",
        ],
      },
    },
  },
  required: ["words"],
};

const singleWordSchema = {
  type: Type.OBJECT,
  properties: {
    word: { type: Type.STRING },
    marathiMeaning: { type: Type.STRING },
    exampleSentence: { type: Type.STRING },
    pronunciation: { type: Type.STRING },
    partOfSpeech: { type: Type.STRING },
  },
  required: [
    "word",
    "marathiMeaning",
    "exampleSentence",
    "pronunciation",
    "partOfSpeech",
  ],
};

function parseEnglishLevel(val: unknown): EnglishLevel {
  return val === "MEDIUM" || val === "HIGH" ? val : "LOW";
}

function parseMarathiScript(val: unknown): MarathiScript {
  return val === "ROMAN_AND_DEVANAGARI" ? "ROMAN_AND_DEVANAGARI" : "ROMAN";
}

function parseSessionType(val: unknown): DailySessionType {
  if (
    val === "INTERVIEW_PRACTICE" ||
    val === "GENERAL_CONVERSATION" ||
    val === "REVISION"
  ) {
    return val;
  }
  return "DAILY_CONVERSATION";
}

export async function verifyFirebaseIdToken(
  idToken: string,
  firebaseWebApiKey?: string
): Promise<string | null> {
  if (!idToken || !firebaseWebApiKey) return null;
  try {
    const resp = await fetch(
      `https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${encodeURIComponent(firebaseWebApiKey)}`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ idToken }),
      }
    );
    if (!resp.ok) return null;
    const data = (await resp.json()) as {
      users?: Array<{ localId?: string }>;
    };
    return data.users?.[0]?.localId ?? null;
  } catch {
    return null;
  }
}

export async function executeGeminiAction(
  apiKey: string,
  modelName: string | undefined,
  body: GeminiRequestBody
): Promise<unknown> {
  const ai = new GoogleGenAI({ apiKey });
  const model = modelName?.trim() || DEFAULT_MODEL;
  const p = body.payload ?? {};

  switch (body.action) {
    case "analyzeAndReply": {
      const userMessage = String(p.userMessage ?? "").trim();
      if (!userMessage) {
        throw new Error("userMessage is required.");
      }
      const level = parseEnglishLevel(p.level);
      const marathiScript = parseMarathiScript(p.marathiScript);
      const history = Array.isArray(p.history)
        ? (p.history as HistoryTurnInput[]).slice(-12)
        : [];
      const topicContext =
        typeof p.topicContext === "string" ? p.topicContext.trim() : "";
      const interviewCategory =
        typeof p.interviewCategory === "string"
          ? p.interviewCategory.trim()
          : "";
      const isVoiceMode = Boolean(p.isVoiceMode);

      const levelInstruction =
        level === "LOW"
          ? "LEVEL: LOW (Beginner). Use very simple English, short sentences, basic daily conversation, and easy vocabulary."
          : level === "MEDIUM"
            ? "LEVEL: MEDIUM (Intermediate). Use natural everyday English, helpful vocabulary, medium-length sentences, and clear grammar coaching."
            : "LEVEL: HIGH (Advanced). Use professional/fluent English, advanced vocabulary, nuanced discussions, and interview/workplace readiness.";

      const scriptInstruction =
        marathiScript === "ROMAN"
          ? "MARATHI SCRIPT: Write marathiExplanation and suggestedVocabulary.marathiMeaning in simple Roman Marathi (Marathi written in English letters, e.g., 'Kalchi goshta aahe, mhanun past tense vapraycha.')."
          : "MARATHI SCRIPT: Write marathiExplanation in simple Roman Marathi first, followed by a brief Marathi Devanagari line in parentheses.";

      const modeLines: string[] = [levelInstruction, scriptInstruction];
      if (interviewCategory) {
        modeLines.push(
          `INTERVIEW PRACTICE MODE ACTIVE (${interviewCategory}):
- Act as a realistic, encouraging human interviewer for "${interviewCategory}".
- Adapt interview question difficulty to ${level} English level.
- Analyze the candidate's answer for grammar mistakes and clarity.
- If there are grammar mistakes, correct them and explain in simple Roman Marathi.
- ALWAYS provide a polished, more professional English way to answer this interview question inside "naturalAlternative" (even if the user's grammar was okay, show how a confident candidate would phrase it!).
- In "assistantReply", briefly acknowledge their answer and ask ONLY ONE relevant follow-up interview question at a time, then wait for their answer.`
        );
      } else if (topicContext) {
        modeLines.push(
          `TOPIC CONTEXT: ${topicContext} (behave as a warm conversation partner, not a rigid questionnaire).`
        );
      }
      if (isVoiceMode) {
        modeLines.push(
          "VOICE MODE: Keep assistantReply concise (2-3 natural spoken sentences) so it sounds great when read aloud."
        );
      }
      modeLines.push(`USER MESSAGE TO ANALYZE AND RESPOND TO:\n${userMessage}`);

      const contents = [
        ...history.map((turn) => ({
          role: turn.role === "user" ? "user" : "model",
          parts: [{ text: turn.text }],
        })),
        {
          role: "user",
          parts: [{ text: modeLines.join("\n") }],
        },
      ];

      const response = await ai.models.generateContent({
        model,
        contents,
        config: {
          systemInstruction: SYSTEM_PROMPT,
          responseMimeType: "application/json",
          responseSchema: tutorResponseSchema,
          temperature: 0.7,
        },
      });

      const parsed = JSON.parse(response.text || "{}") as Record<string, unknown>;
      const hasMistake = Boolean(parsed.hasMistake);
      const isInterview = Boolean(interviewCategory);

      const vocabObj =
        parsed.suggestedVocabulary &&
        typeof parsed.suggestedVocabulary === "object"
          ? (parsed.suggestedVocabulary as Record<string, unknown>)
          : null;
      const suggestedWord =
        vocabObj && String(vocabObj.word ?? "").trim()
          ? {
              word: String(vocabObj.word).trim().slice(0, 80),
              marathiMeaning:
                String(vocabObj.marathiMeaning ?? "").trim() || "English word",
              exampleSentence:
                String(vocabObj.exampleSentence ?? "").trim() ||
                `Try using ${String(vocabObj.word).trim()} in a sentence.`,
              pronunciation:
                String(vocabObj.pronunciation ?? "").trim() ||
                String(vocabObj.word).trim().toLowerCase(),
              partOfSpeech:
                String(vocabObj.partOfSpeech ?? "").trim() || "word",
            }
          : null;

      const rawNaturalAlt = String(parsed.naturalAlternative ?? "").trim();
      const naturalAlternative =
        rawNaturalAlt && (hasMistake || isInterview) ? rawNaturalAlt : null;

      return {
        hasMistake,
        mistakeCategory:
          String(parsed.mistakeCategory ?? "") ||
          (hasMistake ? "Grammar" : "None"),
        originalSentence:
          hasMistake && String(parsed.originalSentence ?? "").trim()
            ? String(parsed.originalSentence).trim()
            : null,
        correctedSentence:
          hasMistake && String(parsed.correctedSentence ?? "").trim()
            ? String(parsed.correctedSentence).trim()
            : null,
        marathiExplanation:
          hasMistake && String(parsed.marathiExplanation ?? "").trim()
            ? String(parsed.marathiExplanation).trim()
            : null,
        naturalAlternative,
        assistantReply:
          String(parsed.assistantReply ?? "").trim() ||
          "That's great! Tell me a little more about that.",
        suggestedVocabulary: suggestedWord,
      };
    }

    case "generateDailyTopic": {
      const level = parseEnglishLevel(p.level);
      const dayOfWeekName = String(p.dayOfWeekName ?? "Monday");
      const scheduledSessionType = parseSessionType(p.scheduledSessionType);
      const interviewsThisWeek = Number(p.interviewsThisWeek ?? 0);
      const weeklyInterviewFrequency = Math.min(
        2,
        Math.max(1, Number(p.weeklyInterviewFrequency ?? 2))
      );
      const preferredCategory =
        typeof p.preferredCategory === "string"
          ? p.preferredCategory.trim()
          : "";
      const preferredInterviewCategory =
        typeof p.preferredInterviewCategory === "string"
          ? p.preferredInterviewCategory.trim()
          : "";
      const recentTopicTitles = Array.isArray(p.recentTopicTitles)
        ? (p.recentTopicTitles as string[]).slice(0, 12)
        : [];
      const dateKey = String(p.dateKey ?? "");
      const allowScheduleAdjustment =
        p.allowScheduleAdjustment === undefined
          ? true
          : Boolean(p.allowScheduleAdjustment);

      const interviewQuotaRemaining = Math.max(
        0,
        weeklyInterviewFrequency - interviewsThisWeek
      );

      const lines: string[] = [
        `Generate ONE fresh English practice session for ${dateKey} (${dayOfWeekName}).`,
        `Learner English level: ${level} (${LEVEL_DESCRIPTIONS[level]}).`,
        "Standard weekly schedule: Mon=Daily Conversation, Tue=Interview Practice, Wed=Daily Conversation, Thu=Daily Conversation, Fri=Interview Practice, Sat=General Conversation, Sun=Revision and Free Conversation.",
        `Weekly interview frequency setting: ${weeklyInterviewFrequency} session(s)/week (STRICT CAP: never exceed 2/week; interviews already done/scheduled this week: ${interviewsThisWeek}; remaining quota: ${interviewQuotaRemaining}).`,
        `Today's default scheduled mode: ${scheduledSessionType}.`,
      ];

      if (
        allowScheduleAdjustment &&
        scheduledSessionType === "INTERVIEW_PRACTICE" &&
        interviewQuotaRemaining <= 0
      ) {
        lines.push(
          `ADJUSTMENT REQUIRED: Weekly interview quota (${weeklyInterviewFrequency}) is already reached this week. Adjust today to DAILY_CONVERSATION and explain briefly in scheduleAdjustedNote.`
        );
      } else if (
        allowScheduleAdjustment &&
        scheduledSessionType !== "INTERVIEW_PRACTICE" &&
        (dayOfWeekName === "Thursday" || dayOfWeekName === "Saturday") &&
        interviewsThisWeek === 0 &&
        preferredInterviewCategory
      ) {
        lines.push(
          `You may schedule INTERVIEW_PRACTICE today since 0 interviews have happened this week, or follow ${scheduledSessionType}.`
        );
      } else {
        lines.push(
          `Follow sessionType = ${scheduledSessionType} unless a smart adjustment is clearly helpful.`
        );
      }

      if (preferredInterviewCategory) {
        lines.push(
          `If sessionType is INTERVIEW_PRACTICE, use interviewCategory = "${preferredInterviewCategory}".`
        );
      } else {
        lines.push(
          `If sessionType is INTERVIEW_PRACTICE, choose from: ${INTERVIEW_CATEGORIES.join(", ")}.`
        );
      }

      if (preferredCategory && preferredCategory !== "All") {
        lines.push(
          `Preferred conversation category (if not interview): ${preferredCategory}.`
        );
      } else {
        lines.push(
          "If not an interview, choose a category from: Daily Life, Education, Technology, AI and Machine Learning, Career, Business, Travel, Friends and Family, General Knowledge, Real-life Situations."
        );
      }

      if (recentTopicTitles.length > 0) {
        lines.push(
          `DO NOT repeat or closely duplicate these recently completed/seen topics: ${recentTopicTitles.join(", ")}.`
        );
      }
      lines.push(
        "Write marathiHint in simple Roman Marathi (Marathi in English letters)."
      );

      const response = await ai.models.generateContent({
        model,
        contents: lines.join("\n"),
        config: {
          responseMimeType: "application/json",
          responseSchema: dailyTopicSchema,
          temperature: 0.85,
        },
      });

      const parsed = JSON.parse(response.text || "{}") as Record<string, unknown>;
      const rawType = parseSessionType(parsed.sessionType);
      const enforcedType =
        rawType === "INTERVIEW_PRACTICE" && interviewQuotaRemaining <= 0
          ? "DAILY_CONVERSATION"
          : rawType;

      const interviewCat =
        enforcedType === "INTERVIEW_PRACTICE"
          ? (
              String(parsed.interviewCategory ?? "").trim() ||
              preferredInterviewCategory ||
              "HR Interview"
            ).slice(0, 75)
          : null;

      const scheduleAdjustedNote =
        String(parsed.scheduleAdjustedNote ?? "").trim().slice(0, 280) || null;

      return {
        title: (
          String(parsed.title ?? "").trim() || "Everyday Conversations"
        ).slice(0, 115),
        category: (
          interviewCat ||
          String(parsed.category ?? "").trim() ||
          preferredCategory ||
          "Daily Life"
        ).slice(0, 55),
        sessionType: enforcedType,
        interviewCategory: interviewCat,
        scheduleAdjustedNote,
        promptSummary: (
          String(parsed.promptSummary ?? "").trim() ||
          "Let's practice speaking naturally about today's topic."
        ).slice(0, 580),
        starterQuestion: (
          String(parsed.starterQuestion ?? "").trim() ||
          "What is one interesting thing that happened in your day?"
        ).slice(0, 480),
        marathiHint: (
          String(parsed.marathiHint ?? "").trim() ||
          "Tumchya shabdat sadhya English madhe uttar dya."
        ).slice(0, 480),
      };
    }

    case "generateInterviewReport": {
      const interviewCategory = String(p.interviewCategory ?? "HR Interview");
      const level = parseEnglishLevel(p.level);
      const marathiScript = parseMarathiScript(p.marathiScript);
      const transcript = Array.isArray(p.transcript)
        ? (p.transcript as HistoryTurnInput[])
        : [];

      const formattedTranscript = transcript
        .map((msg) => {
          const speaker = msg.role === "user" ? "CANDIDATE" : "INTERVIEWER";
          const corr =
            msg.hasMistake && msg.correctedSentence
              ? ` [Correction noted: ${msg.correctedSentence}]`
              : "";
          return `${speaker}: ${msg.text}${corr}`;
        })
        .join("\n");

      const prompt = [
        `Generate an encouraging, detailed Interview Performance Report for a ${interviewCategory} practice session.`,
        `Candidate English Level: ${level}.`,
        marathiScript === "ROMAN"
          ? "Write marathiCoachNote in simple Roman Marathi (English letters)."
          : "Write marathiCoachNote in simple Roman Marathi with Devanagari.",
        "Base ALL feedback strictly on the real transcript below (do not invent answers the candidate did not give):",
        formattedTranscript || "Candidate started the interview session.",
      ].join("\n");

      const response = await ai.models.generateContent({
        model,
        contents: prompt,
        config: {
          responseMimeType: "application/json",
          responseSchema: interviewReportSchema,
          temperature: 0.5,
        },
      });

      const parsed = JSON.parse(response.text || "{}") as Record<string, unknown>;
      return {
        overallSummary: (
          String(parsed.overallSummary ?? "").trim() ||
          "Good effort completing your interview practice!"
        ).slice(0, 950),
        grammarImprovement: (
          String(parsed.grammarImprovement ?? "").trim() ||
          "Focus on consistent verb tenses."
        ).slice(0, 1450),
        vocabularyFeedback: (
          String(parsed.vocabularyFeedback ?? "").trim() ||
          "Build action verbs relevant to your role."
        ).slice(0, 1450),
        sentenceFormation: (
          String(parsed.sentenceFormation ?? "").trim() ||
          "Use clear, concise sentences."
        ).slice(0, 1450),
        answerClarity: (
          String(parsed.answerClarity ?? "").trim() ||
          "State your main point first, then add an example."
        ).slice(0, 1450),
        communicationFeedback: (
          String(parsed.communicationFeedback ?? "").trim() ||
          "Polite and positive communication tone."
        ).slice(0, 1450),
        suggestedBetterAnswers: (
          String(parsed.suggestedBetterAnswers ?? "").trim() ||
          "Structure answers with Situation, Action, and Result."
        ).slice(0, 2400),
        areasToImprove: (
          String(parsed.areasToImprove ?? "").trim() ||
          "Practice speaking smoothly for 30-45 seconds per answer."
        ).slice(0, 1450),
        marathiCoachNote: (
          String(parsed.marathiCoachNote ?? "").trim() ||
          "Chhan prayatna! Darroj thoda sarav kela ki tumcha confidence ajun vadhel."
        ).slice(0, 1450),
      };
    }

    case "generateDailyVocabulary": {
      const level = parseEnglishLevel(p.level);
      const marathiScript = parseMarathiScript(p.marathiScript);
      const existingWords = Array.isArray(p.existingWords)
        ? (p.existingWords as string[]).slice(0, 40)
        : [];

      const lines = [
        `Generate 4 useful English vocabulary words for a learner at ${level} level (${LEVEL_DESCRIPTIONS[level]}).`,
        existingWords.length > 0
          ? `Avoid repeating these already saved words: ${existingWords.join(", ")}.`
          : "",
        marathiScript === "ROMAN"
          ? "Write marathiMeaning in simple Roman Marathi (English letters)."
          : "Write marathiMeaning in simple Roman Marathi with Devanagari in parentheses.",
      ].filter(Boolean);

      const response = await ai.models.generateContent({
        model,
        contents: lines.join("\n"),
        config: {
          responseMimeType: "application/json",
          responseSchema: dailyVocabSchema,
          temperature: 0.8,
        },
      });

      const parsed = JSON.parse(response.text || "{}") as {
        words?: Array<Record<string, unknown>>;
      };
      const words = Array.isArray(parsed.words) ? parsed.words : [];
      return words
        .map((w) => ({
          word: String(w.word ?? "").trim().slice(0, 75),
          marathiMeaning: String(w.marathiMeaning ?? "").trim().slice(0, 190),
          exampleSentence: String(w.exampleSentence ?? "").trim().slice(0, 380),
          pronunciation: String(w.pronunciation ?? "").trim().slice(0, 110),
          partOfSpeech: (
            String(w.partOfSpeech ?? "").trim() || "word"
          ).slice(0, 35),
        }))
        .filter((w) => Boolean(w.word));
    }

    case "enrichVocabularyWord": {
      const word = String(p.word ?? "").trim();
      if (!word) {
        throw new Error("word is required.");
      }
      const level = parseEnglishLevel(p.level);
      const marathiScript = parseMarathiScript(p.marathiScript);

      const prompt = [
        `Explain the English word or phrase '${word}' for a Marathi-speaking learner at ${level} level.`,
        marathiScript === "ROMAN"
          ? "Provide marathiMeaning in simple Roman Marathi (English letters)."
          : "Provide marathiMeaning in simple Roman Marathi with Devanagari.",
      ].join("\n");

      const response = await ai.models.generateContent({
        model,
        contents: prompt,
        config: {
          responseMimeType: "application/json",
          responseSchema: singleWordSchema,
          temperature: 0.4,
        },
      });

      const parsed = JSON.parse(response.text || "{}") as Record<string, unknown>;
      return {
        word: (String(parsed.word ?? "").trim() || word).slice(0, 75),
        marathiMeaning: String(parsed.marathiMeaning ?? "").trim().slice(0, 190),
        exampleSentence: String(parsed.exampleSentence ?? "").trim().slice(0, 380),
        pronunciation: String(parsed.pronunciation ?? "").trim().slice(0, 110),
        partOfSpeech: (
          String(parsed.partOfSpeech ?? "").trim() || "word"
        ).slice(0, 35),
      };
    }

    default:
      throw new Error(`Unsupported Gemini action: ${String(body.action)}`);
  }
}
