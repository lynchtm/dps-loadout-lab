#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Acquire public Wiki article revisions. Existing cache entries are preserved.

Use a new cache for a fresh snapshot. No character or calculator source/output data.
"""
import argparse
import json
import pathlib
import time
import urllib.parse
import urllib.request

parser = argparse.ArgumentParser()
parser.add_argument('--cache', type=pathlib.Path, required=True)
parser.add_argument('--equipment', action='store_true')
args = parser.parse_args()
plugin = pathlib.Path(__file__).resolve().parents[1]
if args.equipment:
    equipment = json.loads((plugin / 'src/main/resources/com/loadoutlab/equipment-facts.json').read_text(encoding='utf-8'))
    titles = sorted({p for item in equipment.values() for p in item['sourcePages']})
    directory = args.cache / 'equipment-pages'
else:
    titles = ['Standard spellbook', 'Ancient Magicks', 'Arceuus spellbook']
    directory = args.cache / 'rules'
directory.mkdir(parents=True, exist_ok=True)

def destination(title):
    return directory / (urllib.parse.quote(title, safe='') + '.json' if args.equipment else title.replace('/', '-') + '.txt')

for offset in range(0, len(titles), 40):
    batch = [title for title in titles[offset:offset + 40] if not destination(title).exists()]
    if not batch:
        continue
    query = dict(action='query', format='json', prop='revisions', rvprop='ids|content', rvslots='main', titles='|'.join(batch))
    if args.equipment:
        query['rvsection'] = '0'
    url = 'https://oldschool.runescape.wiki/api.php?' + urllib.parse.urlencode(query)
    request = urllib.request.Request(url, headers={'User-Agent': 'DPS-Loadout-Lab facts (https://github.com/lynchtm/dps-loadout-lab)'})
    with urllib.request.urlopen(request, timeout=45) as response:
        payload = json.load(response)
    for page in payload['query']['pages'].values():
        if args.equipment:
            destination(page['title']).write_text(json.dumps(page), encoding='utf-8')
        elif 'revisions' in page:
            revision = page['revisions'][0]
            destination(page['title']).write_text(revision['slots']['main']['*'], encoding='utf-8')
            destination(page['title']).with_suffix('.source.json').write_text(json.dumps({'page': page['title'], 'revision': revision['revid']}), encoding='utf-8')
    print('Acquired batch', offset // 40 + 1, flush=True)
    time.sleep(.25)
