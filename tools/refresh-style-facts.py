#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Read rendered Wiki combat-option facts, without reading any calculator implementation."""
import argparse,json,pathlib,re,urllib.parse,urllib.request,hashlib
p=argparse.ArgumentParser();p.add_argument('--cache',type=pathlib.Path,required=True);p.add_argument('--fetch',action='store_true');a=p.parse_args()
plugin=pathlib.Path(__file__).resolve().parents[1];out=plugin/'src/main/resources/com/loadoutlab'
items=json.loads((out/'equipment-facts.json').read_text(encoding='utf-8'))
categories=sorted({x['category'] for x in items.values() if x['category']}|{'Unarmed'})
cache=a.cache/'style-expansions.json'
if a.fetch:
 text='\n'.join('CATEGORY:'+name+'\n{{CombatStyles|'+name+'}}\nENDCATEGORY' for name in categories)
 url='https://oldschool.runescape.wiki/api.php?'+urllib.parse.urlencode(dict(action='expandtemplates',format='json',text=text,prop='wikitext'))
 request=urllib.request.Request(url,headers={'User-Agent':'DPS-Loadout-Lab weapon interface fact acquisition (https://github.com/lynchtm/dps-loadout-lab)'})
 data=json.load(urllib.request.urlopen(request,timeout=60));cache.write_text(json.dumps(data),encoding='utf-8')
text=json.loads(cache.read_text(encoding='utf-8'))['expandtemplates']['wikitext'];styles={}
for name,block in re.findall(r'CATEGORY:([^\n]+)\n(.*?)\nENDCATEGORY',text,re.S):
 rows=[]
 for row in re.findall(r'<tr>(.*?)</tr>',block,re.S):
  cells=re.findall(r'<td[^>]*>(.*?)</td>',row,re.S)
  if len(cells)!=6:continue
  button,attack,stance=cells[1:4]
  if attack not in ['Stab','Slash','Crush','Ranged','Magic','Light','Standard','Heavy']:continue
  bonuses={skill.lower():int(value) for value,skill in re.findall(r'\+(\d+) (Attack|Strength|Defence|Ranged|Magic)',cells[5])}
  rows.append(dict(name=button,type=attack,stance=stance,bonuses=bonuses))
 if rows:styles[name.lower()]=rows
assert 'unarmed' in styles and len(styles)>=20,(styles.keys(),text[:1000])
(out/'weapon-options.json').write_text(json.dumps(styles,sort_keys=True,indent=2)+'\n',encoding='utf-8')
(out/'weapon-options-provenance.json').write_text(json.dumps({'acquired':'2026-09-09','source':'https://oldschool.runescape.wiki/w/Weapons/Categories','api':'https://oldschool.runescape.wiki/api.php?action=expandtemplates','template':'CombatStyles','rawSha256':hashlib.sha256(cache.read_bytes()).hexdigest(),'categories':categories,'method':'Extract button name, attack type, stance and displayed skill bonuses from rendered tables; no template/module implementation is copied.'},indent=2)+'\n',encoding='utf-8')
print('Weapon interfaces:',len(styles),'options:',sum(len(v) for v in styles.values()),'unresolved:',sorted(set(c.lower() for c in categories)-set(styles)))
