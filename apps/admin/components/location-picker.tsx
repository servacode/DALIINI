"use client";

import "maplibre-gl/dist/maplibre-gl.css";

import { useEffect, useEffectEvent, useRef, useState } from "react";

import { Icon } from "./icons";

export type Point = Readonly<{ latitude: number; longitude: number }>;

/** Raqqa's centre: the launch province, where a new facility most likely is. */
const RAQQA: Point = { latitude: 35.9506, longitude: 39.0094 };

/**
 * Where a facility is, chosen on the map rather than typed as two numbers (DECISION-109).
 *
 * Press the map to drop the pin there, drag it to correct it; the coordinates show under the map
 * for whoever wants to check them against another source. The map is the platform's own style
 * (`ADMIN_MAP_STYLE_URL`), the same one the site and the app draw, so the operator places the pin
 * on the streets the public will see it on.
 *
 * With no map configured the picker falls back to the two fields it replaced, so adding a
 * facility never stops at a blank square.
 */
export function LocationPicker({
  styleUrl,
  value,
  onChange,
  invalid,
}: {
  styleUrl: string;
  value: Point | null;
  onChange: (next: Point | null) => void;
  invalid?: boolean;
}) {
  const box = useRef<HTMLDivElement>(null);
  const marker = useRef<import("maplibre-gl").Marker | null>(null);
  const map = useRef<import("maplibre-gl").Map | null>(null);
  const [failed, setFailed] = useState(false);
  // The first value only: the map is built once, and later moves go through the marker.
  const [start] = useState(value);
  const place = useEffectEvent((next: Point) => onChange(next));

  useEffect(() => {
    const container = box.current;
    if (!container || !styleUrl) return;
    let cancelled = false;

    void import("maplibre-gl").then((maplibregl) => {
      if (cancelled) return;
      const vendor = `/vendor/maplibre-${maplibregl.getVersion()}`;
      maplibregl.setWorkerUrl(`${vendor}/maplibre-gl-worker.mjs`);
      if (maplibregl.getRTLTextPluginStatus() === "unavailable") {
        maplibregl.setRTLTextPlugin(`${vendor}/mapbox-gl-rtl-text.js`, true).catch(() => undefined);
      }
      const centre = start ?? RAQQA;
      const instance = new maplibregl.Map({
        container,
        style: styleUrl,
        center: [centre.longitude, centre.latitude],
        zoom: start ? 16 : 13,
        attributionControl: { compact: true },
      });
      map.current = instance;
      instance.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-left");
      instance.on("error", () => {
        if (!instance.loaded()) setFailed(true);
      });

      const pin = document.createElement("div");
      pin.className = "location-pin";
      pin.innerHTML =
        '<svg viewBox="0 0 32 42" width="32" height="42" aria-hidden="true"><path d="M16 1C7.7 1 1 7.6 1 15.8 1 27 16 41 16 41s15-14 15-25.2C31 7.6 24.3 1 16 1z" fill="currentColor" stroke="#fff" stroke-width="2"/><circle cx="16" cy="15.5" r="5.5" fill="#fff"/></svg>';
      const drop = (lng: number, lat: number) => {
        const rounded = { latitude: round(lat), longitude: round(lng) };
        if (!marker.current) {
          marker.current = new maplibregl.Marker({ element: pin, draggable: true, anchor: "bottom" })
            .setLngLat([rounded.longitude, rounded.latitude])
            .addTo(instance);
          marker.current.on("dragend", () => {
            const at = marker.current!.getLngLat();
            place({ latitude: round(at.lat), longitude: round(at.lng) });
          });
        } else {
          marker.current.setLngLat([rounded.longitude, rounded.latitude]);
        }
        place(rounded);
      };
      if (start) drop(start.longitude, start.latitude);
      instance.on("click", (event) => drop(event.lngLat.lng, event.lngLat.lat));
    });

    return () => {
      cancelled = true;
      marker.current = null;
      map.current?.remove();
      map.current = null;
    };
  }, [styleUrl, start]);

  // Removing the location removes the pin; the map stays where it is.
  useEffect(() => {
    if (value === null && marker.current) {
      marker.current.remove();
      marker.current = null;
    }
  }, [value]);

  if (!styleUrl || failed) {
    return <TypedPoint value={value} onChange={onChange} invalid={invalid} noMap={!styleUrl} />;
  }

  return (
    <div className="location-picker" data-invalid={invalid || undefined}>
      <div className="location-map" ref={box} data-testid="location-map" />
      <div className="location-bar">
        <span className="location-hint">
          <Icon name="mapPin" width={14} height={14} />
          {value ? "اسحب الدبوس لتصحيح المكان" : "اضغط على الخريطة لتضع الدبوس"}
        </span>
        {value ? (
          <>
            <span className="location-coords" dir="ltr" data-testid="location-coords">
              {value.latitude.toFixed(5)}, {value.longitude.toFixed(5)}
            </span>
            <button type="button" className="button-link" onClick={() => onChange(null)}>
              إزالة الموقع
            </button>
          </>
        ) : null}
      </div>
    </div>
  );
}

/** Five decimals is about a metre: as exact as a pin dropped by hand can honestly be. */
function round(value: number): number {
  return Math.round(value * 1e5) / 1e5;
}

/** The two fields the map replaced, for a console with no map configured. */
function TypedPoint({
  value,
  onChange,
  invalid,
  noMap,
}: {
  value: Point | null;
  onChange: (next: Point | null) => void;
  invalid?: boolean;
  noMap: boolean;
}) {
  const [lat, setLat] = useState(value ? String(value.latitude) : "");
  const [lng, setLng] = useState(value ? String(value.longitude) : "");
  const push = (nextLat: string, nextLng: string) => {
    const a = Number(nextLat);
    const b = Number(nextLng);
    if (nextLat.trim() && nextLng.trim() && !Number.isNaN(a) && !Number.isNaN(b)) {
      onChange({ latitude: a, longitude: b });
    } else if (!nextLat.trim() && !nextLng.trim()) {
      onChange(null);
    }
  };
  return (
    <div className="stack-tight">
      <p className="field-hint">
        {noMap
          ? "لا خريطة مُعدّة لهذه اللوحة. انسخ الإحداثيتين من أي خريطة: اضغط مطولاً على المكان."
          : "تعذّر تحميل الخريطة. أدخل الإحداثيتين بيدك."}
      </p>
      <div className="form-grid">
        <label className="field">
          <span>خط العرض</span>
          <input
            dir="ltr"
            value={lat}
            placeholder="35.9506"
            aria-invalid={invalid}
            onChange={(event) => {
              setLat(event.target.value);
              push(event.target.value, lng);
            }}
          />
        </label>
        <label className="field">
          <span>خط الطول</span>
          <input
            dir="ltr"
            value={lng}
            placeholder="39.0094"
            aria-invalid={invalid}
            onChange={(event) => {
              setLng(event.target.value);
              push(lat, event.target.value);
            }}
          />
        </label>
      </div>
    </div>
  );
}
