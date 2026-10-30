#!/usr/bin/env python3
"""Verificações offline de integração do projeto; não substituem Gradle/Android Lint."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main"
A = "{http://schemas.android.com/apk/res/android}"


class ProjectContractTest(unittest.TestCase):
    def test_xml_is_well_formed_and_local_resources_exist(self):
        xml_files = sorted(MAIN.rglob("*.xml"))
        resources = set()
        for path in xml_files:
            root = ET.parse(path).getroot()
            if root.tag == "resources":
                resources.update((child.tag, child.attrib["name"]) for child in root if "name" in child.attrib)
            elif path.parent.name in {"layout", "drawable"}:
                resources.add((path.parent.name, path.stem))
        for path in xml_files:
            for kind, name in re.findall(r'@(string|color|bool|layout|drawable|style)/([A-Za-z0-9_.]+)', path.read_text()):
                if kind == "style" and name.startswith("Widget.Material3."):
                    continue  # recurso da dependência Material
                self.assertIn((kind, name), resources, f"{path}: @{kind}/{name}")

    def test_manifest_has_no_permissions_or_resident_components(self):
        root = ET.parse(MAIN / "AndroidManifest.xml").getroot()
        self.assertFalse(root.findall("uses-permission"))
        self.assertFalse(root.findall("uses-permission-sdk-23"))
        app = root.find("application")
        self.assertEqual("false", app.attrib[A + "allowBackup"])
        for component in ("service", "receiver", "provider"):
            self.assertFalse(app.findall(component), component)
        self.assertEqual(2, len(app.findall("activity")))

    def test_exported_browser_filter_has_both_schemes_without_host(self):
        root = ET.parse(MAIN / "AndroidManifest.xml").getroot()
        router = next(a for a in root.find("application").findall("activity")
                      if a.attrib[A + "name"] == ".ui.LinkRouterActivity")
        self.assertEqual("true", router.attrib[A + "exported"])
        filters = router.findall("intent-filter")
        self.assertEqual(1, len(filters))
        intent_filter = filters[0]
        self.assertEqual({"android.intent.action.VIEW"}, {a.attrib[A + "name"] for a in intent_filter.findall("action")})
        self.assertEqual({"android.intent.category.DEFAULT", "android.intent.category.BROWSABLE"},
                         {c.attrib[A + "name"] for c in intent_filter.findall("category")})
        self.assertEqual({"http", "https"}, {d.attrib[A + "scheme"] for d in intent_filter.findall("data")})
        self.assertFalse(any(A + "host" in d.attrib for d in intent_filter.findall("data")))

    def test_queries_remain_scoped(self):
        queries = ET.parse(MAIN / "AndroidManifest.xml").getroot().find("queries")
        self.assertEqual(["com.google.android.googlequicksearchbox"], [p.attrib[A + "name"] for p in queries.findall("package")])
        self.assertEqual({"http", "https"}, {i.find("data").attrib[A + "scheme"] for i in queries.findall("intent")})

    def test_kotlin_resource_references_exist(self):
        resources = set()
        ids = set()
        for path in MAIN.rglob("*.xml"):
            root = ET.parse(path).getroot()
            if root.tag == "resources":
                resources.update((c.tag, c.attrib["name"].replace(".", "_")) for c in root if "name" in c.attrib)
            elif path.parent.name == "layout":
                resources.add(("layout", path.stem))
            ids.update(re.findall(r"@\+id/(\w+)", path.read_text()))
        resources.update(("id", name) for name in ids)
        for path in (ROOT / "app/src").rglob("*.kt"):
            for kind, name in re.findall(r"(?<!android\.)\bR\.(id|string|layout|style)\.(\w+)", path.read_text()):
                self.assertIn((kind, name), resources, f"{path}: R.{kind}.{name}")


if __name__ == "__main__":
    unittest.main(verbosity=2)
