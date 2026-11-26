"""Network preferences for the campus sites."""

from __future__ import annotations


def prefer_ipv4() -> None:
    """Make urllib3 try IPv4 addresses first.

    The campus hosts publish AAAA records, but some networks black-hole IPv6.
    urllib3 otherwise waits for the connect timeout on IPv6 before trying IPv4,
    which loses the release window even though IPv4 connects immediately.
    """

    try:
        from urllib3.util import connection
    except ImportError:  # pragma: no cover - requests always vendors urllib3
        return
    if hasattr(connection, "HAS_IPV6"):
        connection.HAS_IPV6 = False
