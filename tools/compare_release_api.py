"""Compare published RC16 JVM public/protected signatures with locally published 1.0.0."""

import argparse
from pathlib import Path
import subprocess


PUBLIC_CLASSES = {
    "core": ["KeyValue", "KeyValue$Companion", "KeyValue_javaKt", "StorageProvider", "StorageProvider$Defaults"],
    "memory": ["MemoryStoreProvider", "MemoryStoreProvider$MemoryKeyValue"],
    "datastore": ["DataStoreProvider"],
}


def signatures(jar, class_name):
    output = subprocess.check_output(
        ["javap", "-protected", "-s", "-classpath", str(jar), class_name], text=True
    )
    return {line.strip() for line in output.splitlines()
            if line.startswith("  ") and "access$" not in line and line.strip()}


def compare(cache, repository):
    removed = []
    for module, classes in PUBLIC_CLASSES.items():
        artifact = f"storage-{module}-jvm"
        old = [p for p in (cache / artifact / "2.0.0-rc16").rglob("*.jar")
               if not p.name.endswith(("-sources.jar", "-javadoc.jar"))]
        if len(old) != 1:
            raise ValueError(f"Expected one resolved RC16 jar for {artifact}, found {old}")
        new = repository / artifact / "1.0.0" / f"{artifact}-1.0.0.jar"
        for name in classes:
            qualified = f"br.com.arch.toolkit.storage.{module}.{name}"
            before, after = signatures(old[0], qualified), signatures(new, qualified)
            missing = before - after
            added = after - before
            print(f"{qualified}: {len(missing)} removed, {len(added)} added signature lines")
            for line in sorted(missing):
                removed.append(f"{qualified}: {line}")
    if removed:
        raise ValueError("Removed or changed public signatures:\n" + "\n".join(removed))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--cache", type=Path, default=Path.home() / ".gradle/caches/modules-2/files-2.1/io.github.matheus-corregiari")
    parser.add_argument("--repository", type=Path, default=Path("build/release-repository/io/github/matheus-corregiari"))
    args = parser.parse_args()
    compare(args.cache, args.repository)
