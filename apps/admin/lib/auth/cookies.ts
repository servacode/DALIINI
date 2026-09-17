import "server-only";
import { cookies } from "next/headers";

const REFRESH_COOKIE = "__Host-directory_admin_refresh";

export async function setRefreshCookie(value: string, maxAgeSeconds: number): Promise<void> {
  const store = await cookies();
  store.set(REFRESH_COOKIE, value, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    sameSite: "strict",
    path: "/",
    maxAge: maxAgeSeconds,
  });
}

export async function getRefreshCookie(): Promise<string | null> {
  const store = await cookies();
  return store.get(REFRESH_COOKIE)?.value ?? null;
}

export async function clearRefreshCookie(): Promise<void> {
  const store = await cookies();
  store.delete(REFRESH_COOKIE);
}
