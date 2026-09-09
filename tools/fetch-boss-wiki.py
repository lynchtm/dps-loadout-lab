#!/usr/bin/env python3
"""Snapshot public Wiki search/page responses for the offline bossWikiAudit task.

No character or bank information is read. Catalog entries have key and title fields.
Pass --refresh to deliberately replace an existing snapshot with current revisions.
"""
import argparse
import hashlib
import json
import pathlib
import re
import time
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = "DPS-Loadout-Lab-audit/1.0 (https://github.com/lynchtm/dps-loadout-lab)"


def request(params):
    url = API + "?" + urllib.parse.urlencode({"format": "json", **params})
    for attempt in range(3):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": USER_AGENT}), timeout=30) as response:
                payload = response.read(2_000_001)
            if len(payload) > 2_000_000:
                raise ValueError("Wiki response exceeds plugin limit")
            data = json.loads(payload)
            if "error" in data:
                raise ValueError(data["error"])
            return data
        except (OSError, ValueError):
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("catalog", type=pathlib.Path)
    parser.add_argument("output", type=pathlib.Path)
    parser.add_argument("--refresh", action="store_true")
    parser.add_argument("--components", action="store_true", help="Also snapshot searches for component NPC names")
    args = parser.parse_args()
    fixture_dir = pathlib.Path(__file__).resolve().parent.parent / "src/test/resources/boss-wiki"
    if args.output.resolve() == fixture_dir or fixture_dir in args.output.resolve().parents:
        parser.error("Choose a separate snapshot directory; pinned regression fixtures are protected")
    args.output.mkdir(parents=True, exist_ok=True)
    catalog = json.loads(args.catalog.read_text(encoding="utf-8-sig"))
    manifest_path = args.output / "manifest.json"
    previous = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else {}
    if args.refresh or not previous:
        log = request({"action": "parse", "page": "Collection log", "prop": "wikitext|revid"})["parse"]
        bosses = re.split(r"(?m)^==[^=]", log["wikitext"]["*"].split("==Bosses==", 1)[1], maxsplit=1)[0]
        live = {name.strip().casefold() for name in re.findall(r"(?m)^===([^=]+)===$", bosses)}
        pinned = {entry["logEntry"].casefold() for entry in catalog["pages"]}
        if live != pinned:
            raise SystemExit(f"Collection Log changed; update the catalog first. Added: {sorted(live-pinned)}; removed: {sorted(pinned-live)}")
        catalog["source"]["revision"] = str(log["revid"])
    cached = {entry["key"]: entry for entry in previous.get("pages", [])}
    manifest = {"catalogSource": catalog.get("source"), "pages": []}
    for entry in catalog["pages"]:
        old = cached.get(entry["key"])
        if not args.refresh and old and not old.get("error"):
            path = args.output / old["file"]
            if path.exists() and hashlib.sha256(path.read_bytes()).hexdigest() == old.get("sha256"):
                manifest["pages"].append(old)
                continue
        record = dict(entry)
        try:
            titles = []
            for template in ("Recommended equipment", "Equipment"):
                result = request({"action": "query", "list": "search", "srsearch": entry.get("query", entry["key"]) + ' hastemplate:"' + template + '"', "srnamespace": "0", "srlimit": "15"})
                titles.extend(hit["title"] for hit in result["query"]["search"] if hit["title"] not in titles)
            record["searchTitles"] = titles
            result = request({"action": "parse", "page": entry["title"], "prop": "wikitext|revid", "redirects": "1"})["parse"]
            text = result["wikitext"]["*"]
            filename = hashlib.sha256(entry["key"].encode()).hexdigest()[:16] + ".wikitext"
            payload = text.encode("utf-8")
            (args.output / filename).write_bytes(payload)
            record.update(title=result["title"], revision=str(result["revid"]), file=filename, sha256=hashlib.sha256(payload).hexdigest(), fetchedAt=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()))
            print(f"{entry['key']}: {record['title']} @ {record['revision']} ({len(titles)} search hits)", flush=True)
        except (OSError, ValueError, KeyError) as error:
            record["error"] = str(error)
            print(f"{entry['key']}: ERROR {error}", flush=True)
        manifest["pages"].append(record)
        manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    manifest_path.write_text(json.dumps(manifest, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    component_path = args.output / "component-searches.json"
    if args.components and (args.refresh or not component_path.exists()):
        components = {}
        for entry in catalog["pages"]:
            for component in entry.get("components", []):
                components.setdefault(component, set()).add(entry["title"])
        records = []
        for component, expected in sorted(components.items()):
            queries, titles = [], []
            for template in ("Recommended equipment", "Equipment"):
                query = component + ' hastemplate:"' + template + '"'
                data = request({"action": "query", "list": "search", "srsearch": query, "srnamespace": "0", "srlimit": "15"})
                found = [hit["title"] for hit in data["query"]["search"]]
                queries.append({"template": template, "query": query, "searchTitles": found})
                titles.extend(title for title in found if title not in titles)
            records.append({"component": component, "expectedTitles": sorted(expected), "queries": queries, "consolidatedSearchTitles": titles})
        component_path.write_text(json.dumps({"source": API, "fetchedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()), "componentCount": len(records), "components": records}, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    if any("error" in entry for entry in manifest["pages"]):
        raise SystemExit(1)


if __name__ == "__main__":
    main()
