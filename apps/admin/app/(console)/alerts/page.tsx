"use client";

import { AlertsPanel } from "../../../components/smart";
import { PageHero } from "../../../components/ui/extra";

/**
 * The alerts on a page of their own, where the top bar's bell leads. The bell opened the whole
 * dashboard, and the alert it counted was one block among the figures there.
 */
export default function AlertsPage() {
  return (
    <div className="stack" data-testid="alerts-page">
      <PageHero
        eyebrow="التشغيل"
        title="التنبيهات"
        description="ما تلاحظه المنصة بنفسها ويحتاج قراراً منك، يتحدث كل دقيقة."
      />
      <AlertsPanel standalone />
    </div>
  );
}
