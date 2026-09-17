# SessionCredentials

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**accessToken** | **String** | Short-lived JOSE/JWT access token for the Authorization header. | 
**refreshToken** | **String** | Opaque rotating refresh secret. Store it in platform secure storage and replace it on every refresh; the server keeps only a digest. | 
**sessionId** | **UUID** |  | 
**expiresAt** | **Date** | Expiry of the refresh session. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


