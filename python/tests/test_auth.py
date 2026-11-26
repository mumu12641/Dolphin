import logging
from base64 import b64encode
from unittest import TestCase
from unittest.mock import patch

import requests
from Crypto.PublicKey import RSA
from requests.cookies import RequestsCookieJar

from booking_assistant.auth import (
    BOOKING_HOME_URL,
    CAS_CAPTCHA_URL,
    CAS_LOGIN_URL,
    CAS_RSA_URL,
    HustAuthenticator,
    _has_booking_session,
    _login_error_message,
)
from booking_assistant.config import Credentials
from booking_assistant.errors import AuthenticationError

from .helpers import FakeResponse, FakeSession

LOGIN_PAGE = '<input name="lt" value="LT-demo"><input name="execution" value="e1s1">'
CAPTCHA_BYTES = b"GIF89a-demo-captcha"
BOOKING_LANDING = "https://pecg.hust.edu.cn/cggl/index1?ticket=ST-demo"


class AuthenticationResponseTests(TestCase):
    def test_cas_service_matches_current_https_booking_entry(self) -> None:
        self.assertEqual(
            BOOKING_HOME_URL, "https://pecg.hust.edu.cn/cggl/index1"
        )

    def test_recognizes_booking_cookie_by_name_and_domain(self) -> None:
        cookies = RequestsCookieJar()
        cookies.set("JSESSIONID", "cas-session", domain="pass.hust.edu.cn")
        self.assertFalse(_has_booking_session(cookies))

        cookies.set("JSESSIONID", "booking-session", domain="pecg.hust.edu.cn")
        self.assertTrue(_has_booking_session(cookies))

    def test_extracts_remaining_login_attempts_from_response_page(self) -> None:
        document = (
            "<div>连续登录失败5次，账号将被锁定1分钟，剩余次数2</div>"
        )
        self.assertEqual(
            _login_error_message(document),
            "连续登录失败5次，账号将被锁定1分钟，剩余次数2",
        )


