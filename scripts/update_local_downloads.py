#!/usr/bin/env python3
"""Mirror the latest GitHub release ZIPs into an ignored website deployment directory."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import zipfile

REPOSITORY = "zfdang/chinese-chess-fish-android"
PACKAGES = ("chessfish-armv8-dotprod.zip", "chessfish-armv8.zip")


def verify(path, asset):
    if not path.is_file() or path.stat().st_size != asset["size"]:
        raise ValueError(f"Missing or incomplete package: {path.name}")
    digest = asset.get("digest", "")
    if not digest or not digest.startswith("sha256:"):
        raise ValueError(f"Release has no SHA-256 digest: {path.name}")
    sha256 = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            sha256.update(block)
    if "sha256:" + sha256.hexdigest() != digest:
        raise ValueError(f"Checksum mismatch: {path.name}")
    with zipfile.ZipFile(path) as archive:
        files = archive.namelist()
        if len(files) != 1 or not files[0].endswith(".apk") or archive.testzip():
            raise ValueError(f"Invalid APK archive: {path.name}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path,
                        default=Path(__file__).resolve().parents[1] / "docs" / "apk")
    args = parser.parse_args()
    release = json.loads(subprocess.check_output(
        ["gh", "api", f"repos/{REPOSITORY}/releases/latest"], text=True))
    assets = {asset["name"]: asset for asset in release["assets"]}
    if not all(name in assets for name in PACKAGES):
        raise ValueError("Latest release must contain both ARMv8 ZIP packages")
    args.output.mkdir(parents=True, exist_ok=True)
    # Stage and verify both variants before replacing either live download.
    with tempfile.TemporaryDirectory(prefix=".downloads-", dir=args.output) as staging:
        replacements = []
        for name in PACKAGES:
            target = args.output / name
            try:
                verify(target, assets[name])
                print(f"Already verified: {name}")
                continue
            except (ValueError, zipfile.BadZipFile):
                pass
            subprocess.run(["gh", "release", "download", release["tag_name"],
                            "--repo", REPOSITORY, "--pattern", name, "--dir", staging], check=True)
            downloaded = Path(staging) / name
            verify(downloaded, assets[name])
            replacements.append((downloaded, target))
        for downloaded, target in replacements:
            os.replace(downloaded, target)
    print(f"{release['tag_name']}: both packages ready in {args.output}")
    print("Deploy these ZIPs with the website; they are not tracked by Git.")


if __name__ == "__main__":
    main()
