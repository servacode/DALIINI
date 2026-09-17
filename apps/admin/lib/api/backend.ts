import "server-only";

function apiOrigin(): string {
  const value = process.env.ADMIN_API_ORIGIN;
  if (!value) throw new Error("ADMIN_API_ORIGIN is required");
  return value.replace(/\/$/, "");
}

export async function backendRequest(path: string, init: RequestInit = {}): Promise<Response> {
  if (!path.startsWith("/api/v1/")) throw new Error("Backend path must stay under /api/v1/");
  return fetch(`${apiOrigin()}${path}`, {
    ...init,
    cache: "no-store",
    headers: { "Content-Type": "application/json", ...(init.headers ?? {}) },
  });
}
