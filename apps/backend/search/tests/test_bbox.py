from typing import Any, cast

import pytest

from search.selectors import within_bbox


def test_bbox_rejects_invalid_order() -> None:
    with pytest.raises(ValueError):
        within_bbox(cast(Any, object()), "40,36,39,35")


def test_bbox_rejects_wrong_arity() -> None:
    with pytest.raises(ValueError):
        within_bbox(cast(Any, object()), "40,36,41")
