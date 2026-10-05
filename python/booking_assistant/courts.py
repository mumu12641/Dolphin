"""Stable mappings between displayed court numbers and site venue IDs."""

COURTS_BY_STADIUM: dict[str, dict[int, str]] = {
    "69": {
        1: "134",
        2: "295",
        3: "296",
        4: "297",
        5: "298",
        6: "299",
        7: "300",
        8: "301",
        9: "584",
    },
    "45": {
        1: "110",
        2: "133",
        3: "215",
        4: "216",
        5: "218",
        6: "217",
        7: "219",
        8: "220",
        9: "221",
        10: "222",
        11: "223",
        12: "224",
        13: "368",
        14: "369",
        15: "370",
        16: "371",
        17: "372",
        18: "373",
        19: "374",
        20: "375",
        21: "376",
        22: "377",
    },
    "117": {
        1: "587",
        2: "588",
        3: "589",
        4: "590",
        5: "591",
        6: "592",
        7: "593",
        8: "594",
        9: "595",
    },
}

COURT_NUMBERS_BY_STADIUM: dict[str, dict[str, int]] = {
    stadium_id: {venue_id: number for number, venue_id in courts.items()}
    for stadium_id, courts in COURTS_BY_STADIUM.items()
}


def court_number(stadium_id: str, venue_id: str) -> int | None:
    """Return the human-facing court number for a site venue ID."""

    return COURT_NUMBERS_BY_STADIUM.get(stadium_id, {}).get(venue_id)


def venue_ids(stadium_id: str) -> frozenset[str]:
    """Return every known site venue ID for a stadium."""

    return frozenset(COURT_NUMBERS_BY_STADIUM.get(stadium_id, {}))
