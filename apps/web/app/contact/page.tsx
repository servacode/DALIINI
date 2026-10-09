import type { Metadata } from "next";
import Link from "next/link";
import { ContactForm } from "../../components/contact-form";
import { ContentBody } from "../../components/content";
import { EmailLink } from "../../components/email-link";
import { SupportWhatsApp } from "../../components/support-whatsapp";
import { Illustration } from "../../components/ui";
import { getContentPage, getFacility, getSupportContact, isUuid } from "../../lib/api";
import { absoluteUrl, contactEndpoint, publicConfig } from "../../lib/config";
import { parseContactKind, type ContactKind } from "../../lib/contact";
import { pageMetadata } from "../../lib/seo";

/*
 * /contact «تواصل معنا». The form (components/contact-form.tsx) posts from the
 * browser straight to the API. Links can choose the subject and start the
 * message: /contact?kind=owner from the owners' guide, and
 * /contact?facility=<id> from a facility page, which opens a correction that
 * already names the facility and links its page. Those variants stay out of
 * the index. If the team publishes a «تواصل معنا» page (content/pages/contact/),
 * its text leads the page.
 */

type Params = { kind?: string | string[]; facility?: string | string[] };
type Props = { searchParams: Promise<Params> };

const one = (v: string | string[] | undefined) => (typeof v === "string" ? v : Array.isArray(v) ? v[0] : undefined);

export async function generateMetadata({ searchParams }: Props): Promise<Metadata> {
  const params = await searchParams;
  return pageMetadata({
    title: "تواصل معنا",
    description: "راسل فريق دليني: سؤال أو اقتراح، أو سؤال عن إضافة منشأتك، أو تصحيح معلومة عن منشأة.",
    path: "/contact",
    noindex: Boolean(params.kind || params.facility),
  });
}

/* The start of a correction: the facility's name and page, then room for the reader's words. */
async function correctionDraft(facilityId: string): Promise<string> {
  const facility = await getFacility(facilityId);
  if (facility === undefined) return "";
  const url = absoluteUrl(`/f/${facilityId}`);
  return facility ? `تصحيح معلومة عن «${facility.nameAr}»:\n${url}\n\n` : `تصحيح معلومة عن المنشأة:\n${url}\n\n`;
}

export default async function ContactPage({ searchParams }: Props) {
  const params = await searchParams;
  const rawFacility = one(params.facility);
  const facilityId = rawFacility && isUuid(rawFacility) ? rawFacility : undefined;
  const kind: ContactKind = parseContactKind(one(params.kind)) ?? (facilityId ? "CORRECTION" : "GENERAL");
  const [intro, draft, contact] = await Promise.all([
    getContentPage("contact"),
    facilityId ? correctionDraft(facilityId) : Promise.resolve(""),
    getSupportContact(),
  ]);
  const endpoint = contactEndpoint();
  const email = publicConfig.supportEmail.includes("@") ? publicConfig.supportEmail : null;

  return (
    <div className="shell page contact">
      <h1>تواصل معنا</h1>
      <p className="page-intro">اكتب لنا سؤالك أو اقتراحك، أو صحّح معلومة عن منشأة. يقرأ فريقنا كل رسالة.</p>
      {intro ? <div className="contact-intro"><ContentBody body={intro.bodyAr} /></div> : null}
      {/* A chat is quicker than a form for most people, so it is offered first. */}
      <SupportWhatsApp contact={contact} />

      {endpoint ? (
        <>
          <ContactForm
            key={`${kind}-${facilityId ?? ""}`}
            endpoint={endpoint}
            initialKind={kind}
            initialMessage={draft}
            successArt={<Illustration name="success" size={96} />}
          />
          <p className="muted more">نستعمل اسمك ورقمك للرد على رسالتك فقط. لن نطلب منك كلمة المرور أو رمز التحقق أبداً.</p>
          {email ? <p className="muted">تفضّل البريد؟ <EmailLink email={email} /></p> : null}
        </>
      ) : (
        <div className="card state">
          <Illustration name="maintenance" size={88} />
          <strong>نموذج التواصل غير متاح حالياً.</strong>
          <p>{email ? <>راسلنا على البريد: <EmailLink email={email} /></> : "حاول مرة أخرى لاحقاً."}</p>
        </div>
      )}
      <p className="muted">
        في حالة طارئة لا تنتظر الرد: <Link href="/emergency">أرقام الطوارئ</Link>.
      </p>
    </div>
  );
}
