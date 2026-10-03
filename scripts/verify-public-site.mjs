/*
 * The public site, against a backend that is actually running.
 *
 * The site had unit tests and nothing else: no page of it had ever been fetched from a real
 * server by an automated check. That leaves the part a visitor meets entirely unverified —
 * a route that 500s on real data, a page that renders empty because a field was renamed, a
 * link that points nowhere. None of it is visible to a unit test, and all of it is visible
 * to the first person who opens the site.
 *
 * This fetches every public route against a live stack and asserts three things per page: it
 * answers 200, it carries the words that page exists to show, and it carries no error text.
 * Pages are server-rendered, so their content is in the HTML — which is what makes an HTTP
 * check meaningful here rather than a formality.
 *
 * What it deliberately does NOT claim: that the page hydrates. Only a browser can say that,
 * and this says so rather than implying otherwise.
 *
 *   node scripts/verify-public-site.mjs http://127.0.0.1:3013
 */

const base = (process.argv[2] ?? "http://127.0.0.1:3000").replace(/\/$/, "");

/** A page, and a word it must carry if it rendered what it is for. */
const PAGES = [
  { path: "/", must: ["دليني"] },
  { path: "/duty", must: ["المناوب"] },
  { path: "/duty/today", must: ["اليوم"] },
  { path: "/duty/tomorrow", must: ["غدًا", "الغد"] },
  { path: "/duty/week", must: ["الأسبوع"] },
  { path: "/emergency", must: ["الطوارئ"] },
  { path: "/search", must: ["بحث"] },
  { path: "/owners", must: ["منشأ"] },
  { path: "/how-we-verify", must: ["التحقّق", "التحقق"] },
  { path: "/faq", must: ["الأسئلة"] },
  { path: "/contact", must: ["تواصل", "اتصل"] },
  { path: "/support", must: ["الدعم", "مساعدة"] },
  { path: "/privacy", must: ["الخصوصية"] },
  { path: "/terms", must: ["الشروط"] },
  { path: "/delete-account", must: ["حذف"] },
  { path: "/robots.txt", must: ["User-Agent", "User-agent"] },
  { path: "/sitemap.xml", must: ["<urlset", "<?xml"] },
  { path: "/manifest.webmanifest", must: ["name"] },
];

/* Text that means the page failed even though it answered 200. */
const FAILURE_TEXT = [
  "Application error",
  "Internal Server Error",
  "This page could not be found",
  "حدث خطأ",
];

let failures = 0;
const rows = [];

function record(path, status, detail) {
  rows.push({ path, status, detail });
  if (status === "FAIL") failures += 1;
  console.log(`${status.padEnd(5)} ${path.padEnd(26)} ${detail}`);
}

for (const page of PAGES) {
  let response;
  try {
    response = await fetch(`${base}${page.path}`, { redirect: "follow" });
  } catch (error) {
    record(page.path, "FAIL", `unreachable: ${error.message}`);
    continue;
  }
  if (!response.ok) {
    record(page.path, "FAIL", `answered ${response.status}`);
    continue;
  }
  const body = await response.text();
  const broke = FAILURE_TEXT.find((text) => body.includes(text));
  if (broke) {
    record(page.path, "FAIL", `renders "${broke}"`);
    continue;
  }
  // Any one of the expected words is enough: several of these pages word the same idea
  // differently, and pinning the exact phrase would make this a copy test.
  if (!page.must.some((word) => body.includes(word))) {
    record(page.path, "FAIL", `missing all of: ${page.must.join(" / ")}`);
    continue;
  }
  record(page.path, "PASS", `${response.status}, ${(body.length / 1024).toFixed(0)}KB`);
}

/* A province page, named by what the backend actually has rather than by a guess. */
try {
  const api = process.env.PUBLIC_API_ORIGIN ?? "";
  if (api) {
    const provinces = await fetch(`${api}/api/v1/public/provinces/`).then((r) => r.json());
    // The route addresses a province by its stable `code` ("raqqa"), not by its Arabic name:
    // a code survives a rename, and a URL that changes when an editor fixes a spelling is a
    // URL somebody already shared.
    const slug = provinces.items?.[0]?.code;
    if (slug) {
      const path = `/${encodeURIComponent(slug)}`;
      const response = await fetch(`${base}${path}`);
      const body = response.ok ? await response.text() : "";
      const ok = response.ok && !FAILURE_TEXT.some((t) => body.includes(t));
      record(path, ok ? "PASS" : "FAIL", ok ? "a real province from the API" : `answered ${response.status}`);

      // Its first category, by slug, and a facility listed in it. The facility is asked for by
      // id alone, as every link shared before slugs was: it must arrive at its readable address.
      const province = provinces.items[0];
      const categories = await fetch(`${api}/api/v1/public/provinces/${province.id}/categories/`).then((r) => r.json());
      const category = categories.items?.[0];
      if (category?.slug) {
        const listPath = `/${encodeURIComponent(slug)}/${encodeURIComponent(category.slug)}`;
        const list = await fetch(`${base}${listPath}`);
        const listBody = list.ok ? await list.text() : "";
        const listOk = list.ok && !FAILURE_TEXT.some((t) => listBody.includes(t));
        record(listPath, listOk ? "PASS" : "FAIL", listOk ? "a category, by its slug" : `answered ${list.status}`);

        const facilities = await fetch(
          `${api}/api/v1/public/facilities/?provinceId=${province.id}&categoryId=${category.id}&limit=1`,
        ).then((r) => r.json());
        const facility = facilities.items?.[0];
        if (facility) {
          const short = `/f/${facility.id}`;
          const reached = await fetch(`${base}${short}`, { redirect: "follow" });
          const landed = new URL(reached.url).pathname;
          const wanted = facility.slug ? `${short}/${encodeURIComponent(facility.slug)}` : short;
          const ok = reached.ok && landed === wanted;
          record(short, ok ? "PASS" : "FAIL", ok ? "redirected to its readable address" : `landed on ${landed} (${reached.status})`);
        }
      }
    }
  }
} catch (error) {
  record("/<province>", "FAIL", `could not ask the API: ${error.message}`);
}

console.log("");
console.log(`passed=${rows.length - failures}  failed=${failures}  of ${rows.length}`);
console.log("this checks what the server sends, not that the page hydrates; a browser says that.");
process.exit(failures > 0 ? 1 : 0);
