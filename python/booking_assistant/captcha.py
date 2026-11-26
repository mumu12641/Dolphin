"""Animated HUST CAPTCHA preprocessing and OCR."""

from __future__ import annotations

from contextlib import redirect_stdout
from io import BytesIO
from io import StringIO
import logging
import os
import time

import pytesseract
from PIL import Image

from .logging import elapsed_ms


def solve_captcha(
    image_content: bytes,
    *,
    executable: str | None,
    tessdata_path: str | None,
    logger: logging.Logger | None = None,
) -> str:
    started = time.perf_counter()
    frames: list[Image.Image] = []
    with Image.open(BytesIO(image_content)) as gif:
        for index in range(gif.n_frames):
            gif.seek(index)
            frames.append(gif.copy().convert("L"))

    if logger:
        logger.info(
            "验证码图片已解析：%d 帧，%d 字节，耗时 %s",
            len(frames),
            len(image_content),
            elapsed_ms(started),
            extra={"emoji": "🖼️"},
        )

    if not frames:
        if logger:
            logger.warning("验证码图片没有可用帧", extra={"emoji": "⚠️"})
        return ""

    width, height = frames[0].size
    merged = Image.new("L", (width, height), color=255)
    for position in ((x, y) for x in range(width) for y in range(height)):
        if sum(frame.getpixel(position) < 254 for frame in frames) >= 3:
            merged.putpixel(position, 0)

    pytesseract.pytesseract.tesseract_cmd = executable or "tesseract"
    # An environment variable survives installation paths with spaces; a
    # --tessdata-dir string passed through pytesseract's shlex parser does not.
    if tessdata_path:
        os.environ["TESSDATA_PREFIX"] = tessdata_path
    # Some pytesseract builds print the full subprocess command unconditionally.
    # Keep temporary paths and implementation noise out of normal CLI logs.
    with redirect_stdout(StringIO()):
        text = pytesseract.image_to_string(
            merged,
            config="-c tessedit_char_whitelist=0123456789 --psm 6",
        ).strip()

    if logger:
        logger.info(
            "验证码 OCR 完成：识别 %d 位数字，耗时 %s",
            len(text),
            elapsed_ms(started),
            extra={"emoji": "🔍"},
        )
    return text
