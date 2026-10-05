"""Console logging with no credential or session-token output."""

import logging
import sys


def configure_logging(name: str = "booking") -> logging.Logger:
    logger = logging.getLogger(name)
    logger.setLevel(logging.INFO)
    logger.propagate = False
    if logger.handlers:
        return logger

    handler = logging.StreamHandler(sys.stdout)
    handler.setFormatter(
        logging.Formatter(
            "%(asctime)s.%(msecs)03d | %(levelname)-7s | %(emoji)s | %(message)s",
            "%Y-%m-%d %H:%M:%S",
            defaults={"emoji": "💬"},
        )
    )
    logger.addHandler(handler)
    return logger
