import { defineConfig, loadEnv, Plugin } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import {
  executeGeminiAction,
  GeminiRequestBody,
  verifyFirebaseIdToken,
} from "./functions/_lib/geminiEngine";

function localGeminiApiPlugin(): Plugin {
  let serverEnv: Record<string, string> = {};

  return {
    name: "fluentmate-local-gemini-api",
    configResolved(config) {
      serverEnv = loadEnv(config.mode, config.root, "");
    },
    configureServer(server) {
      server.middlewares.use("/api/gemini", async (req, res) => {
        if (req.method !== "POST") {
          res.statusCode = 405;
          res.setHeader("Content-Type", "application/json");
          res.end(JSON.stringify({ error: "Method not allowed" }));
          return;
        }

        const apiKey = (
          serverEnv.GEMINI_API_KEY ||
          process.env.GEMINI_API_KEY ||
          ""
        ).trim();
        if (!apiKey) {
          res.statusCode = 503;
          res.setHeader("Content-Type", "application/json");
          res.end(
            JSON.stringify({
              error:
                "GEMINI_API_KEY is not set in local .env or environment variables.",
            })
          );
          return;
        }

        const authHeader = req.headers.authorization ?? "";
        const idToken = authHeader.startsWith("Bearer ")
          ? authHeader.slice(7).trim()
          : "";
        const firebaseWebKey = (
          serverEnv.FIREBASE_WEB_API_KEY ||
          serverEnv.VITE_FIREBASE_API_KEY ||
          ""
        ).trim();

        if (firebaseWebKey) {
          if (!idToken) {
            res.statusCode = 401;
            res.setHeader("Content-Type", "application/json");
            res.end(
              JSON.stringify({ error: "Missing Firebase Authorization token." })
            );
            return;
          }
          const uid = await verifyFirebaseIdToken(idToken, firebaseWebKey);
          if (!uid) {
            res.statusCode = 401;
            res.setHeader("Content-Type", "application/json");
            res.end(
              JSON.stringify({ error: "Invalid or expired Firebase session." })
            );
            return;
          }
        }

        const chunks: Buffer[] = [];
        req.on("data", (chunk: Buffer) => chunks.push(chunk));
        req.on("end", async () => {
          try {
            const body = JSON.parse(
              Buffer.concat(chunks).toString("utf8")
            ) as GeminiRequestBody;
            const result = await executeGeminiAction(
              apiKey,
              serverEnv.GEMINI_MODEL || process.env.GEMINI_MODEL,
              body
            );
            res.statusCode = 200;
            res.setHeader("Content-Type", "application/json");
            res.end(JSON.stringify({ result }));
          } catch (err) {
            res.statusCode = 500;
            res.setHeader("Content-Type", "application/json");
            res.end(
              JSON.stringify({
                error:
                  err instanceof Error
                    ? err.message
                    : "Local Gemini API error.",
              })
            );
          }
        });
      });
    },
  };
}

export default defineConfig({
  plugins: [react(), tailwindcss(), localGeminiApiPlugin()],
  build: {
    outDir: "dist",
    sourcemap: false,
  },
});
