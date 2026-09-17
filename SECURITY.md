# Security Policy

This repository follows `docs/spec/15-SECURITY-PRIVACY.md`.

## Secrets

Never commit passwords, OTP values, token material, database URLs containing real credentials, storage keys, signing keys, Firebase/APNs credentials, or production provider secrets. Use environment variables and secret stores.

## Reporting

Security findings are treated as release blockers according to the severity model in the testing and operations specifications. Evidence must be redacted before it is committed or attached to tickets.
