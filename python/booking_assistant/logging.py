"""Console logging plus a debug buffer written to disk when a run ends."""

import logging
import os
import re
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any
from urllib.parse import urlsplit

# The desktop client parses this line layout, so keep the prefix unchanged.
_LOG_FORMAT = "%(asctime)s.%(msecs)03d | %(levelname)-7s | %(emoji)s | %(message)s"
_BUFFER_ATTR = "_booking_buffer"


class _BufferHandler(logging.Handler):
    """Keep every record in memory so the run can be saved to a file later."""

    def __init__(self) -> None:
        super().__init__(logging.DEBUG)
        self.lines: list[str] = []

    def emit(self, record: logging.LogRecord) -> None:
        try:
            self.lines.append(self.format(record))
        except Exception:  # pragma: no cover - logging must never break a run
            self.handleError(record)


def _formatter() -> logging.Formatter:
    return logging.Formatter(
        _LOG_FORMAT, "%Y-%m-%d %H:%M:%S", defaults={"emoji": "💬"}
    )


def configure_logging(name: str = "booking") -> logging.Logger:
    logger = logging.getLogger(name)
    # The logger always passes DEBUG so responses can be buffered for the
    # log file; the console handler decides what is actually printed.
    logger.setLevel(logging.DEBUG)
    logger.propagate = False
    if logger.handlers:
        return logger

    level_name = os.environ.get("BOOKING_LOG_LEVEL", "INFO").upper()
    console = logging.StreamHandler(sys.stdout)
    console.setLevel(getattr(logging, level_name, logging.INFO))
    console.setFormatter(_formatter())
    logger.addHandler(console)

    buffer = _BufferHandler()
    buffer.setFormatter(_formatter())
    logger.addHandler(buffer)
    setattr(logger, _BUFFER_ATTR, buffer)
    return logger


def save_logs(
    logger: logging.Logger, directory: str | Path | None = None
) -> Path | None:
    """Write every buffered record to a timestamped file and return its path."""

    buffer = getattr(logger, _BUFFER_ATTR, None)
    if not isinstance(buffer, _BufferHandler):
        return None
    target_dir = Path(directory if directory is not None else os.environ.get("BOOKING_LOG_DIR") or "logs")
    path = target_dir / datetime.now().strftime("booking-%Y%m%d-%H%M%S-%f.log")
    try:
        target_dir.mkdir(parents=True, exist_ok=True)
        descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        with os.fdopen(descriptor, "w", encoding="utf-8") as output:
            output.write("\n".join(buffer.lines) + "\n")
    except OSError:
        logger.warning("无法保存本次运行日志", extra={"emoji": "📝"})
        return None
    return path


def log_http_responses(session: Any, logger: logging.Logger) -> None:
    """Attach a DEBUG hook that records response metadata without credentials."""

    def _hook(response: Any, *args: Any, **kwargs: Any) -> None:
        try:
            _log_response(logger, response)
        except Exception:  # pragma: no cover - never break a booking attempt
            logger.debug("[响应] 摘要记录失败")

    session.hooks.setdefault("response", []).append(_hook)


def _log_response(logger: logging.Logger, response: Any) -> None:
    request = getattr(response, "request", None)
    method = getattr(request, "method", "?")
    url = _endpoint(getattr(response, "url", None) or getattr(request, "url", ""))
    status = getattr(response, "status_code", "?")
    content = getattr(response, "content", b"") or b""
    elapsed = getattr(response, "elapsed", None)
    duration = (
        f"{elapsed.total_seconds() * 1000:.0f} ms" if elapsed is not None else "-"
    )
    logger.debug(
        "[响应] %s %s -> HTTP %s | %d 字节 | %s",
        method,
        url,
        status,
        len(content),
        duration,
    )
    headers = getattr(response, "headers", None) or {}
    content_type = (headers.get("Content-Type") or headers.get("content-type") or "").split(";", 1)[0]
    if content_type:
        logger.debug("[响应] Content-Type: %s", content_type)
    location = headers.get("Location") or headers.get("location")
    if location:
        logger.debug("[响应] Location: %s", _endpoint(location))


def _endpoint(url: str) -> str:
    parts = urlsplit(url)
    origin = f"{parts.scheme}://{parts.hostname}" if parts.hostname else ""
    return origin + re.sub(r"(?i);jsessionid=[^/]+", "", parts.path) or "?"


def elapsed_ms(started: float) -> str:
    """Format a ``time.perf_counter()`` delta for log messages."""

    return f"{(time.perf_counter() - started) * 1000:.0f} ms"


def mask_identifier(value: str | None, visible: int = 4) -> str:
    """Hide most of an identifier while keeping it recognizable in logs."""

    if not value:
        return "(空)"
    text = str(value)
    if len(text) <= visible:
        return "*" * len(text)
    return "*" * (len(text) - visible) + text[-visible:]
