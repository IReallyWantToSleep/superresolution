#!/usr/bin/env python3
"""Update the build switches used by Gradle from environment variables."""

from __future__ import annotations

import os
import re
from pathlib import Path


PROPERTY_ENVIRONMENTS = {
    "is_dev": "GRADLE_IS_DEV",
    "is_vulkan": "GRADLE_IS_VULKAN",
    "enable_auto_download": "GRADLE_ENABLE_AUTO_DOWNLOAD",
    "use_debug_lib": "GRADLE_USE_DEBUG_LIB",
}
PROPERTY_LINE = re.compile(
    r"^(?P<prefix>\s*(?P<key>[A-Za-z0-9_.-]+)\s*=\s*)"
    r"(?P<value>[^\r\n]*)(?P<newline>\r?\n)?$"
)


def read_build_properties() -> dict[str, str]:
    properties: dict[str, str] = {}
    for key, environment_name in PROPERTY_ENVIRONMENTS.items():
        value = os.environ.get(environment_name)
        if value is None:
            raise RuntimeError(
                f"Missing required environment variable {environment_name}"
            )

        normalized = value.strip().lower()
        if normalized not in {"true", "false"}:
            raise RuntimeError(
                f"{environment_name} must be 'true' or 'false', got {value!r}"
            )
        properties[key] = normalized
    return properties


def update_properties_file(path: Path, updates: dict[str, str]) -> None:
    with path.open("r", encoding="utf-8", newline="") as properties_file:
        lines = properties_file.readlines()

    seen: set[str] = set()
    updated_lines: list[str] = []
    for line in lines:
        match = PROPERTY_LINE.match(line)
        if match is None or match.group("key") not in updates:
            updated_lines.append(line)
            continue

        key = match.group("key")
        if key in seen:
            raise RuntimeError(f"Duplicate property in {path}: {key}")
        seen.add(key)

        existing_value = match.group("value")
        trailing_whitespace = existing_value[len(existing_value.rstrip()):]
        updated_lines.append(
            f"{match.group('prefix')}{updates[key]}{trailing_whitespace}"
            f"{match.group('newline') or ''}"
        )

    missing = set(updates) - seen
    if missing:
        missing_keys = ", ".join(sorted(missing))
        raise RuntimeError(f"Missing properties in {path}: {missing_keys}")

    new_contents = "".join(updated_lines)
    with path.open("w", encoding="utf-8", newline="") as properties_file:
        properties_file.write(new_contents)

    print(f"Updated {path}")
    for key, value in updates.items():
        print(f"  {key}={value}")


def main() -> None:
    repository_root = Path(__file__).resolve().parents[2]
    properties_path = Path(
        os.environ.get(
            "GRADLE_PROPERTIES_PATH", repository_root / "gradle.properties"
        )
    ).resolve()
    if not properties_path.is_file():
        raise RuntimeError(f"gradle.properties not found: {properties_path}")

    update_properties_file(properties_path, read_build_properties())


if __name__ == "__main__":
    try:
        main()
    except RuntimeError as error:
        raise SystemExit(f"ERROR: {error}") from error
