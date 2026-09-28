import { ImageResponse } from "next/og";

/*
 * Default link-preview image for every page (WhatsApp, Facebook, X). The
 * bundled OG renderer has no Arabic font and the repo ships none, so the
 * image uses the brand mark and Latin wordmark only; Arabic titles still
 * appear in the preview text from og:title/og:description.
 */
export const alt = "Daliini";
export const size = { width: 1200, height: 630 };
export const contentType = "image/png";

export default function OpengraphImage() {
  return new ImageResponse(
    (
      <div
        style={{
          width: "100%",
          height: "100%",
          display: "flex",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          gap: 36,
          background: "#F7F8F5",
        }}
      >
        <div style={{ width: 180, height: 180, borderRadius: 44, background: "#0B6B47", display: "flex" }} />
        <div style={{ fontSize: 96, fontWeight: 700, color: "#043526" }}>Daliini</div>
      </div>
    ),
    size,
  );
}
