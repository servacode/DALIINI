"""Where a map opens for a province when the user's own position is unknown (INT-092).

Reference data, frozen like `launch_v1`: a later change is a new module and a new migration.
Each point is the centre of the province's main city in WGS84 degrees. Only the launch
province has one; a province activated later gets its centre the same way, or from an
operator. A centre already set, by an operator or an earlier run, is never overwritten.

The specification models a province's geometry as an optional MultiPolygon
(`06-DATA-MODEL.md`); no authoritative boundary data exists in the project yet, so this
carries only the point a map needs.
"""

VERSION = "province-map-centers-v1"

# province code: (latitude, longitude)
CENTERS = {
    "raqqa": (35.9528, 39.0085),
}
