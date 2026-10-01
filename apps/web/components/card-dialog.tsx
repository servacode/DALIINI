"use client";

import { useCallback, useEffect, useRef } from "react";
import { createPortal } from "react-dom";
import { Icon } from "./ui";

/**
 * A small window over the card that opened it.
 *
 * The hours and the report form used to unfold inside the card. That worked, but it pushed one
 * card taller than the rest and the row stopped being a row — the grid looked broken by the act
 * of using it. Here the card never changes size, so twenty facilities read as twenty of the same
 * thing whatever anybody has open.
 *
 * It is a dialog in full: the page behind cannot be scrolled or tabbed into, Escape closes it,
 * a press outside closes it, and the focus goes back to nothing the reader has to hunt for.
 *
 * It is rendered into the body rather than where it is written. A card lifts under the cursor,
 * and a transformed element becomes the containing block for everything fixed inside it — so a
 * dialog written inside the card was laid out inside the card, clipped to it, with the page
 * behind it undimmed. The portal is not a convenience here; it is what makes it a dialog.
 */
export function CardDialog({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: React.ReactNode;
}) {
  const panel = useRef<HTMLDivElement>(null);
  const closer = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    const had = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    closer.current?.focus();
    return () => {
      document.body.style.overflow = had;
    };
  }, []);

  const onKeyDown = useCallback(
    (event: React.KeyboardEvent) => {
      if (event.key === "Escape") {
        event.preventDefault();
        onClose();
        return;
      }
      if (event.key !== "Tab") return;
      const focusable = panel.current?.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );
      if (!focusable || focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    },
    [onClose],
  );

  /* Nothing opens a dialog but a press, so this never renders on the server — but a render
     without a document would throw rather than degrade, and that is worth one line. */
  if (typeof document === "undefined") return null;

  return createPortal(
    <div className="dialog-scrim" onClick={onClose} onKeyDown={onKeyDown} role="presentation">
      <div
        className="dialog"
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="dialog-head">
          <h3>{title}</h3>
          <button type="button" ref={closer} aria-label="إغلاق" onClick={onClose}>
            <Icon name="close" size={18} />
          </button>
        </div>
        <div className="dialog-body">{children}</div>
      </div>
    </div>,
    document.body,
  );
}
