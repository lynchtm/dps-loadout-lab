#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Extract equip levels; missing prose never implies unrestricted equipment."""
import argparse
import datetime
import json
import pathlib
import re
import urllib.parse

SKILLS = 'Attack|Strength|Defence|Ranged|Magic|Prayer|Hitpoints|Slayer|Combat|Agility|Hunter|Thieving'
EQUIP = r'\b(?:wield|wear|equip|wielded|worn|equipped|wearing|wielding|equipping)\b'


def plain(text):
    text = re.sub(r'\{\{(?:SCP|SCP2|Skill)\|(' + SKILLS + r')\|(\d+)(?:\|[^}]*)?\}\}', r'\2 \1', text, flags=re.I)
    text = re.sub(r'\[\[(?:File|Image):.*?\]\]', '', text, flags=re.I)
    text = re.sub(r'\[\[(?:[^]|]+\|)?([^]|]+)\]\]', r'\1', text)
    while re.search(r'\{\{[^{}]*\}\}', text):
        text = re.sub(r'\{\{[^{}]*\}\}', '', text)
    text = re.sub(r'<ref\b.*?</ref>|<!--.*?-->', '', text, flags=re.S)
    return text.replace("'''", '').replace("''", '')


def extract(text):
    for sentence in re.split(r'[.!?](?:\s|$)', plain(text)):
        if not re.search(EQUIP, sentence, re.I):
            continue
        if re.search(r'\b(?:not|never)\s+require', sentence, re.I):
            continue
        if re.search(r'no\s+(?:\w+\s+){0,2}requirements?\s+(?:to|for)\s+(?:be\s+|use or\s+)?(?:wear|wield|equip|worn)', sentence, re.I):
            return {}
        requirement = re.search(r'\b(?:requir(?:e[sd]?|ing|ements?)|must have|need(?:s|ed)?|has at least)\b', sentence, re.I)
        direct = re.search(r'\b(?:wielded|worn|equipped)\s+(?:even\s+)?(?:with|at|by)', sentence, re.I)
        if not requirement and not direct:
            continue
        clause = sentence
        forward = re.search(r'\b(?:requires?|requiring)\b', clause, re.I)
        if forward:
            clause = clause[forward.end():]
        clause = re.split(r',\s+(?:it is|they are) (?:made|created|crafted)\b', clause, maxsplit=1, flags=re.I)[0]
        # Reject production levels, but not adjectives such as upgraded/enchanted.
        if re.search(r'\b(?:to (?:creat|craft|smith|fletch|enchant|upgrad)|(?:made|created|crafted) (?:with|at|by)|to obtain|to acquire)\w*', clause, re.I):
            continue
        if re.search(r'\bor\s+(?:level\s+)?\d+', clause, re.I):
            continue
        clause = re.sub(r'[+-]?\d+\s+(?:' + SKILLS + r')\s+(?:bonus|attack|defence|damage|experience)\b', '', clause, flags=re.I)
        result = {}
        pattern = r'(\d+)\s*(?:levels?\s*)?(?:in\s+)?((' + SKILLS + r')\b(?:(?:\s*,\s*(?:and\s+)?|\s+and\s+)(' + SKILLS + r')\b)*)'
        for match in re.finditer(pattern, clause, re.I):
            for skill in re.findall(SKILLS, match[2], re.I):
                result[skill.lower()] = int(match[1])
        for match in re.finditer(r'\b(' + SKILLS + r')\s+(?:levels?\s+)?(?:of\s+)?(?:at least\s+)?(\d+)', clause, re.I):
            result[match[1].lower()] = int(match[2])
        if result and all(0 < level <= (126 if skill == 'combat' else 99) for skill, level in result.items()):
            return result
    return None


def generate(equipment, cache, reviewed):
    pages, sources = {}, {}
    for file in sorted((cache / 'equipment-pages').glob('*.json')):
        data = json.loads(file.read_text(encoding='utf-8'))
        revision = data.get('revisions', [{}])[0]
        required = extract(revision.get('slots', {}).get('main', {}).get('*', ''))
        if required is not None:
            title = urllib.parse.unquote(file.stem)
            pages[title] = required
            sources[title] = {'revision': revision.get('revid'), 'requirements': required}
            resolved = data.get('resolvedTitle', data['title'])
            if resolved != title:
                sources[title]['sourcePage'] = resolved
    for title, record in reviewed.items():
        pages[title] = record['requirements']
        sources[title] = record
    requirements = {}
    for ident, item in equipment.items():
        candidates = [pages[t] for t in item['sourcePages'] if t in pages]
        if candidates and all(c == candidates[0] for c in candidates):
            requirements[ident] = candidates[0]
        # Launcher compatibility is separate from ammunition equip levels.
        if item['slot'] == 'ammo' and any(word in item['name'].lower() for word in ['arrow', 'bolt', 'blessing']):
            requirements[ident] = {}
        if item['slot'] == 'ammo' and 'broad' in item['name'].lower():
            requirements[ident] = {'slayer': 55, 'ranged': 50 if 'arrow' in item['name'].lower() else 61}
    return requirements, sources


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--cache', type=pathlib.Path, required=True)
    args = parser.parse_args()
    plugin = pathlib.Path(__file__).resolve().parents[1]
    out = plugin / 'src/main/resources/com/loadoutlab'
    equipment = json.loads((out / 'equipment-facts.json').read_text(encoding='utf-8'))
    missing = [title for title in sorted({t for item in equipment.values() for t in item['sourcePages']})
               if not (args.cache / 'equipment-pages' / (urllib.parse.quote(title, safe='') + '.json')).exists()]
    if missing:
        raise ValueError('Incomplete article cache; acquire missing pages before refreshing: ' + ', '.join(missing[:10]))
    reviewed = json.loads((plugin / 'tools/reviewed-equipment-requirements.json').read_text(encoding='utf-8'))
    requirements, sources = generate(equipment, args.cache, reviewed)
    (out / 'equipment-requirements.json').write_text(json.dumps(requirements, sort_keys=True, separators=(',', ':')) + '\n', encoding='utf-8')
    provenance = {'acquired': datetime.date.today().isoformat(), 'api': 'https://oldschool.runescape.wiki/api.php',
                  'method': 'Explicit equip requirements plus revision-attributed manual reviews; missing data stays unknown. Skill levels only: quest, kill and account unlocks need confirmation.', 'pages': sources}
    (out / 'requirements-provenance.json').write_text(json.dumps(provenance, sort_keys=True, indent=2) + '\n', encoding='utf-8')
    report = {'catalogIds': len(equipment), 'verifiedIds': len(requirements),
              'missing': [{'id': int(ident), 'name': item['name'], 'slot': item['slot'], 'sourcePages': item['sourcePages']}
                          for ident, item in sorted(equipment.items(), key=lambda pair: int(pair[0]))
                          if ident not in requirements]}
    (args.cache / 'requirement-coverage.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print('Verified level records', len(requirements), 'of', len(equipment), 'items;', len(sources), 'source pages')


if __name__ == '__main__':
    main()
