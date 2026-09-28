# AdminAnalytics

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**from** | **Date** | Period start, inclusive. | 
**to** | **Date** | Period end, exclusive. | 
**approvalMedianHours** | **Double** | Median submit-to-approval time in the period. | 
**searches** | **Int** | search_submitted events in the period. | 
**zeroResultSearches** | **Int** | search_zero_results in the period. | 
**facilityViews** | **Int** | facility_view events in the period. | 
**directionsRequests** | **Int** | directions_start in the period. | 
**previous** | [**AdminAnalyticsPeriodKpis**](AdminAnalyticsPeriodKpis.md) | The same KPIs for the equally long period just before &#x60;from&#x60;. | 
**activeFacilities** | **Int** |  | 
**pendingReviews** | **Int** |  | 
**ratingAverage** | **Double** |  | 
**events** | [AdminEventCount] | All-time counts per event name. | 

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)


