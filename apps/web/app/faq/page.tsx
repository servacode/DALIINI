import Link from "next/link";
import { ContentBody } from "../../components/content";
import { Empty, JsonLd, Unavailable } from "../../components/ui";
import { getFaq } from "../../lib/api";
import { parseContent } from "../../lib/content";
import { pageMetadata } from "../../lib/seo";

/*
 * /faq — the questions and answers the team publishes from the console
 * (content/faq/), in their order. Each answer opens in place with
 * <details>/<summary>, so the page ships no script. Until one is published the
 * page says «قريباً».
 */
export const revalidate = 300;

export const metadata = pageMetadata({
  title: "الأسئلة الشائعة",
  description: "إجابات قصيرة عن استخدام دليني: الموقع والمسافة، حالة الدوام، المناوبات، إضافة منشأة، وحذف الحساب.",
  path: "/faq",
});

/* An answer as one line of plain text, for the structured data. */
const plain = (answer: string) =>
  parseContent(answer)
    .map((b) => (b.type === "heading" ? b.text : b.type === "paragraph" ? b.lines.join(" ") : b.items.join("، ")))
    .join(" ");

export default async function FaqPage() {
  const items = await getFaq();

  return (
    <div className="shell page">
      <h1>الأسئلة الشائعة</h1>
      {items === null ? (
        <div className="more"><Unavailable /></div>
      ) : items.length === 0 ? (
        <div className="more">
          <Empty>قريباً. حتى ذلك الحين، <Link href="/contact">اسألنا من نموذج التواصل</Link>.</Empty>
        </div>
      ) : (
        <>
          <JsonLd
            data={{
              "@context": "https://schema.org",
              "@type": "FAQPage",
              mainEntity: items.map((item) => ({
                "@type": "Question",
                name: item.questionAr,
                acceptedAnswer: { "@type": "Answer", text: plain(item.answerAr) },
              })),
            }}
          />
          <div className="faq more">
            {items.map((item) => (
              <details key={item.id} className="card">
                <summary>{item.questionAr}</summary>
                <ContentBody body={item.answerAr} />
              </details>
            ))}
          </div>
        </>
      )}
      <p className="more">
        لم تجد إجابتك؟ <Link href="/contact">راسلنا</Link>. صاحب منشأة؟ <Link href="/owners">دليل أصحاب المنشآت</Link>.
      </p>
    </div>
  );
}
