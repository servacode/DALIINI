import Link from "next/link";
import { Empty, JsonLd } from "../../components/ui";
import { getPublishedPage } from "../../lib/api";
import { pageMetadata } from "../../lib/seo";

/*
 * /faq — the FAQ page the team publishes from the console (public/legal/FAQ/),
 * the same text the app shows. The body is plain text: blank lines separate
 * blocks, and a block whose first line ends in «؟» is a question followed by
 * its answer. Until it is published (or while the API is unreachable) the page
 * says «قريباً».
 */
export const revalidate = 300;

export const metadata = pageMetadata({
  title: "الأسئلة الشائعة",
  description: "إجابات قصيرة عن استخدام دليني: الموقع والمسافة، حالة الدوام، المناوبات، إضافة منشأة، وحذف الحساب.",
  path: "/faq",
});

/* The published text opens with its own title line; the page heading already says it. */
const REPEATS_TITLE = /^(ال)?أسئلة (ال)?شائعة[.:]?$/;

type Question = { kind: "qa"; q: string; a: string[] };
type Text = { kind: "text"; lines: string[] };
type Block = Question | Text;

function parse(body: string): Block[] {
  return body
    .replace(/\r\n/g, "\n")
    .split(/\n\s*\n/)
    .map((chunk) => chunk.split("\n").map((l) => l.trim()).filter(Boolean))
    .filter((lines) => lines.length > 0)
    .map((lines): Block =>
      lines[0].endsWith("؟") || lines[0].endsWith("?") ? { kind: "qa", q: lines[0], a: lines.slice(1) } : { kind: "text", lines },
    );
}

export default async function FaqPage() {
  const page = await getPublishedPage("FAQ");
  const blocks = page ? parse(page.bodyAr) : [];
  const questions = blocks.filter((b): b is Question => b.kind === "qa");
  const texts = blocks.filter((b): b is Text => b.kind === "text" && !REPEATS_TITLE.test(b.lines.join(" ")));

  return (
    <div className="shell page">
      <h1>الأسئلة الشائعة</h1>
      {questions.length + texts.length === 0 ? (
        <Empty>قريباً. حتى ذلك الحين، <Link href="/support">راسل الدعم</Link> بسؤالك.</Empty>
      ) : (
        <>
          {questions.length > 0 ? (
            <JsonLd
              data={{
                "@context": "https://schema.org",
                "@type": "FAQPage",
                mainEntity: questions.map((b) => ({
                  "@type": "Question",
                  name: b.q,
                  acceptedAnswer: { "@type": "Answer", text: b.a.join(" ") },
                })),
              }}
            />
          ) : null}
          {texts.map((b, i) => b.lines.map((l) => <p key={`${i}-${l}`}>{l}</p>))}
          <div className="faq">
            {questions.map((b) => (
              <details key={b.q} className="card">
                <summary>{b.q}</summary>
                {b.a.map((line) => <p key={line}>{line}</p>)}
              </details>
            ))}
          </div>
          <p className="more">
            لم تجد إجابتك؟ <Link href="/support">راسل الدعم</Link>. صاحب منشأة؟ <Link href="/owners">دليل أصحاب المنشآت</Link>.
          </p>
        </>
      )}
    </div>
  );
}
