# Font Policy

- Primary Arabic/Latin brand font: Tajawal.
- Use platform/system fallback only when Tajawal is unavailable during bootstrap; production clients must bundle or load the approved licensed font asset according to platform rules.
- Typography roles come only from generated design tokens.
- Feature code must not introduce arbitrary font sizes or weights.
- Arabic is the default locale and RTL is the default layout direction.
- Phone numbers, IDs and other intrinsically LTR values remain LTR inside RTL layouts.
- Dynamic text/large-font behavior must be tested on mobile; Admin must preserve browser zoom and keyboard accessibility.
