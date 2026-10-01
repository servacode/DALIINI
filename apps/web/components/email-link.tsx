/* Renders a mailto link when configured, else the placeholder as plain text. */
export function EmailLink({ email }: { email: string }) {
  return email.includes("@") ? <a className="ltr" href={`mailto:${email}`}>{email}</a> : <span>{email}</span>;
}
