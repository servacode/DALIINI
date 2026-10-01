import { describe, expect, it } from "vitest";

import {
  directionsLink,
  localPhone,
  telLink,
  waLink,
  whatsAppFor,
} from "../../lib/links";

/**
 * The three things a visitor does with a facility: call it, message it, go to it.
 *
 * Every one of them is a link built from stored data, and a link built wrong is a visitor who
 * dials nothing, messages nobody, or is sent to the wrong place — none of which the page can
 * tell them about. That is why these are unit tests and not something noticed by looking.
 */

describe("the number as somebody here would write it", () => {
  it("drops the country code, restores the national zero and groups the digits", () => {
    expect(localPhone("+963933123456")).toBe("0933 123 456");
  });

  it("reads the same number however it was stored", () => {
    for (const stored of ["+963933123456", "963933123456", "00963933123456", "0933123456"]) {
      expect(localPhone(stored)).toBe("0933 123 456");
    }
  });

  it("leaves alone what is not a Syrian mobile, rather than forcing a shape onto it", () => {
    // A landline, a short code, a foreign number: shown as stored, because inventing a
    // grouping for them would be inventing a claim about them.
    expect(localPhone("+96321234567")).toBe("+96321234567");
    expect(localPhone("110")).toBe("110");
  });

  it("has nothing to show when there is nothing stored", () => {
    expect(localPhone(null)).toBeNull();
    expect(localPhone(undefined)).toBeNull();
    expect(localPhone("   ")).toBeNull();
  });
});

describe("dialling", () => {
  it("strips the spaces a person reads but a dialler cannot", () => {
    expect(telLink("0933 123 456")).toBe("tel:0933123456");
  });

  it("offers nothing when there is no number", () => {
    expect(telLink(null)).toBeNull();
    expect(telLink("")).toBeNull();
  });
});

describe("WhatsApp", () => {
  it("keeps international digits only", () => {
    expect(waLink("+963 933 123 456")).toBe("https://wa.me/963933123456");
  });

  it("refuses something too short to be a number at all", () => {
    expect(waLink("123")).toBeNull();
    expect(waLink(null)).toBeNull();
  });

  it("prefers the number the owner gave for WhatsApp", () => {
    expect(whatsAppFor("+963944000000", "+963933123456")).toBe("https://wa.me/963944000000");
  });

  it("falls back to a Syrian mobile, because here the line and the account are one", () => {
    expect(whatsAppFor(null, "+963933123456")).toBe("https://wa.me/963933123456");
  });

  it("never offers a landline, because messaging one goes nowhere", () => {
    expect(whatsAppFor(null, "+96321234567")).toBeNull();
    expect(whatsAppFor(undefined, undefined)).toBeNull();
  });
});

describe("directions", () => {
  it("sends the visitor to the facility, and lets the map decide where they start", () => {
    expect(directionsLink({ latitude: 35.9513, longitude: 39.0116 })).toBe(
      "https://www.google.com/maps/dir/?api=1&destination=35.9513,39.0116",
    );
  });

  it("offers nothing for a facility nobody has placed on the map", () => {
    expect(directionsLink(null)).toBeNull();
    expect(directionsLink(undefined)).toBeNull();
  });
});
