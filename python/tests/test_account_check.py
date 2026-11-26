from contextlib import ExitStack
from datetime import datetime, timedelta
import logging
from types import SimpleNamespace
from unittest import TestCase
from unittest.mock import MagicMock, call, patch

import requests

from booking_assistant import cli
from booking_assistant.client import BASE_URL, BookingSiteClient
from booking_assistant.errors import AccountBlacklistedError, AuthenticationError, SiteProtocolError
from .helpers import FakeResponse, FakeSession
from .test_client import config

TOKEN = '0123456789abcdef0123456789abcdef'
ENTRY = BASE_URL + '/front/syqk'
BLACKLIST = '您已被加入黑名单，暂时无法进行预约！'


class AccountAccessTests(TestCase):
    def client(self, responses):
        session = FakeSession(get_responses=responses)
        return BookingSiteClient(session, logging.getLogger('account-check-tests')), session

    def test_blocked_redirect_chain_stops_before_any_order_request(self):
        client, session = self.client([
            FakeResponse(status_code=302, headers={'Location': '/cggl/'}),
            FakeResponse(status_code=302, headers={'Location': '/cggl/index'}),
            FakeResponse(status_code=302, headers={'Location': 'http://pecg.hust.edu.cn/cggl/front/yuyuexz'}),
            FakeResponse(url=BASE_URL + '/front/yuyuexz', text=f'<script>alert("{BLACKLIST}")</script><input name="token" value="{TOKEN}">'),
        ])
        with self.assertRaises(AccountBlacklistedError):
            client.check_account_access(config())
        self.assertEqual(len(session.get_calls), 4)
        cfg = config()
        self.assertEqual(session.get_calls[0][1]['params'], {
            'cdbh': '69', 'date': cfg.booking_date().isoformat(),
            'starttime': '18:00:00', 'endtime': '20:00:00',
        })
        self.assertTrue(all(url.startswith('https://pecg.hust.edu.cn/') for url, _ in session.get_calls))
        self.assertTrue(all(kwargs['allow_redirects'] is False for _, kwargs in session.get_calls))
        self.assertEqual(session.get_calls[0][1]['headers']['Referer'], BASE_URL + '/front/yuyuexz')
        self.assertEqual(session.post_calls, [])

    def test_browser_listing_referer_is_required_to_receive_the_blacklist_warning(self):
        client, session = self.client([])

        def browser_entry(url, **kwargs):
            session.get_calls.append((url, kwargs))
            page = BASE_URL + '/front/yuyuexz'
            body = f'<input name="token" value="{TOKEN}">'
            if kwargs.get('headers', {}).get('Referer') == page:
                body += f'<script>onload=function(){{alert(HTMLDecode("{BLACKLIST}"));}}</script>'
            return FakeResponse(url=page, text=body)

        session.get = browser_entry
        with self.assertRaises(AccountBlacklistedError):
            client.check_account_access(config())
        self.assertEqual(len(session.get_calls), 1)
        self.assertEqual(session.post_calls, [])

    def test_general_blacklist_warning_does_not_mark_a_healthy_account(self):
        client, session = self.client([FakeResponse(url=ENTRY, text=(
            f'如查出有伪造信息，则将拉入黑名单。<input name="token" value="{TOKEN}">'))])
        client.check_account_access(config())
        self.assertEqual(len(session.get_calls), 1)
        self.assertEqual(session.post_calls, [])

    def test_html_entity_encoded_denial_is_recognized(self):
        encoded = ''.join(f'&#{ord(character)};' for character in BLACKLIST)
        client, _ = self.client([FakeResponse(url=ENTRY, text=encoded)])
        with self.assertRaises(AccountBlacklistedError):
            client.check_account_access(config())

    def test_listing_token_is_not_treated_as_proof_of_eligibility(self):
        client, _ = self.client([FakeResponse(url=BASE_URL + '/front/yuyuexz',
            text=f'<input name="token" value="{TOKEN}">')])
        with self.assertRaisesRegex(SiteProtocolError, '无法确认'):
            client.check_account_access(config())

    def test_unrecognized_booking_page_does_not_pass(self):
        client, _ = self.client([FakeResponse(url=ENTRY, text='暂时无法提供预约服务')])
        with self.assertRaisesRegex(SiteProtocolError, '无法确认'):
            client.check_account_access(config())

    def test_login_redirect_is_not_followed(self):
        client, session = self.client([FakeResponse(status_code=302,
            headers={'Location': 'https://pass.hust.edu.cn/cas/login'})])
        with self.assertRaisesRegex(SiteProtocolError, '登录状态'):
            client.check_account_access(config())
        self.assertEqual(len(session.get_calls), 1)

    def test_server_refusal_is_not_retried_or_misreported_as_blacklist(self):
        client, session = self.client([FakeResponse(status_code=429)])
        with self.assertRaises(SiteProtocolError) as error:
            client.check_account_access(config())
        self.assertNotIsInstance(error.exception, AccountBlacklistedError)
        self.assertEqual(len(session.get_calls), 1)

    def test_network_failure_stops_the_check(self):
        session = FakeSession(get_errors=[requests.Timeout('slow')])
        client = BookingSiteClient(session, logging.getLogger('account-check-tests'))
        with self.assertRaisesRegex(SiteProtocolError, '网络异常'):
            client.check_account_access(config())
        self.assertEqual(len(session.get_calls), 1)

    def test_redirect_loops_are_bounded(self):
        client, session = self.client([FakeResponse(status_code=302,
            headers={'Location': '/cggl/'}) for _ in range(5)])
        with self.assertRaisesRegex(SiteProtocolError, '重定向次数过多'):
            client.check_account_access(config())
        self.assertEqual(len(session.get_calls), 5)


