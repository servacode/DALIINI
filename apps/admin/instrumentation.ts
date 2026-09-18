/**
 * Validate the deployment's configuration once, at boot.
 *
 * Without this, a missing `ADMIN_API_ORIGIN` or an `http` public origin on a real host is
 * only discovered when the first operator tries to sign in, and it surfaces as a 502 that
 * looks like the backend is down. Failing here instead means a misconfigured deployment
 * never starts serving.
 *
 * The work lives in a separate module that is imported dynamically, because this file is
 * bundled for the Edge runtime too and the checks are Node-only.
 */
export async function register(): Promise<void> {
  if (process.env.NEXT_RUNTIME !== "nodejs") return;
  const { validateConfiguration } = await import("./lib/boot/validate-configuration");
  validateConfiguration();
}
