"""One-shot daily-time scheduling used by the CLI."""

from __future__ import annotations

import time as time_module
from datetime import datetime, time, timedelta
from typing import Callable


def next_run_at(schedule_at: time, now: datetime | None = None) -> datetime:
    current = now or datetime.now()
    target = datetime.combine(current.date(), schedule_at)
    if target <= current:
        target += timedelta(days=1)
    return target


def wait_until(
    target: datetime,
    *,
    now: Callable[[], datetime] = datetime.now,
    sleep: Callable[[float], None] = time_module.sleep,
) -> None:
    while True:
        remaining = (target - now()).total_seconds()
        if remaining <= 0:
            return
        sleep(min(remaining, 1.0))
