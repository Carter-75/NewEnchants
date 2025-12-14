#!/usr/bin/env python3
"""NewEnchants – enchants build helper.

The script mirrors the automation workflow we use on Universal Mob War:
 • Fast structural validation (design doc, mixins, gradle)
 • Optional Gradle build
 • Optional log generation
"""

from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import List

class Color:
    BLUE = "\033[94m"
    CYAN = "\033[96m"
    GREEN = "\033[92m"
    RED = "\033[91m"
    YELLOW = "\033[93m"
    RESET = "\033[0m"
    BOLD = "\033[1m"


def log(message: str, color: str = Color.RESET) -> None:
    print(f"{color}{message}{Color.RESET}")


def header(message: str) -> None:
    bar = "═" * 72
    log(bar, Color.BLUE)
    log(message.center(72), Color.BOLD + Color.CYAN)
    log(bar, Color.BLUE)


class EnchantsBuilder:
    def __init__(self, root: Path):
        self.root = root
        self.errors: List[str] = []
        self.warnings: List[str] = []
        self.build_started = False
        self.build_succeeded = False
        self.log_path = root / "enchants_build.log"
        self.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        self.captured_outputs: List[str] = []

    def run(self, mode: str) -> None:
        header("NEWENCHANTS – ENCHANTS BUILD")
        if not self.validate():
            self.finish(False)
            return
        if mode in {"build", "full"} and not self.build():
            self.finish(False)
            return
        if mode == "full":
            self.run_tests()
        self.finish(True)

    # ------------------------------------------------------------------
    # Validation steps
    # ------------------------------------------------------------------
    def validate(self) -> bool:
        header("VALIDATION")
        self.check_enchant_specs()
        self.check_mixins()
        self.check_gradle_props()
        self.check_source_tree()
        return not self.errors

    def check_enchant_specs(self) -> None:
        specs_dir = self.root / "enchants"
        if not specs_dir.exists() or not specs_dir.is_dir():
            self.fail("Enchantment specs directory 'enchants' is missing")
            return
        readme = specs_dir / "README.md"
        if not readme.exists():
            self.warn("Enchantment blueprint README.md missing; keep the design doc for context")
        json_files = sorted(specs_dir.glob("*.json"))
        if not json_files:
            self.fail("No enchant JSON specifications found in 'enchants'")
            return
        total = len(json_files)
        log(f"Enchantment spec files: {total}")
        if total != 50:
            self.warn(f"Expected 50 enchant specs, found {total}")
        required_keys = {"id", "slug", "name", "max_level", "effects", "recipe"}
        for path in json_files:
            try:
                data = json.loads(path.read_text(encoding="utf-8"))
            except json.JSONDecodeError as exc:
                self.fail(f"Spec {path.name} is invalid JSON: {exc}")
                continue
            missing = required_keys.difference(data.keys())
            if missing:
                self.warn(f"Spec {path.name} missing keys: {', '.join(sorted(missing))}")
            effects = data.get("effects") or []
            if not effects:
                self.warn(f"Spec {path.name} has no effects listed")
            recipe = data.get("recipe") or {}
            rows = recipe.get("rows") or []
            if not rows or any(len(row) != 3 for row in rows):
                self.warn(f"Spec {path.name} has malformed recipe rows")

    def check_mixins(self) -> None:
        mixin_file = self.root / "src/main/resources/newenchants.mixins.json"
        if not mixin_file.exists():
            self.fail("Mixin config missing")
            return
        data = mixin_file.read_text(encoding="utf-8")
        required = ["PersistentProjectileEntityMixin", "FireworkRocketEntityMixin"]
        for entry in required:
            if entry not in data:
                self.warn(f"Mixin {entry} not registered")

    def check_gradle_props(self) -> None:
        props = self.root / "gradle.properties"
        if not props.exists():
            self.fail("gradle.properties not found")
            return
        content = props.read_text(encoding="utf-8")
        if "minecraft_version=1.21.1" not in content:
            self.warn("minecraft_version mismatch – expected 1.21.1")
        if "loader_version" not in content:
            self.warn("loader_version missing from gradle.properties")

    def check_source_tree(self) -> None:
        src = self.root / "src/main/java/com/carter/newenchants"
        if not src.exists():
            self.fail("Java sources missing under src/main/java/com/carter/newenchants")
        mixin_pkg = self.root / "src/main/java/com/carter/newenchants/mixin"
        if not mixin_pkg.exists():
            self.warn("Mixin package missing")

    # ------------------------------------------------------------------
    # Build + test helpers
    # ------------------------------------------------------------------
    def build(self) -> bool:
        header("GRADLE BUILD")
        self.build_started = True
        gradlew = "gradlew.bat" if os.name == "nt" else "./gradlew"
        cmd = [gradlew, "clean", "build", "--no-daemon", "--stacktrace"]
        try:
            result = subprocess.run(cmd, cwd=self.root, capture_output=True, text=True, check=False)
        except FileNotFoundError:
            self.fail("Gradle wrapper not found; run ./gradlew once first")
            return False
        if result.returncode == 0:
            self.build_succeeded = True
            log("Gradle build succeeded", Color.GREEN)
        else:
            self.fail("Gradle build failed – see log")
            self.log_output(result.stdout, result.stderr)
            return False
        return True

    def run_tests(self) -> None:
        header("TESTS")
        cmd = ["gradlew.bat" if os.name == "nt" else "./gradlew", "test", "--no-daemon"]
        result = subprocess.run(cmd, cwd=self.root)
        if result.returncode != 0:
            self.warn("Test task reported failures")

    # ------------------------------------------------------------------
    def finish(self, ok: bool) -> None:
        header("SUMMARY")
        status = "SUCCESS" if ok and not self.errors else "FAILED"
        color = Color.GREEN if status == "SUCCESS" else Color.RED
        log(f"Status: {status}", color)
        if self.errors:
            for err in self.errors:
                log(f"  - {err}", Color.RED)
        if self.warnings:
            for warn in self.warnings:
                log(f"  ! {warn}", Color.YELLOW)
        with self.log_path.open("w", encoding="utf-8") as fh:
            fh.write(f"NewEnchants build – {self.timestamp}\n")
            fh.write(f"Status: {status}\n")
            if self.errors:
                fh.write("Errors:\n" + "\n".join(self.errors) + "\n")
            if self.warnings:
                fh.write("Warnings:\n" + "\n".join(self.warnings) + "\n")
            if self.captured_outputs:
                fh.write("\nCaptured task output:\n")
                fh.write("\n\n".join(self.captured_outputs) + "\n")
        if not ok:
            sys.exit(1)

    # ------------------------------------------------------------------
    def fail(self, message: str) -> None:
        self.errors.append(message)
        log(message, Color.RED)

    def warn(self, message: str) -> None:
        self.warnings.append(message)
        log(message, Color.YELLOW)

    def log_output(self, stdout: str, stderr: str) -> None:
        sections: List[str] = []
        if stdout:
            sections.append("=== STDOUT ===\n" + stdout.strip())
        if stderr:
            sections.append("=== STDERR ===\n" + stderr.strip())
        if sections:
            self.captured_outputs.append("\n\n".join(sections))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Enchants build helper for NewEnchants")
    parser.add_argument("mode", choices=["check", "build", "full"], nargs="?", default="check",
                        help="check = validate only, build = validate + gradle build, full = validate + build + tests")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    root = Path(__file__).parent.resolve()
    builder = EnchantsBuilder(root)
    builder.run(args.mode)


if __name__ == "__main__":
    main()
