"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";

import type { ApiErrorBody } from "../errors/messages";
import { isSessionExpired } from "../errors/messages";
import { read } from "./api";

/**
 * Load one read operation and keep it in step with its filters.
 *
 * Three things it takes care of so no screen has to:
 *
 * * **A stale response never wins.** Every request is stamped with the key that produced
 *   it, and only a result whose key still matches is shown. Typing in a filter box fires
 *   several requests; a slow early one cannot overwrite a fast later one.
 * * **An expired session leaves the screen.** A 401 means the refresh already failed
 *   server-side and the cookies are gone, so there is nothing to retry — the operator goes
 *   back to the login screen rather than watching an error that will never clear.
 * * **Refetching is explicit.** A mutation calls `reload()`. The only polling is the
 *   opt-in `refreshMs`, for queues that change under the operator.
 *
 * `loading` is derived rather than stored. Setting it inside the effect body would be a
 * synchronous state update during synchronisation, which cascades renders; comparing the
 * stamped key to the current one answers the same question for free.
 */

export type Resource<T> = Readonly<{
  data: T | null;
  error: ApiErrorBody | null;
  status: number;
  loading: boolean;
  reload: () => void;
}>;

type Snapshot<T> = Readonly<{
  key: string;
  data: T | null;
  error: ApiErrorBody | null;
  status: number;
}>;

export function useResource<T>(
  operation: string,
  params: Record<string, string | undefined> = {},
  options: { enabled?: boolean; refreshMs?: number } = {},
): Resource<T> {
  const enabled = options.enabled ?? true;
  const refreshMs = options.refreshMs ?? 0;
  const router = useRouter();
  const [nonce, setNonce] = useState(0);
  const [snapshot, setSnapshot] = useState<Snapshot<T> | null>(null);

  // Serialised so the effect keys on values rather than object identity, which would
  // re-fetch on every render.
  const serialised = JSON.stringify(params);
  const key = `${operation}|${serialised}|${nonce}`;

  useEffect(() => {
    if (!enabled) return;
    let cancelled = false;
    void read<T>(operation, JSON.parse(serialised) as Record<string, string | undefined>).then(
      (result) => {
        if (cancelled) return;
        if (result.ok) {
          setSnapshot({ key, data: result.data, error: null, status: 200 });
          return;
        }
        setSnapshot({ key, data: null, error: result.error, status: result.status });
        if (isSessionExpired(result.error)) router.replace("/login");
      },
    );
    return () => {
      cancelled = true;
    };
  }, [key, operation, serialised, enabled, router]);

  // Background refresh for queues that change under the operator (the review queue, the
  // dashboard). It replaces the data in place under the same key, so the screen never
  // flashes back to a spinner, and it only runs while the tab is visible — a console left
  // open overnight in a background tab does not keep polling. A failed refresh keeps the
  // last good data rather than swapping a working table for an error.
  useEffect(() => {
    if (!enabled || refreshMs <= 0) return;
    let cancelled = false;
    const timer = window.setInterval(() => {
      if (document.visibilityState !== "visible") return;
      void read<T>(operation, JSON.parse(serialised) as Record<string, string | undefined>).then(
        (result) => {
          if (cancelled || !result.ok) return;
          setSnapshot((current) =>
            current?.key === key ? { key, data: result.data, error: null, status: 200 } : current,
          );
        },
      );
    }, refreshMs);
    return () => {
      cancelled = true;
      window.clearInterval(timer);
    };
  }, [key, operation, serialised, enabled, refreshMs]);

  const reload = useCallback(() => setNonce((value) => value + 1), []);
  const fresh = snapshot?.key === key ? snapshot : null;

  return {
    data: fresh?.data ?? null,
    error: fresh?.error ?? null,
    status: fresh?.status ?? 0,
    loading: enabled && fresh === null,
    reload,
  };
}
