import type { IconName } from "@servacode/design-tokens/icons";
import Link from "next/link";
import type { ReactNode } from "react";
import { Icon } from "../../components/ui";
import { pageMetadata } from "../../lib/seo";

/*
 * /how-we-verify — what "verified" means on a facility page. Facility pages
 * link here next to their trust line, and /duty links to #duty.
 */
export const metadata = pageMetadata({
  title: "كيف نتحقق من المعلومات",
  description: "كيف يراجع دليني مستندات المنشآت قبل النشر، وما معنى «آخر تحقق»، ومن أين تأتي المناوبات، وكيف تبلّغ عن خطأ.",
  path: "/how-we-verify",
});

function Block({ id, icon, title, children }: { id: string; icon: IconName; title: string; children: ReactNode }) {
  return (
    <section id={id} aria-labelledby={`${id}-title`} className="card">
      <h2 id={`${id}-title`} className="with-icon"><Icon name={icon} size={22} />{title}</h2>
      {children}
    </section>
  );
}

export default function HowWeVerifyPage() {
  return (
    <div className="shell page">
      <h1>كيف نتحقق من المعلومات</h1>
      <p className="page-intro">نريد أن تكون كل معلومة في دليني صحيحة وحديثة. هذا ما نفعله قبل النشر وبعده، وما يمكنك فعله إن وجدت خطأ.</p>

      <div className="list verify">
        <Block id="review" icon="shield" title="مراجعة المستندات قبل النشر">
          <p>
            قبل نشر أي منشأة، يراجع فريقنا المستندات التي يرفعها صاحبها، مثل ترخيص المزاولة وإثبات الموقع، ويقارنها بالبيانات
            المكتوبة. لا تظهر المنشأة في الدليل قبل الموافقة، ولا تظهر المستندات للزوار أبداً.
          </p>
        </Block>

        <Block id="last-verified" icon="verified" title="ما معنى «آخر تحقق»؟">
          <p>
            في صفحة كل منشأة سطر يقول متى وافق فريقنا على بياناتها آخر مرة، مثل «تم التحقق قبل ٣ أيام». وبجانبه «آخر تحديث»:
            آخر مرة تغيّرت فيها بيانات المنشأة.
          </p>
        </Block>

        <Block id="reverify" icon="refresh" title="إعادة التحقق عند التعديلات المهمة">
          <p>
            إذا غيّر صاحب المنشأة بياناتها الأساسية، مثل الاسم أو الهاتف أو العنوان أو الموقع، تعود المنشأة إلى المراجعة وتغيب
            عن الدليل حتى نوافق على التعديل. هكذا لا يتغير رقم هاتف أو موقع دون أن نراه.
          </p>
        </Block>

        <Block id="hours" icon="clock" title="أوقات الدوام">
          <p>
            يدخل صاحب المنشأة أوقات الدوام والإغلاقات المؤقتة ويحدّثها بنفسه، ومنها تُحسب حالة «مفتوح الآن» تلقائياً. إن تأخر في
            تحديثها فقد لا تطابق الواقع، لذلك اتصل قبل التوجّه.
          </p>
        </Block>

        <Block id="duty" icon="pharmacy" title="من أين تأتي المناوبات؟">
          <p>
            تسجّل كل صيدلية مواعيد مناوباتها بنفسها من التطبيق، وتظهر «مناوبة الآن» خلال وقت المناوبة فقط. ننصح بالاتصال قبل
            التوجّه، وإن وجدت صيدلية مغلقة في وقت مناوبتها فأبلغنا.
          </p>
          <p><Link href="/duty">اعرض الصيدليات المناوبة الآن</Link></p>
        </Block>

        <Block id="report" icon="flag" title="وجدت معلومة خاطئة؟">
          <p>
            افتح صفحة المنشأة في تطبيق دليني واضغط «الإبلاغ عن مشكلة»، ثم اختر السبب: معلومات خاطئة، أو الموقع خاطئ، أو أوقات
            الدوام خاطئة، أو ليست مناوبة، أو مغلقة نهائياً. يراجع فريقنا كل بلاغ ويصحح المعلومة عند الحاجة.
          </p>
          <p>لا تملك التطبيق؟ <Link href="/support">راسل الدعم</Link> واذكر اسم المنشأة ورابط صفحتها.</p>
        </Block>
      </div>

      <p className="more">
        صاحب منشأة؟ <Link href="/owners">اعرف كيف تضيف منشأتك</Link>.
      </p>
    </div>
  );
}
