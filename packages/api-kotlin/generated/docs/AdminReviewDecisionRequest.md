
# AdminReviewDecisionRequest

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **reason** | **kotlin.String** | Required in practice for a rejection; recorded in the audit trail. |  [optional] |
| **revision** | **kotlin.Int** | CHANGE only: the &#x60;revision&#x60; the reviewer saw. If the owner revised the proposal since, approval is refused with 409 APPLICATION_CHANGED. |  [optional] |



