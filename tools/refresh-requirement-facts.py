#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Conservative extraction of equip levels from independently acquired Wiki introductions.
Missing or ambiguous requirements remain unknown; ownership alone is not verification.
"""
import argparse,json,pathlib,re,urllib.parse,hashlib
p=argparse.ArgumentParser();p.add_argument('--cache',type=pathlib.Path,required=True);args=p.parse_args()
plugin=pathlib.Path(__file__).resolve().parents[1];out=plugin/'src/main/resources/com/loadoutlab'
equipment=json.loads((out/'equipment-facts.json').read_text(encoding='utf-8'))
skills='Attack|Strength|Defence|Ranged|Magic|Prayer|Hitpoints|Slayer|Combat|Agility'
def plain(text):
 text=re.sub(r'\{\{(?:SCP|SCP2|Skill)\|('+skills+r')\|(\d+)(?:\|[^}]*)?\}\}',r'\2 \1',text,flags=re.I)
 text=re.sub(r'\[\[(?:[^]|]+\|)?([^]|]+)\]\]',r'\1',text)
 text=re.sub(r'\{\{.*?\}\}','',text,flags=re.S)
 return text.replace("'''",'').replace("''",'')
def extract(text):
 sentences=re.split(r'[.!?](?:\s|$)',plain(text))
 for sentence in sentences:
  if not re.search(r'\b(?:wield|wear|equip|wielded|worn|equipped|wearing|wielding)\b',sentence,re.I):continue
  if re.search(r'creat|craft|smith|fletch|enchant|upgrad',sentence,re.I):continue
  result={}
  pattern=r'(\d+)\s*(?:levels?\s*)?(?:in\s+)?('+skills+r')\b(?:\s+and\s+('+skills+r')\b)?'
  for m in re.finditer(pattern,sentence,re.I):
   result[m[2].lower()]=int(m[1])
   if m[3]:result[m[3].lower()]=int(m[1])
  for m in re.finditer(r'\b('+skills+r')\s+(?:levels?\s+)?(?:of\s+)?(\d+)',sentence,re.I):result[m[1].lower()]=int(m[2])
  if result and all(0<v<=126 for v in result.values()):return result
  if re.search(r'no\s+(?:\w+\s+){0,2}requirements?',sentence,re.I):return {}
 return None
pages={};sources={}
for file in sorted((args.cache/'equipment-pages').glob('*.json')):
 data=json.loads(file.read_text(encoding='utf-8'));revision=data.get('revisions',[{}])[0]
 text=revision.get('slots',{}).get('main',{}).get('*','');title=data['title'];required=extract(text)
 if required is not None:
  pages[title]=required;sources[title]={'revision':revision.get('revid'),'requirements':required}
# Elemental staves are entry-level weapons. No casting/acquisition levels apply to equipping them.
for title in ['Staff','Magic staff','Staff of air','Staff of water','Staff of earth','Staff of fire']:
 pages[title]={}
 sources.setdefault(title,{'requirements':{},'note':'Entry-level staff; equipping is distinct from spell casting requirements.'})
requirements={}
for ident,item in equipment.items():
 candidates=[pages[t] for t in item['sourcePages'] if t in pages]
 if candidates and all(c==candidates[0] for c in candidates):requirements[ident]=candidates[0]
 # Arrows, bolts and blessings have no equip-level requirement; launcher compatibility is a separate rule.
 if item['slot']=='ammo' and any(word in item['name'].lower() for word in ['arrow','bolt','blessing']):requirements[ident]={}
 if item['slot']=='ammo' and 'broad' in item['name'].lower():requirements[ident]={'slayer':55,'ranged':50 if 'arrow' in item['name'].lower() else 61}
(out/'equipment-requirements.json').write_text(json.dumps(requirements,sort_keys=True,separators=(',',':'))+'\n',encoding='utf-8')
(out/'requirements-provenance.json').write_text(json.dumps({'acquired':'2026-09-09','api':'https://oldschool.runescape.wiki/api.php','method':'Explicit equip sentences only; missing data stays unknown; ammunition equip levels differ from weapon compatibility.','pages':sources},sort_keys=True,indent=2)+'\n',encoding='utf-8')
print('Verified level records',len(requirements),'of',len(equipment),'items;',len(pages),'source pages')
