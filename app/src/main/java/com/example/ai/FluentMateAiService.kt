package com.example.ai

import android.util.Log
import com.example.data.ChatMessage
import com.example.data.DailySessionType
import com.example.data.EnglishLevel
import com.example.data.GeneratedInterviewReportDraft
import com.example.data.GeneratedTopicDraft
import com.example.data.GeneratedWordDraft
import com.example.data.InterviewCategory
import com.example.data.MarathiScript
import com.example.data.TutorTurnResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class FluentMateAiService {

    private val tutorResponseSchema = Schema.obj(
        properties = mapOf(
            "hasMistake" to Schema.boolean(
                description = "True ONLY if the learner's English message has a genuine grammar, spelling, tense, preposition, vocabulary, or unnatural phrasing mistake. False if the sentence is already correct and natural."
            ),
            "mistakeCategory" to Schema.string(
                description = "One of: None, Grammar, Tense, Preposition, Spelling, Vocabulary, Sentence Structure, Natural Usage"
            ),
            "originalSentence" to Schema.string(
                description = "The exact part or sentence from the user that had a mistake, or empty string if hasMistake is false.",
                nullable = true
            ),
            "correctedSentence" to Schema.string(
                description = "The corrected English sentence, or empty string if hasMistake is false.",
                nullable = true
            ),
            "marathiExplanation" to Schema.string(
                description = "Simple, encouraging explanation of the mistake in Marathi (Roman Marathi by default), or empty string if hasMistake is false.",
                nullable = true
            ),
            "naturalAlternative" to Schema.string(
                description = "In Interview mode: a more professional English answer for the interview. In regular chat: a natural native-sounding alternative if helpful.",
                nullable = true
            ),
            "assistantReply" to Schema.string(
                description = "Friendly, natural conversational English reply (or next single interview question when acting as an interviewer)."
            ),
            "suggestedVocabulary" to Schema.obj(
                properties = mapOf(
                    "word" to Schema.string(description = "One useful English vocabulary word from or relevant to this turn"),
                    "marathiMeaning" to Schema.string(description = "Simple Roman Marathi meaning"),
                    "exampleSentence" to Schema.string(description = "Clear example sentence at the learner's level"),
                    "pronunciation" to Schema.string(description = "Simple phonetic pronunciation guide, e.g. kon-fi-duhnt"),
                    "partOfSpeech" to Schema.string(description = "noun, verb, adjective, adverb, or phrase")
                ),
                description = "Optional single helpful vocabulary word to learn from this turn, or null if not needed.",
                nullable = true
            )
        ),
        optionalProperties = listOf(
            "originalSentence",
            "correctedSentence",
            "marathiExplanation",
            "naturalAlternative",
            "suggestedVocabulary"
        )
    )

    private val dailyTopicSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Short, engaging conversation or interview topic title (2-7 words)"),
            "category" to Schema.string(description = "Topic category or interview category name"),
            "sessionType" to Schema.string(
                description = "One of: DAILY_CONVERSATION, INTERVIEW_PRACTICE, GENERAL_CONVERSATION, REVISION"
            ),
            "interviewCategory" to Schema.string(
                description = "If sessionType is INTERVIEW_PRACTICE, one of: HR Interview, Self Introduction, Technical Interview, College Placement Interview, Internship Interview, AI/ML Job Interview, Communication Skills Interview. Otherwise empty string.",
                nullable = true
            ),
            "scheduleAdjustedNote" to Schema.string(
                description = "Brief explanation if the AI adjusted today's schedule from the default weekly plan, or empty string if following the standard schedule.",
                nullable = true
            ),
            "promptSummary" to Schema.string(description = "2-sentence overview of what we will practice today"),
            "starterQuestion" to Schema.string(description = "Friendly opening question (or first single interview question) to kick off the session"),
            "marathiHint" to Schema.string(description = "Helpful tip in simple Roman Marathi on how the learner can start answering")
        ),
        optionalProperties = listOf("interviewCategory", "scheduleAdjustedNote")
    )

    private val interviewReportSchema = Schema.obj(
        properties = mapOf(
            "overallSummary" to Schema.string(description = "2-3 sentence overall evaluation of the interview performance"),
            "grammarImprovement" to Schema.string(description = "Specific grammar feedback and improvement notes from the candidate's answers"),
            "vocabularyFeedback" to Schema.string(description = "Feedback on word choice and professional/interview vocabulary"),
            "sentenceFormation" to Schema.string(description = "Feedback on sentence structure, fluency, and conciseness"),
            "answerClarity" to Schema.string(description = "Feedback on how clearly and directly the candidate answered questions"),
            "communicationFeedback" to Schema.string(description = "Tone, confidence, and professional communication feedback"),
            "suggestedBetterAnswers" to Schema.string(description = "2-3 concrete improved/professional sample answers based on what the learner actually answered"),
            "areasToImprove" to Schema.string(description = "Clear actionable bullet points for what to practice before the next interview"),
            "marathiCoachNote" to Schema.string(description = "Warm, encouraging summary and tip in simple Roman Marathi")
        )
    )

    private val dailyVocabSchema = Schema.obj(
        properties = mapOf(
            "words" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "word" to Schema.string(description = "English word"),
                        "marathiMeaning" to Schema.string(description = "Simple Roman Marathi meaning"),
                        "exampleSentence" to Schema.string(description = "Practical daily English example sentence"),
                        "pronunciation" to Schema.string(description = "Simple phonetic pronunciation"),
                        "partOfSpeech" to Schema.string(description = "noun, verb, adjective, or adverb")
                    )
                ),
                description = "List of 4 practical English words for today"
            )
        )
    )

    private val singleWordSchema = Schema.obj(
        properties = mapOf(
            "word" to Schema.string(description = "English word"),
            "marathiMeaning" to Schema.string(description = "Simple Roman Marathi meaning"),
            "exampleSentence" to Schema.string(description = "Practical English example sentence"),
            "pronunciation" to Schema.string(description = "Simple phonetic pronunciation"),
            "partOfSpeech" to Schema.string(description = "noun, verb, adjective, adverb, or phrase")
        )
    )

    private val tutorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = tutorResponseSchema
                temperature = 0.7f
            },
            systemInstruction = content {
                text(SYSTEM_PROMPT)
            }
        )
    }

    private val topicModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dailyTopicSchema
                temperature = 0.85f
            }
        )
    }

    private val reportModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = interviewReportSchema
                temperature = 0.5f
            }
        )
    }

    private val vocabModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = dailyVocabSchema
                temperature = 0.8f
            }
        )
    }

    private val singleWordModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = singleWordSchema
                temperature = 0.4f
            }
        )
    }

    suspend fun analyzeAndReply(
        userMessage: String,
        level: EnglishLevel,
        marathiScript: MarathiScript,
        history: List<ChatMessage>,
        topicContext: String? = null,
        interviewCategory: String? = null,
        isVoiceMode: Boolean = false
    ): TutorTurnResult {
        try {
            val recentHistory = history.takeLast(12).map { msg ->
                content(role = if (msg.role == "user") "user" else "model") {
                    text(msg.text)
                }
            }

            val levelInstruction = when (level) {
                EnglishLevel.LOW -> "LEVEL: LOW (Beginner). Use very simple English, short sentences, basic daily conversation, and easy vocabulary."
                EnglishLevel.MEDIUM -> "LEVEL: MEDIUM (Intermediate). Use natural everyday English, helpful vocabulary, medium-length sentences, and clear grammar coaching."
                EnglishLevel.HIGH -> "LEVEL: HIGH (Advanced). Use professional/fluent English, advanced vocabulary, nuanced discussions, and interview/workplace readiness."
            }

            val scriptInstruction = when (marathiScript) {
                MarathiScript.ROMAN -> "MARATHI SCRIPT: Write marathiExplanation and suggestedVocabulary.marathiMeaning in simple Roman Marathi (Marathi written in English letters, e.g., 'Kalchi goshta aahe, mhanun past tense vapraycha.')."
                MarathiScript.ROMAN_AND_DEVANAGARI -> "MARATHI SCRIPT: Write marathiExplanation in simple Roman Marathi first, followed by a brief Marathi Devanagari line in parentheses."
            }

            val modeNote = buildString {
                appendLine(levelInstruction)
                appendLine(scriptInstruction)
                if (!interviewCategory.isNullOrBlank()) {
                    appendLine(
                        """
                        INTERVIEW PRACTICE MODE ACTIVE ($interviewCategory):
                        - Act as a realistic, encouraging human interviewer for "$interviewCategory".
                        - Adapt interview question difficulty to ${level.name} (${level.label}) English level.
                        - Analyze the candidate's answer for grammar mistakes and clarity.
                        - If there are grammar mistakes, correct them and explain in simple Roman Marathi.
                        - ALWAYS provide a polished, more professional English way to answer this interview question inside "naturalAlternative" (even if the user's grammar was okay, show how a confident candidate would phrase it!).
                        - In "assistantReply", briefly acknowledge their answer and ask ONLY ONE relevant follow-up interview question at a time, then wait for their answer.
                        """.trimIndent()
                    )
                } else if (!topicContext.isNullOrBlank()) {
                    appendLine("TOPIC CONTEXT: $topicContext (behave as a warm conversation partner, not a rigid questionnaire).")
                }
                if (isVoiceMode) {
                    appendLine("VOICE MODE: Keep assistantReply concise (2-3 natural spoken sentences) so it sounds great when read aloud.")
                }
                appendLine("USER MESSAGE TO ANALYZE AND RESPOND TO:")
                append(userMessage)
            }

            val chat = tutorModel.startChat(history = recentHistory)
            val response = withTimeout(TIMEOUT_MS) {
                chat.sendMessage(modeNote)
            }

            val rawJson = response.text
                ?: throw IllegalStateException("FluentMate AI returned an empty response.")
            val json = JSONObject(rawJson)

            val hasMistake = json.optBoolean("hasMistake", false)
            val isInterview = !interviewCategory.isNullOrBlank()
            val suggestedObj = json.optJSONObject("suggestedVocabulary")
            val suggestedWord = suggestedObj?.let { obj ->
                val w = obj.optString("word", "").trim()
                if (w.isNotBlank()) {
                    GeneratedWordDraft(
                        word = w,
                        marathiMeaning = obj.optString("marathiMeaning", "").ifBlank { "English word" },
                        exampleSentence = obj.optString("exampleSentence", "").ifBlank { "Try using $w in a sentence." },
                        pronunciation = obj.optString("pronunciation", "").ifBlank { w.lowercase() },
                        partOfSpeech = obj.optString("partOfSpeech", "").ifBlank { "word" }
                    )
                } else null
            }

            val naturalAlt = json.optString("naturalAlternative", "")
                .takeIf { it.isNotBlank() && (hasMistake || isInterview) }

            return TutorTurnResult(
                hasMistake = hasMistake,
                mistakeCategory = json.optString("mistakeCategory", if (hasMistake) "Grammar" else "None"),
                originalSentence = json.optString("originalSentence", "").takeIf { hasMistake && it.isNotBlank() },
                correctedSentence = json.optString("correctedSentence", "").takeIf { hasMistake && it.isNotBlank() },
                marathiExplanation = json.optString("marathiExplanation", "").takeIf { hasMistake && it.isNotBlank() },
                naturalAlternative = naturalAlt,
                assistantReply = json.optString("assistantReply", "").ifBlank {
                    "That's great! Tell me a little more about that."
                },
                suggestedVocabulary = suggestedWord
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Tutor AI request timed out", e)
            throw IllegalStateException("AI tutor took too long to respond. Please check your connection and try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tutor AI generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to reach FluentMate AI.")
        }
    }

    suspend fun generateDailyTopic(
        level: EnglishLevel,
        dayOfWeekName: String,
        scheduledSessionType: DailySessionType,
        interviewsThisWeek: Int,
        weeklyInterviewFrequency: Int,
        preferredCategory: String?,
        preferredInterviewCategory: InterviewCategory?,
        recentTopicTitles: List<String>,
        dateKey: String,
        allowScheduleAdjustment: Boolean = true
    ): GeneratedTopicDraft {
        try {
            val interviewQuotaRemaining = (weeklyInterviewFrequency - interviewsThisWeek).coerceAtLeast(0)
            val prompt = buildString {
                appendLine("Generate ONE fresh English practice session for $dateKey ($dayOfWeekName).")
                appendLine("Learner English level: ${level.name} (${level.description}).")
                appendLine("Standard weekly schedule: Mon=Daily Conversation, Tue=Interview Practice, Wed=Daily Conversation, Thu=Daily Conversation, Fri=Interview Practice, Sat=General Conversation, Sun=Revision and Free Conversation.")
                appendLine("Weekly interview frequency setting: $weeklyInterviewFrequency session(s)/week (STRICT CAP: never exceed 2/week; interviews already done/scheduled this week: $interviewsThisWeek; remaining quota: $interviewQuotaRemaining).")
                appendLine("Today's default scheduled mode: ${scheduledSessionType.name} (${scheduledSessionType.label}).")

                if (allowScheduleAdjustment && scheduledSessionType == DailySessionType.INTERVIEW_PRACTICE && interviewQuotaRemaining <= 0) {
                    appendLine("ADJUSTMENT REQUIRED: Weekly interview quota ($weeklyInterviewFrequency) is already reached this week. Adjust today to DAILY_CONVERSATION and explain briefly in scheduleAdjustedNote.")
                } else if (allowScheduleAdjustment &&
                    scheduledSessionType != DailySessionType.INTERVIEW_PRACTICE &&
                    (dayOfWeekName == "Thursday" || dayOfWeekName == "Saturday") &&
                    interviewsThisWeek == 0 &&
                    preferredInterviewCategory != null
                ) {
                    appendLine("You may schedule INTERVIEW_PRACTICE today since 0 interviews have happened this week, or follow ${scheduledSessionType.name}.")
                } else {
                    appendLine("Follow sessionType = ${scheduledSessionType.name} unless a smart adjustment is clearly helpful.")
                }

                if (preferredInterviewCategory != null) {
                    appendLine("If sessionType is INTERVIEW_PRACTICE, use interviewCategory = \"${preferredInterviewCategory.title}\".")
                } else {
                    appendLine("If sessionType is INTERVIEW_PRACTICE, choose from: ${InterviewCategory.entries.joinToString(", ") { it.title }}.")
                }

                if (!preferredCategory.isNullOrBlank() && preferredCategory != "All") {
                    appendLine("Preferred conversation category (if not interview): $preferredCategory.")
                } else {
                    appendLine("If not an interview, choose a category from: Daily Life, Education, Technology, AI and Machine Learning, Career, Business, Travel, Friends and Family, General Knowledge, Real-life Situations.")
                }
                if (recentTopicTitles.isNotEmpty()) {
                    appendLine("DO NOT repeat or closely duplicate these recently completed/seen topics: ${recentTopicTitles.joinToString(", ")}.")
                }
                appendLine("Write marathiHint in simple Roman Marathi (Marathi in English letters).")
            }

            val response = withTimeout(TIMEOUT_MS) {
                topicModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty topic response.")
            val json = JSONObject(rawJson)

            val rawSessionType = DailySessionType.fromRaw(json.optString("sessionType"))
            // Hard guard: never allow INTERVIEW_PRACTICE if weekly quota already exhausted
            val enforcedSessionType = if (rawSessionType == DailySessionType.INTERVIEW_PRACTICE && interviewQuotaRemaining <= 0) {
                DailySessionType.DAILY_CONVERSATION
            } else {
                rawSessionType
            }

            val interviewCat = if (enforcedSessionType == DailySessionType.INTERVIEW_PRACTICE) {
                json.optString("interviewCategory", "")
                    .ifBlank { preferredInterviewCategory?.title ?: InterviewCategory.HR.title }
                    .take(75)
            } else null

            val adjNote = json.optString("scheduleAdjustedNote", "")
                .takeIf { it.isNotBlank() }
                ?.take(280)

            return GeneratedTopicDraft(
                title = json.optString("title", "Everyday Conversations").take(115),
                category = (interviewCat ?: json.optString("category", preferredCategory ?: "Daily Life")).take(55),
                sessionType = enforcedSessionType,
                interviewCategory = interviewCat,
                scheduleAdjustedNote = adjNote,
                promptSummary = json.optString("promptSummary", "Let's practice speaking naturally about today's topic.").take(580),
                starterQuestion = json.optString("starterQuestion", "What is one interesting thing that happened in your day?").take(480),
                marathiHint = json.optString("marathiHint", "Tumchya shabdat sadhya English madhe uttar dya.").take(480)
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Daily topic generation timed out", e)
            throw IllegalStateException("Topic generation timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Daily topic generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate daily topic.")
        }
    }

    suspend fun generateInterviewReport(
        interviewCategory: String,
        level: EnglishLevel,
        marathiScript: MarathiScript,
        transcript: List<ChatMessage>
    ): GeneratedInterviewReportDraft {
        try {
            val formattedTranscript = transcript.joinToString("\n") { msg ->
                val speaker = if (msg.role == "user") "CANDIDATE" else "INTERVIEWER"
                val correctionInfo = if (msg.hasMistake && !msg.correctedSentence.isNullOrBlank()) {
                    " [Correction noted: ${msg.correctedSentence}]"
                } else ""
                "$speaker: ${msg.text}$correctionInfo"
            }

            val prompt = buildString {
                appendLine("Generate an encouraging, detailed Interview Performance Report for a $interviewCategory practice session.")
                appendLine("Candidate English Level: ${level.name} (${level.label}).")
                appendLine(
                    if (marathiScript == MarathiScript.ROMAN) {
                        "Write marathiCoachNote in simple Roman Marathi (English letters)."
                    } else {
                        "Write marathiCoachNote in simple Roman Marathi with Devanagari."
                    }
                )
                appendLine("Base ALL feedback strictly on the real transcript below (do not invent answers the candidate did not give):")
                appendLine(formattedTranscript.ifBlank { "Candidate started the interview session." })
            }

            val response = withTimeout(TIMEOUT_MS) {
                reportModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty interview report response.")
            val json = JSONObject(rawJson)

            return GeneratedInterviewReportDraft(
                overallSummary = json.optString("overallSummary", "Good effort completing your interview practice!").take(950),
                grammarImprovement = json.optString("grammarImprovement", "Focus on consistent verb tenses.").take(1450),
                vocabularyFeedback = json.optString("vocabularyFeedback", "Build action verbs relevant to your role.").take(1450),
                sentenceFormation = json.optString("sentenceFormation", "Use clear, concise sentences.").take(1450),
                answerClarity = json.optString("answerClarity", "State your main point first, then add an example.").take(1450),
                communicationFeedback = json.optString("communicationFeedback", "Polite and positive communication tone.").take(1450),
                suggestedBetterAnswers = json.optString("suggestedBetterAnswers", "Structure answers with Situation, Action, and Result.").take(2400),
                areasToImprove = json.optString("areasToImprove", "Practice speaking smoothly for 30-45 seconds per answer.").take(1450),
                marathiCoachNote = json.optString("marathiCoachNote", "Chhan prayatna! Darroj thoda sarav kela ki tumcha confidence ajun vadhel.").take(1450)
            )
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Report generation timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Interview report generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate interview report.")
        }
    }

    suspend fun generateDailyVocabulary(
        level: EnglishLevel,
        existingWords: List<String>,
        marathiScript: MarathiScript
    ): List<GeneratedWordDraft> {
        try {
            val prompt = buildString {
                appendLine("Generate 4 useful English vocabulary words for a learner at ${level.name} level (${level.description}).")
                if (existingWords.isNotEmpty()) {
                    appendLine("Avoid repeating these already saved words: ${existingWords.take(40).joinToString(", ")}.")
                }
                appendLine(
                    if (marathiScript == MarathiScript.ROMAN) {
                        "Write marathiMeaning in simple Roman Marathi (English letters)."
                    } else {
                        "Write marathiMeaning in simple Roman Marathi with Devanagari in parentheses."
                    }
                )
            }
            val response = withTimeout(TIMEOUT_MS) {
                vocabModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty vocabulary response.")
            val json = JSONObject(rawJson)
            val arr = json.optJSONArray("words") ?: return emptyList()
            return (0 until arr.length()).mapNotNull { idx ->
                val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                val word = obj.optString("word", "").trim()
                if (word.isBlank()) null
                else GeneratedWordDraft(
                    word = word.take(75),
                    marathiMeaning = obj.optString("marathiMeaning", "").take(190),
                    exampleSentence = obj.optString("exampleSentence", "").take(380),
                    pronunciation = obj.optString("pronunciation", "").take(110),
                    partOfSpeech = obj.optString("partOfSpeech", "word").take(35)
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Daily vocabulary generation timed out", e)
            throw IllegalStateException("Vocabulary generation timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Daily vocabulary generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Failed to generate vocabulary.")
        }
    }

    suspend fun enrichVocabularyWord(
        word: String,
        level: EnglishLevel,
        marathiScript: MarathiScript
    ): GeneratedWordDraft {
        try {
            val prompt = buildString {
                appendLine("Explain the English word or phrase '$word' for a Marathi-speaking learner at ${level.name} level.")
                appendLine(
                    if (marathiScript == MarathiScript.ROMAN) {
                        "Provide marathiMeaning in simple Roman Marathi (English letters)."
                    } else {
                        "Provide marathiMeaning in simple Roman Marathi with Devanagari."
                    }
                )
            }
            val response = withTimeout(TIMEOUT_MS) {
                singleWordModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty word enrichment response.")
            val json = JSONObject(rawJson)
            return GeneratedWordDraft(
                word = json.optString("word", word).ifBlank { word }.take(75),
                marathiMeaning = json.optString("marathiMeaning", "").take(190),
                exampleSentence = json.optString("exampleSentence", "").take(380),
                pronunciation = json.optString("pronunciation", "").take(110),
                partOfSpeech = json.optString("partOfSpeech", "word").take(35)
            )
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Word lookup timed out. Please try again.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Word enrichment failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not auto-fill word details.")
        }
    }

    companion object {
        private const val TAG = "FluentMateAiService"
        private const val TIMEOUT_MS = 60_000L

        const val SYSTEM_PROMPT = """
You are FluentMate AI, a friendly and patient English speaking and writing tutor.

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
- Make every interaction educational and enjoyable.
"""
    }
}
