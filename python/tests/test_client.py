import logging
from unittest import TestCase

from booking_assistant.client import (
    PAYMENT_PATH,
    BookingSiteClient,
    OrderReference,
    extract_token,
)
from booking_assistant.config import BookingConfig
from booking_assistant.errors import (
    PaymentBlockedError,
    PaymentError,
    SiteProtocolError,
    VenueUnavailableError,
)

from .helpers import FakeResponse, FakeSession


def config() -> BookingConfig:
    return BookingConfig.from_cli(
        cdbh="69",
        start_time="18:00:00",
        order_date_after_today=2,
        schedule_time="08:00:01",
        select_pay_type="-1",
        priority_list="295,134",
    )


class BookingSiteClientTests(TestCase):
    def setUp(self) -> None:
        self.logger = logging.getLogger("booking-tests")
        self.logger.addHandler(logging.NullHandler())

    def test_extracts_both_known_token_formats(self) -> None:
        token = "0123456789abcdef0123456789abcdef"
        self.assertEqual(extract_token(f'{{"token": "{token}"}}'), token)
        escaped = f'name=\\"token\\" value=\\"{token}\\"'
        self.assertEqual(extract_token(escaped), token)

    def test_falls_back_to_second_read_only_token_page(self) -> None:
        token = "0123456789abcdef0123456789abcdef"
        session = FakeSession(
            get_responses=[
                FakeResponse(status_code=403, text="blocked"),
                FakeResponse(text=f'<input name="token" value="{token}">'),
            ]
        )
        client = BookingSiteClient(session, self.logger)

        self.assertEqual(client.get_booking_token(config()), token)
        self.assertEqual(len(session.get_calls), 2)

    def test_token_reads_are_bounded(self) -> None:
        session = FakeSession(
            get_responses=[FakeResponse(status_code=503) for _ in range(6)]
        )
        client = BookingSiteClient(
            session, self.logger, token_attempts=3, token_retry_delay=0
        )

        with self.assertRaises(SiteProtocolError):
            client.get_booking_token(config())

        self.assertEqual(len(session.get_calls), 6)

    def test_public_home_redirect_is_reported_without_retrying(self) -> None:
        session = FakeSession(
            get_responses=[
                FakeResponse(
                    status_code=200,
                    url="http://pecg.hust.edu.cn/wescms/",
                    text="public home",
                )
            ]
        )
        client = BookingSiteClient(session, self.logger)

        with self.assertRaisesRegex(SiteProtocolError, "公共场馆首页"):
            client.get_booking_token(config())

        self.assertEqual(len(session.get_calls), 1)

    def test_parses_order_ids_without_depending_on_attribute_order(self) -> None:
        session = FakeSession(
            post_responses=[
                FakeResponse(
                    text=(
                        '<input value="123" type="hidden" name="orderId">'
                        '<input name="reserveId" value="456" type="hidden">'
                    )
                )
            ]
        )
        client = BookingSiteClient(session, self.logger)

        order = client.create_order(config(), "295", "a" * 32)

        self.assertEqual(order, OrderReference("123", "456"))

    def test_reports_occupied_venue_as_retryable(self) -> None:
        session = FakeSession(
            post_responses=[FakeResponse(text="当前时段场地已被人预约")]
        )
        client = BookingSiteClient(session, self.logger)

        with self.assertRaises(VenueUnavailableError):
            client.create_order(config(), "295", "a" * 32)

    def test_decodes_utf8_site_messages_mislabeled_as_latin1(self) -> None:
        message = "当前时段场地已被人预约"
        raw = message.encode("utf-8")
        session = FakeSession(
            post_responses=[
                FakeResponse(
                    text=raw.decode("latin1"),
                    content=raw,
                    encoding="ISO-8859-1",
                    apparent_encoding="utf-8",
                )
            ]
        )
        client = BookingSiteClient(session, self.logger)

        with self.assertRaises(VenueUnavailableError):
            client.create_order(config(), "295", "a" * 32)

    def test_payment_is_blocked_before_any_http_request_by_default(self) -> None:
        session = FakeSession()
        client = BookingSiteClient(session, self.logger)

        with self.assertRaises(PaymentBlockedError):
            client.pay(config(), OrderReference("123", "456"), "a" * 32)

        self.assertEqual(session.post_calls, [])

    def test_opted_in_payment_uses_one_fake_request(self) -> None:
        session = FakeSession(post_responses=[FakeResponse(status_code=200)])
        client = BookingSiteClient(session, self.logger, payment_enabled=True)

        client.pay(config(), OrderReference("123", "456"), "a" * 32)

        self.assertEqual(len(session.post_calls), 1)
        self.assertTrue(session.post_calls[0][0].endswith(PAYMENT_PATH))

    def test_failed_payment_is_never_retried(self) -> None:
        session = FakeSession(
            post_responses=[FakeResponse(status_code=504), FakeResponse(status_code=200)]
        )
        client = BookingSiteClient(session, self.logger, payment_enabled=True)

        with self.assertRaises(PaymentError):
            client.pay(config(), OrderReference("123", "456"), "a" * 32)

        self.assertEqual(len(session.post_calls), 1)
