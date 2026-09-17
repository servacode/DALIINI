import json
import logging
from datetime import UTC, datetime


class JsonFormatter(logging.Formatter):
    _blocked = {"password", "otp", "token", "authorization", "database_url", "secret"}

    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, object] = {
            "timestamp": datetime.now(UTC).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "message": record.getMessage(),
        }
        request_id = getattr(record, "request_id", None)
        if request_id:
            payload["requestId"] = request_id
        return json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
