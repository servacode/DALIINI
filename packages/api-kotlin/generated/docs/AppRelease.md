
# AppRelease

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **platform** | [**PushPlatformEnum**](PushPlatformEnum.md) |  |  |
| **minimumVersionCode** | **kotlin.Int** | A build below this must stop and say so. Zero means nothing is blocked, which is what an unconfigured backend answers. |  |
| **latestVersionCode** | **kotlin.Int** | A build below this may offer an update, but must keep working. |  |
| **storeUrl** | **kotlin.String** | Where to get the newer build. Empty means show the notice without a button. |  |
| **noticeAr** | **kotlin.String** | What the blocking screen says. Empty falls back to the app&#39;s own wording. |  |



