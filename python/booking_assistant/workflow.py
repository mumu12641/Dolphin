"""Booking orchestration with a single, explicit payment boundary."""

from __future__ import annotations

import logging
import time
from dataclasses import dataclass

from .client import BookingSiteClient, OrderReference
from .config import BookingConfig
from .courts import court_number
from .errors import PaymentError, VenueUnavailableError
from .logging import elapsed_ms, mask_identifier


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
        started = time.perf_counter()
        booking_date = self._config.booking_date().isoformat()
        candidates = [
            f"{court_number(self._config.stadium_id, venue_id) or '?'} 号场({venue_id})"
            for venue_id in self._config.priority_venue_ids
        ]
        self._logger.info(
            "预约 %s %s—%s",
            booking_date,
            self._config.start_time_text.removesuffix(":00"),
            self._config.end_time_text.removesuffix(":00"),
            extra={"emoji": "📅"},
        )
        self._logger.info(
            "候选场地顺序：%s",
            " → ".join(candidates),
            extra={"emoji": "📋"},
        )

        token_started = time.perf_counter()
        token = self._client.get_booking_token(self._config)
        self._logger.info(
            "token 阶段完成，耗时 %s，开始逐个尝试场地",
            elapsed_ms(token_started),
            extra={"emoji": "🔑"},
        )

        for index, venue_id in enumerate(self._config.priority_venue_ids, start=1):
            number = court_number(self._config.stadium_id, venue_id)
            total = len(self._config.priority_venue_ids)
            self._logger.info(
                "尝试 %s 号场（%d/%d，场地 ID %s）",
                number,
                index,
                total,
                venue_id,
                extra={"emoji": "🏸"},
            )
            order_started = time.perf_counter()
            try:
                order = self._client.create_order(self._config, venue_id, token)
            except VenueUnavailableError:
                self._logger.warning(
                    "%s 号场已占用（本次尝试 %.0f ms），尝试下一场",
                    number,
                    (time.perf_counter() - order_started) * 1000,
                    extra={"emoji": "🚧"},
                )
                continue
            self._logger.info(
                "%s 号场订单已创建（orderId=%s，耗时 %s），正在支付",
                number,
                mask_identifier(order.order_id),
                elapsed_ms(order_started),
                extra={"emoji": "🧾"},
            )

            pay_started = time.perf_counter()
            try:
                self._client.pay(self._config, order, token)
            except PaymentError:
                self._logger.error(
                    "支付结果未确认（orderId=%s，耗时 %s），请检查订单；已停止换场",
                    mask_identifier(order.order_id),
                    elapsed_ms(pay_started),
                    extra={"emoji": "💳"},
                )
                raise

            self._logger.info(
                "%s 号场预约及支付请求成功：%s %s—%s，orderId=%s，总耗时 %s",
                number,
                booking_date,
                self._config.start_time_text.removesuffix(":00"),
                self._config.end_time_text.removesuffix(":00"),
                mask_identifier(order.order_id),
                elapsed_ms(started),
                extra={"emoji": "🎉"},
            )
            return BookingOutcome(venue_id=venue_id, order=order)

        self._logger.warning(
            "所有候选场地均不可用（共 %d 个，耗时 %s）",
            len(self._config.priority_venue_ids),
            elapsed_ms(started),
            extra={"emoji": "📭"},
        )
        return None
