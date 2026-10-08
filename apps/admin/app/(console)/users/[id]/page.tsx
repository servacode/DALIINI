import { redirect } from "next/navigation";

/**
 * There is no account detail page any more: a card holds its whole account (DECISION-106).
 *
 * The route stays, because links to it are already written down — in audit entries, in
 * messages between operators, in someone's bookmarks — and a dead link is a worse answer
 * than a redirect. It sends the reader to the list with that card open, which is where the
 * same information now lives.
 */
export default async function UserDetailRedirect({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  redirect(`/users?id=${encodeURIComponent(id)}`);
}
