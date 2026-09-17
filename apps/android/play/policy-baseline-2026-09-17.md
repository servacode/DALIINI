# Google Play policy baseline — checked 2026-09-17

Re-check immediately before submission.

Official references:
- Target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Data safety: https://support.google.com/googleplay/android-developer/answer/10787469
- Account deletion: https://support.google.com/googleplay/android-developer/answer/13327111
- New personal account testing: https://support.google.com/googleplay/android-developer/answer/14151465

Current release facts:
- New apps and updates submitted after 2026-08-31 must target Android 16 / API 36+.
- Apps with account creation require both an in-app account deletion path and an external web deletion resource.
- Data Safety must be completed for closed, open and production tracks; internal-only testing is exempt from display requirements.
- The 12-testers/14-days closed-test requirement applies to qualifying personal developer accounts created after 2023-11-13. Do not assume it applies when Play Console already grants production access or the account is otherwise exempt; inspect the actual account state.
