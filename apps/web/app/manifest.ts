import type { MetadataRoute } from "next";
import { SITE_NAME } from "../lib/config";

/**
 * What a phone needs to keep the site.
 *
 * Most of the people this is for reach it on a phone, over a connection they pay for, and many of
 * them will not install an app for one pharmacy. Without this file the browser has nothing to add
 * to a home screen with: no name, no icon, no colours — "Add to Home Screen" produces a grey
 * square labelled with the URL, which nobody taps twice.
 *
 * It is not an offline mode and does not pretend to be one; there is no service worker here. It
 * is the smaller, honest thing: a name, the brand's own icon, and the bar's green so the phone's
 * own chrome stops being white above a dark green page.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: `${SITE_NAME} — دليلك المحلي`,
    short_name: SITE_NAME,
    description: "دليل محلي لمعلومات واضحة وقابلة للتحقق عن المنشآت والخدمات الصحية.",
    lang: "ar",
    dir: "rtl",
    start_url: "/",
    scope: "/",
    display: "standalone",
    orientation: "portrait",
    background_color: "#f7f7f5",
    theme_color: "#042623",
    icons: [
      { src: "/icon.png", sizes: "512x512", type: "image/png", purpose: "any" },
      { src: "/apple-icon.png", sizes: "180x180", type: "image/png" },
    ],
  };
}
