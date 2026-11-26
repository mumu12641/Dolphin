"""Public command-line interface; option names match the original program."""

from __future__ import annotations

import argparse
import json
import logging
import os
import sys
from collections.abc import Sequence
from contextlib import closing
from datetime import datetime, timedelta

import requests

from . import __version__
from .auth import CAS_LOGIN_URL, HustAuthenticator
from .client import BookingSiteClient
from .config import BookingConfig, Credentials
from .courts import court_number
from .errors import (
    AccountBlacklistedError,
    AuthenticationError,
    BookingAssistantError,
    ConfigurationError,
)
from .logging import (
    configure_logging,
    log_http_responses,
    mask_identifier,
    save_logs,
)
from .network import prefer_ipv4
from .scheduler import next_run_at, wait_until
from .workflow import BookingWorkflow

_STAMP_FORMAT = "%Y-%m-%d %H:%M:%S"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="HUST Booking Assistant")
    parser.add_argument("--cdbh", type=str, help="场地编号")
    parser.add_argument("--start_time", type=str, help="开始时间，格式为HH:MM:SS")
    parser.add_argument(
        "--order_date_after_today", type=int, help="预订日期在今天之后的天数"
    )
    parser.add_argument(
        "--schedule_time", type=str, help="定时任务执行时间，格式为HH:MM:SS"
    )
    parser.add_argument("--select_pay_type", type=str, help="支付类型")
    parser.add_argument(
        "--priority_list", type=str, help="场地优先级列表，逗号分隔的字符串"
    )
    parser.add_argument("--user_name", type=str, help="用户名")
    parser.add_argument("--password", type=str, help="密码")
    parser.add_argument("--check-account", action="store_true", help="仅检查登录和预约资格，检查后退出")
    return parser


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except (AttributeError, OSError):
                pass


def _log_run_config(
    logger: logging.Logger, config: BookingConfig, credentials: Credentials
) -> None:
    priority = " → ".join(
        f"{court_number(config.stadium_id, venue_id) or '?'} 号场({venue_id})"
        for venue_id in config.priority_venue_ids
    )
    logger.info("启动 HUST 羽毛球场预约助手 v%s", __version__, extra={"emoji": "🚀"})
    logger.info(
        "场馆编号 %s；预约日期 %s（今天 +%d 天）；预约时段 %s—%s",
        config.stadium_id,
        config.booking_date().isoformat(),
        config.days_after_today,
        config.start_time_text,
        config.end_time_text,
        extra={"emoji": "🏟️"},
    )
    logger.info(
        "优先场地顺序：%s", priority, extra={"emoji": "📋"}
    )
    logger.info(
        "支付类型 %s；账号 %s（已脱敏）",
        config.pay_type,
        mask_identifier(credentials.username, visible=2),
        extra={"emoji": "👤"},
    )


def _credentials(args: argparse.Namespace) -> Credentials:
    if os.environ.get("DOLPHIN_CREDENTIALS_STDIN") != "1":
        return Credentials(username=args.user_name, password=args.password)

    try:
        values = json.loads(sys.stdin.readline())
        if not isinstance(values, dict):
            raise ValueError("expected an object")
        return Credentials(username=values.get("username"), password=values.get("password"))
    except (ValueError, TypeError) as exc:
        raise ConfigurationError("无法读取桌面端传入的账号信息") from exc


def _login_and_check(session: requests.Session, config: BookingConfig,
                     credentials: Credentials, logger: logging.Logger) -> None:
    started = datetime.now()
    HustAuthenticator(session, logger).login(credentials)
    BookingSiteClient(session, logger).check_account_access(config)
    logger.info("登录及账号检查耗时 %.2f 秒", (datetime.now() - started).total_seconds(),
                extra={"emoji": "⏱️"})


def _cas_login_page_forbidden(error: AuthenticationError) -> bool:
    cause = error.__cause__
    if not isinstance(cause, requests.HTTPError) or cause.response is None:
        return False
    request = cause.response.request
    return (
        cause.response.status_code == 403
        and request is not None
        and request.method == "GET"
        and request.url.partition("?")[0] == CAS_LOGIN_URL
    )


