from realtime.events import EventName


def test_required_event_catalog_is_present():
    assert {item.value for item in EventName} == {
        "public.province.configuration_changed",
        "public.facility.changed",
        "public.facility.availability_changed",
        "public.duty.changed",
        "user.application.changed",
        "user.facility.changed",
        "admin.review_queue.changed",
        "admin.system.changed",
    }
