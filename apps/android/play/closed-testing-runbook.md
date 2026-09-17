# Closed testing runbook

1. Confirm `P20 RELEASE QUALITY PASS` or document why only an earlier track is being used; do not promote to RC with open P0/P1 issues.
2. Produce a signed AAB from the exact recorded commit and record SHA-256/versionCode/versionName.
3. Upload first to Internal testing and install from Play, not by sideload only.
4. Run smoke/device QA against staging/approved production-like services.
5. If Play Console requires closed testing for this developer account/app, create a closed track and keep at least the current required number of eligible testers opted in for the required continuous period. As checked 2026-09-17 for qualifying new personal accounts: 12 testers for 14 continuous days.
6. Give testers concrete tasks: registration/login, location denied/approximate/precise, search, detail, map, owner flow, offline recovery, push, RTL/large font, account deletion.
7. Record tester feedback and each code/product change resulting from it.
8. Re-run P20 release quality and generate a new RC for any code change.
9. Apply for production access only when Play Console requires it and the actual eligibility criteria are met.
