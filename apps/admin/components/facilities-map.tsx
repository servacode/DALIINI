"use client";

import "maplibre-gl/dist/maplibre-gl.css";
import Link from "next/link";
import { useEffect, useRef } from "react";

import { useLookups } from "../lib/client/use-lookups";
import { useResource } from "../lib/client/use-resource";
import { useUrlFilters } from "../lib/client/use-url-filters";
import {
  EmptyState,
  ErrorState,
  FilterBar,
  LoadingState,
  PageHeader,
  StatusBadge,
  type Tone,
  termsFor,
} from "./ui";

/**
 * Every located facility the filters select, on the base map (DECISION-075).
 *
 * The filters are the list's own, in the address, so "these on a map" and "these in a table"
 * are one click apart. A dot's colour is its status; pressing one opens a card with the name
 * and a link to the record. The facilities that have no location yet are counted and linked,
 * because a map that silently leaves them out looks complete when it is not.
 *
 * The map is the platform's own style (the site's and the app's), configured by
 * `ADMIN_MAP_STYLE_URL`; without one the page says so and still gives the counts. MapLibre is
 * loaded only on this page, and its worker and Arabic shaping come from this console.
 */

type Point = Readonly<{
  id: string;
  nameAr: string;
  status: string;
  categoryId: string;
  latitude: number;
  longitude: number;
}>;

type MapData = Readonly<{ items: readonly Point[]; truncated: boolean; withoutLocation: number }>;

const STATUS = termsFor("facilityStatus");
const NUMBER = new Intl.NumberFormat("ar-SY");

const TONE_TOKENS: Record<Tone, [string, string]> = {
  positive: ["--ad-success", "#1f8a5b"],
  warning: ["--ad-warning", "#b7791f"],
  danger: ["--ad-danger", "#c0392b"],
  info: ["--ad-info", "#2563eb"],
  brand: ["--ad-brand", "#0f6b4f"],
  accent: ["--ad-accent", "#c9a227"],
  neutral: ["--ad-text-muted", "#6b7280"],
};

/** A token's colour as MapLibre can read it, or the fallback when it is not a plain colour. */
function tokenColor(tone: Tone): string {
  const [name, fallback] = TONE_TOKENS[tone];
  const value = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  return /^#[0-9a-f]{3,8}$/i.test(value) || /^rgba?\(/i.test(value) ? value : fallback;
}

export function FacilitiesMap({ styleUrl }: { styleUrl: string }) {
  const [filters, setFilters] = useUrlFilters({ status: "", province: "", category: "", issue: "" });
  const lookups = useLookups();
  const data = useResource<MapData>("facilitiesMap", filters);
  const points = data.data?.items ?? [];

  const counts = new Map<string, number>();
  for (const point of points) counts.set(point.status, (counts.get(point.status) ?? 0) + 1);
  const listQuery = new URLSearchParams(
    Object.entries(filters).filter(([key, value]) => value && key !== "issue"),
  );
  listQuery.set("issue", "NO_LOCATION");

  return (
    <div className="stack">
      <PageHeader
        title="خريطة المنشآت"
        description="المنشآت التي تختارها الفلاتر على الخريطة، بلون حالتها."
        actions={
          <Link className="button-ghost" href={`/facilities?${new URLSearchParams(Object.entries(filters).filter(([, v]) => v)).toString()}`}>
            عرضها كجدول
          </Link>
        }
      />
      <FilterBar
        fields={[
          {
            name: "status",
            label: "الحالة",
            type: "select",
            options: Object.entries(STATUS).map(([value, meta]) => ({ value, label: meta.label })),
          },
          lookups.provinceFilter,
          lookups.categoryFilter,
        ]}
        values={filters}
        onApply={(next) => setFilters({ ...next, issue: filters.issue ?? "" })}
      />
      {data.loading && !data.data ? <LoadingState /> : null}
      {data.error ? <ErrorState error={data.error} onRetry={data.reload} /> : null}
      {data.data ? (
        <>
          <div className="map-summary" data-testid="map-summary">
            <span>
              على الخريطة: <b className="tabular">{NUMBER.format(points.length)}</b>
              {data.data.truncated ? " (أول ٥٬٠٠٠ فقط، ضيّق الفلاتر لترى البقية)" : null}
            </span>
            <ul className="map-legend">
              {[...counts.entries()].map(([status, count]) => (
                <li key={status}>
                  <StatusBadge tone={STATUS[status]?.tone ?? "neutral"}>
                    {STATUS[status]?.label ?? status}
                  </StatusBadge>
                  <b className="tabular">{NUMBER.format(count)}</b>
                </li>
              ))}
            </ul>
            {data.data.withoutLocation > 0 ? (
              <Link href={`/facilities?${listQuery.toString()}`} data-testid="map-unlocated">
                بلا موقع: {NUMBER.format(data.data.withoutLocation)}، اعرضها لإضافة مواقعها
              </Link>
            ) : null}
          </div>
          {!styleUrl ? (
            <EmptyState
              title="لم تُضبط خريطة لهذه اللوحة"
              hint="تظهر الخريطة حين يُضبط عنوان نمطها في إعداد الخادم (ADMIN_MAP_STYLE_URL)، وهو نمط خريطة الموقع والتطبيق نفسه."
            />
          ) : points.length === 0 ? (
            <EmptyState title="لا منشآت بموقع تطابق الفلاتر" />
          ) : (
            <MapCanvas styleUrl={styleUrl} points={points} />
          )}
        </>
      ) : null}
    </div>
  );
}

