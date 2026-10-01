/*
 * The text of a page published from the console, as the API sends it: plain
 * text where a blank line separates paragraphs, a line starting with "- " is a
 * list item and one starting with "## " a heading. No HTML is ever sent, and
 * none is produced here: the blocks render as ordinary React elements.
 */
export type ContentBlock =
  | { type: "heading"; text: string }
  | { type: "paragraph"; lines: string[] }
  | { type: "list"; items: string[] };

export function parseContent(body: string): ContentBlock[] {
  const blocks: ContentBlock[] = [];
  let paragraph: string[] = [];
  let list: string[] = [];
  const endParagraph = () => {
    if (paragraph.length > 0) blocks.push({ type: "paragraph", lines: paragraph });
    paragraph = [];
  };
  const endList = () => {
    if (list.length > 0) blocks.push({ type: "list", items: list });
    list = [];
  };

  for (const raw of body.replace(/\r\n?/g, "\n").split("\n")) {
    const line = raw.trim();
    if (!line) {
      endParagraph();
      endList();
    } else if (line.startsWith("## ")) {
      endParagraph();
      endList();
      const text = line.slice(3).trim();
      if (text) blocks.push({ type: "heading", text });
    } else if (line.startsWith("- ")) {
      endParagraph();
      const item = line.slice(2).trim();
      if (item) list.push(item);
    } else {
      endList();
      paragraph.push(line);
    }
  }
  endParagraph();
  endList();
  return blocks;
}

/* The opening words of a text, on one line, for a meta description. */
export function contentSummary(body: string, max = 160): string {
  const text = parseContent(body)
    .filter((b) => b.type !== "heading")
    .map((b) => (b.type === "paragraph" ? b.lines.join(" ") : b.items.join("، ")))
    .join(" ")
    .replace(/\s+/g, " ")
    .trim();
  return text.length <= max ? text : `${text.slice(0, max - 1).trimEnd()}…`;
}
