# Product Specification

## 1. Home

Top:
- current location/province.
- location state/refresh action.
- search icon.

Content:
- advertisement slider.
- main category grid.
- nearby section.
- chips: All / Open Now / Duty Now when relevant.
- facility cards.

Bottom navigation:
- Home.
- Map.
- Account.

No Search bottom tab.

## 2. Province and location

Behavior:
- restore last selected province immediately.
- attempt foreground location automatically where permission exists.
- do not block app startup.
- if precise coordinates exist, use them for nearest ordering.
- if only approximate coordinates exist, do not claim exact precision.
- if permission denied, manual province remains fully usable.

Province selection and current GPS are related but not identical:
- selected province determines directory scope.
- coordinates determine distance/order.
- backend may map coordinates to a province if trusted polygon/reference data exists.
- never silently switch province against explicit user selection without clear UX.

## 3. Categories

Categories come from API.

Category grid uses:
- name.
- iconKey.
- order.
- capabilities.

Click opens generic directory screen with categoryId.

## 4. Directory list

Filters are capability-driven.

Always possible:
- category.
- province.

Optional:
- nearest.
- open now.
- duty now.
- specialty.
- service.
- city/neighborhood.
- text search.

General category list is province-wide. Do not cut by a hidden small radius.

## 5. Search

Search entry is available from Home top bar.

Search:
- debounced.
- cancellable.
- paginated/cursor-based.
- supports zero-result state.
- stores recent searches locally only if product privacy policy permits.
- can search facility, category, location, specialty/service.

## 6. Facility details

Public:
- image gallery.
- Arabic name.
- optional English name.
- category.
- state.
- distance when known.
- rating average/count.
- address.
- city/neighborhood.
- phone.
- map.
- directions.
- hours.
- next open.
- specialty/services.
- category-specific public profile.
- rating action if logged in.

Private/internal must never leak:
- evidence.
- reviewer notes.
- memberships unless owner/admin.
- internal specialization policy fields.
- raw storage keys.
- audit.

## 7. Ratings

- login required.
- 1 to 5.
- one rating per user/facility.
- editable.
- delete optional; baseline allowed.
- no text reviews.
- own facility member cannot rate.
- public DTO exposes aggregate only.

## 8. Account

Sections:
- profile.
- selected/profile province.
- profile image.
- my ratings.
- my facilities.
- sessions/devices.
- settings.
- privacy.
- request account deletion.
- logout.

## 9. Register

Flow:
1. name.
2. mobile.
3. province.
4. OTP.
5. password.
6. success/session.

No business type attached to account.

## 10. Recovery

- mobile.
- OTP/link through provider abstraction.
- new password only in app/web secure flow.
- successful reset revokes sessions.

## 11. Owner: My Facilities

Show:
- facility name.
- category.
- province.
- lifecycle state.
- last update.
- required action.

Actions depend on state.

## 12. Owner onboarding

Steps:
1. province/category.
2. basic info.
3. map point.
4. hours.
5. public images.
6. specialized fields.
7. verification evidence.
8. review.
9. submit.
10. status.

Draft auto-save.

If onboarding later disables:
- existing draft can be edited.
- submit/re-submit rechecks current policy.

## 13. Active facility management

Owner may:
- edit info.
- edit contact.
- edit location.
- edit images.
- edit hours.
- create/cancel temporary closure.
- duty actions.
- managers.

Sensitive edits cause REVERIFICATION_REQUIRED.

## 14. Pharmacy duty

Owner:
- schedule future shift.
- start now if valid.
- edit according to policy.
- cancel.
- end early.

Public:
- Duty Now.
- temporary closure overrides.

## 15. Admin

Admin is task-oriented.

Core modules:
- Dashboard.
- Applications.
- Facilities.
- Users.
- Roles.
- Provinces.
- Category Groups.
- Categories.
- Capabilities.
- Verification Policies.
- Ads.
- Audit.
- Analytics.
- Settings.
- System Status.

## 16. Ads

Home ads can be:
- global.
- province-targeted.
- optionally category-contextual.
- scheduled.
- ordered.
- duration-controlled.
- clickable to safe destinations.

## 17. Maintenance

Admin can enable typed maintenance settings.

Mobile behavior:
- receive API maintenance response.
- show branded maintenance screen.
- retry.
- no crash loop.

## 18. Public web

Minimum pages:
- `/`
- `/privacy`
- `/terms`
- `/support`
- `/delete-account`

Optional future:
- public facility pages.
- SEO directory.

## 19. Account deletion

Because account creation exists, provide:
- in-app path.
- external web path.
- authenticated or identity-confirmed request.
- data deletion/anonymization rules.
- retention exceptions explained.

## 20. Product exclusions

Do not add:
- appointments.
- ordering.
- delivery.
- payments.
- patient records.
- chat.
without approved Change Request.
