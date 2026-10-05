"""Build the private Windows Python runtime used by Dolphin's MSI."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from urllib.request import urlopen
from zipfile import ZipFile

PYTHON_VERSION = "3.13.16"
PYTHON_ZIP = f"python-{PYTHON_VERSION}-embed-amd64.zip"
PYTHON_URL = f"https://www.python.org/ftp/python/{PYTHON_VERSION}/{PYTHON_ZIP}"
PYTHON_SHA256 = "97dae5274cc54867065e8d5a3226e48c35017ed332a0fdb0e27d5b5821961297"
TESSERACT_ARCHIVE_SHA256 = "041a270af9c5c6786d8f240afc4c351c8322a696bc3c9c2ce543abb0020a1846"

TESSERACT_FILES = (
    "tesseract.exe", "iconv.dll", "libarchive-13.dll", "libbz2-1.dll",
    "libcurl-4.dll", "libgcc_s_seh-1.dll", "libgif-7.dll", "libidn2-0.dll",
    "libintl-8.dll", "libjbig-2.dll", "libjpeg-8.dll", "liblept-5.dll",
    "liblz4-1.dll", "liblzma-5.dll", "liblzo2-2.dll", "libnghttp2-14.dll",
    "libopenjp2.dll", "libpng16-16.dll", "libstdc++-6.dll",
    "libtesseract-5.dll", "libtiff-5.dll", "libunistring-2.dll",
    "libwebp-7.dll", "libwinpthread-1.dll", "libxml2-2.dll",
    "libzstd-1.dll", "zlib1.dll",
)


def _within(path: Path, parent: Path) -> bool:
    return path.resolve().is_relative_to(parent.resolve())


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _download_python(cache: Path) -> Path:
    cache.mkdir(parents=True, exist_ok=True)
    archive = cache / PYTHON_ZIP
    if archive.is_file() and _sha256(archive) == PYTHON_SHA256:
        return archive
    temporary = archive.with_suffix(".download")
    try:
        with urlopen(PYTHON_URL, timeout=60) as response, temporary.open("wb") as target:
            shutil.copyfileobj(response, target)
        if _sha256(temporary) != PYTHON_SHA256:
            raise RuntimeError(f"Python archive checksum mismatch: {PYTHON_URL}")
        temporary.replace(archive)
    finally:
        temporary.unlink(missing_ok=True)
    return archive


def _run(command: list[str], **kwargs: object) -> None:
    subprocess.run(command, check=True, **kwargs)


def _install_wheels(lockfile: Path, cache: Path, site_packages: Path) -> None:
    wheelhouse = cache / "wheels"
    wheelhouse.mkdir(parents=True, exist_ok=True)
    target = [
        "--only-binary=:all:", "--platform", "win_amd64",
        "--python-version", "3.13", "--implementation", "cp", "--abi", "cp313",
    ]
    pip = [sys.executable, "-m", "pip"]
    _run(pip + ["download", "--disable-pip-version-check", "--require-hashes",
                *target, "--dest", str(wheelhouse), "-r", str(lockfile)])
    _run(pip + ["install", "--disable-pip-version-check", "--require-hashes",
                "--no-index", "--find-links", str(wheelhouse), "--no-deps",
                "--no-compile", *target, "--target", str(site_packages),
                "-r", str(lockfile)])


def _copy_tesseract(source: Path, target: Path) -> None:
    if source.is_file():
        if _sha256(source) != TESSERACT_ARCHIVE_SHA256:
            raise RuntimeError(f"Tesseract archive checksum mismatch: {source}")
        names = (*TESSERACT_FILES, "tessdata/eng.traineddata", "doc/LICENSE")
        with ZipFile(source) as archive:
            missing = [name for name in names if name not in archive.namelist()]
            if missing:
                raise FileNotFoundError(f"Tesseract archive is incomplete: {', '.join(missing)}")
            for name in names:
                destination = target / ("LICENSE" if name == "doc/LICENSE" else name)
                destination.parent.mkdir(parents=True, exist_ok=True)
                with archive.open(name) as original, destination.open("wb") as output:
                    shutil.copyfileobj(original, output)
        return
    missing = [name for name in TESSERACT_FILES if not (source / name).is_file()]
    if not (source / "tessdata" / "eng.traineddata").is_file():
        missing.append("tessdata/eng.traineddata")
    if not (source / "doc" / "LICENSE").is_file():
        missing.append("doc/LICENSE")
    if missing:
        raise FileNotFoundError(f"Tesseract runtime is incomplete at {source}: {', '.join(missing)}")
    target.mkdir(parents=True)
    for name in TESSERACT_FILES:
        shutil.copy2(source / name, target / name)
    (target / "tessdata").mkdir()
    shutil.copy2(source / "tessdata" / "eng.traineddata", target / "tessdata")
    shutil.copy2(source / "doc" / "LICENSE", target / "LICENSE")


def _verify_runtime(root: Path) -> None:
    python = root / "python" / "python.exe"
    env = os.environ.copy()
    for name in ("PYTHONHOME", "PYTHONPATH", "PYTHONUSERBASE"):
        env.pop(name, None)
    env["PYTHONIOENCODING"] = "utf-8"
    _run([str(python), "-I", "-c",
          "import PIL, Crypto, pytesseract, requests, booking_assistant.cli"], env=env)
    tesseract = root / "src" / "Tesseract-OCR"
    env["TESSDATA_PREFIX"] = str(tesseract / "tessdata")
    _run([str(python), "-I", "-c",
          "import sys, pytesseract; from PIL import Image; "
          "pytesseract.pytesseract.tesseract_cmd = sys.argv[1]; "
          "pytesseract.image_to_string(Image.new('RGB', (100, 50), 'white'), config='--psm 6')",
          str(tesseract / "tesseract.exe")], env=env)
    result = subprocess.run([str(tesseract / "tesseract.exe"), "--list-langs"],
                            check=True, capture_output=True, text=True, env=env)
    if "eng" not in result.stdout:
        raise RuntimeError("Bundled Tesseract cannot load eng.traineddata")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--tesseract", type=Path, required=True,
                        help="Pinned runtime ZIP or Tesseract runtime directory")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--cache", type=Path, required=True)
    args = parser.parse_args()

    if os.name != "nt":
        parser.error("The current desktop package targets Windows x64")
    project = Path(__file__).resolve().parents[1]
    allowed = project / "composeApp" / "build"
    output = args.output.resolve()
    if not _within(output, allowed) or output == allowed.resolve():
        parser.error(f"Output must be inside {allowed}")
    source = args.source.resolve()
    if not (source / "main.py").is_file():
        parser.error(f"Missing Python source: {source}")
    tesseract = args.tesseract.resolve()
    if not (tesseract.is_dir() or tesseract.is_file()):
        parser.error(f"Missing Tesseract runtime: {tesseract}")

    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = Path(tempfile.mkdtemp(prefix="booking runtime ", dir=output.parent))
    try:
        python_dir = temporary / "python"
        python_dir.mkdir()
        with ZipFile(_download_python(args.cache.resolve())) as archive:
            archive.extractall(python_dir)
        (python_dir / "python313._pth").write_text(
            "python313.zip\n.\nLib/site-packages\n../src\nimport site\n", encoding="utf-8"
        )
        _install_wheels(source / "requirements.lock", args.cache.resolve(),
                        python_dir / "Lib" / "site-packages")
        script_dir = temporary / "src"
        script_dir.mkdir()
        shutil.copy2(source / "main.py", script_dir / "main.py")
        shutil.copytree(source / "booking_assistant", script_dir / "booking_assistant",
                        ignore=shutil.ignore_patterns("__pycache__", "*.pyc"))
        _copy_tesseract(tesseract, script_dir / "Tesseract-OCR")
        _verify_runtime(temporary)
        if output.exists():
            if not _within(output, allowed) or output.is_symlink():
                raise RuntimeError(f"Refusing to replace unexpected output: {output}")
            shutil.rmtree(output)
        temporary.replace(output)
        print(f"Staged private booking runtime: {output}")
    finally:
        if temporary.exists() and _within(temporary, allowed):
            shutil.rmtree(temporary)


if __name__ == "__main__":
    main()
