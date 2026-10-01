"use client";

import { useRouter } from "next/navigation";
import { useCallback, useState } from "react";

import { downloadExport } from "../lib/client/files";
import { isSessionExpired, messageFor } from "../lib/errors/messages";
import { Icons } from "./icons";
import { Toast } from "./ui";

/**
 * Download one CSV export, with the filters the screen is showing.
 *
 * The file opens in Excel with Arabic intact (the backend writes a byte-order mark). A
 * refusal — no permission, an expired session — is said in a sentence rather than saved to
 * disk, and an expired session goes back to the login screen as everywhere else.
 */
export function ExportButton({
  name,
  params = {},
  label = "تصدير إلى إكسل",
  testId,
}: {
  name: "facilities" | "reports" | "audit";
  params?: Record<string, string | undefined>;
  label?: string;
  testId?: string;
}) {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const dismiss = useCallback(() => setError(null), []);

  async function run(): Promise<void> {
    setPending(true);
    setError(null);
    const result = await downloadExport(name, params);
    setPending(false);
    if (result.ok) return;
    if (isSessionExpired(result.error)) {
      router.replace("/login");
      return;
    }
    setError(`تعذّر التصدير. ${messageFor(result.error)}`);
  }

  return (
    <>
      <button
        type="button"
        className="button-ghost"
        onClick={run}
        disabled={pending}
        aria-busy={pending || undefined}
        data-testid={testId ?? `export-${name}`}
      >
        <Icons.download />
        {pending ? "جارٍ التصدير…" : label}
      </button>
      <Toast message={error} tone="danger" onDismiss={dismiss} />
    </>
  );
}
