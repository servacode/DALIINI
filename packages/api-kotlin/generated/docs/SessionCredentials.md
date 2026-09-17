
# SessionCredentials

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **accessToken** | **kotlin.String** | Short-lived JOSE/JWT access token for the Authorization header. |  |
| **refreshToken** | **kotlin.String** | Opaque rotating refresh secret. Store it in platform secure storage and replace it on every refresh; the server keeps only a digest. |  |
| **sessionId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **expiresAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | Expiry of the refresh session. |  |



