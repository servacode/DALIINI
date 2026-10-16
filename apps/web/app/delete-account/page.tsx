import { EmailLink } from "../../components/email-link";
import { publicConfig } from "../../lib/config";
import { pageMetadata } from "../../lib/seo";

/*
 * Required by Google Play's account-deletion policy: this public URL is the
 * listing's external deletion resource. Play asks that it name the app, give the
 * steps prominently, and say what is deleted, what is kept and for how long.
 * Every line below is what `request_account_deletion` (accounts/services.py)
 * does; change the two together. Keep the path stable.
 */
export const metadata = pageMetadata({
  title: "حذف حساب دليني",
  description: "كيفية حذف حسابك في تطبيق دليني والبيانات المرتبطة به، من داخل التطبيق أو عبر البريد، وما يُحذف وما يُحتفظ به ومدّته.",
  path: "/delete-account",
});

export default function DeleteAccountPage() {
  return (
    <article className="shell legal">
      <h1>حذف حسابك في تطبيق دليني</h1>
      <p>
        هذه الصفحة لمستخدمي تطبيق <strong>دليني</strong> (من ServaCode) على Android. يمكنك حذف حسابك وبياناتك
        في أي وقت، ويتم الحذف فورًا.
      </p>

      <h2>الطريقة الأولى: من داخل التطبيق</h2>
      <ol>
        <li>افتح تطبيق دليني وسجّل الدخول.</li>
        <li>اضغط على «حسابي».</li>
        <li>اختر «حذف الحساب» وأكّد الطلب.</li>
      </ol>
      <p>
        إذا كنت المالك الوحيد لمنشأة ما زالت تعمل على المنصة، فانقل ملكيتها لشخص آخر أو أغلقها من التطبيق أولًا، ثم
        احذف الحساب.
      </p>

      <h2>الطريقة الثانية: إذا لم تتمكن من استخدام التطبيق</h2>
      <p>
        راسلنا على <EmailLink email={publicConfig.privacyEmail} /> من بريد أو رقم يمكننا التحقق من خلاله أن الحساب
        لك، واذكر رقم الهاتف المسجّل به. ننفّذ الطلب خلال 7 أيام ونبلغك بذلك. لن نطلب منك كلمة المرور أو رمز
        التحقق أبدًا.
      </p>

      <h2>ما يُحذف فورًا</h2>
      <ul>
        <li>رقم هاتفك واسمك وعنوانك ومحافظتك.</li>
        <li>صورة ملفك الشخصي، وتُحذف من التخزين أيضًا.</li>
        <li>كلمة المرور، وكل جلسات الدخول على كل أجهزتك.</li>
        <li>الأماكن المحفوظة في المفضلة، والرسائل والإشعارات الموجهة إليك.</li>
        <li>ارتباطك بأي منشأة كمالك أو موظف، وأي صلاحيات إدارية.</li>
      </ul>

      <h2>ما يُحتفظ به، ولماذا، ولكم من الوقت</h2>
      <ul>
        <li>
          <strong>تقييماتك بالنجوم للمنشآت:</strong> تبقى جزءًا من تقييم المنشأة العام، لكنها لا تعود تحمل اسمك ولا أي
          شيء يدل عليك.
        </li>
        <li>
          <strong>سجلات الأمان والتدقيق</strong> (مثل وقت طلب الحذف): تبقى لحماية المنصة من الاحتيال، مرتبطة بحساب
          مجهول لا يحمل رقمك ولا اسمك.
        </li>
        <li>
          <strong>النسخ الاحتياطية:</strong> قد تبقى بياناتك في النسخ الاحتياطية المشفّرة حتى 30 يومًا، ثم تُحذف
          تلقائيًا.
        </li>
        <li>
          <strong>إحصاءات الاستخدام:</strong> مجهولة الهوية من الأصل ولا ترتبط بحسابك، وتُحذف بعد 180 يومًا.
        </li>
      </ul>

      <h2>بعد الحذف</h2>
      <p>
        لا يمكن استرجاع الحساب. يمكنك فتح حساب جديد لاحقًا بنفس الرقم، ويبدأ فارغًا.
      </p>
    </article>
  );
}
