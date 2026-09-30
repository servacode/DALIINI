"use client";

import { term } from "@servacode/design-tokens/vocabulary";
import { useCallback, useState } from "react";
import type { CompactFacility, FacilityDetail, HoursEntry } from "../lib/api";
import { directionsLink, localPhone, telLink, whatsAppFor } from "../lib/links";
import { CardDialog } from "./card-dialog";
import { Icon, Rating, StatusBadge } from "./ui";

/**
 * The facilities of a category, as cards side by side.
 *
 * A card leads with the photograph its owner uploaded, because a person choosing a pharmacy
 * recognises the shopfront before they read the name. Until one is uploaded the card shows the
 * category's own mark on a soft ground rather than a grey rectangle: an empty frame reads as
 * something that failed to load, and nothing failed.
 *
 * Everything a card offers happens on the card. There is no page behind it and nothing opens
 * over it: call, WhatsApp and the route are one tap, the opening hours unfold in place, and the
 * link is shared from where it is read. A visitor never loses the list they were reading, so
 * they never have to find their way back to it.
 *
 * The hours are fetched the first time they are asked for. Twelve cards would otherwise mean
 * twelve requests for a week of hours nobody opened.
 */

/* Backend weekdays follow Python's date.weekday(): 0 = Monday … 6 = Sunday. */
const WEEKDAYS_AR = ["الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"];
/* Display order starting Saturday, the usual first day of the week locally. */
const DISPLAY_ORDER = [5, 6, 0, 1, 2, 3, 4];
const hhmm = (time: string) => time.slice(0, 5);

