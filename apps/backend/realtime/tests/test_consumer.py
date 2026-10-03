"""What a client can send the socket, and what it gets back."""

from typing import Any

import pytest
from asgiref.sync import async_to_sync
from channels.testing import WebsocketCommunicator

from locations.models import Province
from realtime.consumers import DirectoryConsumer

PATH = "/ws/v1/directory/"


async def _exchange(*messages: Any) -> list[dict[str, Any]]:
    communicator = WebsocketCommunicator(DirectoryConsumer.as_asgi(), PATH)
    connected, _ = await communicator.connect()
    assert connected
    replies = []
    for message in messages:
        await communicator.send_json_to(message)
        replies.append(await communicator.receive_json_from(timeout=5))
    await communicator.disconnect()
    return replies


@pytest.mark.django_db(transaction=True)
@pytest.mark.parametrize(
    "province_id",
    ["not-a-uuid", "", 42, None, ["a"], {"id": "x"}, "' OR 1=1 --"],
)
def test_a_province_id_that_is_not_one_is_refused_and_the_socket_stays_open(
    province_id: Any,
) -> None:
    replies = async_to_sync(_exchange)(
        {"action": "subscribeProvince", "provinceId": province_id},
        {"action": "nothing"},
    )

    assert replies[0] == {"type": "error", "code": "INVALID_PROVINCE"}
    # The socket still answers: the bad id did not take it down.
    assert replies[1] == {"type": "error", "code": "UNKNOWN_ACTION"}


@pytest.mark.django_db(transaction=True)
def test_an_active_province_is_joined_however_its_id_is_spelled() -> None:
    province = Province.objects.create(code="raqqa-ws", name_ar="الرقة", active=True)
    replies = async_to_sync(_exchange)(
        {"action": "subscribeProvince", "provinceId": str(province.id).upper()},
    )

    assert replies[0] == {"type": "subscribed", "scope": "province"}


@pytest.mark.django_db(transaction=True)
def test_a_message_that_is_not_an_object_is_refused() -> None:
    replies = async_to_sync(_exchange)(["subscribeProvince"], "hello")

    assert replies == [
        {"type": "error", "code": "UNKNOWN_ACTION"},
        {"type": "error", "code": "UNKNOWN_ACTION"},
    ]
