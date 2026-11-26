from datetime import datetime, time, timedelta
from unittest import TestCase

from booking_assistant.scheduler import next_run_at, wait_until


class SchedulerTests(TestCase):
    def test_uses_today_when_time_is_still_ahead(self) -> None:
        now = datetime(2026, 9, 5, 7, 59, 59)
        self.assertEqual(
            next_run_at(time(8, 0, 1), now), datetime(2026, 9, 5, 8, 0, 1)
        )

    def test_uses_tomorrow_when_time_has_passed(self) -> None:
        now = datetime(2026, 9, 5, 8, 0, 2)
        self.assertEqual(
            next_run_at(time(8, 0, 1), now), datetime(2026, 9, 6, 8, 0, 1)
        )

    def test_wait_until_advances_a_fake_clock_to_the_target(self) -> None:
        clock = {"now": datetime(2026, 9, 5, 7, 59, 59)}
        target = datetime(2026, 9, 5, 8, 0, 2)

        def now() -> datetime:
            return clock["now"]

        def sleep(seconds: float) -> None:
            clock["now"] += timedelta(seconds=seconds)

        wait_until(target, now=now, sleep=sleep)

        self.assertGreaterEqual(clock["now"], target)
