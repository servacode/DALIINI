"use client";

import { term } from "@servacode/design-tokens/vocabulary";
import Link from "next/link";
import { useCallback, useState } from "react";
import type { CompactFacility, FacilityDetail, HoursEntry } from "../lib/api";
import { directionsLink, localPhone, telLink, whatsAppFor } from "../lib/links";
import { WEEKDAYS_AR, WEEKDAY_DISPLAY_ORDER } from "../lib/dates";
import { facilityPath } from "../lib/paths";
import { CardDialog } from "./card-dialog";
import { Icon, Rating, StatusBadge } from "./ui";

/**
 * The facilities of a category, as compact cards: one column on a phone, a grid on a desk.
 *
 * A card is a row a thumb can scan, not a poster. The shopfront its owner uploaded is a small
 * square beside the name, or the category's mark on a soft ground until there is one; the name
 * and whether it is open now come first, where it is second, and the ways to reach it last, in
 * one row. A photograph the width of the screen made twelve pharmacies a scroll of several
 * metres on a phone, most of it the same empty frame.
 *
 * What a visitor usually wants happens on the card: call, WhatsApp and the route are one tap,
 * the opening hours and the report open over the list rather than away from it, and the link is
 * shared from where it is read. The name leads to the facility's own page, for everything else
 * (its photographs, its map, the whole week of hours).
 *
 * The hours are fetched the first time they are asked for. Twelve cards would otherwise mean
 * twelve requests for a week of hours nobody opened.
 */

const hhmm = (time: string) => time.slice(0, 5);

function Hours({ hours }: { hours: HoursEntry[] }) {
  return (
    <table className="hours">
      <tbody>
        {WEEKDAY_DISPLAY_ORDER.map((weekday) => {
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
/* What is open over the card: its short menu, or one of the two things the menu leads to. */
type Panel = "menu" | "hours" | "report" | null;

function Card({ f }: { f: CompactFacility }) {
  const [panel, setPanel] = useState<Panel>(null);
  const [hours, setHours] = useState<HoursEntry[] | null>(null);
  const [hoursFailed, setHoursFailed] = useState(false);
  const [hint, setHint] = useState<Hint>(null);
  const [sending, setSending] = useState<Sending>("idle");

  const tel = telLink(f.phone);
  const wa = whatsAppFor(f.whatsapp, f.phone);
  const directions = directionsLink(f.location);
  const shown = localPhone(f.phone);
  const area = f.neighborhood?.nameAr ?? f.city?.nameAr ?? null;
  const page = facilityPath(f);

  const openHours = useCallback(() => {
    setPanel("hours");
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
    const url = `${window.location.origin}${facilityPath(f)}`;
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
  }, [f]);

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
    <article className={f.imageUrl ? "facility-card has-photo" : "facility-card"}>
      {/* The shopfront, when an owner has uploaded one: across the top of the card, at the size
          a photograph deserves. A reader recognises a place they have walked past long before
          they read its name. With no photograph there is no empty band — the category's mark
          sits beside the name instead, and the card stays compact. */}
      <Link
        href={page}
        className={f.imageUrl ? "facility-photo" : "facility-thumb"}
        tabIndex={-1}
        aria-hidden="true"
      >
        {f.imageUrl ? (
          /* eslint-disable-next-line @next/next/no-img-element -- remote media, no loader */
          <img src={f.imageUrl} alt="" loading="lazy" />
        ) : (
          <Icon name="building" size={26} />
        )}
      </Link>

      <div className="facility-main">
        <h3>
          <Link href={page} className="title-link">{f.nameAr}</Link>
        </h3>
        {/* Whether it is open leads the line under the name, where it never squeezes the name. */}
        <p className="facility-sub">
          <StatusBadge state={f.availability.state} />
          <span>{f.category.nameAr}</span>
          {area ? <span>{area}</span> : null}
          <Rating average={f.ratingAverage} count={f.ratingCount} />
        </p>
        {/* Where it is, in words rather than on a map: a reader knows their own streets. */}
        {f.addressAr ? <p className="facility-address">{f.addressAr}</p> : null}
      </div>

      {/*
        * What people come for, on the card itself. The number is shown, not hidden behind the
        * word "call": a browser on a desk cannot dial, and the digits are what it can use. The
        * quieter three (hours, share, report) are icons at the end of the same row.
        */}
      <div className="facility-ways">
        {tel ? (
          <a className="way-button way-call" href={tel} aria-label={`اتصل بـ${f.nameAr}: ${shown}`}>
            <Icon name="phone" size={17} />
            <span className="ltr">{shown}</span>
          </a>
        ) : null}
        {wa ? (
          <a
            className="way-button way-whatsapp"
            href={wa}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`راسل ${f.nameAr} على واتساب`}
            title="واتساب"
          >
            <Icon name="whatsapp" size={17} />
          </a>
        ) : null}
        {directions ? (
          <a
            className="way-button"
            href={directions}
            target="_blank"
            rel="noopener noreferrer"
            aria-label={`الطريق إلى ${f.nameAr}`}
            title="الطريق"
          >
            <Icon name="directions" size={17} />
          </a>
        ) : null}
        {/* Labelled, not a bare icon. Three things live behind it — the hours, the link, a
            report of something wrong — and none of them is guessable from a drawing. */}
        <button
          type="button"
          className="way-button facility-more"
          aria-haspopup="dialog"
          onClick={() => setPanel("menu")}
          aria-label={`المزيد عن ${f.nameAr}: الدوام، المشاركة، الإبلاغ`}
        >
          <Icon name="menu" size={17} />
          <span>المزيد</span>
        </button>
      </div>

      {hint ? (
        <p className="facility-hint" role="status">
          {hint === "copied" ? "نُسخ الرابط." : "تعذّر نسخ الرابط."}
        </p>
      ) : null}

      {panel === "menu" ? (
        <CardDialog title={f.nameAr} onClose={() => setPanel(null)}>
          <div className="card-menu">
            <button type="button" onClick={openHours}>
              <Icon name="clock" />
              أوقات الدوام
            </button>
            <button
              type="button"
              onClick={() => {
                setPanel(null);
                void share();
              }}
            >
              <Icon name="share" />
              مشاركة الرابط
            </button>
            <Link href={page}>
              <Icon name="externalLink" />
              صفحة المنشأة
            </Link>
            <button type="button" className="danger" onClick={() => setPanel("report")}>
              <Icon name="flag" />
              الإبلاغ عن خطأ
            </button>
          </div>
        </CardDialog>
      ) : null}

      {panel === "hours" ? (
        <CardDialog title={`أوقات دوام ${f.nameAr}`} onClose={() => setPanel(null)}>
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

      {panel === "report" ? (
        <CardDialog title={`الإبلاغ عن ${f.nameAr}`} onClose={() => setPanel(null)}>
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
