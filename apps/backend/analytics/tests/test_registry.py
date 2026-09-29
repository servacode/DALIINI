from analytics.registry import EVENT_REGISTRY, validate_event


def test_baseline_event_registry_is_complete() -> None:
    expected = {
        "app_open",
        "home_view",
        "province_selected",
        "location_permission_result",
        "category_open",
        "search_submitted",
        "search_zero_results",
        "facility_view",
        "map_open",
        "marker_open",
        "phone_tap",
        "directions_start",
        "rating_submit",
        "owner_draft_create",
        "owner_submit",
        "application_status_view",
    }
    assert expected <= set(EVENT_REGISTRY)


def test_registry_rejects_precise_location_and_tokens() -> None:
    for forbidden in ("latitude", "longitude", "token", "phone", "evidenceId"):
        try:
            validate_event("app_open", {forbidden: "x"})
        except ValueError:
            pass
        else:
            raise AssertionError(f"Forbidden field accepted: {forbidden}")


def test_registry_rejects_undocumented_fields() -> None:
    try:
        validate_event("facility_view", {"facilityId": "x", "surprise": True})
    except ValueError:
        pass
    else:
        raise AssertionError("Undocumented field was accepted")
