"""Small HTML extractors for the two forms used by the site."""

from html.parser import HTMLParser


class _InputValueParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__()
        self.values: dict[str, str] = {}

    def handle_starttag(
        self, tag: str, attrs: list[tuple[str, str | None]]
    ) -> None:
        if tag.lower() != "input":
            return
        attributes = dict(attrs)
        name = attributes.get("name") or attributes.get("id")
        value = attributes.get("value")
        if name and value is not None:
            self.values[name] = value


def input_values(document: str) -> dict[str, str]:
    parser = _InputValueParser()
    parser.feed(document)
    return parser.values
