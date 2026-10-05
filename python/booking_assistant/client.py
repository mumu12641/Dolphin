"""HTTP adapter for the booking site's read, order, and payment boundaries."""

from __future__ import annotations

import logging
import re
import time
from dataclasses import dataclass
from typing import Any
from urllib.parse import urlparse

import requests

from .auth import USER_AGENT
from .config import BookingConfig
from .errors import (
    PaymentBlockedError,
    PaymentError,
    SiteProtocolError,
    VenueUnavailableError,
)
from .html import input_values

BASE_URL = "https://pecg.hust.edu.cn/cggl"
TOKEN_PATHS = ("/front/syqk", "/front/yuyuexz")
CREATE_ORDER_PATH = "/front/step2"
PAYMENT_PATH = "/front/repay"

_UNAVAILABLE_MESSAGES = (
    "该时段该区域有人正在预约，暂不可预约",
    "当前时段场地已被人预约",
)


@dataclass(frozen=True, slots=True)
class OrderReference:
    order_id: str
    reserve_id: str


def extract_token(document: str) -> str | None:
    patterns = (
        r'["\']token["\']\s*:\s*["\']([a-zA-Z0-9_-]{16,128})["\']',
        r'name=\\?["\']token\\?["\'][^>]*?value=\\?["\']([a-zA-Z0-9_-]{16,128})',
        r'value=\\?["\']([a-zA-Z0-9_-]{16,128})\\?["\'][^>]*?name=\\?["\']token',
    )
    for pattern in patterns:
        match = re.search(pattern, document, re.IGNORECASE | re.DOTALL)
        if match:
            return match.group(1)
    return None


def _response_text(response: Any) -> str:
    content = getattr(response, "content", b"")
    if not content:
        return response.text

    encoding = getattr(response, "encoding", None)
    if not encoding or encoding.lower() == "iso-8859-1":
        encoding = getattr(response, "apparent_encoding", None) or "utf-8"
    try:
        return content.decode(encoding)
    except (LookupError, UnicodeDecodeError):
        return content.decode("utf-8", errors="replace")


class BookingSiteClient:
    """Requests-based site client.

    Payment is denied by default. The production CLI is the only composition root
    that explicitly opts in; tests and diagnostics therefore cannot accidentally
    reach the charge endpoint.
    """

    def __init__(
        self,
        session: Any,
        logger: logging.Logger,
        *,
        payment_enabled: bool = False,
        timeout: float = 20,
        token_attempts: int = 3,
        token_retry_delay: float = 0.2,
    ) -> None:
        self._session = session
        self._logger = logger
        self._payment_enabled = payment_enabled
        self._timeout = timeout
        self._token_attempts = token_attempts
        self._token_retry_delay = token_retry_delay

    def get_booking_token(self, config: BookingConfig) -> str:
        params = {
            "cdbh": config.stadium_id,
            "date": config.booking_date().isoformat(),
            "starttime": config.start_time_text,
            "endtime": config.end_time_text,
        }
        failures: list[str] = []
        for attempt in range(self._token_attempts):
            for path in TOKEN_PATHS:
                try:
                    response = self._session.get(
                        BASE_URL + path,
                        params=params if path.endswith("syqk") else None,
                        headers=self._headers(),
                        timeout=self._timeout,
                    )
                except requests.RequestException as exc:
                    failures.append(f"{path}: {type(exc).__name__}")
                    continue

                final_path = urlparse(response.url).path
                if final_path.startswith("/wescms"):
                    raise SiteProtocolError(
                        "预约系统未接受登录会话，页面已跳转到公共场馆首页"
                    )
                token = (
                    extract_token(_response_text(response))
                    if response.status_code == 200
                    else None
                )
                if token:
                    return token
                failures.append(f"{path}: HTTP {response.status_code}，未找到 token")
            if attempt + 1 < self._token_attempts:
                time.sleep(self._token_retry_delay)

        detail = "; ".join(failures)
        raise SiteProtocolError(f"无法从预约页面获取 token（{detail}）")

    def create_order(
        self, config: BookingConfig, venue_id: str, token: str
    ) -> OrderReference:
        try:
            response = self._session.post(
                BASE_URL + CREATE_ORDER_PATH,
                headers=self._form_headers(config),
                data={
                    "starttime": config.start_time_text,
                    "endtime": config.end_time_text,
                    "partnerCardtype": "1",
                    "choosetime": venue_id,
                    "changdibh": config.stadium_id,
                    "date": config.booking_date().isoformat(),
                    "token": token,
                },
                timeout=self._timeout,
            )
        except requests.RequestException as exc:
            raise SiteProtocolError(
                "创建订单请求异常，结果未知；不会重试或继续换场"
            ) from exc
        if response.status_code != 200:
            raise SiteProtocolError(f"创建订单失败：HTTP {response.status_code}")

        document = _response_text(response)
        values = input_values(document)
        order_id = values.get("orderId")
        reserve_id = values.get("reserveId")
        if order_id and reserve_id:
            return OrderReference(order_id=order_id, reserve_id=reserve_id)

        if any(message in document for message in _UNAVAILABLE_MESSAGES):
            raise VenueUnavailableError("该场地已被占用")
        if "您已被加入黑名单" in document:
            raise SiteProtocolError("账号已被预约系统加入黑名单")
        raise SiteProtocolError("创建订单响应中缺少 orderId 或 reserveId")

    def pay(
        self, config: BookingConfig, order: OrderReference, token: str
    ) -> None:
        if not self._payment_enabled:
            raise PaymentBlockedError("支付安全闸门未开启，已阻止付款请求")

        try:
            response = self._session.post(
                BASE_URL + PAYMENT_PATH,
                headers=self._payment_headers(order),
                data={
                    "orderId": order.order_id,
                    "reserveId": order.reserve_id,
                    "data": "",
                    "id": "",
                    "select_pay_type": config.pay_type,
                    "token": token,
                },
                timeout=self._timeout,
            )
        except requests.Timeout as exc:
            raise PaymentError(
                "支付请求超时，结果未知；为避免重复扣款，本次不会重试"
            ) from exc
        except requests.RequestException as exc:
            raise PaymentError(
                "支付请求异常，结果未知；为避免重复扣款，本次不会重试"
            ) from exc

        if response.status_code != 200:
            raise PaymentError(
                f"支付返回 HTTP {response.status_code}；为避免重复扣款，本次不会重试"
            )

    @staticmethod
    def _headers() -> dict[str, str]:
        return {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.7",
            "User-Agent": USER_AGENT,
        }

    def _form_headers(self, config: BookingConfig) -> dict[str, str]:
        headers = self._headers()
        headers.update(
            {
                "Content-Type": "application/x-www-form-urlencoded",
                "Origin": "https://pecg.hust.edu.cn",
                "Referer": (
                    f"{BASE_URL}/front/syqk?cdbh={config.stadium_id}"
                    f"&date={config.booking_date().isoformat()}"
                    f"&starttime={config.start_time_text}"
                    f"&endtime={config.end_time_text}"
                ),
            }
        )
        return headers

    def _payment_headers(self, order: OrderReference) -> dict[str, str]:
        headers = self._headers()
        headers.update(
            {
                "Content-Type": "application/x-www-form-urlencoded",
                "Origin": "https://pecg.hust.edu.cn",
                "Referer": (
                    f"{BASE_URL}/front/toPay?reserveId={order.reserve_id}"
                    f"&orderId={order.order_id}"
                ),
            }
        )
        return headers
