import json
import logging
from contextvars import ContextVar
from datetime import UTC, datetime

# The request id of the request being served, so every log line it produces carries it.
current_request_id: ContextVar[str] = ContextVar("current_request_id", default="")

# LogRecord attributes that are not caller-supplied `extra` fields.
_RESERVED = set(
    logging.LogRecord("", 0, "", 0, "", None, None).__dict__.keys()
) | {"message", "asctime", "request_id", "taskName"}


def _camel(name: str) -> str:
    head, *rest = name.split("_")
    return head + "".join(part.title() for part in rest)


class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        if not getattr(record, "request_id", None):
            record.request_id = current_request_id.get()
        return True


class JsonFormatter(logging.Formatter):
    _blocked = {"password", "otp", "token", "authorization", "database_url", "secret"}

    def _is_blocked(self, key: str) -> bool:
        lowered = key.lower()
        return any(word in lowered for word in self._blocked)

    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, object] = {
            "timestamp": datetime.now(UTC).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        request_id = getattr(record, "request_id", None) or current_request_id.get()
        if request_id:
            payload["requestId"] = request_id
        for key, value in record.__dict__.items():
            if key in _RESERVED or key.startswith("_") or self._is_blocked(key):
                continue
            if isinstance(value, str | int | float | bool) or value is None:
                payload[_camel(key)] = value
            else:
                payload[_camel(key)] = str(value)
        if record.exc_info:
            payload["exception"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
