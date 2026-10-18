"""Writes a Markdown table of unit test results per module, for the GitHub Actions run summary."""

import glob
import xml.etree.ElementTree as ET
from collections import defaultdict

totals = defaultdict(lambda: {"tests": 0, "failed": 0, "skipped": 0})
failures = []

for path in sorted(glob.glob("**/build/test-results/test*/TEST-*.xml", recursive=True)):
    module = ":" + path.split("/build/")[0].replace("/", ":")
    suite = ET.parse(path).getroot()
    t = totals[module]
    t["tests"] += int(suite.get("tests", 0))
    t["failed"] += int(suite.get("failures", 0)) + int(suite.get("errors", 0))
    t["skipped"] += int(suite.get("skipped", 0))
    for case in suite.iter("testcase"):
        if case.find("failure") is not None or case.find("error") is not None:
            failures.append(f"{module} · {case.get('classname', '').split('.')[-1]} > {case.get('name')}")

print("## Unit tests\n")
if not totals:
    print("No test results found (the build may have failed before tests ran).")
else:
    print("| Module | Tests | Failed | Skipped |")
    print("| --- | ---: | ---: | ---: |")
    for module, t in sorted(totals.items()):
        mark = "❌ " if t["failed"] else ""
        print(f"| {mark}`{module}` | {t['tests']} | {t['failed']} | {t['skipped']} |")
    all_tests = sum(t["tests"] for t in totals.values())
    all_failed = sum(t["failed"] for t in totals.values())
    print(f"| **Total** | **{all_tests}** | **{all_failed}** | |")
    if failures:
        print("\n### Failed tests\n")
        for name in failures:
            print(f"- {name}")
