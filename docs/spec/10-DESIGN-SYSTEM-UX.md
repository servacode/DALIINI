# Unified Design System and UX Specification

## 1. Goal

Android, iOS, Admin and Public Web should look like the same brand, without forcing identical platform UI code.

## 2. Token source

```text
packages/design-tokens/
  tokens/
    colors.json
    semantic.json
    typography.json
    spacing.json
    radius.json
    elevation.json
    motion.json
  schema/
  scripts/
  generated/
```

Generate:
- TypeScript/CSS.
- Kotlin.
- Swift.

## 3. Baseline palette

```text
primary            #0B6B47
primaryStrong      #06452F
primaryDeep        #043526
primarySoft        #DCEFE6
primarySofter      #F0F8F4

background         #F7F8F5
surface            #FFFFFF
surfaceAlt         #F1F4F2

textPrimary        #15231C
textSecondary      #5D6B64
textMuted          #7A8780

border             #DDE4E0
borderStrong       #C5D0CA

success            #138A5B
warning            #C98214
danger             #C23B3B
info               #2E6FB5
```

Validate WCAG contrast before freeze.

## 4. Typography

Baseline font: **Tajawal**.

Roles:
```text
display
headlineLarge
headlineMedium
titleLarge
titleMedium
bodyLarge
bodyMedium
bodySmall
labelLarge
labelMedium
```

Do not use arbitrary fontSize/weight inside features after tokenization.

## 5. Spacing scale

```text
2, 4, 8, 12, 16, 20, 24, 32, 40, 48, 64
```

Semantic aliases:
- xs.
- sm.
- md.
- lg.
- xl.
- xxl.

## 6. Radius

```text
small 8
medium 12
large 16
xl 20
pill 999
```

## 7. Elevation

Use restrained shadows.

Mobile:
- cards mostly border + minimal elevation.
Admin:
- surface hierarchy not shadow-heavy.

## 8. Motion

- short feedback 100–150ms.
- standard transitions 200–250ms.
- avoid distracting spring on operational Admin.
- respect reduced-motion settings.

## 9. Components

Must exist before feature duplication:
- AppText.
- Button.
- IconButton.
- TextField.
- PhoneField.
- OTPField.
- PasswordField.
- Select.
- SearchField.
- Chip.
- Badge.
- StatusBadge.
- Card.
- FacilityCard.
- CategoryTile.
- SectionHeader.
- EmptyState.
- ErrorState.
- OfflineBanner.
- Skeleton.
- Modal/Dialog.
- BottomSheet.
- Toast/Snackbar.
- AppBar.
- BottomNavigation.
- Tabs.
- MapMarker.
- ImageUploader.
- EvidenceUploader.
- HoursEditor.

Admin:
- DataTable.
- FilterBar.
- Pagination.
- SideNav.
- FormSection.
- ConfirmDialog.
- DiffViewer.
- AuditTimeline.

## 10. RTL rules

Arabic default:
- logical start/end, never assume left/right for content.
- icons that imply direction mirrored where semantically correct.
- numeric/phone content may remain LTR inside RTL layout.
- maps remain geographic, not mirrored.
- charts axis labels tested.

## 11. Screen states

Every data screen:
```text
Loading
Content
Empty
Error
OfflineContent
PermissionRequired (if applicable)
Maintenance
```

## 12. Home visual composition

```text
Current location / province         Search
Advertisement slider
Section: Main categories
Grid
Section: Nearby
Filter chips
Horizontal/vertical cards based on final Figma
Bottom navigation
```

## 13. Visual QA

For every implemented screen:
- compare with approved Figma.
- Android phone screenshot.
- narrow screen.
- large font.
- dark mode only if V3 includes it; baseline light mode first unless explicitly designed.
- RTL.
- Arabic text overflow.
- system bars.

## 14. Accessibility

- minimum touch target ~48dp Android.
- semantic labels.
- contrast.
- dynamic type where possible.
- keyboard/focus on Admin.
- form errors textual.
- not color-only.
