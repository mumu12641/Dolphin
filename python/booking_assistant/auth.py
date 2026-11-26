"""HUST CAS authentication using the caller's requests session."""

from __future__ import annotations

import json
import logging
import os
import re
import sys
import time
from base64 import b64decode, b64encode
from html import unescape
from pathlib import Path
from typing import Any
from urllib.parse import urljoin, urlparse

import requests
from Crypto.Cipher import PKCS1_v1_5
from Crypto.PublicKey import RSA
from .captcha import solve_captcha
from .config import Credentials
from .errors import AuthenticationError
from .html import input_values
from .logging import elapsed_ms
from .network import prefer_ipv4

CAS_LOGIN_URL = "https://pass.hust.edu.cn/cas/login"
CAS_CAPTCHA_URL = "https://pass.hust.edu.cn/cas/code"
CAS_RSA_URL = "https://pass.hust.edu.cn/cas/rsa"
# The booking entry currently redirects to CAS with this exact HTTPS service
# value. CAS tickets are service-bound; using the former HTTP value makes the
# booking application fail while consuming the callback ticket.
BOOKING_HOME_URL = "https://pecg.hust.edu.cn/cggl/index1"
USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"
)


def _runtime_roots() -> list[Path]:
    roots: list[Path] = []
    extracted = getattr(sys, "_MEIPASS", None)
    if extracted:
        roots.append(Path(extracted))
    if getattr(sys, "frozen", False):
        roots.append(Path(sys.executable).resolve().parent)
    else:
        roots.append(Path(__file__).resolve().parent.parent)
    roots.append(Path.cwd())
    return list(dict.fromkeys(roots))


def _find_tesseract() -> tuple[str | None, str | None]:
    for root in _runtime_roots():
        executable = root / "Tesseract-OCR" / "tesseract.exe"
        tessdata = root / "Tesseract-OCR" / "tessdata"
        if executable.is_file() and tessdata.is_dir():
            return os.fspath(executable), os.fspath(tessdata)
    return None, None


def _response_json(response: Any) -> dict[str, Any]:
    try:
        value = response.json()
    except (AttributeError, json.JSONDecodeError, ValueError) as exc:
        raise AuthenticationError("统一认证未返回有效 RSA 公钥") from exc
    if not isinstance(value, dict):
        raise AuthenticationError("统一认证返回了无法识别的 RSA 公钥")
    return value


