import { afterEach, describe, expect, it, vi } from "vitest";

/**
 * What the site does when it has not been told where it lives.
 *
 * Every value here is read from the environment at import, so each case reloads the module with
 * a different one. The behaviour that matters is the unconfigured one: a build with no domain,
 * no API origin and no package must still build and still render — the alternative is a site
 * that cannot be deployed until every account exists, which is the opposite of useful.
 */

const ENVIRONMENT = { ...process.env };

/** Every key this module reads. Cleared before each load so one case cannot leak into another. */
const KEYS = [
  "NEXT_PUBLIC_ROOT_DOMAIN",
  "NEXT_PUBLIC_API_ORIGIN",
  "NEXT_PUBLIC_ANDROID_PACKAGE",
  "NEXT_PUBLIC_PLAY_STORE_URL",
  "NEXT_PUBLIC_APP_STORE_URL",
  "NEXT_PUBLIC_APP_DOWNLOAD_URL",
] as const;

async function configWith(values: Partial<Record<(typeof KEYS)[number], string>>) {
  // A full reset, not a merge: several of these cases load the module twice in one test, and
  // the second load must see only what it was given.
  for (const key of KEYS) delete process.env[key];
  Object.assign(process.env, values);
  vi.resetModules();
  return import("../../lib/config");
}

afterEach(() => {
  process.env = { ...ENVIRONMENT };
});

describe("the site's own address", () => {
  it("adds https to a bare domain", async () => {
    const { siteUrl } = await configWith({ NEXT_PUBLIC_ROOT_DOMAIN: "daliini.sy" });
    expect(siteUrl().origin).toBe("https://daliini.sy");
  });

  it("keeps a scheme that was given", async () => {
    const { siteUrl } = await configWith({ NEXT_PUBLIC_ROOT_DOMAIN: "https://www.daliini.sy" });
    expect(siteUrl().origin).toBe("https://www.daliini.sy");
  });

  it("falls back to a name that resolves nowhere rather than failing the build", async () => {
    // CI builds this site with no domain. A crash here would mean the site cannot be built
    // until a domain is bought, and a placeholder that cannot be reached claims nothing.
    const { siteUrl, absoluteUrl } = await configWith({});
    expect(siteUrl().hostname.endsWith(".invalid")).toBe(true);
    expect(absoluteUrl("/duty")).toBe("https://example.invalid/duty");
  });
});

describe("the contact form's endpoint", () => {
  it("is the API's own origin, with the path appended", async () => {
    const { contactEndpoint } = await configWith({
      NEXT_PUBLIC_API_ORIGIN: "https://api.daliini.sy",
    });
    expect(contactEndpoint()).toBe("https://api.daliini.sy/api/v1/contact/");
  });

  it("is nothing when the origin is a placeholder, so the form is hidden rather than broken", async () => {
    const { contactEndpoint } = await configWith({
      NEXT_PUBLIC_API_ORIGIN: "https://api.<ROOT_DOMAIN>",
    });
    expect(contactEndpoint()).toBeNull();
  });

  it("is nothing when the origin is not a URL at all", async () => {
    const { contactEndpoint } = await configWith({ NEXT_PUBLIC_API_ORIGIN: "api.daliini.sy" });
    expect(contactEndpoint()).toBeNull();
  });

  it("refuses a scheme a browser would not post to", async () => {
    const { contactEndpoint } = await configWith({
      NEXT_PUBLIC_API_ORIGIN: "javascript:alert(1)",
    });
    expect(contactEndpoint()).toBeNull();
  });
});

describe("opening a facility in the app", () => {
  const configured = {
    NEXT_PUBLIC_ROOT_DOMAIN: "daliini.sy",
    NEXT_PUBLIC_ANDROID_PACKAGE: "com.servacode.directory",
  };

  it("addresses the app and falls back to this very page", async () => {
    const { appOpenUrl } = await configWith(configured);
    const url = appOpenUrl("abc-123")!;
    expect(url.startsWith("intent://daliini.sy/f/abc-123#Intent;")).toBe(true);
    expect(url).toContain("package=com.servacode.directory;");
    expect(url).toContain(encodeURIComponent("https://daliini.sy/f/abc-123"));
  });

  it("escapes an id rather than letting it shape the link", async () => {
    const { appOpenUrl } = await configWith(configured);
    expect(appOpenUrl("a/b#c")).toContain("/f/a%2Fb%23c#Intent;");
  });

  it("offers nothing without a package, and nothing without a domain", async () => {
    const { appOpenUrl: noPackage } = await configWith({
      NEXT_PUBLIC_ROOT_DOMAIN: "daliini.sy",
    });
    expect(noPackage("abc")).toBeNull();

    const { appOpenUrl: noDomain } = await configWith({
      NEXT_PUBLIC_ANDROID_PACKAGE: "com.servacode.directory",
    });
    expect(noDomain("abc")).toBeNull();
  });

  it("refuses a package name that is not one", async () => {
    const { androidPackage } = await configWith({
      NEXT_PUBLIC_ANDROID_PACKAGE: "not a package",
    });
    expect(androidPackage()).toBeNull();
  });
});

describe("the download button", () => {
  it("prefers a direct build while there is one", async () => {
    const { publicConfig } = await configWith({
      NEXT_PUBLIC_APP_DOWNLOAD_URL: "https://daliini.sy/app.apk",
      NEXT_PUBLIC_PLAY_STORE_URL: "https://play.google.com/store/apps/details?id=x",
    });
    expect(publicConfig.appDownloadUrl).toBe("https://daliini.sy/app.apk");
  });

  it("falls back to the store, and to nothing when neither is set", async () => {
    const { publicConfig: store } = await configWith({
      NEXT_PUBLIC_PLAY_STORE_URL: "https://play.google.com/store/apps/details?id=x",
    });
    expect(store.appDownloadUrl).toContain("play.google.com");

    const { publicConfig: none } = await configWith({});
    expect(none.appDownloadUrl).toBeNull();
  });
});
