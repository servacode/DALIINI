
# ApiError

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **code** | **kotlin.String** | Stable machine-readable code. Clients branch on this and never on &#x60;message&#x60;. Transport codes are VALIDATION_ERROR, AUTHENTICATION_REQUIRED, AUTHENTICATION_FAILED, PERMISSION_DENIED, NOT_FOUND, METHOD_NOT_ALLOWED, NOT_ACCEPTABLE, UNSUPPORTED_MEDIA_TYPE, THROTTLED and INTERNAL_ERROR; domain codes such as DUTY_OVERLAP_OR_INVALID are documented on the operations that raise them. |  |
| **message** | **kotlin.String** | Human-readable Arabic message, safe to display. Never parsed by clients. |  |
| **details** | **kotlin.collections.Map&lt;kotlin.String, kotlin.collections.List&lt;kotlin.String&gt;&gt;** | Field-scoped messages keyed by request field path. Nested paths are joined with dots and list indices are bracketed, for example &#x60;contacts[1].phone&#x60;. A message with no field of its own appears under &#x60;nonFieldErrors&#x60;. Empty when the error is not field-scoped. |  |
| **requestId** | **kotlin.String** | The request correlation id, identical to the &#x60;X-Request-ID&#x60; response header and to the id recorded in the server logs. Quote it in a support report. |  |



