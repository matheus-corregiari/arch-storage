"""Validate the release manifest, local Maven files and public dependency metadata."""

import argparse
import json
from pathlib import Path
import xml.etree.ElementTree as ET


GROUP = "io.github.matheus-corregiari"
MODULES = ("core", "memory", "datastore")
SUFFIXES = ("", "-android", "-jvm", "-js", "-wasm-js", "-iosarm64", "-iossimulatorarm64")


def validate_manifest(text, version="1.0.0"):
    rows = [tuple(line.split("\t")) for line in text.splitlines()]
    expected = {(GROUP, f"storage-{module}{suffix}", version) for module in MODULES for suffix in SUFFIXES}
    if len(rows) != 21 or set(rows) != expected:
        raise ValueError("Expected exactly 21 distinct release coordinates at " + version)
    return rows


def verify(repository, manifest):
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    for group, artifact, version in validate_manifest(manifest.read_text()):
        directory = repository / group.replace(".", "/") / artifact / version
        stem = directory / f"{artifact}-{version}"
        pom = ET.parse(str(stem) + ".pom").getroot()
        for field, expected in (("groupId", group), ("artifactId", artifact), ("version", version)):
            if pom.findtext("m:" + field, namespaces=ns) != expected:
                raise ValueError(f"Wrong {field} in {stem}.pom")
        metadata = json.loads(Path(str(stem) + ".module").read_text())
        for variant in metadata["variants"]:
            for file in variant.get("files", []):
                if not (directory / file["url"]).is_file():
                    raise ValueError(f"Missing published file: {directory / file['url']}")
        dependencies = pom.findall("m:dependencies/m:dependency", ns)
        for dep in dependencies:
            name = dep.findtext("m:artifactId", namespaces=ns)
            if name.startswith("storage-") and dep.findtext("m:version", namespaces=ns) != version:
                raise ValueError(f"Mismatched storage dependency in {stem}.pom")
        if artifact.endswith("-jvm"):
            compile_deps = {dep.findtext("m:artifactId", namespaces=ns) for dep in dependencies
                            if dep.findtext("m:scope", "compile", ns) == "compile"}
            required = ({"kotlinx-coroutines-core-jvm", "kotlinx-serialization-json-jvm", "runtime-desktop"}
                        if artifact == "storage-core-jvm" else {"storage-core-jvm"})
            if artifact == "storage-datastore-jvm":
                required |= {"datastore-jvm", "datastore-preferences-jvm"}
            if not required <= compile_deps:
                raise ValueError(f"Missing public compile dependencies for {artifact}: {required - compile_deps}")
        print(f"Verified {group}:{artifact}:{version}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path("build/release-repository"))
    parser.add_argument("--manifest", type=Path, default=Path("build/ci/publications.tsv"))
    args = parser.parse_args()
    verify(args.repository, args.manifest)
