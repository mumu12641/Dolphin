"""Validated, side-effect-free booking configuration."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import date, datetime, time, timedelta

from .courts import COURTS_BY_STADIUM, venue_ids
from .errors import ConfigurationError

_TIME_FORMAT = "%H:%M:%S"


def _parse_time(value: str, option: str) -> time:
    try:
        return datetime.strptime(value, _TIME_FORMAT).time()
    except (TypeError, ValueError) as exc:
        raise ConfigurationError(f"{option} 必须使用 HH:MM:SS 格式") from exc


@dataclass(frozen=True, slots=True)
class Credentials:
    username: str
    password: str = field(repr=False)

    def __post_init__(self) -> None:
        if not self.username:
            raise ConfigurationError("--user_name 不能为空")
        if not self.password:
            raise ConfigurationError("--password 不能为空")


@dataclass(frozen=True, slots=True)
class BookingConfig:
    stadium_id: str
    start_at: time
    days_after_today: int
    schedule_at: time
    pay_type: str
    priority_venue_ids: tuple[str, ...]

    @classmethod
    def from_cli(
        cls,
        *,
        cdbh: str | None,
        start_time: str | None,
        order_date_after_today: int | None,
        schedule_time: str | None,
        select_pay_type: str | None,
        priority_list: str | None,
    ) -> "BookingConfig":
        missing = [
            name
            for name, value in (
                ("--cdbh", cdbh),
                ("--start_time", start_time),
                ("--order_date_after_today", order_date_after_today),
                ("--schedule_time", schedule_time),
                ("--select_pay_type", select_pay_type),
                ("--priority_list", priority_list),
            )
            if value is None or value == ""
        ]
        if missing:
            raise ConfigurationError(f"缺少必要参数: {', '.join(missing)}")

        assert cdbh is not None
        assert start_time is not None
        assert order_date_after_today is not None
        assert schedule_time is not None
        assert select_pay_type is not None
        assert priority_list is not None

        if cdbh not in COURTS_BY_STADIUM:
            known = ", ".join(COURTS_BY_STADIUM)
            raise ConfigurationError(f"未知场馆编号 {cdbh}；当前支持: {known}")
        if order_date_after_today < 0:
            raise ConfigurationError("--order_date_after_today 不能为负数")

        priorities = tuple(item.strip() for item in priority_list.split(",") if item.strip())
        if not priorities:
            raise ConfigurationError("--priority_list 至少需要一个场地 ID")
        if len(set(priorities)) != len(priorities):
            raise ConfigurationError("--priority_list 中不能包含重复场地 ID")

        unknown = [item for item in priorities if item not in venue_ids(cdbh)]
        if unknown:
            raise ConfigurationError(
                f"场地 ID 不属于场馆 {cdbh}: {', '.join(unknown)}"
            )

        return cls(
            stadium_id=cdbh,
            start_at=_parse_time(start_time, "--start_time"),
            days_after_today=order_date_after_today,
            schedule_at=_parse_time(schedule_time, "--schedule_time"),
            pay_type=select_pay_type,
            priority_venue_ids=priorities,
        )

    def booking_date(self, today: date | None = None) -> date:
        return (today or date.today()) + timedelta(days=self.days_after_today)

    @property
    def start_time_text(self) -> str:
        return self.start_at.strftime(_TIME_FORMAT)

    @property
    def end_time_text(self) -> str:
        start = datetime.combine(date.min, self.start_at)
        return (start + timedelta(hours=2)).strftime(_TIME_FORMAT)

    @property
    def schedule_time_text(self) -> str:
        return self.schedule_at.strftime(_TIME_FORMAT)
