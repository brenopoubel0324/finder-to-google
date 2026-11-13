#!/usr/bin/env python3
"""Inspeciona R8 sem assinatura no CI; não prepara APK para distribuição."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import zipfile
from prepare_release import check_manifest_metadata, run


def require_unsigned(path):
    data = path.read_bytes()
    eocd = data.rfind(b"PK\x05\x06")
    if eocd < 0 or len(data) < eocd + 22:
        raise ValueError("ZIP inválido")
    central = struct.unpack_from("<I", data, eocd + 16)[0]
    if central >= 16 and data[central - 16:central] == b"APK Sig Block 42":
        raise ValueError("Assinatura APK inesperada no lane sem chave")
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None or not {"AndroidManifest.xml", "classes.dex"} <= set(archive.namelist()):
            raise ValueError("APK incompleto")
        if any(re.fullmatch(r"META-INF/[^/]+\.(RSA|DSA|EC|SF)", name, re.I) for name in archive.namelist()):
            raise ValueError("Assinatura JAR inesperada")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, default=Path("app/build/outputs/apk/release/app-release-unsigned.apk"))
    parser.add_argument("--report", type=Path, default=Path("app/build/reports/ci-release-inspection.json"))
    args = parser.parse_args()
    if any(os.environ.get(name) for name in ("FINDER_RELEASE_KEYSTORE", "FINDER_RELEASE_KEY_ALIAS", "FINDER_RELEASE_STORE_PASSWORD", "FINDER_RELEASE_KEY_PASSWORD")):
        raise ValueError("O lane de CI não recebe credenciais de produção")
    require_unsigned(args.apk)
    sdk = os.environ.get("ANDROID_HOME") or os.environ["ANDROID_SDK_ROOT"]
    tool = str(Path(sdk) / "build-tools/35.0.0/aapt2")
    gradle = Path("app/build.gradle.kts").read_text()
    version = re.search(r'versionName = "([0-9A-Za-z.\-]+)"', gradle).group(1)
    code = int(re.search(r"versionCode = (\d+)", gradle).group(1))
    check_manifest_metadata(run(tool, "dump", "badging", str(args.apk)),
                            run(tool, "dump", "xmltree", "--file", "AndroidManifest.xml", str(args.apk)), version, code)
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps({"source_commit": run("git", "rev-parse", "HEAD").strip(),
        "version": version, "version_code": code, "apk_sha256": hashlib.sha256(args.apk.read_bytes()).hexdigest(),
        "manifest_checked": True, "signature_verified": False, "production_release": False,
        "note": "Validação R8 sem chave. Não distribuível; assinatura e gate definitivo exigidos na entrega."}, indent=2) + "\n")


if __name__ == "__main__":
    main()
