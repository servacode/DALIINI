
# AdminTaskReports

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **count** | **kotlin.Int** | OPEN reports. |  |
| **facilityCount** | **kotlin.Int** | Facilities with an OPEN report. |  |
| **overdueCount** | **kotlin.Int** | OPEN reports older than the SLA. |  |
| **oldest** | [**kotlin.collections.List&lt;AdminTaskReportGroup&gt;**](AdminTaskReportGroup.md) | Up to 10 facilities: those with 2 or more open reports first, then oldest. |  |



