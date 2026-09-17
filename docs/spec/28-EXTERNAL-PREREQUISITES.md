# External Prerequisites and Only Legitimate Manual Inputs

The architecture does not need owner clarification. These are external assets/credentials that cannot be invented.

## Required before Staging can be fully external

- GitHub repository access.
- Render account/project.
- object storage account/keys.
- Sentry project if used.

## Required before Production domain cutover

- purchased root domain.
- registrar or Cloudflare access.

## Required before account OTP Production

- selected production OTP/SMS/WhatsApp-capable provider.
- credentials.
- verified sender if required.

The code must use provider interfaces so this does not block core development.

## Required before Google Play release

- Play Console developer account access.
- final app listing name.
- developer/contact information.
- privacy/support domain active.
- upload keystore secure password.
- Firebase project for FCM.
- store assets.

## Required before iOS release

- Apple Developer account.
- App Store Connect.
- APNs signing credentials.
- macOS/Xcode environment.

## Paid actions

A coding agent must not purchase:
- domain.
- hosting plan.
- developer program.
- SMS credits.

without the account owner/payment authorization available in its environment.

This is not a product-design blocker: finish implementation and prepare exact deployment steps while credentials are absent.
