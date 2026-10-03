// The public read paths at launch scale (DECISION-083).
//
//   k6 run -e API=https://api.<ROOT> infrastructure/load/public.js
//
// Each virtual user is somebody looking for a pharmacy: Home for their province, a section's
// list, the next page, one facility, the map around them, a search, and tonight's duty roster,
// with a few seconds between screens. The thresholds are per screen, so a slow map cannot hide
// behind fast lists.
//
// Settings, all optional: VUS (default 40), HOLD (default 60s), PROVINCE (code, default raqqa),
// HOSTS (`name=ip,name=ip`, to reach a stack by its public names without DNS), INSECURE=1 (a
// local authority's certificate, as in CI). Run it against staging or CI's throwaway stack;
// against production only by agreement, and never with the rate limits raised there.
import http from "k6/http";
import { check, group, sleep } from "k6";

const API = (__ENV.API || "").replace(/\/$/, "");
if (!API) throw new Error("set API, for example -e API=https://api.staging.example.org");
const VUS = Number(__ENV.VUS || 40);
const HOLD = __ENV.HOLD || "60s";
const PROVINCE = __ENV.PROVINCE || "raqqa";
const TERMS = ["الشفاء", "النور", "صيدلية", "مخبر", "الفرات", "عيادة الأمل"];

const hosts = {};
for (const pair of (__ENV.HOSTS || "").split(",").filter(Boolean)) {
  const [name, ip] = pair.split("=");
  hosts[name] = ip;
}

export const options = {
  hosts,
  insecureSkipTLSVerify: __ENV.INSECURE === "1",
  scenarios: {
    visitors: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: [
        { duration: "20s", target: VUS },
        { duration: HOLD, target: VUS },
        { duration: "10s", target: 0 },
      ],
      gracefulRampDown: "10s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.01"],
    checks: ["rate>0.99"],
    "http_req_duration{screen:home}": ["p(95)<800"],
    "http_req_duration{screen:list}": ["p(95)<600"],
    "http_req_duration{screen:detail}": ["p(95)<500"],
    "http_req_duration{screen:map}": ["p(95)<1000"],
    "http_req_duration{screen:search}": ["p(95)<800"],
    "http_req_duration{screen:duty}": ["p(95)<600"],
    http_req_duration: ["p(99)<2000"],
  },
  summaryTrendStats: ["avg", "med", "p(90)", "p(95)", "p(99)", "max"],
};

const JSON_ACCEPT = { headers: { Accept: "application/json" } };

function get(path, screen) {
  const response = http.get(`${API}/api/v1/public${path}`, { ...JSON_ACCEPT, tags: { screen } });
  check(response, { [`${screen} answers 200`]: (r) => r.status === 200 });
  return response;
}

// What a phone learns once, before it starts browsing: the province and its sections.
export function setup() {
  const provinces = http.get(`${API}/api/v1/public/provinces/`, JSON_ACCEPT).json("items") || [];
  const province = provinces.find((p) => p.code === PROVINCE) || provinces[0];
  if (!province) throw new Error("no public province: seed the load data first");
  const categories =
    http.get(`${API}/api/v1/public/provinces/${province.id}/categories/`, JSON_ACCEPT).json("items") || [];
  if (categories.length === 0) throw new Error(`no public sections in ${province.code}`);
  return {
    province: province.id,
    categories: categories.map((c) => c.id),
    centre: [province.mapCenter.latitude, province.mapCenter.longitude],
  };
}

function pick(items) {
  return items[Math.floor(Math.random() * items.length)];
}

export default function visit(data) {
  const province = data.province;
  const [lat, lon] = data.centre;
  group("home", () => {
    get(`/home/?provinceId=${province}`, "home");
  });
  sleep(1 + Math.random() * 2);

  let facilityId = null;
  group("list", () => {
    const category = pick(data.categories);
    const near = Math.random() < 0.5 ? `&latitude=${lat}&longitude=${lon}` : "";
    const first = get(`/facilities/?provinceId=${province}&categoryId=${category}${near}`, "list");
    const items = first.status === 200 ? first.json("items") || [] : [];
    if (items.length) facilityId = pick(items).id;
    const next = first.status === 200 ? first.json("nextCursor") : null;
    if (next && Math.random() < 0.4) {
      const cursor = encodeURIComponent(next);
      get(`/facilities/?provinceId=${province}&categoryId=${category}${near}&cursor=${cursor}`, "list");
    }
  });
  sleep(1 + Math.random() * 2);

  if (facilityId) {
    group("detail", () => {
      get(`/facilities/${facilityId}/`, "detail");
    });
    sleep(1 + Math.random() * 3);
  }

  group("map", () => {
    const dLat = 0.03 + Math.random() * 0.05;
    const dLon = 0.04 + Math.random() * 0.06;
    const bbox = [lon - dLon, lat - dLat, lon + dLon, lat + dLat].map((v) => v.toFixed(5)).join(",");
    get(`/map/facilities/?provinceId=${province}&bbox=${bbox}`, "map");
  });
  sleep(1 + Math.random() * 2);

  if (Math.random() < 0.6) {
    group("search", () => {
      get(`/search/?provinceId=${province}&q=${encodeURIComponent(pick(TERMS))}`, "search");
    });
    sleep(1 + Math.random() * 2);
  }

  if (Math.random() < 0.5) {
    group("duty", () => {
      get(`/duty/?provinceId=${province}`, "duty");
    });
    sleep(1 + Math.random() * 2);
  }
}
