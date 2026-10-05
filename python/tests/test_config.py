from datetime import date
from unittest import TestCase

from booking_assistant.config import BookingConfig
from booking_assistant.errors import ConfigurationError


class BookingConfigTests(TestCase):
    def make_config(self, **overrides: object) -> BookingConfig:
        values = {
            "cdbh": "69",
            "start_time": "18:00:00",
            "order_date_after_today": 2,
            "schedule_time": "08:00:01",
            "select_pay_type": "-1",
            "priority_list": "295,134,297",
        }
        values.update(overrides)
        return BookingConfig.from_cli(**values)  # type: ignore[arg-type]

    def test_builds_dates_times_and_priority_in_original_order(self) -> None:
        config = self.make_config()

        self.assertEqual(config.booking_date(date(2026, 9, 5)), date(2026, 9, 7))
        self.assertEqual(config.start_time_text, "18:00:00")
        self.assertEqual(config.end_time_text, "20:00:00")
        self.assertEqual(config.priority_venue_ids, ("295", "134", "297"))

    def test_rejects_venue_ids_from_another_stadium(self) -> None:
        with self.assertRaisesRegex(ConfigurationError, "不属于场馆"):
            self.make_config(priority_list="587")

    def test_reports_missing_original_options(self) -> None:
        with self.assertRaisesRegex(ConfigurationError, "--start_time"):
            self.make_config(start_time=None)