def main(argv: Sequence[str] | None = None) -> int:
    _configure_console_encoding()
    parser = build_parser()
    args = parser.parse_args(argv)

    try:
        config = BookingConfig.from_cli(
            cdbh=args.cdbh,
            start_time=args.start_time,
            order_date_after_today=args.order_date_after_today,
            schedule_time=args.schedule_time,
            select_pay_type=args.select_pay_type,
            priority_list=args.priority_list,
        )
        credentials = _credentials(args)
    except ConfigurationError as exc:
        parser.error(str(exc))

    logger = configure_logging()
    prefer_ipv4()
    logger.debug("网络请求优先使用 IPv4", extra={"emoji": "🛰️"})
    _log_run_config(logger, config, credentials)
    session: requests.Session | None = None
    try:
        target = next_run_at(config.schedule_at)
        now = datetime.now()
        logger.info(
            "当前时间 %s（时区 %s）；定时执行 %s，约 %.0f 秒后",
            now.strftime(_STAMP_FORMAT),
            now.astimezone().tzname() or "本地",
            target.strftime(_STAMP_FORMAT),
            max(0.0, (target - now).total_seconds()),
            extra={"emoji": "⏰"},
        )
        logger.info("启动时检查账号状态", extra={"emoji": "🛂"})
        try:
            with closing(requests.Session()) as startup_session:
                log_http_responses(startup_session, logger)
                _login_and_check(startup_session, config, credentials, logger)
        except AuthenticationError as exc:
            if args.check_account or not _cas_login_page_forbidden(exc):
                raise
            logger.warning(
                "统一认证登录页返回 HTTP 403，启动时无法检查预约资格。将按原计划定时登录。",
                extra={"emoji": "⚠️"},
            )
        if args.check_account:
            logger.info("账号检查完成，程序退出", extra={"emoji": "✅"})
            return 0

        session = requests.Session()
        log_http_responses(session, logger)
        # A fresh session matters, but authentication at the exact release time
        # would lose several seconds. Authenticate shortly beforehand instead.
        login_at = target - timedelta(seconds=30)
        logger.info(
            "计划登录时间：%s（开抢前 30 秒）", login_at.strftime(_STAMP_FORMAT),
            extra={"emoji": "🔐"},
        )
        wait_until(login_at)
        login_started = datetime.now()
        HustAuthenticator(session, logger).login(credentials)
        logger.info(
            "登录阶段耗时 %.2f 秒",
            (datetime.now() - login_started).total_seconds(),
            extra={"emoji": "⏱️"},
        )
        wait_until(target)

        started_at = datetime.now()
        logger.info("到达执行时刻，开始预约", extra={"emoji": "🔔"})
        # This is the sole production composition point that enables payment.
        client = BookingSiteClient(session, logger, payment_enabled=True)
        outcome = BookingWorkflow(config, client, logger).run()
        if outcome is None:
            logger.warning("预约未成功：没有可用场地", extra={"emoji": "📭"})
            return 1
        logger.info(
            "预约完成：%s 号场，日期 %s，orderId=%s，耗时 %.2f 秒",
            court_number(config.stadium_id, outcome.venue_id),
            config.booking_date().isoformat(),
            mask_identifier(outcome.order.order_id),
            (datetime.now() - started_at).total_seconds(),
            extra={"emoji": "⏱️"},
        )
        return 0
    except AccountBlacklistedError as exc:
        logger.error("%s；已停止本次预约，请联系场馆管理员核实", exc, extra={"emoji": "⛔"})
        return 1
    except BookingAssistantError as exc:
        logger.error(
            "预约失败（%s）：%s", type(exc).__name__, exc, extra={"emoji": "💥"}
        )
        return 1
    except KeyboardInterrupt:
        logger.warning("预约已取消", extra={"emoji": "⏹️"})
        return 130
    except Exception as exc:
        logger.error("预约出现异常（%s）", type(exc).__name__, extra={"emoji": "🐛"})
        return 1
    finally:
        logger.debug("关闭 HTTP 会话", extra={"emoji": "🔌"})
        if session is not None:
            session.close()
        log_path = save_logs(logger)
        if log_path is not None:
            logger.info("本次运行日志已保存到 %s", log_path, extra={"emoji": "📝"})
