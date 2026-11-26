"""The desktop client parses this exact console log structure."""

import logging
import os
import tempfile
import unittest
from datetime import timedelta
from pathlib import Path
from types import SimpleNamespace
from unittest.mock import patch

from urllib3.util import connection

from booking_assistant.logging import (
    configure_logging,
    log_http_responses,
    mask_identifier,
    save_logs,
)
from booking_assistant.network import prefer_ipv4


class LoggingTests(unittest.TestCase):
    def test_log_line_has_full_timestamp_level_and_message(self):
        logger = configure_logging("booking-format-test")
        try:
            record = logging.LogRecord(
                logger.name, logging.INFO, __file__, 1, "定时执行：%s", ("08:00:03",), None
            )
            record.emoji = "⏰"
            line = logger.handlers[0].formatter.format(record)
            self.assertRegex(
                line,
                r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3} \| INFO\s+\| ⏰ \| 定时执行：08:00:03$",
            )
        finally:
            for handler in logger.handlers[:]:
                logger.removeHandler(handler)
                handler.close()

    def test_masks_identifiers_but_keeps_a_recognizable_tail(self):
        self.assertEqual(mask_identifier("202610310001234567"), "**************4567")
        self.assertEqual(mask_identifier("abcdef"), "**cdef")
        self.assertEqual(mask_identifier("abc"), "***")
        self.assertEqual(mask_identifier(None), "(空)")

    def test_prefers_ipv4_for_campus_site_requests(self):
        previous = connection.HAS_IPV6
        try:
            connection.HAS_IPV6 = True
            prefer_ipv4()
            self.assertFalse(connection.HAS_IPV6)
        finally:
            connection.HAS_IPV6 = previous


class BufferedLoggingTests(unittest.TestCase):
    def setUp(self) -> None:
        self.loggers: list[logging.Logger] = []

    def tearDown(self) -> None:
        for logger in self.loggers:
            for handler in logger.handlers[:]:
                logger.removeHandler(handler)
                handler.close()

    def logger(self, name: str) -> logging.Logger:
        configured = configure_logging(name)
        self.loggers.append(configured)
        return configured

    def saved_text(self, logger: logging.Logger) -> str:
        with tempfile.TemporaryDirectory() as directory:
            path = save_logs(logger, directory=directory)
            self.assertIsNotNone(path)
            name = Path(path).name
            self.assertTrue(name.startswith("booking-"))
            self.assertTrue(name.endswith(".log"))
            return Path(path).read_text(encoding="utf-8")

    def test_debug_is_buffered_while_the_console_stays_at_info(self) -> None:
        with patch.dict("os.environ", {}, clear=False):
            os.environ.pop("BOOKING_LOG_LEVEL", None)
            logger = self.logger("booking-level-test")

        self.assertEqual(logger.level, logging.DEBUG)
        self.assertEqual(
            [handler.level for handler in logger.handlers],
            [logging.INFO, logging.DEBUG],
        )

    def test_save_logs_writes_info_and_debug(self) -> None:
        logger = self.logger("booking-save-test")
        logger.handlers[0].setLevel(logging.CRITICAL)
        logger.info("阶段一", extra={"emoji": "🚀"})
        logger.debug("准备登录")

        text = self.saved_text(logger)

        self.assertIn("INFO", text)
        self.assertIn("阶段一", text)
        self.assertIn("DEBUG", text)
        self.assertIn("准备登录", text)

    def test_response_hook_omits_ticket_token_cookie_and_body(self) -> None:
        logger = self.logger("booking-hook-test")
        session = SimpleNamespace(hooks={})
        log_http_responses(session, logger)

        response = SimpleNamespace(
            status_code=200,
            url="https://pecg.hust.edu.cn/cggl/front/syqk?ticket=example-ticket",
            request=SimpleNamespace(
                method="GET", url="https://pecg.hust.edu.cn/cggl/front/syqk"
            ),
            headers={
                "Content-Type": "text/html; charset=utf-8",
                "Set-Cookie": "JSESSIONID=example-cookie",
                "Location": "/cggl/index1;jsessionid=example-path-cookie?token=example-token",
            },
            content="<html>token=abc123</html>".encode("utf-8"),
            elapsed=timedelta(milliseconds=183),
        )
        session.hooks["response"][0](response)

        text = self.saved_text(logger)
        self.assertIn(
            "GET https://pecg.hust.edu.cn/cggl/front/syqk -> HTTP 200", text
        )
        self.assertIn("183 ms", text)
        self.assertIn("text/html", text)
        self.assertIn("Location: /cggl/index1", text)
        for secret in ("abc123", "example-ticket", "example-cookie", "example-path-cookie", "example-token"):
            self.assertNotIn(secret, text)

    def test_binary_responses_do_not_store_the_body(self) -> None:
        logger = self.logger("booking-binary-test")
        session = SimpleNamespace(hooks={})
        log_http_responses(session, logger)

        response = SimpleNamespace(
            status_code=200,
            url="https://pass.hust.edu.cn/cas/code",
            request=SimpleNamespace(
                method="GET", url="https://pass.hust.edu.cn/cas/code"
            ),
            headers={"Content-Type": "image/gif"},
            content=b"GIF89a\x00\x01\x02",
            elapsed=timedelta(milliseconds=12),
        )
        session.hooks["response"][0](response)

        text = self.saved_text(logger)
        self.assertIn("image/gif", text)
        self.assertNotIn("GIF89a", text)

    def test_large_responses_record_the_size_only(self) -> None:
        logger = self.logger("booking-truncate-test")
        session = SimpleNamespace(hooks={})
        log_http_responses(session, logger)

        response = SimpleNamespace(
            status_code=200,
            url="https://example.invalid/big",
            request=SimpleNamespace(method="GET", url="https://example.invalid/big"),
            headers={"Content-Type": "text/plain"},
            content=b"y" * 25000,
            elapsed=timedelta(milliseconds=5),
        )
        session.hooks["response"][0](response)

        text = self.saved_text(logger)
        self.assertIn("25000 字节", text)
        self.assertNotIn("y" * 10, text)


if __name__ == "__main__":
    unittest.main()
