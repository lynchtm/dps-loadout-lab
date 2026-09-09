#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Extract spell levels and damage from acquired Wiki spellbook articles (not calculator data)."""
import json,pathlib,re,argparse,hashlib
p=argparse.ArgumentParser();p.add_argument('--cache',type=pathlib.Path,required=True);a=p.parse_args()
rows=[]
for title,book in [('Standard spellbook','standard'),('Ancient Magicks','ancient'),('Arceuus spellbook','arceuus')]:
    text=(a.cache/'rules'/(title+'.txt')).read_text(encoding='utf-8')
    for block in text.split('|-'):
        pattern=r'\|\{\{plinkt\|([^}|]+)\}\}' if book=='standard' else r'\|\[\[([^]|]+)\]\]\s*\n\|(\d+)'
        match=re.search(pattern,block)
        if not match:continue
        name=match.group(1)
        # The damage cell follows XP; consume only plain numeric cells, skipping rune/cost markup.
        numeric=re.findall(r'^\|\s*(\d+(?:\.\d+)?)(?:→\d+)?(?:\s*\([^)]+\))?\s*$',block,re.M)
        if len(numeric)<3:continue
        level,exp,hit=numeric[:3]
        if not float(hit).is_integer() or not 0<int(hit)<=40:continue
        if book=='arceuus' and not(name.endswith('Grasp') or name.endswith('Demonbane')):continue
        if book=='ancient' and not any(name.endswith(t) for t in ['Rush','Burst','Blitz','Barrage']):continue
        element=next((e.lower() for e in ['Water','Earth','Fire'] if name.startswith(e+' ')), 'air' if name.startswith('Wind ') else None)
        rows.append(dict(name=name,spellbook=book,element=element,level=int(level),max_hit=int(hit),sourcePage=title))
rows=sorted({r['name']:r for r in rows}.values(),key=lambda r:(r['level'],r['name']))
assert len(rows)>=40,(len(rows),rows)
out=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources/com/loadoutlab'
(out/'spells.json').write_text(json.dumps(rows,indent=2)+'\n',encoding='utf-8')
print('Spell facts',len(rows),[(r['name'],r['max_hit']) for r in rows])
