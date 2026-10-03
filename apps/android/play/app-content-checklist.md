# Play App Content checklist

Do not mark an item complete without matching Play Console evidence.

- [ ] Privacy policy URL is live on the final owned domain.
- [ ] Data Safety reconciled against the exact RC AAB and dependency graph.
- [ ] Account deletion URL is live and accepts a deletion request path.
- [ ] In-app deletion path tested from a Play-installed build.
- [ ] App access/reviewer instructions supplied with valid disposable review credentials where required.
- [ ] Ads declaration reviewed against the shipped first-party advertisement surfaces.
- [ ] Content rating questionnaire completed accurately.
- [ ] Target audience completed accurately.
- [ ] Location permission use matches while-in-use behavior; no background location declaration.
- [ ] Notification permission behavior documented.
- [ ] Foreground-service declaration: navigation now runs a `location` foreground service while a trip is under way (DECISION-078). Declare it under App content → Foreground service permissions as "Navigation", with a short video of a trip continuing with the screen locked. The app still asks for no background location permission.
- [ ] Country availability intentionally selected from territories actually offered by Play Console.
- [ ] Developer identity/contact requirements completed in the account.
