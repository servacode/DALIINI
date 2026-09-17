# Maps, Geo, Routing and Navigation

## 1. Geo truth

PostGIS owns:
- facility coordinates.
- nearest.
- distance.
- bbox filtering.
- optional polygon containment.

## 2. Coordinate storage

SRID 4326.

Validate:
```text
latitude -90..90
longitude -180..180
```

## 3. Public listing

With coordinates:
- nearest-first.

Without:
- distance null.
- deterministic non-distance ordering.

No client fake distance.

## 4. Map rendering

MapLibre Native.

Production must have:
- licensed/approved map style.
- stable tile provider.
- attribution.
- rate/cost understanding.
- caching rules.

Never ship `demotiles.maplibre.org` as Production dependency.

## 5. Map API

Use viewport endpoint:
```text
GET /public/map/facilities/?bbox=west,south,east,north&provinceId=...
```

Return compact:
- id.
- lat/lng.
- category icon.
- status.
- short label if needed.

Cluster client-side or server-side based on measured density.

## 6. Owner picker

- show current location suggestion.
- draggable pin.
- address lookup optional.
- province/city consistency validation.
- explicit save.

## 7. Routing

Interface:
```text
RoutingProvider.route(origin, destination, profile)
```

Initial OSRM adapter.

Do not hardcode public OSRM demo endpoint in Production.

## 8. Geocoding

Interface:
```text
GeocodingProvider.forward(query)
GeocodingProvider.reverse(point)
```

Nominatim-compatible adapter supported.

Respect provider usage policy.

## 9. Built-in navigation engine

State:
```text
Idle
Routing
Navigating
Rerouting
Arrived
Error
```

Tracks:
- current route.
- current maneuver.
- remaining distance.
- ETA.
- off-route distance.
- reroute cooldown.
- arrival threshold.

## 10. Voice

Arabic maneuver phrase builder.

Platform TTS fallback:
- Android native TTS.
- iOS AVSpeechSynthesizer.

Optional remote high-quality provider later through interface.

## 11. Road test

Navigation not qualified until real road test verifies:
- GPS drift.
- maneuver timing.
- reroute.
- screen lock/lifecycle.
- voice.
- network loss.
