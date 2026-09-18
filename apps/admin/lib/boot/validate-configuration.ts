import "server-only";

import { assertApiOriginConfigured } from "../api/client";
import { secureCookies } from "../auth/cookies";

/**
 * Node-only boot validation, kept out of `instrumentation.ts` itself.
 *
 * `instrumentation.ts` is bundled for the Edge runtime as well as Node, and `process.exit`
 * does not exist there. Importing this module dynamically, behind the runtime check, keeps
 * the Node-only call out of the Edge bundle.
 */
export function validateConfiguration(): void {
  try {
    assertApiOriginConfigured();
    if (!secureCookies()) {
      console.warn(
        "[admin] ADMIN_PUBLIC_ORIGIN is a loopback http origin: session cookies will be " +
          "written without Secure and without the __Host- prefix. Local development only.",
      );
    }
  } catch (error) {
    // Throwing is not enough: Next logs it and carries on serving, which would leave a
    // misconfigured deployment answering requests it cannot answer safely. Stop the
    // process instead, so the failure is visible to whoever deployed it.
    console.error(`[admin] refusing to start: ${(error as Error).message}`);
    process.exit(1);
  }
}
