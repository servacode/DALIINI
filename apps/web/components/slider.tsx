"use client";

import Link from "next/link";
import { useCallback, useEffect, useRef, useState } from "react";
import type { Slide } from "../lib/api";
import { Icon } from "./ui";

/**
 * The strip of slides under the bar, filled from the console.
 *
 * What it does and why:
 *
 *  - **It advances itself, but not while you are looking at it.** A hand on a slide, a cursor
 *    over it or a keyboard focus inside it holds the strip until you leave, because a carousel that
 *    moves while somebody is reading it is a carousel nobody finishes reading.
 *  - **Each slide holds for its own time.** The console sets `slideDurationMs` per slide; one
 *    can be held longer without slowing the rest.
 *  - **It stops entirely when the visitor asked for less motion.** `prefers-reduced-motion` is
 *    not a hint about animation speed: for a thing that moves on its own it means do not.
 *  - **Everything is reachable without it moving.** Dots, arrows, arrow keys and a swipe.
 */

const MIN_MS = 3000;
const MAX_MS = 12000;

function hrefFor(slide: Slide): string | null {
  const { type, payload } = slide.action ?? { type: "NONE", payload: {} };
  if (type === "FACILITY" && typeof payload.facilityId === "string") {
    return `/f/${payload.facilityId}`;
  }
  if (type === "EXTERNAL_URL" && typeof payload.url === "string") {
    const url = payload.url;
    return url.startsWith("https://") ? url : null;
  }
  // A category or an in-app route is somewhere only the app can go; the slide still shows.
  return null;
}

export function Slider({ slides }: { slides: Slide[] }) {
  const [index, setIndex] = useState(0);
  const [held, setHeld] = useState(false);
  const touchStart = useRef<number | null>(null);
  const count = slides.length;

  const go = useCallback(
    (next: number) => setIndex(((next % count) + count) % count),
    [count],
  );

  useEffect(() => {
    if (count < 2 || held) return;
    if (window.matchMedia?.("(prefers-reduced-motion: reduce)").matches) return;
    const ms = Math.min(Math.max(slides[index]?.slideDurationMs ?? 5000, MIN_MS), MAX_MS);
    const timer = window.setTimeout(() => go(index + 1), ms);
    return () => window.clearTimeout(timer);
  }, [count, held, index, slides, go]);

  if (count === 0) return null;

  return (
    <section
      className="slider"
      aria-roledescription="carousel"
      aria-label="إعلانات المنصة"
      onMouseEnter={() => setHeld(true)}
      onMouseLeave={() => setHeld(false)}
      onFocusCapture={() => setHeld(true)}
      onBlurCapture={() => setHeld(false)}
      onKeyDown={(event) => {
        if (event.key === "ArrowRight") go(index - 1);
        if (event.key === "ArrowLeft") go(index + 1);
      }}
      onTouchStart={(event) => {
        touchStart.current = event.touches[0]?.clientX ?? null;
      }}
      onTouchEnd={(event) => {
        const from = touchStart.current;
        const to = event.changedTouches[0]?.clientX;
        touchStart.current = null;
        if (from == null || to == null) return;
        const moved = to - from;
        if (Math.abs(moved) < 40) return;
        go(index + (moved > 0 ? -1 : 1));
      }}
    >
      <div className="slider-track">
        {slides.map((slide, at) => {
          const href = hrefFor(slide);
          const inner = (
            <>
              {/* eslint-disable-next-line @next/next/no-img-element -- remote media, no loader */}
              <img src={slide.imageUrl} alt={slide.titleAr ?? ""} loading={at === 0 ? "eager" : "lazy"} />
              {slide.titleAr || slide.subtitleAr ? (
                <div className="slide-caption">
                  {slide.titleAr ? <strong>{slide.titleAr}</strong> : null}
                  {slide.subtitleAr ? <span>{slide.subtitleAr}</span> : null}
                </div>
              ) : null}
            </>
          );
          return (
            <div
              className="slide"
              key={slide.id}
              data-current={at === index || undefined}
              aria-hidden={at === index ? undefined : true}
              role="group"
              aria-roledescription="slide"
              aria-label={`${at + 1} من ${count}`}
            >
              {href ? (
                href.startsWith("https://") ? (
                  <a href={href} target="_blank" rel="noopener noreferrer">
                    {inner}
                  </a>
                ) : (
                  <Link href={href}>{inner}</Link>
                )
              ) : (
                inner
              )}
            </div>
          );
        })}
      </div>

      {count > 1 ? (
        <>
          <button
            type="button"
            className="slider-arrow slider-prev"
            aria-label="السابق"
            onClick={() => go(index - 1)}
          >
            <Icon name="chevron" size={20} />
          </button>
          <button
            type="button"
            className="slider-arrow slider-next"
            aria-label="التالي"
            onClick={() => go(index + 1)}
          >
            <Icon name="chevron" size={20} />
          </button>
          <div className="slider-dots" role="tablist" aria-label="الشرائح">
            {slides.map((slide, at) => (
              <button
                key={slide.id}
                type="button"
                role="tab"
                aria-selected={at === index}
                aria-label={`الشريحة ${at + 1}`}
                onClick={() => go(at)}
              />
            ))}
          </div>
        </>
      ) : null}
    </section>
  );
}