function MapCanvas({ styleUrl, points }: { styleUrl: string; points: readonly Point[] }) {
  const box = useRef<HTMLDivElement>(null);
  const key = JSON.stringify(points);

  useEffect(() => {
    const container = box.current;
    if (!container) return;
    const pins = JSON.parse(key) as Point[];
    let map: import("maplibre-gl").Map | null = null;
    let cancelled = false;

    void import("maplibre-gl").then((maplibregl) => {
      if (cancelled) return;
      const vendor = `/vendor/maplibre-${maplibregl.getVersion()}`;
      maplibregl.setWorkerUrl(`${vendor}/maplibre-gl-worker.mjs`);
      if (maplibregl.getRTLTextPluginStatus() === "unavailable") {
        maplibregl.setRTLTextPlugin(`${vendor}/mapbox-gl-rtl-text.js`, true).catch(() => undefined);
      }
      const first = pins[0]!;
      map = new maplibregl.Map({
        container,
        style: styleUrl,
        center: [first.longitude, first.latitude],
        zoom: 12,
        attributionControl: { compact: true },
      });
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-left");
      map.on("error", () => {
        if (!map?.loaded()) container.dataset.failed = "true";
      });

      const colors = Object.fromEntries(
        Object.entries(STATUS).map(([status, meta]) => [status, tokenColor(meta.tone)]),
      );
      const ring = getComputedStyle(document.documentElement).getPropertyValue("--ad-surface").trim();

      map.on("load", () => {
        if (!map) return;
        map.addSource("facilities", {
          type: "geojson",
          data: {
            type: "FeatureCollection",
            features: pins.map((pin) => ({
              type: "Feature",
              geometry: { type: "Point", coordinates: [pin.longitude, pin.latitude] },
              properties: { id: pin.id, name: pin.nameAr, status: pin.status },
            })),
          },
        });
        map.addLayer({
          id: "facilities",
          type: "circle",
          source: "facilities",
          paint: {
            "circle-radius": ["interpolate", ["linear"], ["zoom"], 8, 4, 14, 7],
            "circle-color": [
              "match",
              ["get", "status"],
              ...Object.entries(colors).flat(),
              tokenColor("neutral"),
            ] as never,
            "circle-stroke-width": 1.5,
            "circle-stroke-color": /^#|^rgb/i.test(ring) ? ring : "#ffffff",
          },
        });
        map.on("mouseenter", "facilities", () => {
          if (map) map.getCanvas().style.cursor = "pointer";
        });
        map.on("mouseleave", "facilities", () => {
          if (map) map.getCanvas().style.cursor = "";
        });
        map.on("click", "facilities", (event) => {
          const feature = event.features?.[0];
          if (!map || !feature || feature.geometry.type !== "Point") return;
          const { id, name, status } = feature.properties as { id: string; name: string; status: string };
          // Built from text nodes, never markup: a facility's name is owner-supplied.
          const card = document.createElement("div");
          card.className = "map-card";
          const title = document.createElement("strong");
          title.textContent = name;
          const state = document.createElement("span");
          state.textContent = STATUS[status]?.label ?? status;
          const open = document.createElement("a");
          open.href = `/facilities/${encodeURIComponent(id)}`;
          open.textContent = "فتح السجل";
          card.append(title, state, open);
          new maplibregl.Popup({ closeButton: true, offset: 10 })
            .setLngLat(feature.geometry.coordinates as [number, number])
            .setDOMContent(card)
            .addTo(map);
        });
        if (pins.length > 1) {
          const bounds = new maplibregl.LngLatBounds();
          for (const pin of pins) bounds.extend([pin.longitude, pin.latitude]);
          map.fitBounds(bounds, { padding: 48, maxZoom: 15, duration: 0 });
        }
      });
    });

    return () => {
      cancelled = true;
      map?.remove();
    };
  }, [styleUrl, key]);

  return (
    <div
      ref={box}
      className="console-map"
      role="region"
      aria-label="خريطة المنشآت"
      data-testid="facilities-map"
    />
  );
}
