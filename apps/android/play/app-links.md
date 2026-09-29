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

Set it to the bare host the site is served on — the value of the site's `NEXT_PUBLIC_ROOT_DOMAIN`,
without `https://` or a path. The app claims that host and its `www.` form. The defaults are
reserved names that never resolve (RFC 2606), so an unconfigured build claims no real site, and
`validatePlayRelease` refuses a production bundle that still has one.

## The file

Both hosts — `<host>` and `www.<host>` — must serve this at `/.well-known/assetlinks.json`:
HTTP 200, `Content-Type: application/json`, **no redirect** (Android does not follow one for this
file), reachable without cookies or a login. Android 12+ verifies each host on its own; Android
6–11 needs every host in the filter to verify, so a `www.` that only redirects breaks those.

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
adb shell pm get-app-links com.servacode.directory     # each host should read "verified"
adb shell am start -a android.intent.action.VIEW -d "https://<host>/duty"
```

## The site's "open in the app"

The facility page's button builds an intent for a `daliini://facility/<id>` scheme
(`apps/web/lib/config.ts`, `appOpenUrl`). The app declares no such scheme, so that button should
link to the facility's own https address instead — which opens the app once verified and the
page otherwise — or, to force the app with a store fallback:
`intent://<host>/f/<id>#Intent;scheme=https;package=com.servacode.directory;S.browser_fallback_url=<encoded fallback>;end`.
