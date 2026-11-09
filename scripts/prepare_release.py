#!/usr/bin/env python3
"""Verifica um APK release e prepara entrega explícita, sem chaves ou publicação."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "br.com.findertogoogle"
CERTIFICATE = "a99bd676fb3a8603605fd94e2fd0cbc7cf36f9d313623931635fa4e1780741c8"


def run(*args):
    return subprocess.run(args, check=True, capture_output=True, text=True).stdout


def check_components(tree):
    expected = {
        ("activity", PACKAGE + ".ui.SettingsActivity"): {"exported": "true"},
        ("activity", PACKAGE + ".ui.LinkRouterActivity"): {"exported": "true"},
        ("provider", "androidx.startup.InitializationProvider"): {
            "exported": "false", "authorities": PACKAGE + ".androidx-startup"},
        ("receiver", "androidx.profileinstaller.ProfileInstallReceiver"): {
            "exported": "true", "permission": "android.permission.DUMP"},
    }
    seen = set()
    current = None
    attributes = {}

    def finish():
        if current is None:
            return
        kind, _ = current
        key = (kind, attributes.get("name"))
        if key in seen or key not in expected or any(attributes.get(k) != v for k, v in expected[key].items()):
            raise ValueError("Componente ou proteção inesperados no APK")
        seen.add(key)

    for line in tree.splitlines():
        indent = len(line) - len(line.lstrip())
        if current is not None and indent <= current[1] and line.strip():
            finish()
            current = None
        match = re.match(r"\s*E: (activity|service|receiver|provider|activity-alias)\b", line)
        if match:
            current = (match.group(1), indent)
            attributes = {}
        elif current is not None and indent == current[1] + 2:
            attribute = re.search(r":(\w+)\([^)]*\)=(?:\"([^\"]*)\"|(\S+))", line)
            if attribute:
                attributes[attribute.group(1)] = attribute.group(2) if attribute.group(2) is not None else attribute.group(3)
    finish()
    if seen != set(expected):
        raise ValueError("Lista de componentes diferente da release esperada")


def check_metadata(badging, tree, signature, version, code):
    """Falha fechada para variante/versão/chave/componentes incorretos."""
    required = f"package: name='{PACKAGE}' versionCode='{code}' versionName='{version}'"
    if not badging.startswith(required + " "):
        raise ValueError("Pacote ou versão inesperados")
    if "application-debuggable" in badging or re.search(r":debuggable\([^)]*\)=(?!false\b)", tree):
        raise ValueError("APK com depuração habilitada")
    if "minSdkVersion:'29'" not in badging or "targetSdkVersion:'36'" not in badging:
        raise ValueError("SDK inesperado")
    permissions = re.findall(r"^uses-permission[^:]*: name='([^']+)'", badging, re.M)
    if set(permissions) - {PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"}:
        raise ValueError("Permissão inesperada no APK")
    check_components(tree)
    if not re.search(r":allowBackup\([^)]*\)=false\b", tree):
        raise ValueError("Backup não está explicitamente desabilitado")
    certificates = re.findall(r"^Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)$", signature, re.M)
    if [c.lower() for c in certificates] != [CERTIFICATE]:
        raise ValueError("Certificado diferente da chave definitiva")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True, help="Pasta nova, fora do repositório")
    args = parser.parse_args()
    output = args.output.resolve()
    if output == ROOT or ROOT in output.parents:
        parser.error("A entrega deve ficar fora do repositório")
    if output.exists():
        parser.error("A pasta de entrega já existe; não sobrescrever artefatos")
    if run("git", "-C", str(ROOT), "status", "--porcelain").strip():
        parser.error("Faça commit das alterações antes de compilar e preparar a entrega")
    gradle = (ROOT / "app/build.gradle.kts").read_text()
    version = re.search(r'versionName = "([0-9A-Za-z.\-]+)"', gradle).group(1)
    code = int(re.search(r"versionCode = (\d+)", gradle).group(1))
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        parser.error("Defina ANDROID_HOME ou ANDROID_SDK_ROOT")
    build_tools = Path(sdk) / "build-tools/35.0.0"
    apk = args.apk.resolve()
    signature = run(str(build_tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk))
    badging = run(str(build_tools / "aapt2"), "dump", "badging", str(apk))
    tree = run(str(build_tools / "aapt2"), "dump", "xmltree", "--file", "AndroidManifest.xml", str(apk))
    check_metadata(badging, tree, signature, version, code)
    data = apk.read_bytes()
    name = f"Finder-to-Google-{version}-release.apk"
    digest = hashlib.sha256(data).hexdigest()
    manifest = {"application_id": PACKAGE, "version": version, "version_code": code,
                "source_commit": run("git", "-C", str(ROOT), "rev-parse", "HEAD").strip(),
                "apk": name, "bytes": len(data), "sha256": digest, "certificate_sha256": CERTIFICATE,
                "min_sdk": 29, "target_sdk": 36, "debuggable": False,
                "device_validation": "pending_exact_release_apk"}
    output.parent.mkdir(parents=True, exist_ok=True)
    # Só publicar a pasta local após todos os checks e a verificação do ZIP.
    with tempfile.TemporaryDirectory(dir=output.parent) as temporary:
        staging = Path(temporary) / "delivery"
        staging.mkdir()
        (staging / name).write_bytes(data)
        (staging / (name + ".sha256")).write_text(f"{digest}  {name}\n")
        (staging / "release-manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
        documents = {"INSTALACAO.md": ROOT / "docs/INSTALACAO_RELEASE.md",
                     "PRIVACIDADE.md": ROOT / "docs/PRIVACIDADE.md",
                     "LANCAMENTO_1.1.0.md": ROOT / "docs/LANCAMENTO_1.1.0.md",
                     "CHANGELOG.md": ROOT / "CHANGELOG.md"}
        for target, source in documents.items():
            shutil.copyfile(source, staging / target)
        members = sorted(p.name for p in staging.iterdir())
        archive = staging / f"Finder-to-Google-{version}-release.zip"
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
            for member in members:
                z.write(staging / member, member)
        with zipfile.ZipFile(archive) as z:
            if z.testzip() is not None or z.namelist() != members or z.read(name) != data:
                raise ValueError("Falha na verificação do ZIP")
        os.rename(staging, output)
    print(json.dumps(manifest, indent=2))


if __name__ == "__main__":
    main()
