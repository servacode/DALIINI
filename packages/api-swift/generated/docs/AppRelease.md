# AppRelease

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**platform** | [**PushPlatformEnum**](PushPlatformEnum.md) |  | 
**minimumVersionCode** | **Int** | A build below this must stop and say so. Zero means nothing is blocked, which is what an unconfigured backend answers. | 
**latestVersionCode** | **Int** | A build below this may offer an update, but must keep working. | 
**storeUrl** | **String** | Where to get the newer build. Empty means show the notice without a button. | 
**noticeAr** | **String** | What the blocking screen says. Empty falls back to the app&#39;s own wording. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


