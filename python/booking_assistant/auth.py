"""HUST CAS authentication using the caller's requests session."""

from __future__ import annotations

import json
import logging
import os
import re
import sys
from base64 import b64decode, b64encode
from html import unescape
from pathlib import Path
from typing import Any
from urllib.parse import urljoin

from Crypto.Cipher import PKCS1_v1_5
from Crypto.PublicKey import RSA
from .captcha import solve_captcha
from .config import Credentials
from .errors import AuthenticationError
from .html import input_values

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
        self._session = session
        self._logger = logger
        self._timeout = timeout

    def login(self, credentials: Credentials) -> None:
        """Authenticate in-place; credentials and cookies are never logged."""

        params = {"service": BOOKING_HOME_URL}
        headers = {"User-Agent": USER_AGENT}
        self._logger.info("正在登录统一认证", extra={"emoji": "🔐"})

        try:
            login_page = self._session.get(
                CAS_LOGIN_URL, params=params, headers=headers, timeout=self._timeout
            )
            login_page.raise_for_status()
            hidden = input_values(login_page.text)
            nonce = hidden.get("lt")
            execution = hidden.get("execution")
            if not nonce or not execution:
                raise AuthenticationError("统一认证登录页结构已变化")

            captcha_response = self._session.get(
                CAS_CAPTCHA_URL, headers=headers, timeout=self._timeout
            )
            captcha_response.raise_for_status()
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

            tesseract_path, tessdata_path = _find_tesseract()
            captcha = solve_captcha(
                captcha_response.content,
                executable=tesseract_path,
                tessdata_path=tessdata_path,
            )
            if not captcha:
                raise AuthenticationError("验证码识别失败")

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
            if not location:
                message = _login_error_message(response.text)
                raise AuthenticationError(message or "统一认证拒绝了登录请求")

            landing_page = self._session.get(
                urljoin(CAS_LOGIN_URL, location),
                headers=headers,
                allow_redirects=True,
                timeout=self._timeout,
            )
            if "pass.hust.edu.cn/cas/login" in landing_page.url:
                raise AuthenticationError("登录后又返回统一认证页面")
            if landing_page.status_code >= 400:
                raise AuthenticationError(
                    f"预约系统消费登录票据失败：HTTP {landing_page.status_code}"
                )
            if not _has_booking_session(self._session.cookies):
                raise AuthenticationError("登录成功后未获得预约系统会话")
        except AuthenticationError:
            raise
        except Exception as exc:
            raise AuthenticationError(f"登录请求失败: {exc}") from exc

        self._logger.info("登录成功", extra={"emoji": "🔓"})


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
