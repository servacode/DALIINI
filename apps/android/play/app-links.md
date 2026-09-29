# App Links: what the site must publish

The app opens the site's own https addresses. There is no custom scheme: a link Android has not
verified for the app opens the site in the browser, which is the right fallback.

| Link | Opens |
| --- | --- |
| `https://<host>/f/{facilityId}` | the facility's page |
| `https://<host>/duty`, `https://<host>/duty/today` | «المناوبات»: who is on duty, today / tomorrow / this week |
| `https://<host>/{provinceCode}` | Home, with that province chosen (`raqqa`, `damascus`, … — the 14 codes in `apps/backend/directory/reference_data/launch_v1.py`) |

Every other path (`/privacy`, `/faq`, `/owners`, `/search`, a province's category pages, …) is
not claimed and stays in the browser. The manifest lists the paths one by one for that reason,
and `DeepLinks.parse` (`core/model`, unit-tested) reads what arrives; anything else opens the app
as its icon would.

## The host

The host is a build setting, never committed:

| Flavour | Package | Property (Gradle property or environment variable) | Default |
| --- | --- | --- | --- |
| production | `com.servacode.directory` | `DIRECTORY_APP_LINK_HOST` | `root-domain.invalid` |
| staging | `com.servacode.directory.staging` | `DIRECTORY_STAGING_APP_LINK_HOST` | `staging.root-domain.invalid` |
| local | `com.servacode.directory.local` | `DIRECTORY_LOCAL_APP_LINK_HOST` | the staging host |

Set it to the host the site is served on — the value of the site's `NEXT_PUBLIC_ROOT_DOMAIN`,
without `https://` or a path. It is the host every link the site makes carries (sharing,
canonical addresses, «افتح في التطبيق»), and the app claims that one host only.

Its other form (`www.` or bare) is left out on purpose. Render, like most hosts, adds it and
redirects it to the main one, and a redirecting host cannot verify. Android 12 and later would
just skip it, but Android 7 to 11 (this app starts at Android 7) then verify none of the app's
links. A `www.` link that reaches the app some other way is still read as the same site.

The defaults are reserved names that never resolve (RFC 2606), so an unconfigured build claims
no real site, and `validatePlayRelease` refuses a production bundle that still has one.

## The file

The host must serve this at `/.well-known/assetlinks.json`: HTTP 200,
`Content-Type: application/json`, **no redirect** (Android does not follow one for this file),
reachable without cookies or a login. The site already does, from `NEXT_PUBLIC_ANDROID_PACKAGE`
and `ANDROID_CERT_SHA256` (`apps/web/app/.well-known/assetlinks.json/route.ts`); it answers 404
until both are set.

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.servacode.directory",
      "sha256_cert_fingerprints": [
        "<PLAY_APP_SIGNING_KEY_SHA256>",
        "<UPLOAD_KEY_SHA256>"
      ]
    }
  }
]
```

- `<PLAY_APP_SIGNING_KEY_SHA256>`: Play Console → the app → Test and release → App integrity →
  App signing → "App signing key certificate", SHA-256. This is the key installs from Play carry.
- `<UPLOAD_KEY_SHA256>`: the same page's "Upload key certificate", or
  `keytool -list -v -keystore "$ANDROID_UPLOAD_KEYSTORE_PATH" -alias "$ANDROID_UPLOAD_KEY_ALIAS"`.
  Only needed so a release signed locally with the upload key verifies too.

Fingerprints are colon-separated uppercase hex (`AB:CD:…`), as keytool prints them. They are
public; the keys are not, and nothing here needs them.

The staging site publishes the same file with `"package_name": "com.servacode.directory.staging"`
and the staging signing certificate. A local build is signed with the stable debug key
(`DIRECTORY_DEBUG_KEYSTORE_PATH`); add `com.servacode.directory.local` with that key's SHA-256 to
the host it points at, if links should verify on development phones.

## Checking

```bash
# What Google's verifier reads for the site.
curl -s "https://digitalassetlinks.googleapis.com/v1/statements:list?source.web.site=https://<host>&relation=delegate_permission/common.handle_all_urls"

# On a device with the build installed.
adb shell pm verify-app-links --re-verify com.servacode.directory
adb shell pm get-app-links com.servacode.directory     # the host should read "verified"
adb shell am start -a android.intent.action.VIEW -d "https://<host>/duty"
```

## The site's "open in the app"

The facility page's button, shown on Android only, is an intent for the page's own https
address addressed to the app (`apps/web/lib/config.ts`, `appOpenUrl`):
`intent://<host>/f/<id>#Intent;scheme=https;package=com.servacode.directory;S.browser_fallback_url=<the page>;end`.
With the app installed it opens the facility there; without it, the browser stays on the page.
