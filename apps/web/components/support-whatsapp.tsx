import type { SupportContact } from "../lib/api";
import { Icon } from "./ui";

/**
 * «Write to us on WhatsApp»: the number the team answers by hand, one press from a chat
 * (DECISION-102). Renders nothing when no number is configured — never an empty field.
 */
export function SupportWhatsApp({
  contact,
  compact,
}: {
  contact: SupportContact;
  compact?: boolean;
}) {
  if (!contact.whatsapp || !contact.whatsappLink) return null;
  return (
    <a
      className={compact ? "support-whatsapp support-whatsapp-compact" : "support-whatsapp"}
      href={contact.whatsappLink}
      target="_blank"
      rel="noopener noreferrer"
      data-testid="support-whatsapp"
    >
      <Icon name="whatsapp" size={compact ? 16 : 20} />
      <span>
        <strong>راسلنا على واتساب</strong>
        {compact ? null : <span className="ltr">{contact.whatsapp}</span>}
      </span>
    </a>
  );
}
