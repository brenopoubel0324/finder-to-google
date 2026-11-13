from pathlib import Path
import tempfile
import unittest
import zipfile
from ci_inspect_release import require_unsigned


class UnsignedArtifactTest(unittest.TestCase):
    def test_only_complete_unsigned_container_is_accepted(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "app.apk"
            for extra in (None, "META-INF/CERT.RSA", "META-INF/CERT.SF"):
                with zipfile.ZipFile(path, "w") as z:
                    z.writestr("AndroidManifest.xml", b"fixture")
                    z.writestr("classes.dex", b"fixture")
                    if extra:
                        z.writestr(extra, b"signature")
                if extra:
                    with self.assertRaises(ValueError):
                        require_unsigned(path)
                else:
                    require_unsigned(path)
            path.write_bytes(b"not a zip")
            with self.assertRaises(ValueError):
                require_unsigned(path)
            with zipfile.ZipFile(path, "w") as z:
                z.writestr("AndroidManifest.xml", b"fixture")
            with self.assertRaises(ValueError):
                require_unsigned(path)


if __name__ == "__main__":
    unittest.main()
