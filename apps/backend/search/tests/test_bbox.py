import pytest

from search.selectors import within_bbox


def test_bbox_rejects_invalid_order():
    with pytest.raises(ValueError):
        within_bbox(object(), "40,36,39,35")


def test_bbox_rejects_wrong_arity():
    with pytest.raises(ValueError):
        within_bbox(object(), "40,36,41")
