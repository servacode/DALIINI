# External Blockers

Only genuine external blockers are recorded here. Bugs and local toolchain issues are not blockers.

| ID | Description | Why internal work cannot resolve it | Owner input/action required | Work completed despite blocker | Waiting on blocker |
|---|---|---|---|---|---|
| EXT-001 | Final purchased `ROOT_DOMAIN` not yet available in this execution environment | Domain ownership and registrar/DNS authority are external assets | Provide/authorize owned domain at production cutover | Domain-neutral config and hostname conventions can be fully built | Production DNS/TLS cutover, final legal/store URLs, app-link association |
| EXT-002 | Production provider credentials not available (OTP, object storage, Sentry, FCM/APNs, hosting) | Credentials must come from owned external accounts and cannot be invented | Supply through secure environment when reaching connection/release gates | Provider abstractions, env contracts, local/staging-compatible implementation can proceed | Provider-connected staging/production qualification for each service |
| EXT-003 | Store/developer account credentials and signing secrets not available | Google Play/Apple access and signing secrets are external/irreversible assets | Authorized account access and secure secret injection at release phases | Build/release automation and policy materials can be prepared | Play/App Store track submission and production verification |
