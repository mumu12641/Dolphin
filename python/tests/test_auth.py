from unittest import TestCase

from requests.cookies import RequestsCookieJar

from booking_assistant.auth import (
    BOOKING_HOME_URL,
    _has_booking_session,
    _login_error_message,
)


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
