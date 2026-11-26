"""Opt-in smoke test for the real HUST CAS login endpoint.

Run it explicitly with real credentials:

    BOOKING_LIVE_TESTS=1 \
    BOOKING_TEST_USERNAME=... \
    BOOKING_TEST_PASSWORD=... \
    .venv/bin/python -m unittest tests.test_live_auth -v

It only authenticates and checks the booking-site session cookie. It never
requests a booking token, creates an order, or calls the payment endpoint.
"""

from __future__ import annotations

import os
import unittest

import requests

from booking_assistant.auth import HustAuthenticator
from booking_assistant.config import Credentials
from booking_assistant.logging import configure_logging


def _live_credentials() -> Credentials:
    username = os.environ.get("BOOKING_TEST_USERNAME")
    password = os.environ.get("BOOKING_TEST_PASSWORD")
    if not username or not password:
        raise unittest.SkipTest(
            "set BOOKING_TEST_USERNAME and BOOKING_TEST_PASSWORD for live login"
        )
    return Credentials(username=username, password=password)


@unittest.skipUnless(
    os.environ.get("BOOKING_LIVE_TESTS") == "1",
    "set BOOKING_LIVE_TESTS=1 to run against the real site",
)
class LiveAuthenticationTests(unittest.TestCase):
    def test_real_cas_login_creates_booking_session(self) -> None:
        logger = configure_logging("booking-live-test")
        session = requests.Session()
        try:
            HustAuthenticator(session, logger, timeout=30).login(_live_credentials())
            self.assertTrue(
                any(
                    cookie.name == "JSESSIONID"
                    and "pecg.hust.edu.cn" in cookie.domain
                    for cookie in session.cookies
                ),
                "login succeeded without a booking-site session cookie",
            )
        finally:
            session.close()