class StartupAccountCheckTests(TestCase):
    def scenario(self, *, account_error=None, refresh_error=None, extra=(), startup_response=None,
                 close_to_target=False, cancel_wait=False):
        events = []
        startup_session, fresh_session = MagicMock(), MagicMock()
        now = datetime(2026, 11, 19, 16, 0)
        target = now + (timedelta(seconds=5) if close_to_target else timedelta(hours=16))
        with ExitStack() as stack:
            stack.enter_context(patch.dict('os.environ', {'DOLPHIN_CREDENTIALS_STDIN': ''}))
            stack.enter_context(patch.object(cli, '_configure_console_encoding'))
            stack.enter_context(patch.object(cli, 'configure_logging', return_value=MagicMock()))
            clock = stack.enter_context(patch.object(cli, 'datetime'))
            clock.now.return_value = now
            stack.enter_context(patch.object(cli, 'next_run_at', return_value=target))
            sessions = stack.enter_context(patch.object(cli.requests, 'Session',
                side_effect=[startup_session, fresh_session]))
            real_authenticator = cli.HustAuthenticator
            auth = stack.enter_context(patch.object(cli, 'HustAuthenticator'))

            def authenticator(session, _logger):
                def login(_credentials):
                    events.append('login')
                    if session is startup_session and startup_response is not None:
                        session.get.return_value = startup_response
                        real_authenticator(session, _logger).login(_credentials)
                    if session is fresh_session and refresh_error is not None:
                        raise refresh_error
                return SimpleNamespace(login=login)

            auth.side_effect = authenticator
            client = stack.enter_context(patch.object(cli, 'BookingSiteClient'))

            def account_check(_):
                events.append('check')
                if account_error is not None:
                    raise account_error
            client.return_value.check_account_access.side_effect = account_check
            wait = stack.enter_context(patch.object(cli, 'wait_until'))

            def waiting(_):
                events.append('wait')
                if cancel_wait:
                    raise KeyboardInterrupt
            wait.side_effect = waiting
            workflow = stack.enter_context(patch.object(cli, 'BookingWorkflow'))

            def book():
                events.append('book')
                return SimpleNamespace(venue_id='134', order=SimpleNamespace(order_id='123456'))

            workflow.return_value.run.side_effect = book
            result = cli.main([
                '--cdbh', '69', '--start_time', '18:00:00', '--order_date_after_today', '2',
                '--schedule_time', '08:00:03', '--select_pay_type=-1', '--priority_list', '134',
                '--user_name', 'test-user', '--password', 'test-password', *extra,
            ])
        return SimpleNamespace(result=result, events=events, sessions=sessions, auth=auth,
            client=client, wait=wait, workflow=workflow, startup=startup_session,
            fresh=fresh_session, target=target)

    def test_blacklisted_startup_exits_before_wait_or_booking(self):
        run = self.scenario(account_error=AccountBlacklistedError('黑名单'))
        self.assertEqual(run.result, 1)
        self.assertEqual(run.events, ['login', 'check'])
        run.wait.assert_not_called()
        run.workflow.assert_not_called()
        run.startup.close.assert_called_once()
        self.assertEqual(run.sessions.call_count, 1)

    def test_check_only_exits_after_success_without_waiting_or_booking(self):
        run = self.scenario(extra=('--check-account',))
        self.assertEqual(run.result, 0)
        self.assertEqual(run.events, ['login', 'check'])
        run.wait.assert_not_called()
        run.workflow.assert_not_called()
        self.assertTrue(all(not call.kwargs.get('payment_enabled') for call in run.client.call_args_list))

    def test_pre_release_refresh_only_logs_in_then_booking_runs_at_target(self):
        run = self.scenario()
        self.assertEqual(run.result, 0)
        self.assertEqual(run.events, ['login', 'check', 'wait', 'login', 'wait', 'book'])
        self.assertEqual(run.wait.call_args_list,
                         [call(run.target - timedelta(seconds=30)), call(run.target)])
        run.client.return_value.check_account_access.assert_called_once()
        self.assertEqual(run.sessions.call_count, 2)
        self.assertIs(run.auth.call_args_list[0].args[0], run.startup)
        self.assertIs(run.auth.call_args_list[1].args[0], run.fresh)
        run.startup.close.assert_called_once()
        run.fresh.close.assert_called_once()
        run.workflow.return_value.run.assert_called_once()

    def test_start_near_target_still_uses_original_fresh_login(self):
        run = self.scenario(close_to_target=True)
        self.assertEqual(run.result, 0)
        self.assertEqual(run.events, ['login', 'check', 'wait', 'login', 'wait', 'book'])
        self.assertEqual(run.wait.call_args_list,
                         [call(run.target - timedelta(seconds=30)), call(run.target)])
        run.client.return_value.check_account_access.assert_called_once()
        self.assertEqual(run.sessions.call_count, 2)
        self.assertIs(run.client.call_args_list[0].args[0], run.startup)
        self.assertIs(run.auth.call_args_list[1].args[0], run.fresh)
        self.assertIs(run.client.call_args_list[1].args[0], run.fresh)
        run.startup.close.assert_called_once()
        run.fresh.close.assert_called_once()

    def test_unknown_account_status_exits_without_waiting(self):
        run = self.scenario(account_error=SiteProtocolError('网络异常'))
        self.assertEqual(run.result, 1)
        run.wait.assert_not_called()
        run.workflow.assert_not_called()

    @staticmethod
    def cas_entry_response(status):
        response = requests.Response()
        response.status_code = status
        response.reason = 'Forbidden' if status == 403 else 'Server Error'
        response.request = requests.Request('GET', cli.CAS_LOGIN_URL, params={'service': 'test'}).prepare()
        response.url = response.request.url
        return response

    def test_cas_login_page_403_does_not_discard_the_scheduled_booking(self):
        run = self.scenario(startup_response=self.cas_entry_response(403))
        self.assertEqual(run.result, 0)
        self.assertEqual(run.events, ['login', 'wait', 'login', 'wait', 'book'])
        self.assertEqual(run.wait.call_args_list,
                         [call(run.target - timedelta(seconds=30)), call(run.target)])
        run.client.return_value.check_account_access.assert_not_called()
        run.startup.get.assert_called_once()
        run.startup.post.assert_not_called()
        run.startup.close.assert_called_once()

    def test_check_only_reports_cas_login_page_403_as_failure(self):
        run = self.scenario(startup_response=self.cas_entry_response(403), extra=('--check-account',))
        self.assertEqual(run.result, 1)
        run.wait.assert_not_called()
        run.workflow.assert_not_called()
        run.startup.get.assert_called_once()
        run.startup.post.assert_not_called()

    def test_other_startup_login_errors_still_stop_the_program(self):
        run = self.scenario(startup_response=self.cas_entry_response(500))
        self.assertEqual(run.result, 1)
        run.wait.assert_not_called()
        run.workflow.assert_not_called()

    def test_refresh_login_failure_stops_before_booking(self):
        run = self.scenario(refresh_error=AuthenticationError('刷新登录失败'))
        self.assertEqual(run.result, 1)
        self.assertEqual(run.events, ['login', 'check', 'wait', 'login'])
        run.workflow.assert_not_called()
        run.fresh.close.assert_called_once()

    def test_cancel_during_wait_is_handled_after_the_startup_session_is_closed(self):
        run = self.scenario(cancel_wait=True)
        self.assertEqual(run.result, 130)
        run.startup.close.assert_called_once()
        run.workflow.assert_not_called()
