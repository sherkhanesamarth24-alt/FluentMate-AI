import {
  executeGeminiAction,
  GeminiRequestBody,
  verifyFirebaseIdToken,
} from "../_lib/geminiEngine";

interface Env {
  GEMINI_API_KEY?: string;
  GEMINI_MODEL?: string;
  FIREBASE_WEB_API_KEY?: string;
  VITE_FIREBASE_API_KEY?: string;
}

function jsonResponse(data: unknown, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
    },
  });
}

export const onRequestPost: PagesFunction<Env> = async (context) => {
  const apiKey = context.env.GEMINI_API_KEY?.trim();
  if (!apiKey) {
    return jsonResponse(
      {
        error:
          "Server GEMINI_API_KEY is not configured. Set GEMINI_API_KEY in Cloudflare Pages Secrets (never as a VITE_ variable).",
      },
      503
    );
  }

  const authHeader = context.request.headers.get("Authorization") ?? "";
  const idToken = authHeader.startsWith("Bearer ")
    ? authHeader.slice(7).trim()
    : "";

  const firebaseWebKey =
    context.env.FIREBASE_WEB_API_KEY?.trim() ||
    context.env.VITE_FIREBASE_API_KEY?.trim();

  if (firebaseWebKey) {
    if (!idToken) {
      return jsonResponse({ error: "Missing Firebase Authorization token." }, 401);
    }
    const uid = await verifyFirebaseIdToken(idToken, firebaseWebKey);
    if (!uid) {
      return jsonResponse({ error: "Invalid or expired Firebase session." }, 401);
    }
  }

  let body: GeminiRequestBody;
  try {
    body = (await context.request.json()) as GeminiRequestBody;
  } catch {
    return jsonResponse({ error: "Invalid JSON request body." }, 400);
  }

  if (!body || typeof body.action !== "string") {
    return jsonResponse({ error: "Missing Gemini action." }, 400);
  }

  try {
    const result = await executeGeminiAction(
      apiKey,
      context.env.GEMINI_MODEL,
      body
    );
    return jsonResponse({ result }, 200);
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "Failed to reach Gemini AI.";
    return jsonResponse({ error: message }, 500);
  }
};
