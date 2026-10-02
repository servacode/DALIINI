import { describe, expect, it } from "vitest";

import { CONTACT_KINDS, MESSAGE_MAX, NAME_MAX, PHONE_MAX, parseContactKind } from "../../lib/contact";
import { UNAVAILABLE_METADATA, pageMetadata } from "../../lib/seo";

/**
 * Two small modules whose mistakes are invisible on the page.
 *
 * Metadata is never seen by a visitor — it is seen by WhatsApp, by a search engine, and by
 * whoever is sent the link. A page that forgets to carry its own image or lets a broken one
 * into the index looks perfectly fine to everyone who could notice. And the contact form's
 * kind arrives from a query string that anyone can write.
 */

describe("a page's metadata", () => {
  const meta = pageMetadata({
    title: "صيدليات الرقة",
    description: "الصيدليات المناوبة الليلة.",
    path: "/الرقة/صيدليات",
  });

  it("carries the page's own title, description and canonical path", () => {
    expect(meta.title).toBe("صيدليات الرقة");
    expect(meta.alternates?.canonical).toBe("/الرقة/صيدليات");
  });

  it("repeats them for the preview card, because Next replaces rather than merges", () => {
    // The whole reason this helper exists: a page that sets openGraph partially loses the
    // rest of it, and the link posted to WhatsApp arrives bare.
    expect(meta.openGraph?.title).toBe("صيدليات الرقة");
    expect(meta.twitter?.title).toBe("صيدليات الرقة");
    expect(meta.openGraph?.locale).toBe("ar_SY");
  });

  it("re-attaches the share image the override would otherwise drop", () => {
    const images = meta.openGraph?.images as readonly { url: string; width: number }[];
    expect(images?.[0]?.url).toBe("/opengraph-image.png");
    expect(images?.[0]?.width).toBe(1200);
    expect(meta.twitter?.images).toBeTruthy();
  });

  it("is indexable unless the page says otherwise", () => {
    expect(meta.robots).toBeUndefined();

    const hidden = pageMetadata({ title: "t", description: "d", path: "/p", noindex: true });
    expect(hidden.robots).toEqual({ index: false, follow: true });
  });

  it("keeps a page whose data failed out of the index entirely", () => {
    // Not merely noindex: such a page has no title of its own to offer, so it carries the
    // site's name and nothing that could be cached as a result.
    expect(UNAVAILABLE_METADATA.robots).toEqual({ index: false, follow: false });
    expect(UNAVAILABLE_METADATA.title).toEqual({ absolute: "دليني" });
  });
});

describe("the contact form's kind", () => {
  it("offers the three kinds, each named from the shared vocabulary", () => {
    expect(CONTACT_KINDS).toHaveLength(3);
    expect(CONTACT_KINDS.map((kind) => kind.value)).toEqual(["GENERAL", "OWNER", "CORRECTION"]);
    expect(CONTACT_KINDS.every((kind) => kind.label.trim().length > 0)).toBe(true);
  });

  it("reads the kind out of a link, however it was written", () => {
    expect(parseContactKind("owner")).toBe("OWNER");
    expect(parseContactKind("CORRECTION")).toBe("CORRECTION");
    expect(parseContactKind("  general  ")).toBe("GENERAL");
  });

  it("ignores anything else rather than guessing", () => {
    // The value comes from a query string, which anyone may write.
    expect(parseContactKind("complaint")).toBeUndefined();
    expect(parseContactKind("")).toBeUndefined();
    expect(parseContactKind(null)).toBeUndefined();
    expect(parseContactKind(undefined)).toBeUndefined();
  });

  it("states the API's own limits, so the form stops a message the backend would refuse", () => {
    expect(NAME_MAX).toBe(120);
    expect(PHONE_MAX).toBe(20);
    expect(MESSAGE_MAX).toBe(1000);
  });
});
