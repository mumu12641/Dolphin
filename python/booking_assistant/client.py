"""HTTP adapter for the booking site's read, order, and payment boundaries."""

from __future__ import annotations

import logging
import re
import time
from collections.abc import Callable
from dataclasses import dataclass
from html import unescape
from typing import Any
from urllib.parse import urljoin, urlparse

import requests

from .auth import USER_AGENT
from .config import BookingConfig
from .errors import (
    AccountBlacklistedError,
    PaymentBlockedError,
    PaymentError,
    SiteProtocolError,
    VenueUnavailableError,
)
from .html import input_values
from .logging import elapsed_ms, mask_identifier
from .network import prefer_ipv4

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


def _raise_if_blacklisted(document: str) -> None:
    document = unescape(document)
    if any(message in document for message in (
        "您已被加入黑名单", "账号已被加入黑名单", "账户已被加入黑名单",
    )):
        raise AccountBlacklistedError("账号已被预约系统加入黑名单，暂时无法预约")


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
        payment_attempts: int = 3,
        payment_retry_delay: float = 0.5,
        payment_connect_timeout: float = 5.0,
        sleep: Callable[[float], None] = time.sleep,
    ) -> None:
        prefer_ipv4()
        self._session = session
        self._logger = logger
        self._payment_enabled = payment_enabled
        self._timeout = timeout
        self._token_attempts = token_attempts
        self._token_retry_delay = token_retry_delay
        # Retry only a timeout while connecting to the payment endpoint.
        self._payment_attempts = max(1, payment_attempts)
        self._payment_retry_delay = payment_retry_delay
        self._payment_connect_timeout = payment_connect_timeout
        self._sleep = sleep

    def check_account_access(self, config: BookingConfig) -> None:
        """Read the normal reservation entry without creating an order."""
        url = BASE_URL + "/front/syqk"
        params: dict[str, str] | None = {
            "cdbh": config.stadium_id,
            "date": config.booking_date().isoformat(),
            "starttime": config.start_time_text,
            "endtime": config.end_time_text,
        }
        allowed_paths = {
            "/cggl/", "/cggl/index", "/cggl/index1",
            "/cggl/front/yuyuexz", "/cggl/front/syqk",
        }
        headers = self._headers()
        headers["Referer"] = BASE_URL + "/front/yuyuexz"
        self._logger.info("正在检查账号预约资格", extra={"emoji": "🔎"})
        for _ in range(5):
            try:
                response = self._session.get(
                    url, params=params, headers=headers,
                    timeout=self._timeout, allow_redirects=False,
                )
            except requests.RequestException as exc:
                raise SiteProtocolError("账号资格检查网络异常，请检查网络后重新启动") from exc
            document = _response_text(response)
            _raise_if_blacklisted(document)
            if response.status_code in (301, 302, 303, 307, 308):
                location = response.headers.get("Location")
                if not location:
                    raise SiteProtocolError("账号资格检查返回了无目标地址的重定向")
                destination = urlparse(urljoin(url, location))
                if (destination.scheme not in ("http", "https")
                        or destination.netloc not in (
                            "pecg.hust.edu.cn", "pecg.hust.edu.cn:80", "pecg.hust.edu.cn:443",
                        )
                        or destination.path not in allowed_paths):
                    raise SiteProtocolError("账号资格检查被跳转到登录页或未知页面，请核实登录状态")
                # The site emits HTTP redirects; keep the session on HTTPS.
                url = "https://pecg.hust.edu.cn" + destination.path
                if destination.query:
                    url += "?" + destination.query
                params = None
                continue
            if response.status_code != 200:
                raise SiteProtocolError(f"账号资格检查失败：HTTP {response.status_code}")
            final_path = urlparse(getattr(response, "url", "") or url).path
            if final_path != "/cggl/front/syqk" or not extract_token(document):
                raise SiteProtocolError(
                    f"无法确认账号预约资格：未进入正常预约页面（{final_path}），已停止本次任务"
                )
            self._logger.info("账号预约资格检查通过", extra={"emoji": "🟢"})
            return
        raise SiteProtocolError("账号资格检查重定向次数过多，已停止本次任务")

    def get_booking_token(self, config: BookingConfig) -> str:
        booking_date = config.booking_date().isoformat()
        params = {
            "cdbh": config.stadium_id,
            "date": booking_date,
            "starttime": config.start_time_text,
            "endtime": config.end_time_text,
        }
        started = time.perf_counter()
        self._logger.info(
            "开始读取预约 token：场馆 %s，日期 %s，时段 %s—%s，最多 %d 轮",
            config.stadium_id,
            booking_date,
            config.start_time_text,
            config.end_time_text,
            self._token_attempts,
            extra={"emoji": "🔎"},
        )
        failures: list[str] = []
        for attempt in range(self._token_attempts):
            for path in TOKEN_PATHS:
                step_started = time.perf_counter()
                self._logger.info(
                    "GET %s（第 %d/%d 轮）",
                    path,
                    attempt + 1,
                    self._token_attempts,
                    extra={"emoji": "🌐"},
                )
                try:
                    response = self._session.get(
                        BASE_URL + path,
                        params=params if path.endswith("syqk") else None,
                        headers=self._headers(),
                        timeout=self._timeout,
                    )
                except requests.RequestException as exc:
                    failures.append(f"{path}: {type(exc).__name__}")
                    self._logger.warning(
                        "GET %s 网络异常：%s，耗时 %s",
                        path,
                        type(exc).__name__,
                        elapsed_ms(step_started),
                        extra={"emoji": "📡"},
                    )
                    continue

                final_path = urlparse(getattr(response, "url", "")).path
                body_size = len(getattr(response, "content", b"") or b"")
                self._logger.info(
                    "GET %s -> HTTP %d，最终路径 %s，响应 %d 字节，耗时 %s",
                    path,
                    response.status_code,
                    final_path or "(未知)",
                    body_size,
                    elapsed_ms(step_started),
                    extra={"emoji": "📥"},
                )
                if final_path.startswith("/wescms"):
                    self._logger.error(
                        "登录会话未被预约系统接受，已跳转到公共场馆首页",
                        extra={"emoji": "🚪"},
                    )
                    raise SiteProtocolError(
                        "预约系统未接受登录会话，页面已跳转到公共场馆首页"
                    )
                document = _response_text(response)
                _raise_if_blacklisted(document)
                if response.status_code == 429:
                    raise SiteProtocolError("预约系统限流（HTTP 429），已停止读取预约凭证")
                token = (
                    extract_token(document)
                    if response.status_code == 200
                    else None
                )
                if token:
                    self._logger.info(
                        "已获取预约 token：长度 %d（内容不记录），累计耗时 %s",
                        len(token),
                        elapsed_ms(started),
                        extra={"emoji": "🔑"},
                    )
                    return token
                failures.append(f"{path}: HTTP {response.status_code}，未找到 token")
                self._logger.warning(
                    "GET %s 未找到 token：HTTP %d，响应 %d 字节",
                    path,
                    response.status_code,
                    body_size,
                    extra={"emoji": "🔁"},
                )
            if attempt + 1 < self._token_attempts:
                self._logger.info(
                    "第 %d 轮未取得 token，%.1f 秒后重试",
                    attempt + 1,
                    self._token_retry_delay,
                    extra={"emoji": "🔁"},
                )
                time.sleep(self._token_retry_delay)

        detail = "; ".join(failures)
        self._logger.error(
            "读取 token 失败，共尝试 %d 次，耗时 %s：%s",
            self._token_attempts,
            elapsed_ms(started),
            detail,
            extra={"emoji": "❌"},
        )
        raise SiteProtocolError(f"无法从预约页面获取 token（{detail}）")

    def create_order(
        self, config: BookingConfig, venue_id: str, token: str
    ) -> OrderReference:
        booking_date = config.booking_date().isoformat()
        started = time.perf_counter()
        self._logger.info(
            "创建订单：场地 %s | %s %s—%s",
            venue_id,
            booking_date,
            config.start_time_text,
            config.end_time_text,
            extra={"emoji": "🧾"},
        )
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
                    "date": booking_date,
                    "token": token,
                },
                timeout=self._timeout,
            )
        except requests.RequestException as exc:
            self._logger.error(
                "创建订单网络异常：%s，耗时 %s，结果未知",
                type(exc).__name__,
                elapsed_ms(started),
                extra={"emoji": "📡"},
            )
            raise SiteProtocolError(
                "创建订单请求异常，结果未知；不会重试或继续换场"
            ) from exc

        document = _response_text(response)
        self._logger.info(
            "创建订单响应：HTTP %d，正文 %d 字节，耗时 %s",
            response.status_code,
            len(getattr(response, "content", b"") or b""),
            elapsed_ms(started),
            extra={"emoji": "📨"},
        )
        if response.status_code != 200:
            self._logger.error(
                "创建订单失败：HTTP %d", response.status_code, extra={"emoji": "❌"}
            )
            raise SiteProtocolError(f"创建订单失败：HTTP {response.status_code}")

        values = input_values(document)
        order_id = values.get("orderId")
        reserve_id = values.get("reserveId")
        if order_id and reserve_id:
            self._logger.info(
                "订单解析成功：orderId=%s，reserveId=%s",
                mask_identifier(order_id),
                mask_identifier(reserve_id),
                extra={"emoji": "✅"},
            )
            return OrderReference(order_id=order_id, reserve_id=reserve_id)

        if any(message in document for message in _UNAVAILABLE_MESSAGES):
            self._logger.warning(
                "场地 %s 创建订单被拒绝：该时段已被占用",
                venue_id,
                extra={"emoji": "🚧"},
            )
            raise VenueUnavailableError("该场地已被占用")
        if "您已被加入黑名单" in document:
            self._logger.error(
                "创建订单被拒绝：账号已进入预约系统黑名单", extra={"emoji": "⛔"}
            )
            raise SiteProtocolError("账号已被预约系统加入黑名单")
        self._logger.error(
            "创建订单响应缺少 orderId/reserveId：HTTP %d，正文 %d 字节",
            response.status_code,
            len(document),
            extra={"emoji": "❓"},
        )
        raise SiteProtocolError("创建订单响应中缺少 orderId 或 reserveId")

    def pay(
        self, config: BookingConfig, order: OrderReference, token: str
    ) -> None:
        if not self._payment_enabled:
            self._logger.warning(
                "支付安全闸门未开启，已阻止付款请求",
                extra={"emoji": "🛡️"},
            )
            raise PaymentBlockedError("支付安全闸门未开启，已阻止付款请求")

        started = time.perf_counter()
        # A short connect timeout keeps a dead route from eating the release
        # window; the read timeout stays generous for the site's slow reply.
        timeout = (self._payment_connect_timeout, self._timeout)
        self._logger.info(
            "提交支付请求：orderId=%s，reserveId=%s，支付类型 %s",
            mask_identifier(order.order_id),
            mask_identifier(order.reserve_id),
            config.pay_type,
            extra={"emoji": "💳"},
        )
        for attempt in range(1, self._payment_attempts + 1):
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
                    timeout=timeout,
                    allow_redirects=False,
                )
            except requests.ConnectTimeout as exc:
                if attempt < self._payment_attempts:
                    self._logger.warning(
                        "支付连接超时（第 %d/%d 次），%.1f 秒后重试",
                        attempt,
                        self._payment_attempts,
                        self._payment_retry_delay,
                        extra={"emoji": "🔁"},
                    )
                    self._sleep(self._payment_retry_delay)
                    continue
                self._logger.error(
                    "支付连接超时，已重试 %d 次，耗时 %s；请求未发出",
                    self._payment_attempts - 1,
                    elapsed_ms(started),
                    extra={"emoji": "⌛"},
                )
                raise PaymentError(
                    f"支付连接超时，已重试 {self._payment_attempts - 1} 次，请求未发出"
                ) from exc
            except requests.Timeout as exc:
                self._logger.error(
                    "支付请求超时，耗时 %s，结果未知；不重试",
                    elapsed_ms(started),
                    extra={"emoji": "⌛"},
                )
                raise PaymentError(
                    "支付请求超时，结果未知；为避免重复扣款，本次不会重试"
                ) from exc
            except requests.RequestException as exc:
                self._logger.error(
                    "支付请求网络异常：%s，耗时 %s，结果未知；不重试",
                    type(exc).__name__,
                    elapsed_ms(started),
                    extra={"emoji": "📡"},
                )
                raise PaymentError(
                    "支付请求异常，结果未知；为避免重复扣款，本次不会重试"
                ) from exc

            if response.status_code != 200:
                self._logger.error(
                    "支付返回 HTTP %d，耗时 %s；不重试",
                    response.status_code,
                    elapsed_ms(started),
                    extra={"emoji": "❌"},
                )
                raise PaymentError(
                    f"支付返回 HTTP {response.status_code}；为避免重复扣款，本次不会重试"
                )
            self._logger.info(
                "支付请求成功：HTTP %d，耗时 %s",
                response.status_code,
                elapsed_ms(started),
                extra={"emoji": "💰"},
            )
            return

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