class HustAuthenticator:
    def __init__(self, session: Any, logger: logging.Logger, timeout: float = 20) -> None:
        prefer_ipv4()
        self._session = session
        self._logger = logger
        self._timeout = timeout

    def login(self, credentials: Credentials) -> None:
        """Authenticate in-place; credentials and cookies are never logged."""

        params = {"service": BOOKING_HOME_URL}
        headers = {"User-Agent": USER_AGENT}
        started = time.perf_counter()
        stage = "打开统一认证登录页"
        self._logger.info(
            "正在登录统一认证（服务：%s）",
            urlparse(BOOKING_HOME_URL).netloc,
            extra={"emoji": "🔐"},
        )

        try:
            step_started = time.perf_counter()
            login_page = self._session.get(
                CAS_LOGIN_URL, params=params, headers=headers, timeout=self._timeout
            )
            login_page.raise_for_status()
            hidden = input_values(login_page.text)
            nonce = hidden.get("lt")
            execution = hidden.get("execution")
            self._logger.info(
                "登录页已加载：HTTP %d，耗时 %s，lt=%s，execution=%s",
                login_page.status_code,
                elapsed_ms(step_started),
                "已获取" if nonce else "缺失",
                "已获取" if execution else "缺失",
                extra={"emoji": "📄"},
            )
            if not nonce or not execution:
                raise AuthenticationError("统一认证登录页结构已变化")

            stage = "下载验证码"
            step_started = time.perf_counter()
            captcha_response = self._session.get(
                CAS_CAPTCHA_URL, headers=headers, timeout=self._timeout
            )
            captcha_response.raise_for_status()
            self._logger.info(
                "验证码已下载：HTTP %d，%d 字节，耗时 %s",
                captcha_response.status_code,
                len(getattr(captcha_response, "content", b"") or b""),
                elapsed_ms(step_started),
                extra={"emoji": "🧩"},
            )

            stage = "获取 RSA 公钥"
            step_started = time.perf_counter()
            key_response = self._session.post(
                CAS_RSA_URL, headers=headers, timeout=self._timeout
            )
            key_response.raise_for_status()

            public_key = _response_json(key_response).get("publicKey")
            if not isinstance(public_key, str):
                raise AuthenticationError("统一认证响应中缺少 RSA 公钥")
            cipher = PKCS1_v1_5.new(RSA.import_key(b64decode(public_key)))
            encrypted_username = b64encode(
                cipher.encrypt(credentials.username.encode("utf-8"))
            ).decode("ascii")
            encrypted_password = b64encode(
                cipher.encrypt(credentials.password.encode("utf-8"))
            ).decode("ascii")
            self._logger.info(
                "RSA 公钥已就绪：HTTP %d，密钥 %d 字节，账号与密码已本地加密，耗时 %s",
                key_response.status_code,
                len(public_key),
                elapsed_ms(step_started),
                extra={"emoji": "🔑"},
            )

            stage = "识别验证码"
            tesseract_path, tessdata_path = _find_tesseract()
            self._logger.debug(
                "OCR 运行时：tesseract=%s，tessdata=%s",
                tesseract_path or "系统 PATH",
                tessdata_path or "默认",
                extra={"emoji": "🧠"},
            )
            captcha = solve_captcha(
                captcha_response.content,
                executable=tesseract_path,
                tessdata_path=tessdata_path,
                logger=self._logger,
            )
            if not captcha:
                raise AuthenticationError("验证码识别失败")

            stage = "提交登录表单"
            step_started = time.perf_counter()
            response = self._session.post(
                CAS_LOGIN_URL,
                params=params,
                headers=headers,
                data={
                    "rsa": None,
                    "ul": encrypted_username,
                    "pl": encrypted_password,
                    "code": captcha,
                    "phoneCode": None,
                    "lt": nonce,
                    "execution": execution,
                    "_eventId": "submit",
                },
                allow_redirects=False,
                timeout=self._timeout,
            )
            location = response.headers.get("Location")
            self._logger.info(
                "登录表单已提交：HTTP %d，耗时 %s，%s",
                response.status_code,
                elapsed_ms(step_started),
                "收到跳转" if location else "没有跳转",
                extra={"emoji": "📤"},
            )
            if not location:
                message = _login_error_message(response.text)
                if message:
                    for value in (credentials.username, credentials.password, captcha, nonce, execution):
                        message = message.replace(value, "[已隐藏]")
                raise AuthenticationError(message or "统一认证拒绝了登录请求")

            stage = "预约系统消费登录票据"
            redirect_host = urlparse(location).netloc or "未知主机"
            self._logger.info(
                "统一认证已放行，正在跟随跳转到 %s（票据内容不记录）",
                redirect_host,
                extra={"emoji": "↪️"},
            )
            step_started = time.perf_counter()
            landing_page = self._session.get(
                urljoin(CAS_LOGIN_URL, location),
                headers=headers,
                allow_redirects=True,
                timeout=self._timeout,
            )
            self._logger.info(
                "预约系统响应：HTTP %d，最终路径 %s，耗时 %s",
                landing_page.status_code,
                urlparse(getattr(landing_page, "url", "")).path or "(未知)",
                elapsed_ms(step_started),
                extra={"emoji": "🌐"},
            )
            if "pass.hust.edu.cn/cas/login" in landing_page.url:
                raise AuthenticationError("登录后又返回统一认证页面")
            if landing_page.status_code >= 400:
                raise AuthenticationError(
                    f"预约系统消费登录票据失败：HTTP {landing_page.status_code}"
                )

            stage = "检查预约系统会话"
            cookie_names = sorted(
                {
                    cookie.name
                    for cookie in getattr(self._session, "cookies", [])
                    if "pecg.hust.edu.cn" in getattr(cookie, "domain", "")
                }
            )
            self._logger.debug(
                "预约系统 Cookie 名称：%s（值不记录）",
                ", ".join(cookie_names) or "无",
                extra={"emoji": "🍪"},
            )
            if not _has_booking_session(self._session.cookies):
                raise AuthenticationError("登录成功后未获得预约系统会话")
        except AuthenticationError as exc:
            self._logger.warning(
                "登录阶段失败（%s）：%s，累计耗时 %s",
                stage,
                exc,
                elapsed_ms(started),
                extra={"emoji": "🚫"},
            )
            raise
        except Exception as exc:
            self._logger.error(
                "登录请求异常（%s）：%s，累计耗时 %s",
                stage,
                type(exc).__name__,
                elapsed_ms(started),
                extra={"emoji": "💥"},
            )
            status = exc.response.status_code if isinstance(exc, requests.HTTPError) and exc.response is not None else None
            reason = f"HTTP {status}" if status is not None else type(exc).__name__
            raise AuthenticationError(f"登录请求失败（{stage}）：{reason}") from exc

        self._logger.info(
            "登录成功，统一认证耗时 %s", elapsed_ms(started), extra={"emoji": "🔓"}
        )


def _login_error_message(document: str) -> str | None:
    match = re.search(
        r'<[^>]+(?:id|class)=["\'][^"\']*(?:error|msg)[^"\']*["\'][^>]*>'
        r"\s*(.*?)\s*</[^>]+>",
        document,
        re.IGNORECASE | re.DOTALL,
    )
    if match:
        message = re.sub(r"<[^>]+>", "", match.group(1)).strip()
        if message:
            return unescape(message)

    plain_text = re.sub(r"\s+", "", unescape(re.sub(r"<[^>]+>", " ", document)))
    remaining = re.search(
        r"连续登录失败\d+次，?账号将被锁定\d+分钟，?剩余次数\d+", plain_text
    )
    return remaining.group(0) if remaining else None


def _has_booking_session(cookies: Any) -> bool:
    return any(
        cookie.name == "JSESSIONID" and "pecg.hust.edu.cn" in cookie.domain
        for cookie in cookies
    )
