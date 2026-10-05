"""Validate the release version and collect the MSI and checksum."""

from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import re
import shutil


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-version", action="store_true")
    args = parser.parse_args()
    project = Path(__file__).resolve().parents[1]
    config = (project / "composeApp" / "build.gradle.kts").read_text(encoding="utf-8")
    match = re.search(r'packageVersion\s*=\s*"(\d+\.\d+\.\d+)"', config)
    if not match:
        parser.error("No semantic packageVersion found in composeApp/build.gradle.kts")
    version = match.group(1)
    if os.environ.get("GITHUB_REF_TYPE") == "tag":
        tag = os.environ.get("GITHUB_REF_NAME")
        if tag != f"v{version}":
            parser.error(f"Tag {tag} does not match packageVersion {version}")
    if github_output := os.environ.get("GITHUB_OUTPUT"):
        with Path(github_output).open("a", encoding="utf-8") as output:
            output.write(f"version={version}\n")
    print(f"Release version: {version}")
    if args.check_version:
        return

    filename = f"Dolphin-{version}.msi"
    installer = project / "composeApp" / "build" / "compose" / "binaries" / "main-release" / "msi" / filename
    if not installer.is_file():
        parser.error(f"Build :composeApp:packageReleaseMsi first; missing {installer}")
    destination = project / "build" / "release" / version
    destination.mkdir(parents=True, exist_ok=True)
    packaged = destination / filename
    shutil.copy2(installer, packaged)
    with packaged.open("rb") as source:
        digest = hashlib.file_digest(source, "sha256").hexdigest()
    (destination / "SHA256SUMS").write_text(f"{digest}  {filename}\n", encoding="utf-8", newline="\n")
    print(f"Release files: {destination}")


if __name__ == "__main__":
    main()
