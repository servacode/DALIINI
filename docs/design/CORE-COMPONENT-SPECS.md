# Core Component Specifications

These are shared behavioral specifications; each platform implements them natively with generated tokens.

## Required shared components

AppText, Button, IconButton, TextField, PhoneField, OTPField, PasswordField, Select, SearchField, Chip, Badge, StatusBadge, Card, FacilityCard, CategoryTile, SectionHeader, EmptyState, ErrorState, OfflineBanner, Skeleton, Modal/Dialog, BottomSheet, Toast/Snackbar, AppBar, BottomNavigation, Tabs, MapMarker, ImageUploader, EvidenceUploader, HoursEditor.

Admin additionally provides DataTable, FilterBar, Pagination, SideNav, FormSection, ConfirmDialog, DiffViewer and AuditTimeline.

## Rules

- Values come from design tokens; features do not hardcode brand colors, spacing, radii or text roles.
- Touch targets on Android are at least 48dp for actionable controls.
- Every control has semantic/accessibility labels and visible textual error states.
- Destructive actions use explicit confirmation where irreversible/high impact.
- Loading, empty, error, offline, permission-required and maintenance states are first-class screen states.
- RTL uses logical start/end. Directional icons mirror only when their meaning is directional.
- Maps are never mirrored.
