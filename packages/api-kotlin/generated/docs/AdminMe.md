
# AdminMe

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **userId** | [**java.util.UUID**](java.util.UUID.md) |  |  |
| **displayName** | **kotlin.String** |  |  |
| **mfa** | [**MfaStatus**](MfaStatus.md) | The second sign-in step for this operator and session (accountMfaRetrieve). |  |
| **permissions** | **kotlin.collections.List&lt;kotlin.String&gt;** | Every permission code the caller holds, deduplicated and sorted. An operator whose roles carry no permissions gets an empty list, which is a valid state. |  |



