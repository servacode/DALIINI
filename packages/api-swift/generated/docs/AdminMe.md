# AdminMe

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**userId** | **UUID** |  | 
**displayName** | **String** |  | 
**mfa** | [**MfaStatus**](MfaStatus.md) | The second sign-in step for this operator and session (accountMfaRetrieve). | 
**permissions** | **[String]** | Every permission code the caller holds, deduplicated and sorted. An operator whose roles carry no permissions gets an empty list, which is a valid state. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


