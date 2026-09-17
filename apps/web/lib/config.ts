export const publicConfig = {
  rootDomain: process.env.NEXT_PUBLIC_ROOT_DOMAIN ?? "ROOT_DOMAIN",
  supportEmail: process.env.SUPPORT_EMAIL ?? "SUPPORT_EMAIL",
  privacyEmail: process.env.PRIVACY_CONTACT_EMAIL ?? "PRIVACY_CONTACT_EMAIL",
} as const;
