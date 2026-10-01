
# Availability

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **state** | [**AvailabilityStateEnum**](AvailabilityStateEnum.md) |  |  |
| **nextOpenAt** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) |  |  |
| **isOpenNow** | **kotlin.Boolean** | Whether the doors are open at this moment, by the facility&#39;s own business hours and temporary closures. Independent of duty: unlike &#x60;state&#x60;, which collapses both into one value and lets DUTY win, this stays true for a facility that is open while its duty shift runs. |  |
| **isOnDutyToday** | **kotlin.Boolean** | Whether the facility appears on today&#39;s duty roster, taking today to be the local day in Asia/Damascus. A different question from being open: a pharmacy on tonight&#39;s roster is on duty today from midnight, hours before it opens. False simply means it is not on the roster; clients must not render that as a badge of its own. |  |