class AuthenticationFlowTests(TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        key = RSA.generate(1024)
        cls.public_key = b64encode(key.publickey().export_key()).decode("ascii")

    def setUp(self) -> None:
        self.logger = logging.getLogger("auth-flow-tests")
        self.logger.addHandler(logging.NullHandler())
        self.credentials = Credentials("demo-user", "demo-password")

    def make_session(
        self,
        *,
        login_response: FakeResponse | None = None,
        landing_url: str = BOOKING_LANDING,
        landing_status: int = 200,
        booking_cookie: bool = True,
        landing_retries: list[FakeResponse] | None = None,
    ) -> FakeSession:
        cookies = RequestsCookieJar()
        if booking_cookie:
            cookies.set("JSESSIONID", "booking-session", domain="pecg.hust.edu.cn")
        session = FakeSession(
            get_responses=[
                FakeResponse(text=LOGIN_PAGE),
                FakeResponse(content=CAPTCHA_BYTES),
                FakeResponse(status_code=landing_status, url=landing_url),
                *(landing_retries or []),
            ],
            post_responses=[
                FakeResponse(json_data={"publicKey": self.public_key}),
                login_response
                or FakeResponse(
                    status_code=302,
                    headers={"Location": BOOKING_LANDING},
                ),
            ],
        )
        session.cookies = cookies
        return session

    def test_full_login_flow_uses_every_cas_endpoint_and_hides_secrets(self) -> None:
        session = self.make_session()

        with patch("booking_assistant.auth.solve_captcha", return_value="1234"):
            with self.assertLogs(self.logger, level=logging.INFO) as captured:
                HustAuthenticator(session, self.logger).login(self.credentials)

        self.assertEqual(len(session.get_calls), 3)
        self.assertEqual(len(session.post_calls), 2)
        self.assertEqual(session.get_calls[0][0], CAS_LOGIN_URL)
        self.assertEqual(
            session.get_calls[0][1]["params"], {"service": BOOKING_HOME_URL}
        )
        self.assertEqual(session.get_calls[1][0], CAS_CAPTCHA_URL)
        self.assertEqual(session.get_calls[2][0], BOOKING_LANDING)
        self.assertEqual(session.post_calls[0][0], CAS_RSA_URL)
        self.assertEqual(session.post_calls[1][0], CAS_LOGIN_URL)

        login_kwargs = session.post_calls[1][1]
        self.assertFalse(login_kwargs["allow_redirects"])
        data = login_kwargs["data"]
        self.assertEqual(data["lt"], "LT-demo")
        self.assertEqual(data["execution"], "e1s1")
        self.assertEqual(data["code"], "1234")
        self.assertEqual(data["_eventId"], "submit")
        self.assertNotEqual(data["ul"], self.credentials.username)
        self.assertNotEqual(data["pl"], self.credentials.password)

        messages = "\n".join(record.getMessage() for record in captured.records)
        self.assertIn("登录成功", messages)
        self.assertNotIn(self.credentials.password, messages)
        self.assertNotIn("LT-demo", messages)
        self.assertNotIn("ST-demo", messages)

    def test_landing_429_stops_without_replaying_the_ticket(self) -> None:
        session = self.make_session(landing_status=429)

        with patch("booking_assistant.auth.solve_captcha", return_value="1234"):
            with self.assertRaisesRegex(AuthenticationError, "429"):
                HustAuthenticator(session, self.logger).login(self.credentials)

        self.assertEqual(len(session.get_calls), 3)

    def test_missing_login_form_fields_stops_before_captcha(self) -> None:
        session = FakeSession(get_responses=[FakeResponse(text="<html></html>")])

        with self.assertRaisesRegex(AuthenticationError, "结构已变化"):
            with self.assertLogs(self.logger, level=logging.WARNING) as captured:
                HustAuthenticator(session, self.logger).login(self.credentials)

        self.assertEqual(len(session.get_calls), 1)
        self.assertEqual(session.post_calls, [])
        self.assertIn(
            "登录阶段失败（打开统一认证登录页）",
            "\n".join(record.getMessage() for record in captured.records),
        )

    def test_captcha_failure_does_not_submit_credentials(self) -> None:
        session = self.make_session()

        with patch("booking_assistant.auth.solve_captcha", return_value=""):
            with self.assertRaisesRegex(AuthenticationError, "验证码识别失败"):
                HustAuthenticator(session, self.logger).login(self.credentials)

        self.assertEqual(len(session.post_calls), 1)
        self.assertEqual(session.post_calls[0][0], CAS_RSA_URL)

    def test_rejected_login_reports_site_message(self) -> None:
        message = "连续登录失败5次，账号将被锁定1分钟，剩余次数2"
        session = self.make_session(
            login_response=FakeResponse(
                status_code=200, text=f'<div id="errorMsg">{message}</div>'
            )
        )

        with patch("booking_assistant.auth.solve_captcha", return_value="1234"):
            with self.assertRaisesRegex(AuthenticationError, message):
                with self.assertLogs(self.logger, level=logging.WARNING) as captured:
                    HustAuthenticator(session, self.logger).login(self.credentials)

        self.assertEqual(len(session.post_calls), 2)
        self.assertIn(
            "登录阶段失败（提交登录表单）",
            "\n".join(record.getMessage() for record in captured.records),
        )

    def test_landing_back_on_cas_is_rejected(self) -> None:
        session = self.make_session(
            landing_url="https://pass.hust.edu.cn/cas/login?service=x"
        )

        with patch("booking_assistant.auth.solve_captcha", return_value="1234"):
            with self.assertRaisesRegex(AuthenticationError, "返回统一认证"):
                HustAuthenticator(session, self.logger).login(self.credentials)

    def test_missing_booking_session_is_rejected(self) -> None:
        session = self.make_session(booking_cookie=False)

        with patch("booking_assistant.auth.solve_captcha", return_value="1234"):
            with self.assertRaisesRegex(AuthenticationError, "未获得预约系统会话"):
                HustAuthenticator(session, self.logger).login(self.credentials)

    def test_network_error_is_reported_with_stage(self) -> None:
        session = FakeSession(
            get_errors=[requests.ConnectionError("dns failure")]
        )

        with self.assertRaisesRegex(AuthenticationError, "登录请求失败"):
            with self.assertLogs(self.logger, level=logging.ERROR) as captured:
                HustAuthenticator(session, self.logger).login(self.credentials)

        messages = "\n".join(record.getMessage() for record in captured.records)
        self.assertIn("登录请求异常（打开统一认证登录页）", messages)
        self.assertIn("ConnectionError", messages)
        self.assertNotIn(self.credentials.password, messages)
