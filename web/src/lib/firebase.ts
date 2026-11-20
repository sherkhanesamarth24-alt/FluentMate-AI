import { FirebaseApp, getApps, initializeApp } from "firebase/app";
import {
  Auth,
  GoogleAuthProvider,
  getAuth,
  signInWithPopup,
  signInWithRedirect,
  signOut,
} from "firebase/auth";
import { Firestore, getFirestore } from "firebase/firestore";

const DEFAULT_DATABASE_ID =
  "ai-studio-android-fluentma-03fa4c1b-c4f5-4780-aa9d-a90c5a9a3ecd";

export interface FirebaseEnvStatus {
  configured: boolean;
  missingKeys: string[];
  projectId: string;
  databaseId: string;
}

const REQUIRED_WEB_KEYS = [
  "VITE_FIREBASE_API_KEY",
  "VITE_FIREBASE_AUTH_DOMAIN",
  "VITE_FIREBASE_PROJECT_ID",
  "VITE_FIREBASE_APP_ID",
] as const;

export function getFirebaseEnvStatus(): FirebaseEnvStatus {
  const env = import.meta.env;
  const missingKeys = REQUIRED_WEB_KEYS.filter(
    (key) => !String(env[key] ?? "").trim()
  );
  return {
    configured: missingKeys.length === 0,
    missingKeys,
    projectId:
      String(env.VITE_FIREBASE_PROJECT_ID ?? "").trim() ||
      "gen-lang-client-0095552660",
    databaseId:
      String(env.VITE_FIREBASE_DATABASE_ID ?? "").trim() || DEFAULT_DATABASE_ID,
  };
}

let cachedApp: FirebaseApp | null = null;
let cachedAuth: Auth | null = null;
let cachedDb: Firestore | null = null;

export function getFirebaseServices(): {
  app: FirebaseApp;
  auth: Auth;
  db: Firestore;
} | null {
  const status = getFirebaseEnvStatus();
  if (!status.configured) return null;

  if (!cachedApp) {
    const existing = getApps();
    cachedApp =
      existing.length > 0
        ? existing[0]
        : initializeApp({
            apiKey: String(import.meta.env.VITE_FIREBASE_API_KEY).trim(),
            authDomain: String(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN).trim(),
            projectId: String(import.meta.env.VITE_FIREBASE_PROJECT_ID).trim(),
            storageBucket:
              String(import.meta.env.VITE_FIREBASE_STORAGE_BUCKET ?? "").trim() ||
              undefined,
            messagingSenderId:
              String(
                import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID ?? ""
              ).trim() || undefined,
            appId: String(import.meta.env.VITE_FIREBASE_APP_ID).trim(),
          });
  }

  if (!cachedAuth) {
    cachedAuth = getAuth(cachedApp);
  }

  if (!cachedDb) {
    cachedDb =
      status.databaseId === "(default)"
        ? getFirestore(cachedApp)
        : getFirestore(cachedApp, status.databaseId);
  }

  return { app: cachedApp, auth: cachedAuth, db: cachedDb };
}

export async function signInWithGoogle(auth: Auth): Promise<void> {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  try {
    await signInWithPopup(auth, provider);
  } catch (error: unknown) {
    const code =
      typeof error === "object" && error !== null && "code" in error
        ? String((error as { code: unknown }).code)
        : "";
    if (
      code === "auth/popup-blocked" ||
      code === "auth/operation-not-supported-in-this-environment"
    ) {
      await signInWithRedirect(auth, provider);
      return;
    }
    throw error;
  }
}

export async function signOutUser(auth: Auth): Promise<void> {
  await signOut(auth);
}
