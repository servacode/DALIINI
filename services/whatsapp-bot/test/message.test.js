// The message tells a person how long their code is good for, and the backend decides when it
// stops working. They drifted: the message said ten minutes while the backend expired the code
// at five, so for five minutes a correct code was refused while the message still promised it.
// This reads the backend's own value rather than repeating it, so the two cannot disagree again.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

import { CODE_MINUTES, codeMessage } from "../src/message.js";

const here = dirname(fileURLToPath(import.meta.url));
const services = join(here, "..", "..", "..", "apps", "backend", "accounts", "services.py");

test("the minutes in the message are the backend's own expiry", () => {
  const source = readFileSync(services, "utf8");
  const match = source.match(/^OTP_TTL = timedelta\(minutes=(\d+)\)$/m);
  assert.ok(match, "OTP_TTL is no longer written as timedelta(minutes=N); update this test");
  assert.equal(CODE_MINUTES, Number(match[1]));
});

test("the message carries the code on a line of its own and no link", () => {
  const text = codeMessage("123456");
  assert.ok(text.split("\n").includes("123456"));
  assert.doesNotMatch(text, /https?:\/\//);
  assert.match(text, new RegExp(`صالح ${CODE_MINUTES} دقائق`));
});
