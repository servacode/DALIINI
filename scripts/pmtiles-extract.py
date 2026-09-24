"""Unpack a PMTiles v3 archive into plain z/x/y tiles.

MapLibre Android speaks XYZ over HTTP and knows nothing of the pmtiles:// scheme, which the
archive's own style uses. Rather than teach the app a new protocol, the archive is opened once
here and written out as the files every map renderer already understands.

The tiles stay gzip-compressed exactly as they are stored; whoever serves them says so with
Content-Encoding, which is what the format intends and what keeps them small on the wire.

Format reference: PMTiles v3 — 127-byte header, varint directories, Hilbert-ordered tile ids.
"""

import argparse
import gzip
import json
import pathlib
import struct
import sys


def varint(buf, pos):
    result = shift = 0
    while True:
        byte = buf[pos]
        pos += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, pos
        shift += 7


def read_header(data):
    if data[:7] != b"PMTiles":
        raise SystemExit("not a PMTiles archive")
    if data[7] != 3:
        raise SystemExit(f"only version 3 is understood, found {data[7]}")
    f = struct.unpack_from("<QQQQQQQQQQQ", data, 8)
    return {
        "root_offset": f[0], "root_length": f[1],
        "meta_offset": f[2], "meta_length": f[3],
        "leaf_offset": f[4], "leaf_length": f[5],
        "data_offset": f[6], "data_length": f[7],
        "addressed": f[8], "entries": f[9], "contents": f[10],
        "clustered": data[96], "internal_compression": data[97],
        "tile_compression": data[98], "tile_type": data[99],
        "min_zoom": data[100], "max_zoom": data[101],
    }


def decompress(blob, kind):
    """1 = none, 2 = gzip. The archives here use gzip for directories and tiles alike."""
    return gzip.decompress(blob) if kind == 2 else blob


def read_directory(blob):
    """Entries come as four columns: id deltas, run lengths, lengths, then offsets."""
    pos = 0
    count, pos = varint(blob, pos)
    ids, last = [], 0
    for _ in range(count):
        delta, pos = varint(blob, pos)
        last += delta
        ids.append(last)
    runs = []
    for _ in range(count):
        value, pos = varint(blob, pos)
        runs.append(value)
    lengths = []
    for _ in range(count):
        value, pos = varint(blob, pos)
        lengths.append(value)
    offsets = []
    for i in range(count):
        value, pos = varint(blob, pos)
        # Zero means "immediately after the one before", which is how a clustered archive
        # avoids repeating an offset for every tile in a run.
        offsets.append(offsets[i - 1] + lengths[i - 1] if value == 0 and i > 0 else value - 1)
    return [
        {"id": ids[i], "run": runs[i], "length": lengths[i], "offset": offsets[i]}
        for i in range(count)
    ]


def tile_id_to_zxy(tile_id):
    """Hilbert order, as the format specifies: zoom by accumulation, then the curve."""
    acc, z = 0, 0
    while True:
        tiles = (1 << z) * (1 << z)
        if acc + tiles > tile_id:
            break
        acc += tiles
        z += 1
    position = tile_id - acc
    x = y = 0
    side = 1 << z
    s = 1
    while s < side:
        rx = 1 & (position // 2)
        ry = 1 & (position ^ rx)
        if ry == 0:
            if rx == 1:
                x, y = s - 1 - x, s - 1 - y
            x, y = y, x
        x += s * rx
        y += s * ry
        position //= 4
        s *= 2
    return z, x, y


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("archive")
    parser.add_argument("out")
    parser.add_argument("--max-zoom", type=int, default=None)
    args = parser.parse_args()

    data = pathlib.Path(args.archive).read_bytes()
    header = read_header(data)
    print(f"  zoom {header['min_zoom']}..{header['max_zoom']}  "
          f"entries={header['entries']}  tile_type={header['tile_type']}  "
          f"compression={header['tile_compression']}")

    meta = decompress(
        data[header["meta_offset"]:header["meta_offset"] + header["meta_length"]],
        header["internal_compression"],
    )
    out = pathlib.Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    (out / "metadata.json").write_bytes(meta)

    root = read_directory(
        decompress(data[header["root_offset"]:header["root_offset"] + header["root_length"]],
                   header["internal_compression"])
    )

    written = skipped = 0
    stack = list(root)
    while stack:
        entry = stack.pop()
        if entry["run"] == 0:
            # A leaf directory: its entries live in the leaf section.
            start = header["leaf_offset"] + entry["offset"]
            stack.extend(read_directory(
                decompress(data[start:start + entry["length"]], header["internal_compression"])
            ))
            continue
        for step in range(entry["run"]):
            z, x, y = tile_id_to_zxy(entry["id"] + step)
            if args.max_zoom is not None and z > args.max_zoom:
                skipped += 1
                continue
            start = header["data_offset"] + entry["offset"]
            blob = data[start:start + entry["length"]]
            path = out / "tiles" / str(z) / str(x)
            path.mkdir(parents=True, exist_ok=True)
            (path / f"{y}.pbf").write_bytes(blob)
            written += 1

    print(f"  wrote {written} tiles" + (f", skipped {skipped} above the zoom cap" if skipped else ""))
    try:
        print("  metadata:", json.dumps(json.loads(meta), ensure_ascii=False)[:200])
    except Exception:
        pass


if __name__ == "__main__":
    sys.exit(main())
