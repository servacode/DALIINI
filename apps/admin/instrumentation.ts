/**
 * Validate the deployment's configuration once, at boot.
 *
 * Without this, a missing `ADMIN_API_ORIGIN` or an `http` public origin on a real host is
 * only discovered when the first operator tries to sign in, and it surfaces as a 502 that
 * looks like the backend is down. Failing here instead means a misconfigured deployment
 * never starts serving, which is the outcome anyone would want.
 */
export async function register(): Promise<void> {
  if (process.env.NEXT_RUNTIME !== "nodejs") return;
  const { secureCookies } = await import("./lib/auth/cookies");
  const { assertApiOriginConfigured } = await import("./lib/api/client");

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
