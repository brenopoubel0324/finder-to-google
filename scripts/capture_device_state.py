#!/usr/bin/env python3
"""Captura manual finita por ADB; sem polling, reset de estatísticas ou alteração do aparelho."""
import argparse
import json
from datetime import datetime, timezone
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "br.com.findertogoogle"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True, help="Serial exato de adb devices; não publica o serial")
    parser.add_argument("--output", type=Path, required=True, help="Pasta nova fora do repositório")
    args = parser.parse_args()
    output = args.output.resolve()
    if output.exists() or output == ROOT or ROOT in output.parents:
        parser.error("Use uma pasta nova fora do repositório")
    adb = ["adb", "-s", args.serial]
    subprocess.run(adb + ["get-state"], check=True, capture_output=True, timeout=15)
    output.mkdir(parents=True)
    queries = {
        "model": ["getprop", "ro.product.model"],
        "android": ["getprop", "ro.build.version.release"],
        "sdk": ["getprop", "ro.build.version.sdk"],
        "package": ["dumpsys", "package", PACKAGE],
        "services": ["dumpsys", "activity", "services", PACKAGE],
        "battery": ["dumpsys", "battery"],
        "batterystats": ["dumpsys", "batterystats", "--charged", PACKAGE],
        "meminfo": ["dumpsys", "meminfo", PACKAGE],
        "gfxinfo": ["dumpsys", "gfxinfo", PACKAGE],
        "pid": ["pidof", PACKAGE],
    }
    results = {"captured_at": datetime.now(timezone.utc).isoformat(), "package": PACKAGE, "commands": {}}
    for name, command in queries.items():
        try:
            result = subprocess.run(adb + ["shell"] + command, capture_output=True, text=True, timeout=30)
            (output / (name + ".txt")).write_text(result.stdout + result.stderr)
            results["commands"][name] = {"exit_code": result.returncode}
        except subprocess.TimeoutExpired:
            results["commands"][name] = {"timeout": True}
    (output / "capture.json").write_text(json.dumps(results, indent=2) + "\n")
    print("Captura concluída. Compare deltas no mesmo aparelho; sem estimar energia a partir de CPU/Views.")


if __name__ == "__main__":
    main()
