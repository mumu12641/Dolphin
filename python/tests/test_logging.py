"""The desktop client parses this exact console log structure."""

import logging
import unittest

from booking_assistant.logging import configure_logging


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


if __name__ == "__main__":
    unittest.main()
