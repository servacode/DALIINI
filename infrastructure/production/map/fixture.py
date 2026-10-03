#!/usr/bin/env python3
"""A one-tile map of central Raqqa, for checking the map host without the real map (DECISION-082).

    python3 infrastructure/production/map/fixture.py <map-dir>

Writes <map-dir>/syria.pmtiles: a PMTiles v3 archive holding one zoom-12 vector tile in the
OpenMapTiles schema the style reads, with Arabic names on a city, a town, a street and a
pharmacy. The smoke test serves it through Martin and Caddy, and render-check.mjs draws it with
the site's MapLibre and the brand's glyphs, so Arabic shaping is checked on every pull request
without downloading Syria. The real archive comes from build-map.sh.
"""
from __future__ import annotations

import gzip
import json
import math
import pathlib
import struct
import sys

ZOOM = 12
EXTENT = 4096
RAQQA = (35.9506, 39.0089)  # lat, lon of the clock tower


# --- protobuf, as much of it as a vector tile needs ---------------------------------------------
def varint(value: int) -> bytes:
    out = bytearray()
    while True:
        byte = value & 0x7F
        value >>= 7
        if value:
            out.append(byte | 0x80)
        else:
            out.append(byte)
            return bytes(out)


def field(number: int, wire: int) -> bytes:
    return varint((number << 3) | wire)


def string(number: int, text: str) -> bytes:
    data = text.encode("utf-8")
    return field(number, 2) + varint(len(data)) + data


def message(number: int, body: bytes) -> bytes:
    return field(number, 2) + varint(len(body)) + body


def packed(number: int, values: list[int]) -> bytes:
    return message(number, b"".join(varint(v) for v in values))


def zigzag(n: int) -> int:
    return (n << 1) ^ (n >> 31)


# --- where things are on the tile -----------------------------------------------------------------
def world(lat: float, lon: float) -> tuple[float, float]:
    """Web Mercator in tiles at ZOOM."""
    n = 1 << ZOOM
    x = (lon + 180.0) / 360.0 * n
    rad = math.radians(lat)
    y = (1.0 - math.log(math.tan(rad) + 1.0 / math.cos(rad)) / math.pi) / 2.0 * n
    return x, y


TILE_X, TILE_Y = (int(v) for v in world(*RAQQA))


def on_tile(lat: float, lon: float) -> tuple[int, int]:
    x, y = world(lat, lon)
    return round((x - TILE_X) * EXTENT), round((y - TILE_Y) * EXTENT)


def geometry(points: list[tuple[int, int]]) -> list[int]:
    """MoveTo the first point, then LineTo the rest, each as a zigzagged delta."""
    commands: list[int] = []
    cx = cy = 0
    for i, (x, y) in enumerate(points):
        if i == 0:
            commands.append((1 & 0x7) | (1 << 3))
        elif i == 1:
            commands.append((2 & 0x7) | ((len(points) - 1) << 3))
        commands += [zigzag(x - cx), zigzag(y - cy)]
        cx, cy = x, y
    return commands


def layer(name: str, features: list[tuple[int, dict[str, str], list[tuple[float, float]]]]) -> bytes:
    keys: list[str] = []
    values: list[str] = []
    body = b""
    for number, (kind, tags, coordinates) in enumerate(features, 1):
        refs: list[int] = []
        for key, value in tags.items():
            if key not in keys:
                keys.append(key)
            if value not in values:
                values.append(value)
            refs += [keys.index(key), values.index(value)]
        points = [on_tile(lat, lon) for lat, lon in coordinates]
        feature = (
            field(1, 0) + varint(number)
            + packed(2, refs)
            + field(3, 0) + varint(kind)
            + packed(4, geometry(points))
        )
        body += message(2, feature)
    return message(
        3,
        field(15, 0) + varint(2)
        + string(1, name)
        + body
        + b"".join(string(3, key) for key in keys)
        + b"".join(message(4, string(1, value)) for value in values)
        + field(5, 0) + varint(EXTENT),
    )


POINT, LINE = 1, 2
STREET = [(35.9560, 38.9930), (35.9506, 39.0089), (35.9450, 39.0250)]
TILE = b"".join([
    layer("transportation", [(LINE, {"class": "primary"}, STREET)]),
    layer("transportation_name", [
        (LINE, {"class": "primary", "name": "Tal Abyad Street", "name:ar": "شارع تل أبيض"}, STREET),
    ]),
    layer("place", [
        (POINT, {"class": "city", "name": "Raqqa", "name:ar": "الرقة"}, [RAQQA]),
        (POINT, {"class": "town", "name": "Al-Mansoura", "name:ar": "المنصورة"},
         [(35.9300, 38.9750)]),
    ]),
    layer("poi", [
        (POINT, {"class": "pharmacy", "subclass": "pharmacy", "name:ar": "صيدلية الشفاء"},
         [(35.9520, 39.0120)]),
    ]),
])


# --- PMTiles v3 -------------------------------------------------------------------------------------
def tile_id(z: int, x: int, y: int) -> int:
    """Zoom by accumulation, then the Hilbert curve, as the format specifies."""
    acc = ((1 << (2 * z)) - 1) // 3
    n = 1 << z
    d = 0
    s = n >> 1
    while s > 0:
        rx = 1 if x & s else 0
        ry = 1 if y & s else 0
        d += s * s * ((3 * rx) ^ ry)
        if ry == 0:
            if rx == 1:
                x, y = n - 1 - x, n - 1 - y
            x, y = y, x
        s >>= 1
    return acc + d


def archive() -> bytes:
    data = gzip.compress(TILE, mtime=0)
    tid = tile_id(ZOOM, TILE_X, TILE_Y)
    # One entry: id delta, run length, length, offset + 1.
    directory = gzip.compress(varint(1) + varint(tid) + varint(1) + varint(len(data)) + varint(1),
                              mtime=0)
    metadata = gzip.compress(json.dumps({
        "name": "Raqqa fixture",
        "attribution": "© OpenMapTiles © OpenStreetMap contributors",
        "vector_layers": [
            {"id": name, "fields": {}, "minzoom": ZOOM, "maxzoom": ZOOM}
            for name in ("transportation", "transportation_name", "place", "poi")
        ],
    }, ensure_ascii=False).encode("utf-8"), mtime=0)

    root_offset = 127
    meta_offset = root_offset + len(directory)
    data_offset = meta_offset + len(metadata)
    lat, lon = RAQQA
    header = b"PMTiles" + bytes([3]) + struct.pack(
        "<QQQQQQQQQQQ",
        root_offset, len(directory),
        meta_offset, len(metadata),
        data_offset, 0,  # no leaf directories
        data_offset, len(data),
        1, 1, 1,
    ) + bytes([1, 2, 2, 1, ZOOM, ZOOM]) + struct.pack(
        "<iiii", int((lon - 0.05) * 1e7), int((lat - 0.05) * 1e7),
        int((lon + 0.05) * 1e7), int((lat + 0.05) * 1e7),
    ) + bytes([ZOOM]) + struct.pack("<ii", int(lon * 1e7), int(lat * 1e7))
    assert len(header) == 127, len(header)
    return header + directory + metadata + data


def main() -> None:
    out = pathlib.Path(sys.argv[1] if len(sys.argv) > 1 else ".")
    out.mkdir(parents=True, exist_ok=True)
    (out / "syria.pmtiles").write_bytes(archive())
    print(f"fixture: {out / 'syria.pmtiles'} (tile {ZOOM}/{TILE_X}/{TILE_Y})")


if __name__ == "__main__":
    main()
