import type { OperationRoute } from "../../lib/operations/catalog";

export function OperationPage({ route }: { route: OperationRoute }) {
  return (
    <section className="operation-stack">
      <div className="page-heading">
        <div>
          <p className="eyebrow">تشغيل المنصة</p>
          <h1>{route.title}</h1>
          <p>{route.description}</p>
        </div>
        {route.primaryAction ? <button type="button">{route.primaryAction}</button> : null}
      </div>
      <div className="panel operation-panel">
        <div className="status-row">
          <span>الصلاحية</span>
          <code>{route.permission}</code>
        </div>
        <div className="status-row">
          <span>مصدر البيانات</span>
          <code>{route.endpoint}</code>
        </div>
        <p className="muted">
          الربط الشبكي النهائي يمر حصراً عبر BFF والعميل المولد من OpenAPI بعد إغلاق P10.
        </p>
      </div>
    </section>
  );
}
