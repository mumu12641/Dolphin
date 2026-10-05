"""Booking orchestration with a single, explicit payment boundary."""

from __future__ import annotations

import logging
from dataclasses import dataclass

from .client import BookingSiteClient, OrderReference
from .config import BookingConfig
from .courts import court_number
from .errors import PaymentError, VenueUnavailableError


@dataclass(frozen=True, slots=True)
class BookingOutcome:
    venue_id: str
    order: OrderReference


class BookingWorkflow:
    def __init__(
        self,
        config: BookingConfig,
        client: BookingSiteClient,
        logger: logging.Logger,
    ) -> None:
        self._config = config
        self._client = client
        self._logger = logger

    def run(self) -> BookingOutcome | None:
        booking_date = self._config.booking_date().isoformat()
        self._logger.info(
            "预约 %s %s—%s",
            booking_date,
            self._config.start_time_text.removesuffix(":00"),
            self._config.end_time_text.removesuffix(":00"),
            extra={"emoji": "📅"},
        )
        token = self._client.get_booking_token(self._config)

        for venue_id in self._config.priority_venue_ids:
            number = court_number(self._config.stadium_id, venue_id)
            self._logger.info("尝试 %s 号场", number, extra={"emoji": "🏸"})
            try:
                order = self._client.create_order(self._config, venue_id, token)
            except VenueUnavailableError:
                self._logger.warning("%s 号场已占用，尝试下一场", number, extra={"emoji": "🚧"})
                continue

            self._logger.info("订单已创建，正在支付", extra={"emoji": "🧾"})
            try:
                self._client.pay(self._config, order, token)
            except PaymentError:
                self._logger.error(
                    "支付结果未确认，请检查订单；已停止换场",
                    extra={"emoji": "💳"},
                )
                raise

            self._logger.info("%s 号场预约及支付请求成功", number, extra={"emoji": "🎉"})
            return BookingOutcome(venue_id=venue_id, order=order)

        self._logger.warning("所有候选场地均不可用", extra={"emoji": "📭"})
        return None
