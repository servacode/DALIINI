
# AdminAnalytics

## Properties
| Name | Type | Description | Notes |
| ------------ | ------------- | ------------- | ------------- |
| **from** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | Period start, inclusive. |  |
| **to** | [**java.time.OffsetDateTime**](java.time.OffsetDateTime.md) | Period end, exclusive. |  |
| **approvalMedianHours** | **kotlin.Double** | Median submit-to-approval time in the period. |  |
| **searches** | **kotlin.Int** | search_submitted events in the period. |  |
| **zeroResultSearches** | **kotlin.Int** | search_zero_results in the period. |  |
| **facilityViews** | **kotlin.Int** | facility_view events in the period. |  |
| **directionsRequests** | **kotlin.Int** | directions_start in the period. |  |
| **previous** | [**AdminAnalyticsPeriodKpis**](AdminAnalyticsPeriodKpis.md) | The same KPIs for the equally long period just before &#x60;from&#x60;. |  |
| **activeFacilities** | **kotlin.Int** |  |  |
| **pendingReviews** | **kotlin.Int** |  |  |
| **ratingAverage** | **kotlin.Double** |  |  |
| **events** | [**kotlin.collections.List&lt;AdminEventCount&gt;**](AdminEventCount.md) | All-time counts per event name. |  |



