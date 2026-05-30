#!/usr/bin/env python3
"""Scan project-owned text files for invalid UTF-8 and common mojibake.

The checker intentionally skips dependency, build-output, and runtime-artifact
directories. Everything else with a known text-like extension must decode as
UTF-8 and must not contain high-confidence encoding-damage markers.
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


DEFAULT_EXCLUDED_DIRS = {
    ".git",
    ".codex",
    ".codex_tmp",
    ".idea",
    ".vscode",
    "coverage",
    "dist",
    "node_modules",
    "runtime_state",
    "target",
}

TEXT_EXTENSIONS = {
    ".bat",
    ".cmd",
    ".conf",
    ".css",
    ".editorconfig",
    ".env",
    ".example",
    ".gitignore",
    ".html",
    ".ini",
    ".java",
    ".js",
    ".json",
    ".md",
    ".properties",
    ".ps1",
    ".py",
    ".scss",
    ".sql",
    ".txt",
    ".vue",
    ".xml",
    ".yaml",
    ".yml",
}

TEXT_FILENAMES = {
    ".editorconfig",
    ".env",
    ".env.example",
    ".gitignore",
}

MOJIBAKE_MARKERS = {
    chr(0xFFFD): "replacement character",
    chr(0x951F): "UTF-8/GBK mojibake marker",
    "\u00ef\u00bf\u00bd": "replacement character mojibake",
    "\u9286\u3006": "CJK punctuation mojibake",
    "\u9225": "curly quote mojibake",
    "\u922b": "arrow mojibake",
    "\u00c2": "Latin-1 spacing mojibake",
    "\u93c9\u51ae\u6aac": "permission mojibake",
    "\u9435\u3126\u57db": "user mojibake",
    "\u7459\u638a": "role mojibake",
    "\u7487\u950b": "request/message mojibake",
    "\u941f\u6b1f\u5e1f": "Chinese text mojibake",
    "\u95ba\u5925\u5574": "Chinese text mojibake",
    "\u93c7\u5b58\u67ca": "update mojibake",
    "\u9352\u6d98\u7f13": "create mojibake",
    "\u59dd\u52ee\u6e6a": "Chinese text mojibake",
    "\u7ee0\uffe5\u60a7": "management mojibake",
    ("fa" + chr(0x3F) + "ade"): "lost cedilla placeholder",
}

PLACEHOLDER_RE = re.compile(r"\?{4,}")
QUOTED_DOUBLE_QUESTION_RE = re.compile(r"""(['"])[^'"\n]*\?\?[^'"\n]*\1""")


@dataclass(frozen=True)
class Finding:
    path: Path
    line: int | None
    reason: str
    snippet: str = ""

    def format(self, root: Path) -> str:
        rel = self.path.relative_to(root) if self.path.is_relative_to(root) else self.path
        location = str(rel)
        if self.line is not None:
            location = f"{location}:{self.line}"
        if self.snippet:
            return f"{location}: {self.reason}: {self.snippet}"
        return f"{location}: {self.reason}"


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[1],
        help="Project root to scan (default: repository root inferred from this script).",
    )
    parser.add_argument(
        "--include-runtime",
        action="store_true",
        help="Also scan runtime_state. Dependency and build-output directories remain excluded.",
    )
    return parser.parse_args(argv)


def is_text_file(path: Path) -> bool:
    name = path.name
    suffix = path.suffix.lower()
    return name in TEXT_FILENAMES or suffix in TEXT_EXTENSIONS


def is_excluded(path: Path, include_runtime: bool) -> bool:
    excluded = set(DEFAULT_EXCLUDED_DIRS)
    if include_runtime:
        excluded.discard("runtime_state")
    return any(part in excluded for part in path.parts)


def iter_text_files(root: Path, include_runtime: bool) -> list[Path]:
    files: list[Path] = []
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        rel = path.relative_to(root)
        if is_excluded(rel, include_runtime):
            continue
        if is_text_file(path):
            files.append(path)
    return sorted(files)


def visible_cjk_ratio(text: str) -> float:
    visible = [ch for ch in text if not ch.isspace()]
    if not visible:
        return 0.0
    cjk = sum(1 for ch in visible if "\u4e00" <= ch <= "\u9fff")
    return cjk / len(visible)


def looks_like_reversible_gb_mojibake(line: str) -> bool:
    stripped = line.strip()
    if len(stripped) < 3:
        return False
    try:
        repaired = stripped.encode("gb18030").decode("utf-8")
    except (UnicodeEncodeError, UnicodeDecodeError):
        return False
    return repaired != stripped and visible_cjk_ratio(repaired) > 0.30


def scan_text(path: Path, text: str) -> list[Finding]:
    findings: list[Finding] = []
    for line_no, line in enumerate(text.splitlines(), start=1):
        for column, char in enumerate(line, start=1):
            codepoint = ord(char)
            if (codepoint < 32 and char != "\t") or codepoint == 127:
                findings.append(
                    Finding(
                        path,
                        line_no,
                        f"control character U+{codepoint:04X} at column {column}",
                        line.encode("unicode_escape").decode("ascii")[:180],
                    )
                )
                break

        for marker, reason in MOJIBAKE_MARKERS.items():
            if marker in line:
                findings.append(Finding(path, line_no, reason, line.strip()[:180]))

        if PLACEHOLDER_RE.search(line):
            findings.append(Finding(path, line_no, "four or more question-mark placeholders", line.strip()[:180]))

        if QUOTED_DOUBLE_QUESTION_RE.search(line):
            findings.append(Finding(path, line_no, "quoted double-question placeholder", line.strip()[:180]))

        if looks_like_reversible_gb_mojibake(line):
            findings.append(Finding(path, line_no, "reversible UTF-8/GB18030 mojibake", line.strip()[:180]))

    return findings


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    root = args.root.resolve()
    if not root.exists():
        print(f"Root does not exist: {root}", file=sys.stderr)
        return 2

    findings: list[Finding] = []
    files = iter_text_files(root, args.include_runtime)
    for path in files:
        data = path.read_bytes()
        try:
            text = data.decode("utf-8")
        except UnicodeDecodeError as exc:
            findings.append(Finding(path, None, f"invalid UTF-8: {exc}"))
            continue
        findings.extend(scan_text(path, text))

    if findings:
        print(f"Text encoding check failed: {len(findings)} finding(s) in {len(files)} scanned file(s).")
        for finding in findings:
            print(finding.format(root))
        return 1

    print(f"Text encoding check passed: {len(files)} file(s) scanned.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