function Hours({ hours }: { hours: HoursEntry[] }) {
  return (
    <table className="hours">
      <tbody>
        {DISPLAY_ORDER.map((weekday) => {
          const spans = hours
            .filter((entry) => entry.weekday === weekday)
            .sort((a, b) => a.sequence - b.sequence);
          return (
            <tr key={weekday}>
              <th scope="row">{WEEKDAYS_AR[weekday]}</th>
              <td className={spans.length === 0 ? "muted" : "ltr"}>
                {spans.length === 0
                  ? "مغلق"
                  : spans.map((s) => `${hhmm(s.opensAt)} – ${hhmm(s.closesAt)}`).join("، ")}
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

/*
 * The reasons a listing can be wrong — the same six the app offers, so a report from a card and
 * one from the app are the same thing in the console. Their Arabic comes from the shared
 * vocabulary, so the reason a reader picked is the reason an operator reads.
 */
const REPORT_REASONS = [
  "WRONG_INFO",
  "WRONG_HOURS",
  "CLOSED_PERMANENTLY",
  "WRONG_LOCATION",
  "NOT_ON_DUTY",
  "OTHER",
] as const;

type Hint = "copied" | "failed" | null;
type Sending = "idle" | "sending" | "sent" | "throttled" | "failed";

function Card({ f }: { f: CompactFacility }) {
  const [showHours, setShowHours] = useState(false);
  const [hours, setHours] = useState<HoursEntry[] | null>(null);
  const [hoursFailed, setHoursFailed] = useState(false);
  const [hint, setHint] = useState<Hint>(null);
  const [reporting, setReporting] = useState(false);
  const [sending, setSending] = useState<Sending>("idle");

  const tel = telLink(f.phone);
  const wa = whatsAppFor(f.whatsapp, f.phone);
  const directions = directionsLink(f.location);
  const shown = localPhone(f.phone);
  const where = [f.addressAr, f.neighborhood?.nameAr, f.city?.nameAr].filter(Boolean).join("، ");

  const openHours = useCallback(() => {
    setShowHours(true);
    if (hours || hoursFailed) return;
    fetch(`/api/facility/${f.id}`)
      .then((response) => (response.ok ? response.json() : Promise.reject(response.status)))
      .then((detail: FacilityDetail) => setHours(detail.hours))
      .catch(() => setHoursFailed(true));
  }, [f.id, hours, hoursFailed]);

  /*
   * Share sends the site's own address for this facility — the same link that opens the app on a
   * phone that has it. On a phone the system's own share sheet does it; everywhere else the link
   * goes to the clipboard, which is what a person would have done by hand anyway.
   */
  const share = useCallback(async () => {
    const url = `${window.location.origin}/f/${f.id}`;
    try {
      if (navigator.share) {
        await navigator.share({ title: f.nameAr, url });
        return;
      }
      await navigator.clipboard.writeText(url);
      setHint("copied");
    } catch {
      /* A cancelled share sheet is not a failure; a refused clipboard is. */
      if (!navigator.share) setHint("failed");
    }
    window.setTimeout(() => setHint(null), 2500);
  }, [f.id, f.nameAr]);

  const report = useCallback(
    async (reason: string) => {
      setSending("sending");
      try {
        const response = await fetch(`/api/facility/${f.id}/report`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ reason }),
        });
        if (response.ok) setSending("sent");
        else if (response.status === 429) setSending("throttled");
        else setSending("failed");
      } catch {
        setSending("failed");
      }
    },
    [f.id],
  );

  return (
    <article className="facility-card">
      {/*
        * A strip before the picture: whether it is open, and the two things you look at rather
        * than act on. Keeping them here leaves the picture unobstructed and gives every card the
        * same first line, whatever its photograph happens to be.
        */}
      <div className="facility-bar">
        <StatusBadge state={f.availability.state} />
        <div className="facility-bar-actions">
          <button type="button" aria-haspopup="dialog" onClick={openHours}>
            <Icon name="clock" size={15} />
            الدوام
          </button>
          <button type="button" onClick={share}>
            <Icon name="share" size={15} />
            مشاركة
          </button>
        </div>
      </div>

      <div className="facility-photo">
        {f.imageUrl ? (
          /* eslint-disable-next-line @next/next/no-img-element -- remote media, no loader */
          <img src={f.imageUrl} alt="" loading="lazy" />
        ) : (
          <Icon name="building" size={28} />
        )}
      </div>

      <div className="facility-body">
        {/* What it is. */}
        <h3>{f.nameAr}</h3>
        <div className="facility-where">
          <span className="meta">
            <span>{f.category.nameAr}</span>
            {f.city ? <span>· {f.city.nameAr}</span> : null}
          </span>
          <Rating average={f.ratingAverage} count={f.ratingCount} />
        </div>

        {/*
          * The number itself, not the word "call": a browser on a desk cannot dial, so a button
          * saying "call" leads nowhere there. It is set as a fact among the facts, and on a
          * phone it is still a `tel:` link and still dials.
          */}
        {/* Where it is, in words rather than on a map: a reader knows their own streets. */}
        {where ? <p className="facility-address">{where}</p> : null}

        {/*
          * The number, and the two ways to use it. A browser on a desk cannot dial, so the digits
          * are shown rather than hidden behind the word "call" — and they are shown the way they
          * are written here, without the country code.
          */}
        {shown ? (
          <div className="facility-phone">
            {tel ? (
              <a
                className="way way-call"
                href={tel}
                aria-label={`اتصل بـ${f.nameAr}`}
                title="اتصال"
              >
                <Icon name="phone" size={17} />
              </a>
            ) : null}
            <span className="ltr">{shown}</span>
            {wa ? (
              <a
                className="way way-whatsapp"
                href={wa}
                target="_blank"
                rel="noopener noreferrer"
                aria-label={`راسل ${f.nameAr} على واتساب`}
                title="واتساب"
              >
                <Icon name="whatsapp" size={17} />
              </a>
            ) : null}
          </div>
        ) : null}

        {/* The floor of the card: the way there, and a way to say it is wrong. */}
        <div className="facility-actions">
          {directions ? (
            <a
              className="button button-sm"
              href={directions}
              target="_blank"
              rel="noopener noreferrer"
              aria-label={`الطريق إلى ${f.nameAr}`}
            >
              <Icon name="directions" size={16} />
              الطريق
            </a>
          ) : null}
          <button
            type="button"
            className="button button-danger button-sm"
            aria-haspopup="dialog"
            onClick={() => setReporting(true)}
          >
            <Icon name="flag" size={16} />
            إبلاغ
          </button>
        </div>

        {hint ? (
          <p className="facility-hint" role="status">
            {hint === "copied" ? "نُسخ الرابط." : "تعذّر نسخ الرابط."}
          </p>
        ) : null}
      </div>

      {showHours ? (
        <CardDialog title={`أوقات دوام ${f.nameAr}`} onClose={() => setShowHours(false)}>
          {hours ? (
            hours.length > 0 ? (
              <Hours hours={hours} />
            ) : (
              <p className="muted">لم تُسجَّل أوقات دوام لهذه المنشأة.</p>
            )
          ) : hoursFailed ? (
            <p className="muted">تعذّر تحميل أوقات الدوام.</p>
          ) : (
            <p className="muted">جارٍ التحميل…</p>
          )}
        </CardDialog>
      ) : null}

      {reporting ? (
        <CardDialog title={`الإبلاغ عن ${f.nameAr}`} onClose={() => setReporting(false)}>
          {sending === "sent" ? (
            <p role="status">شكراً، سيراجع فريقنا البلاغ.</p>
          ) : sending === "throttled" ? (
            <p role="status">أرسلت بلاغات كثيرة خلال وقت قصير. حاول لاحقاً.</p>
          ) : (
            <>
              <p className="muted">ما الخطأ في هذه المنشأة؟</p>
              <div className="report-reasons">
                {REPORT_REASONS.map((reason) => (
                  <button
                    key={reason}
                    type="button"
                    disabled={sending === "sending"}
                    onClick={() => report(reason)}
                  >
                    {term("reportReason", reason).ar}
                  </button>
                ))}
              </div>
              {sending === "failed" ? (
                <p role="status">تعذّر إرسال البلاغ. حاول مرة أخرى.</p>
              ) : null}
            </>
          )}
        </CardDialog>
      ) : null}
    </article>
  );
}

export function FacilityCards({ items }: { items: CompactFacility[] }) {
  return (
    <ul className="facility-grid">
      {items.map((f) => (
        <li key={f.id}>
          <Card f={f} />
        </li>
      ))}
    </ul>
  );
}
