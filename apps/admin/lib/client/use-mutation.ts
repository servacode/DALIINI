"use client";

import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";

import type { ApiErrorBody } from "../errors/messages";
import { isSessionExpired } from "../errors/messages";
import { write } from "./api";

/**
 * Run one write operation and report what happened.
 *
 * Nothing is optimistic. The screen shows the new state only after the backend has
 * confirmed it, because a mutation can be refused by a permission check, a domain rule or a
 * conflict — and a row that flips and then flips back is worse than a row that waits.
 *
 * A 401 leaves for the login screen, as elsewhere: the refresh already failed server-side.
 */

export type Mutation = Readonly<{
  run: (operation: string, body?: Record<string, unknown>) => Promise<boolean>;
  pending: boolean;
  error: ApiErrorBody | null;
  status: number;
  reset: () => void;
}>;

export function useMutation(): Mutation {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<ApiErrorBody | null>(null);
  const [status, setStatus] = useState(0);

  const run = useCallback(
    async (operation: string, body: Record<string, unknown> = {}): Promise<boolean> => {
      setPending(true);
      setError(null);
      const result = await write(operation, body);
      setPending(false);
      setStatus(result.ok ? 200 : result.status);
      if (result.ok) return true;
      setError(result.error);
      if (isSessionExpired(result.error)) router.replace("/login");
      return false;
    },
    [router],
  );

  const reset = useCallback(() => {
    setError(null);
    setStatus(0);
  }, []);

  return { run, pending, error, status, reset };
}
