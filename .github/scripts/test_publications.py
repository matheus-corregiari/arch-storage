"""Release manifest regression tests; no registry writes or credentials are used."""

import sys
from pathlib import Path
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "tools"))
from verify_publications import GROUP, MODULES, SUFFIXES, validate_manifest


class PublicationManifestTest(unittest.TestCase):
    def setUp(self):
        self.rows = [f"{GROUP}\tstorage-{module}{suffix}\t1.0.0"
                     for module in MODULES for suffix in SUFFIXES]

    def test_accepts_exact_release_publications(self):
        self.assertEqual(21, len(validate_manifest("\n".join(self.rows))))

    def test_rejects_missing_duplicate_extra_wrong_version_and_wrong_group(self):
        cases = [self.rows[:-1], self.rows[:-1] + [self.rows[0]],
                 self.rows + [f"{GROUP}\tstorage-core-iosx64\t1.0.0"],
                 [row.replace("1.0.0", "0.0.0-SNAPSHOT") for row in self.rows],
                 [row.replace(GROUP, "wrong.group") for row in self.rows]]
        for rows in cases:
            with self.subTest(rows=rows), self.assertRaises(ValueError):
                validate_manifest("\n".join(rows))


if __name__ == "__main__":
    unittest.main()
