# RahalGo map and navigation — audit and reuse matrix

**Date:** 2026-09-24 · **Subject:** what Directory should take from RahalGo's mature map and
navigation stack, and what it must not.

## What was found

`D:\RahalGo` — a delivery platform by the same owner, with a Gradle multi-app Android build
(`mobile/`: `app-customer`, `app-driver`, `app-merchant`, `app-rep`) and two modules that matter
here:

| Module | Size | What it is |
|---|---|---|
| `mobile/map` | ~25 files | MapLibre hosting, style repository, offline map packages, downloader, region picker, lifecycle bridge, marker animator, point picker |
| `mobile/driver-navigation` | ~17,600 lines including tests | A complete turn-by-turn stack: route progress, projection, off-route detection, rerouting, alternative routes, arrival, GPS quality, voice planning, trace recording and replay |

Its navigation is materially more experienced than this app's: it has driven real roads and
carries the scars — jitter tolerance, parallel-road suspicion, wrong-way detection, GPS grading,
replay tests against recorded traces.

## Reuse matrix

### REUSE_AS_IS — nothing

Not one file is taken verbatim. RahalGo's navigation is expressed in its own domain types
(`NavRoute`, `NavFix`, `NavManeuver`, `GeoPoint`), its own house style, and in several places its
delivery vocabulary (a "trip" has a merchant, a customer and a courier). Copying a file would
drag that vocabulary into a health directory and leave two codebases that drift apart. Directory
already has a working engine that passed source qualification; the value to take is what RahalGo
*learned*, not its text.

### ADAPT — three behaviours, re-expressed in Directory's own types

| Taken | From | What it is | Where it now lives |
|---|---|---|---|
| **A single reading never decides** | `OffRouteDetector` (a score that must reach `confirmScore` with fresh evidence before a detour is believed) | Directory rerouted the moment one fix fell outside the corridor. A phone under a bridge does that and corrects itself a second later, and the driver gets a new route and a voice talking over them for nothing. | `NavigationEngine`: `offRouteConfirmations`, an `offRouteStreak` that a good reading clears |
| **A reading's accuracy is part of what it means** | `BusinessArrival.effectiveRadiusM(accuracy)` and its `MAX_ACCURACY_M` | Directory compared a raw distance against a fixed radius. A fix with 120 m of uncertainty was allowed to end a trip. Now a vague reading cannot arrive at all, and a merely imprecise one widens the radius instead of being believed exactly. | `NavigationEngine`: `unusableAccuracyMeters`, the widened arrival radius, and `LocationFix.accuracyMeters` carried from the provider |
| **The route is seen before it is followed** | RahalGo's driver flow previews a trip's route with its distance and time before guidance starts | Pressing "الطريق" started live navigation at once — camera following and voice — for someone who only wanted to know how far it is. | `RoutePreviewScreen` / `RoutePreviewViewModel`, and `FacilityDetail → RoutePreview → BuiltInNavigation` |

Each is covered by tests in `NavigationEngineTest` that fail if the old behaviour returns.

### DO_NOT_REUSE

| Not taken | Why |
|---|---|
| Delivery domain: orders, drivers, couriers, merchants, pricing, delivery zones, wallet, payments | Nothing in a health directory means any of it |
| RahalGo API DTOs and its backend contract | Directory's contract is generated from its own OpenAPI document; a second source of truth is how clients drift |
| Offline map packages, downloader, archive leases, region picker (`mobile/map/data/*`) | A real capability, and a decision with storage, licensing and update consequences that no one has taken for this product. Recorded as a possibility, not smuggled in |
| Map style repository, night styles, style switching | Directory has one style from `MAP_STYLE_URL` and one visual language |
| Alternative routes (`RouteChoices`, `AltRouteLayer`, `RouteChoiceGate`) | Meaningful for a courier being paid by the trip; noise for someone walking to a pharmacy |
| Parallel-road suspicion, wrong-way detection | Both need data and tuning this product has never gathered. Adopting them untuned would produce confident wrong answers |
| Trace recorder and replay drive | Valuable for road testing, and a separate piece of work with its own privacy question: a recorded trace is a record of where a person went |
| Voice provider wiring, clips, secrets | Directory has its own `NavigationVoice` abstraction and Arabic phrase builder. No provider key is taken, and none is added |
| Branding, colours, typography | Directory has its own design system |

## Ownership

Nothing in Directory depends on the RahalGo repository at build time or at run time. What was
adapted is Directory's own code, in Directory's types, with the reasoning recorded above and in
the comments at each site. There is no shared module, no copied file and no submodule.

## Still not verified

The adapted rules are covered by unit tests and nothing more. Live navigation as a whole —
route drawing on a moving map, rerouting on a real detour, voice guidance, and INT-096's marker
and route accumulation under repeated real fixes — remains `SOURCE/CI VERIFIED — ROAD TEST
REQUIRED`. A road test is the only thing that can change that, and no claim here anticipates it.
