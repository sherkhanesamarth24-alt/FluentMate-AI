const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validProfilePayload(uid, now) {
  return {
    userId: uid,
    displayName: "Samarth",
    email: "samarth@example.com",
    englishLevel: "LOW",
    themeMode: "DARK",
    marathiScript: "ROMAN",
    autoSpeakAiReplies: true,
    speechRate: 1.0,
    totalConversations: 0,
    totalMessagesSent: 0,
    grammarMistakesDetected: 0,
    topicsCompletedCount: 0,
    vocabularyLearnedCount: 0,
    totalPracticeSeconds: 0,
    currentStreakDays: 0,
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, now)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("chatSessions").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vocabulary").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dailyTopics").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("dailyStats").get());
});

test("User profile: create, partial update, reject ghost fields and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );

  // Partial update allowed fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      englishLevel: "MEDIUM",
      totalMessagesSent: 2,
      lastPracticeDate: "2026-11-19",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt change
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      englishLevel: "HIGH",
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ englishLevel: "HIGH", updatedAt: new Date(Date.now() - 1000) }));
});

test("ChatSession & ChatMessage: create, list, update, delete and @DocumentId id-field rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const sessionRef = aliceDb.collection("users").doc(ALICE_UID).collection("chatSessions").doc("sess_1");
  await assertSucceeds(
    sessionRef.set({
      userId: ALICE_UID,
      title: "Daily English Chat",
      englishLevel: "LOW",
      mode: "CHAT",
      messageCount: 0,
      mistakesCount: 0,
      lastMessagePreview: "Started a new conversation",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject redundant @DocumentId `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatSessions").doc("sess_bad").set({
      id: "sess_bad",
      userId: ALICE_UID,
      title: "Bad Session",
      englishLevel: "LOW",
      mode: "CHAT",
      messageCount: 0,
      mistakesCount: 0,
      lastMessagePreview: "Bad",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Create user message & assistant correction message
  await assertSucceeds(
    sessionRef.collection("messages").doc("msg_1").set({
      userId: ALICE_UID,
      sessionId: "sess_1",
      role: "user",
      text: "I am go to college yesterday.",
      englishLevel: "LOW",
      hasMistake: false,
      voiceMessage: false,
      createdAt: created,
    })
  );

  await assertSucceeds(
    sessionRef.collection("messages").doc("msg_2").set({
      userId: ALICE_UID,
      sessionId: "sess_1",
      role: "assistant",
      text: "What did you do at college yesterday?",
      englishLevel: "LOW",
      hasMistake: true,
      originalSentence: "I am go to college yesterday.",
      correctedSentence: "I went to college yesterday.",
      marathiExplanation: "Kalchi goshta aahe, mhanun past tense vapraycha. Go cha past form Went aahe.",
      naturalAlternative: "I went to college yesterday.",
      mistakeCategory: "Tense",
      suggestedWord: "attend",
      suggestedWordMarathi: "Upasthit rahane (to be present at)",
      suggestedWordExample: "I attended all my lectures yesterday.",
      suggestedWordPronunciation: "uh-TEND",
      suggestedWordPos: "verb",
      voiceMessage: false,
      createdAt: new Date(Date.now() - 5000),
    })
  );

  // List queries succeed for owner, fail for Bob
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("chatSessions").get());
  await assertSucceeds(sessionRef.collection("messages").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chatSessions").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("chatSessions").doc("sess_1").collection("messages").get());

  // Update session counters
  await assertSucceeds(
    sessionRef.update({
      messageCount: 2,
      mistakesCount: 1,
      lastMessagePreview: "What did you do at college yesterday?",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Delete message & session
  await assertSucceeds(sessionRef.collection("messages").doc("msg_1").delete());
  await assertSucceeds(sessionRef.delete());
});

test("DailyTopic, VocabularyWord & DailyProgressStat: full CRUD parity & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  const topicRef = aliceDb.collection("users").doc(ALICE_UID).collection("dailyTopics").doc("2026-11-19");
  await assertSucceeds(
    topicRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-19",
      title: "My Daily Routine",
      category: "Daily Life",
      englishLevel: "LOW",
      promptSummary: "Talk about your morning, college, and evening habits in simple English.",
      starterQuestion: "What time do you usually wake up in the morning?",
      marathiHint: "Tumhi sakali kiti vajta uthta te sadhya English madhe sanga.",
      completed: false,
      turnsCompleted: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    topicRef.update({
      completed: true,
      turnsCompleted: 3,
      sessionId: "sess_topic_1",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const vocabRef = aliceDb.collection("users").doc(ALICE_UID).collection("vocabulary").doc("word_1");
  await assertSucceeds(
    vocabRef.set({
      userId: ALICE_UID,
      word: "Confident",
      marathiMeaning: "Atmavishvas aslela",
      exampleSentence: "I feel confident when I speak English.",
      pronunciation: "KON-fi-duhnt",
      partOfSpeech: "adjective",
      englishLevel: "LOW",
      dateLearned: "2026-11-19",
      learned: false,
      source: "DAILY",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    vocabRef.update({
      learned: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const statRef = aliceDb.collection("users").doc(ALICE_UID).collection("dailyStats").doc("2026-11-19");
  await assertSucceeds(
    statRef.set({
      userId: ALICE_UID,
      dateKey: "2026-11-19",
      practiceSeconds: 180,
      messagesCount: 4,
      mistakesCount: 1,
      wordsLearnedCount: 1,
      topicsCompletedCount: 1,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("dailyTopics").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("vocabulary").get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("dailyStats").get());

  // Weekly Interview Practice settings, topic, session & report
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID, created))
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      weeklyInterviewFrequency: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      weeklyInterviewFrequency: 5,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("chatSessions").doc("sess_interview").set({
      userId: ALICE_UID,
      title: "HR Interview Practice",
      interviewCategory: "HR Interview",
      englishLevel: "MEDIUM",
      mode: "INTERVIEW",
      messageCount: 1,
      mistakesCount: 0,
      lastMessagePreview: "Tell me about a challenge you solved.",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("interviewReports").doc("rep_1").set({
      userId: ALICE_UID,
      sessionId: "sess_interview",
      interviewCategory: "HR Interview",
      englishLevel: "MEDIUM",
      overallSummary: "Good clarity and calm tone.",
      grammarImprovement: "Watch past tense irregular verbs.",
      vocabularyFeedback: "Good use of teamwork words.",
      sentenceFormation: "Keep answers concise and structured.",
      answerClarity: "Clear main point with example.",
      communicationFeedback: "Confident and polite tone.",
      suggestedBetterAnswers: "Use STAR method for behavioral answers.",
      areasToImprove: "Past tense consistency.",
      marathiCoachNote: "Chhan prayatna! Past tense var thoda laksha dya.",
      questionsAnswered: 3,
      createdAt: created,
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("interviewReports").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("interviewReports").get());

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vocabulary").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyTopics").doc("2026-11-19").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dailyStats").doc("2026-11-19").get());
});
