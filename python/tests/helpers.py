from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

import requests
from requests.cookies import RequestsCookieJar


@dataclass
class FakeResponse:
    status_code: int = 200
    text: str = ""
    url: str = "https://example.invalid/"
    headers: dict[str, str] = field(default_factory=dict)
    content: bytes = b""
    encoding: str | None = None
    apparent_encoding: str | None = None
    json_data: Any = None

    def json(self) -> Any:
        if self.json_data is None:
            raise ValueError("response has no JSON payload")
        return self.json_data

    def raise_for_status(self) -> None:
        if self.status_code >= 400:
            raise requests.HTTPError(f"HTTP {self.status_code}")


class FakeSession:
    def __init__(
        self,
        *,
        get_responses: list[FakeResponse] | None = None,
        post_responses: list[FakeResponse] | None = None,
        get_errors: list[Exception | None] | None = None,
        post_errors: list[Exception | None] | None = None,
    ) -> None:
        self.get_responses = list(get_responses or [])
        self.post_responses = list(post_responses or [])
        self.get_errors = list(get_errors or [])
        self.post_errors = list(post_errors or [])
        self.get_calls: list[tuple[str, dict[str, Any]]] = []
        self.post_calls: list[tuple[str, dict[str, Any]]] = []
        self.cookies = RequestsCookieJar()

    def _raise_scheduled_error(self, errors: list[Exception | None]) -> None:
        if not errors:
            return
        error = errors.pop(0)
        if error is not None:
            raise error

    def get(self, url: str, **kwargs: Any) -> FakeResponse:
        self.get_calls.append((url, kwargs))
        self._raise_scheduled_error(self.get_errors)
        if not self.get_responses:
            raise AssertionError(f"unexpected GET {url}")
        return self.get_responses.pop(0)

    def post(self, url: str, **kwargs: Any) -> FakeResponse:
        self.post_calls.append((url, kwargs))
        self._raise_scheduled_error(self.post_errors)
        if not self.post_responses:
            raise AssertionError(f"unexpected POST {url}")
        return self.post_responses.pop(0)
