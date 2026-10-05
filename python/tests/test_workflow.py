import logging
from unittest import TestCase

from booking_assistant.client import OrderReference
from booking_assistant.config import BookingConfig
from booking_assistant.errors import PaymentError, VenueUnavailableError
from booking_assistant.workflow import BookingWorkflow


def config() -> BookingConfig:
    return BookingConfig.from_cli(
        cdbh="69",
        start_time="18:00:00",
        order_date_after_today=2,
        schedule_time="08:00:01",
        select_pay_type="-1",
        priority_list="295,134,297",
    )


class FakeClient:
    def __init__(self, payment_error: bool = False) -> None:
        self.created: list[str] = []
        self.paid: list[OrderReference] = []
        self.payment_error = payment_error

    def get_booking_token(self, _: BookingConfig) -> str:
        return "a" * 32

    def create_order(
        self, _: BookingConfig, venue_id: str, token: str
    ) -> OrderReference:
        self.created.append(venue_id)
        if venue_id == "295":
            raise VenueUnavailableError()
        return OrderReference("123", "456")

    def pay(
        self, _: BookingConfig, order: OrderReference, token: str
    ) -> None:
        self.paid.append(order)
        if self.payment_error:
            raise PaymentError("ambiguous")


class WorkflowTests(TestCase):
    def setUp(self) -> None:
        self.logger = logging.getLogger("workflow-tests")
        self.logger.addHandler(logging.NullHandler())

    def test_tries_priority_order_then_pays_exactly_once(self) -> None:
        client = FakeClient()

        with self.assertLogs(self.logger, level=logging.INFO) as captured:
            outcome = BookingWorkflow(config(), client, self.logger).run()  # type: ignore[arg-type]

        self.assertTrue(all(getattr(record, "emoji", "") for record in captured.records))
        self.assertGreater(
            len({record.emoji for record in captured.records if record.levelno == logging.INFO}),
            2,
        )

        self.assertEqual(client.created, ["295", "134"])
        self.assertEqual(client.paid, [OrderReference("123", "456")])
        self.assertEqual(outcome.venue_id, "134")  # type: ignore[union-attr]

    def test_does_not_try_another_venue_after_ambiguous_payment(self) -> None:
        client = FakeClient(payment_error=True)

        with self.assertRaises(PaymentError):
            BookingWorkflow(config(), client, self.logger).run()  # type: ignore[arg-type]

        self.assertEqual(client.created, ["295", "134"])
        self.assertEqual(len(client.paid), 1)
