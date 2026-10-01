"use client";

import Link from "next/link";

import { useResource } from "../lib/client/use-resource";
import { Icon } from "./icons";

type Alert = Readonly<{ kind: string; severity: "info" | "warning" | "critical" }>;

/**
 * The top bar's system indicator, visible from every screen.
 *
 * A bell counts the alerts that need someone (critical and warning), red when any is
 * critical, and opens the dashboard where they are listed. When maintenance mode is on, a
 * pill says so, because an operator must never forget the public app is switched off. It
 * reads the same alerts endpoint as the dashboard, refreshed in place every minute, and
 * stays silent for an operator who cannot read the dashboard.
 */
export function TopbarStatus({ enabled }: { enabled: boolean }) {
  const alerts = useResource<{ items: Alert[] }>("alerts", {}, { enabled, refreshMs: 60_000 });
  if (!enabled || !alerts.data) return null;
  const items = alerts.data.items;
  const urgent = items.filter((alert) => alert.severity !== "info");
  const critical = urgent.some((alert) => alert.severity === "critical");
  const maintenance = items.some((alert) => alert.kind === "MAINTENANCE_ON");
  const label =
    urgent.length === 0
      ? "لا تنبيهات تحتاج انتباهك"
      : urgent.length === 1
        ? "تنبيه واحد يحتاج انتباهك"
        : `${urgent.length} تنبيهات تحتاج انتباهك`;

  return (
    <>
      {maintenance ? (
        <Link href="/settings" className="maintenance-pill" data-testid="maintenance-pill">
          <Icon name="tool" />
          وضع الصيانة مفعّل
        </Link>
      ) : null}
      <Link
        href="/dashboard"
        className="icon-button bell"
        aria-label={label}
        title={label}
        data-testid="alerts-bell"
      >
        <Icon name="bell" />
        {urgent.length > 0 ? (
          <span className="bell-count" data-critical={critical || undefined} aria-hidden="true">
            {urgent.length > 9 ? "9+" : urgent.length}
          </span>
        ) : null}
      </Link>
    </>
  );
}
