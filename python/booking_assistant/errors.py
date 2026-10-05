"""Domain exceptions shown as concise command-line errors."""


class BookingAssistantError(Exception):
    """Base class for expected failures."""


class ConfigurationError(BookingAssistantError):
    """The command-line booking configuration is invalid."""


class AuthenticationError(BookingAssistantError):
    """HUST unified authentication failed."""


class SiteProtocolError(BookingAssistantError):
    """The booking site returned an unexpected response."""


class VenueUnavailableError(BookingAssistantError):
    """A requested venue is no longer available."""


class PaymentBlockedError(BookingAssistantError):
    """A caller attempted payment without explicit authorization."""


class PaymentError(BookingAssistantError):
    """The one permitted payment request failed or became ambiguous."""
