import type { NextConfig } from "next";

/**
 * Static security headers.
 *
 * The Content Security Policy is not here. It needs a fresh nonce per request, which a
 * build-time header cannot carry, so `proxy.ts` sets it — together with HSTS on https
 * deployments. Two CSP headers would both be enforced, and a nonce-less one here would
 * block exactly the scripts the proxy's nonce exists to allow.
 */
const nextConfig: NextConfig = {
  poweredByHeader: false,
  transpilePackages: ["@servacode/design-tokens", "@servacode/api-typescript"],
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "same-origin" },
          { key: "X-Frame-Options", value: "DENY" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
        ],
      },
    ];
  },
};

export default nextConfig;
