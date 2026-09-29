from realtime.groups import admin_group, province_group, user_group


def test_groups_do_not_expose_raw_identifiers() -> None:
    raw = "sensitive-user-id"
    assert raw not in user_group(raw)
    assert raw not in province_group(raw)
    assert "review_queue" not in admin_group("review_queue")
