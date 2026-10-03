"use client";

import "maplibre-gl/dist/maplibre-gl.css";
import { useEffect, useMemo, useRef } from "react";
import { publicConfig } from "../lib/config";
import type { MapPoint } from "../lib/map-points";

/**
 * Facilities on the base map, as pins that lead to their pages (DECISION-071).
 *
 * The map is the app's own style, and it appears only where one is configured
 * (NEXT_PUBLIC_MAP_STYLE_URL); without one this renders nothing. It never stands alone: every pin is
 * a facility that is also in the list or on the page beside it, so a reader who cannot use a map
 * (a screen reader, an old phone, a slow connection) loses nothing.
 *
 * MapLibre is loaded only when a map is actually shown, so the pages that have none, and the
 * moment before this one scrolls into view, carry none of its weight. Its worker and the Arabic
 * text shaping come from this site (`public/vendor`, `scripts/vendor-map.mjs`), not from a CDN.
 * One finger scrolls the page and two move the map, so a map in the middle of a page never traps
 * a thumb that was only scrolling past it.
 */

export function FacilityMap({
  points,
  label,
  zoom = 15,
  className = "",
}: {
  points: readonly MapPoint[];
  label: string;
  zoom?: number;
  className?: string;
}) {
  const box = useRef<HTMLDivElement>(null);
  const style = publicConfig.mapStyleUrl;
  // The pins as a value, so a parent that renders again with the same pins does not rebuild the map.
  const key = JSON.stringify(points);
  const pins = useMemo(() => JSON.parse(key) as MapPoint[], [key]);

  useEffect(() => {
    if (!style || !box.current || pins.length === 0) return;
    const container = box.current;
    let map: import("maplibre-gl").Map | null = null;
    let cancelled = false;

    void import("maplibre-gl").then((maplibregl) => {
      if (cancelled) return;
      const vendor = `/vendor/maplibre-${maplibregl.getVersion()}`;
      maplibregl.setWorkerUrl(`${vendor}/maplibre-gl-worker.mjs`);
      if (maplibregl.getRTLTextPluginStatus() === "unavailable") {
        // Lazy: fetched the first time a label in Arabic is drawn, not before.
        maplibregl.setRTLTextPlugin(`${vendor}/mapbox-gl-rtl-text.js`, true).catch(() => undefined);
      }

      const first = pins[0]!;
      map = new maplibregl.Map({
        container,
        style,
        center: [first.longitude, first.latitude],
        zoom,
        attributionControl: { compact: true },
        cooperativeGestures: true,
      });
      map.addControl(new maplibregl.NavigationControl({ showCompass: false }), "top-left");
      // A style or tile that cannot be fetched (the map host down, a request cut short as the
      // page is left) is not the reader's problem: before the map has drawn, the map steps aside
      // and the list beside it carries on; after, a missing tile is just a blank square. Handling
      // the event is also what keeps MapLibre from logging every such failure as an error.
      map.on("error", () => {
        if (!map?.loaded()) container.dataset.failed = "true";
      });

      for (const pin of pins) {
        const element = document.createElement("a");
        element.href = pin.href;
        element.className = `map-pin map-pin-${pin.state.toLowerCase().replace("_", "-")}`;
        element.title = pin.name;
        element.setAttribute("aria-label", pin.name);
        new maplibregl.Marker({ element, anchor: "bottom" })
          .setLngLat([pin.longitude, pin.latitude])
          .addTo(map);
      }

      if (pins.length > 1) {
        const bounds = new maplibregl.LngLatBounds();
        for (const pin of pins) bounds.extend([pin.longitude, pin.latitude]);
        map.fitBounds(bounds, { padding: 56, maxZoom: zoom, duration: 0 });
      }
    });

    return () => {
      cancelled = true;
      map?.remove();
    };
  }, [style, pins, zoom]);

  if (!style || pins.length === 0) return null;
  return <div ref={box} className={`facility-map ${className}`} role="region" aria-label={label} />;
}
