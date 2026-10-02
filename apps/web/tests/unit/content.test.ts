import { describe, expect, it } from "vitest";

import { contentSummary, parseContent } from "../../lib/content";

/**
 * The text an operator writes in the console, turned into a page.
 *
 * What makes this worth testing is what it must never do. The body arrives as plain text from
 * a form, and it becomes React elements — never HTML. If a line of it could ever reach the page
 * as markup, the privacy policy would be an injection point, written by whoever can edit
 * content. So the shape is checked here, and «no HTML is produced» is checked by the fact that
 * every block carries text and nothing else.
 */

describe("turning written text into blocks", () => {
  it("reads a blank line as the end of a paragraph", () => {
    const blocks = parseContent("سطر أول\nسطر ثانٍ\n\nفقرة أخرى");

    expect(blocks).toEqual([
      { type: "paragraph", lines: ["سطر أول", "سطر ثانٍ"] },
      { type: "paragraph", lines: ["فقرة أخرى"] },
    ]);
  });

  it("reads «## » as a heading and «- » as a list", () => {
    const blocks = parseContent("## العنوان\n- أولًا\n- ثانيًا");

    expect(blocks).toEqual([
      { type: "heading", text: "العنوان" },
      { type: "list", items: ["أولًا", "ثانيًا"] },
    ]);
  });

  it("closes an open list when ordinary text follows it", () => {
    const blocks = parseContent("- بند\nكلام بعده");

    expect(blocks.map((block) => block.type)).toEqual(["list", "paragraph"]);
  });

  it("reads the same text however the line endings were written", () => {
    // An operator pasting from Word sends CRLF; the page must not gain blank paragraphs.
    expect(parseContent("أ\r\nب")).toEqual(parseContent("أ\nب"));
    expect(parseContent("أ\rب")).toEqual(parseContent("أ\nب"));
  });

  it("drops a marker with nothing after it rather than making an empty block", () => {
    expect(parseContent("## ")).toEqual([]);
    expect(parseContent("- ")).toEqual([]);
    expect(parseContent("   \n  \n")).toEqual([]);
    expect(parseContent("")).toEqual([]);
  });

  it("produces blocks of text only — never markup", () => {
    // Whatever was written stays a string. This is the line between a content page and an
    // injection point, and it is held here rather than in the component.
    const blocks = parseContent("<script>alert(1)</script>\n\n- <b>bold</b>");

    expect(blocks).toEqual([
      { type: "paragraph", lines: ["<script>alert(1)</script>"] },
      { type: "list", items: ["<b>bold</b>"] },
    ]);
  });
});

describe("the opening words, for a description", () => {
  it("joins the prose and leaves the headings out", () => {
    expect(contentSummary("## عنوان\nالجملة الأولى.\n\nوالثانية.")).toBe(
      "الجملة الأولى. والثانية.",
    );
  });

  it("reads a list as a sentence", () => {
    expect(contentSummary("- أ\n- ب")).toBe("أ، ب");
  });

  it("cuts a long text at a whole character and marks that it was cut", () => {
    const summary = contentSummary("ا".repeat(400));

    expect(summary.length).toBe(160);
    expect(summary.endsWith("…")).toBe(true);
  });

  it("leaves a short text exactly as it is, with no ellipsis", () => {
    expect(contentSummary("قصيرة.")).toBe("قصيرة.");
  });

  it("has nothing to say about an empty page", () => {
    expect(contentSummary("")).toBe("");
    expect(contentSummary("## عنوان وحده")).toBe("");
  });
});
