import unittest
from prepare_release import CERTIFICATE, PACKAGE, check_metadata


class ReleaseGateTest(unittest.TestCase):
    def setUp(self):
        self.badging = f"package: name='{PACKAGE}' versionCode='7' versionName='1.1.0' platformBuildVersionCode='36'\nminSdkVersion:'29'\ntargetSdkVersion:'36'\n"
        self.tree = 'E: application\n  A: android:allowBackup(0x01010280)=false\n'
        for kind, name, attributes in [
            ('activity', PACKAGE + '.ui.SettingsActivity', {'exported': 'true'}),
            ('activity', PACKAGE + '.ui.LinkRouterActivity', {'exported': 'true'}),
            ('provider', 'androidx.startup.InitializationProvider', {'exported': 'false', 'authorities': '"' + PACKAGE + '.androidx-startup"'}),
            ('receiver', 'androidx.profileinstaller.ProfileInstallReceiver', {'exported': 'true', 'permission': '"android.permission.DUMP"'}),
        ]:
            self.tree += f'  E: {kind}\n    A: android:name(0x01010003)="{name}"\n'
            for key, value in attributes.items():
                self.tree += f'    A: android:{key}(0x00000000)={value}\n'
        self.signature = f"Signer #1 certificate SHA-256 digest: {CERTIFICATE}\n"

    def verify(self, badging=None, tree=None, signature=None):
        check_metadata(badging or self.badging, tree or self.tree, signature or self.signature, "1.1.0", 7)

    def test_accepts_expected_release(self):
        self.verify()

    def test_rejects_wrong_version_debug_and_permission(self):
        for content in (self.badging.replace("versionCode='7'", "versionCode='6'"),
                        self.badging.replace("versionName='1.1.0'", "versionName='1.1.0-debug'"),
                        self.badging + "application-debuggable\n",
                        self.badging + "uses-permission: name='android.permission.INTERNET'\n"):
            with self.subTest(content=content), self.assertRaises(ValueError):
                self.verify(badging=content)

    def test_rejects_wrong_or_multiple_signers(self):
        for signature in (self.signature.replace(CERTIFICATE, "0" * 64),
                          self.signature + f"Signer #2 certificate SHA-256 digest: {CERTIFICATE}\n"):
            with self.subTest(signature=signature), self.assertRaises(ValueError):
                self.verify(signature=signature)

    def test_rejects_background_components_backup_and_debug(self):
        for tree in (self.tree + " E: service\n", self.tree + " E: receiver\n",
                     self.tree.replace("=false", "=true"),
                     self.tree + " A: android:debuggable(0x0101000f)=true\n"):
            with self.subTest(tree=tree), self.assertRaises(ValueError):
                self.verify(tree=tree)

    def test_rejects_androidx_components_with_weakened_protection(self):
        for tree in (self.tree.replace('android.permission.DUMP', 'android.permission.INTERNET'),
                     self.tree.replace('android:exported(0x00000000)=false', 'android:exported(0x00000000)=true'),
                     self.tree.replace('androidx.profileinstaller.ProfileInstallReceiver', 'org.other.Receiver')):
            with self.subTest(tree=tree), self.assertRaises(ValueError):
                self.verify(tree=tree)

    def test_rejects_alias_in_place_of_required_activity(self):
        with self.assertRaises(ValueError):
            self.verify(tree=self.tree.replace('E: activity', 'E: activity-alias', 1))


if __name__ == "__main__":
    unittest.main()
