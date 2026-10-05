"""Public command-line interface; option names match the original program."""

from __future__ import annotations

import argparse
import json
import os
import sys
from collections.abc import Sequence
from datetime import datetime, timedelta

import requests

from .auth import HustAuthenticator
from .client import BookingSiteClient
from .config import BookingConfig, Credentials
from .errors import BookingAssistantError, ConfigurationError
from .logging import configure_logging
from .scheduler import next_run_at, wait_until
from .workflow import BookingWorkflow


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
    return parser


def _configure_console_encoding() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if reconfigure:
            try:
                reconfigure(encoding="utf-8", errors="replace")
            except (AttributeError, OSError):
                pass


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
    session = requests.Session()
    try:
        target = next_run_at(config.schedule_at)
        logger.info("定时执行：%s", target.strftime("%m-%d %H:%M:%S"), extra={"emoji": "⏰"})
        # A fresh session matters, but authentication at the exact release time
        # would lose several seconds. Authenticate shortly beforehand instead.
        login_at = target - timedelta(seconds=30)
        wait_until(login_at)
        HustAuthenticator(session, logger).login(credentials)
        wait_until(target)

        started_at = datetime.now()
        # This is the sole production composition point that enables payment.
        client = BookingSiteClient(session, logger, payment_enabled=True)
        outcome = BookingWorkflow(config, client, logger).run()
        if outcome is None:
            return 1
        logger.info(
            "预约完成，耗时 %.2f 秒",
            (datetime.now() - started_at).total_seconds(),
            extra={"emoji": "⏱️"},
        )
        return 0
    except BookingAssistantError as exc:
        logger.error("预约失败：%s", exc, extra={"emoji": "💥"})
        return 1
    except KeyboardInterrupt:
        logger.warning("预约已取消", extra={"emoji": "⏹️"})
        return 130
    except Exception:
        logger.exception("预约出现异常", extra={"emoji": "🐛"})
        return 1
    finally:
        session.close()
