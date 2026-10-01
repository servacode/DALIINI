import { CopyLink } from "./copy-link";
import { Icon } from "./ui";

/*
 * Share links are plain URLs to each service's own share page: no third-party
 * script, nothing loaded until the reader taps one.
 */
export function ShareLinks({ title, url }: { title: string; url: string }) {
  const u = encodeURIComponent(url);
  const text = encodeURIComponent(title);
  const links = [
    { label: "واتساب", icon: "whatsapp" as const, href: `https://wa.me/?text=${encodeURIComponent(`${title}\n${url}`)}` },
    { label: "فيسبوك", href: `https://www.facebook.com/sharer/sharer.php?u=${u}` },
    { label: "تيليجرام", href: `https://t.me/share/url?url=${u}&text=${text}` },
  ];
  return (
    <section className="share" aria-labelledby="share-title">
      <h2 id="share-title" className="with-icon"><Icon name="share" size={20} />شارك هذه الصفحة</h2>
      <div className="actions">
        {links.map((l) => (
          <a key={l.label} className="button button-alt" href={l.href} target="_blank" rel="noopener noreferrer">
            {l.icon ? <Icon name={l.icon} /> : null}
            {l.label}
            <span className="sr-only"> (يفتح في نافذة جديدة)</span>
          </a>
        ))}
        <CopyLink url={url} />
      </div>
    </section>
  );
}
