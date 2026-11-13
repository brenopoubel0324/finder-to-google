#!/usr/bin/env python3
"""Seleciona validação pelo diff real; base ausente promove ao completo."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

SHA = re.compile(r"[0-9a-f]{40}(?:[0-9a-f]{24})?\Z")
DAILY_UI = {"app/src/main/java/br/com/findertogoogle/ui/" + name for name in
            ("SettingsPages.kt", "SettingsState.kt", "PageFocus.kt", "MaxWidthLinearLayout.kt", "UiSupport.kt")}


def classify(paths):
    if not paths:
        return "complete", "empty_diff"
    modes = []
    for status, path in paths:
        if status not in {"A", "D", "M"}:
            return "complete", "special_change"
        if path in {"README.md", "CHANGELOG.md", "LICENSE"} or (path.startswith("docs/") and path.endswith(".md")):
            modes.append("docs")
        elif path in DAILY_UI or path.startswith(("app/src/main/res/layout/", "app/src/main/res/values/", "app/src/main/res/values-", "app/src/main/res/drawable/", "app/src/test/")):
            modes.append("daily")
        else:
            return "complete", "risk_or_unknown_path"
    return ("daily", "application_change") if "daily" in modes else ("docs", "documentation_only")


def git(repo, *args):
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True, timeout=30).stdout


def select(repo, event_name, event, head):
    if event_name == "workflow_dispatch":
        return "complete", "manual", 0
    if event_name == "push" and event.get("ref", "").startswith("refs/tags/"):
        return "complete", "tag", 0
    if event_name == "push":
        base = event.get("before", "")
    elif event_name == "pull_request":
        base = event.get("pull_request", {}).get("base", {}).get("sha", "")
    else:
        return "complete", "unknown_event", 0
    if not isinstance(base, str) or not SHA.fullmatch(base) or set(base) == {"0"}:
        return "complete", "missing_base", 0
    if not SHA.fullmatch(head):
        return "complete", "invalid_head", 0
    try:
        if git(repo, "rev-parse", "HEAD").decode().strip() != head:
            return "complete", "head_mismatch", 0
        try:
            git(repo, "cat-file", "-e", base + "^{commit}")
        except subprocess.CalledProcessError:
            git(repo, "fetch", "--no-tags", "--depth=1", "origin", base)
        raw = git(repo, "diff", "--name-status", "--no-renames", "-z", base, head, "--")
    except (subprocess.CalledProcessError, subprocess.TimeoutExpired):
        return "complete", "unavailable_diff", 0
    fields = raw.split(b"\0")
    if fields[-1] != b"" or (len(fields) - 1) % 2:
        raise ValueError("Resposta git diff inválida")
    paths = [(os.fsdecode(fields[i]), os.fsdecode(fields[i + 1])) for i in range(0, len(fields) - 1, 2)]
    mode, reason = classify(paths)
    return mode, reason, len(paths)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--event-name", default=os.environ.get("GITHUB_EVENT_NAME", ""))
    parser.add_argument("--event-path", type=Path, default=os.environ.get("GITHUB_EVENT_PATH"))
    parser.add_argument("--head", default=os.environ.get("GITHUB_SHA", ""))
    args = parser.parse_args()
    try:
        event = json.loads(args.event_path.read_text()) if args.event_path else {}
    except (OSError, ValueError):
        event = {}
    mode, reason, count = select(args.repo, args.event_name, event, args.head)
    output = f"mode={mode}\nreason={reason}\nchanged_files={count}\n"
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a") as file:
            file.write(output)
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as file:
            file.write(f"### Validação selecionada: {mode}\n\nMotivo: `{reason}`; arquivos no diff: {count}.\n\n"
                       "docs: sem build Android. daily: testes/Lint/build debug. complete: acrescenta compilação de instrumentação e R8 sem assinatura. "
                       "Nenhum modo executa testes físicos ou substitui assinatura definitiva na entrega.\n")
    print(output, end="")


if __name__ == "__main__":
    main()
