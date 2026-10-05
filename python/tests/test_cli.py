from io import StringIO
from unittest import TestCase
from unittest.mock import patch

from booking_assistant.cli import _credentials, build_parser


class CliContractTests(TestCase):
    def test_original_option_names_are_unchanged(self) -> None:
        parser = build_parser()
        actual = {
            option
            for action in parser._actions
            for option in action.option_strings
            if option.startswith("--") and option != "--help"
        }
        self.assertEqual(
            actual,
            {
                "--cdbh",
                "--start_time",
                "--order_date_after_today",
                "--schedule_time",
                "--select_pay_type",
                "--priority_list",
                "--user_name",
                "--password",
            },
        )

    def test_reads_desktop_credentials_from_stdin(self) -> None:
        args = build_parser().parse_args([])
        with patch.dict("os.environ", {"DOLPHIN_CREDENTIALS_STDIN": "1"}):
            with patch("sys.stdin", StringIO('{"username":"user","password":"secret"}\n')):
                credentials = _credentials(args)
        self.assertEqual(credentials.username, "user")
        self.assertEqual(credentials.password, "secret")
