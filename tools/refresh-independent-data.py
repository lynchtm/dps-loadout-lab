#!/usr/bin/env python3
"""Acquire factual combat inputs directly from RuneLite and OSRS Wiki Bucket APIs.

BSD-2-Clause; Copyright (c) 2026 Tommy Lynch. No calculator repository is read.
The output data retains separate Wiki/Jagex attribution; it is not BSD artwork/code.
Use --cache <dir> to transform a previously captured raw acquisition without network.
"""
import argparse,collections,hashlib,json,pathlib,re,time,urllib.parse,urllib.request
PLUGIN=pathlib.Path(__file__).resolve().parents[1]
FIELDS={
 'infobox_monster':['page_name','name','id','version_anchor','default_version','hitpoints','size','attack_speed','attack_level','strength_level','defence_level','ranged_level','magic_level','stab_defence_bonus','slash_defence_bonus','crush_defence_bonus','magic_defence_bonus','light_range_defence_bonus','standard_range_defence_bonus','heavy_range_defence_bonus','range_defence_bonus','magic_attack_bonus','attribute','flat_armour','elemental_weakness','elemental_weakness_percent'],
 'infobox_bonuses':['page_name','combat_style','equipment_slot','weapon_attack_speed'],
 'infobox_item':['page_name','item_name','item_id','version_anchor'],
}
def request(url):
 for attempt in range(3):
  try:
   req=urllib.request.Request(url,headers={'User-Agent':'DPS-Loadout-Lab/data/2 (https://github.com/lynchtm/dps-loadout-lab)'})
   with urllib.request.urlopen(req,timeout=45) as r: raw=r.read(20000001)
   if len(raw)>20000000:raise ValueError('Response too large')
   data=json.loads(raw)
   if isinstance(data,dict) and 'error' in data:raise ValueError(str(data['error']))
   return data
  except (OSError,ValueError):
   if attempt==2:raise
   time.sleep(1+attempt)
def main():
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--cache',type=pathlib.Path,required=True);ap.add_argument('--fetch',action='store_true');args=ap.parse_args()
 args.cache.mkdir(parents=True,exist_ok=True)
 urls={'stats':'https://static.runelite.net/item/stats.ids.min.json','names':'https://static.runelite.net/cache/item/names.json'}
 if args.fetch:
  for name,url in urls.items():(args.cache/(name+'.json')).write_text(json.dumps(request(url)),encoding='utf-8')
  for name,fields in FIELDS.items():
   rows=[]
   for offset in range(0,100000,500):
    query="bucket('%s').select(%s).limit(500).offset(%d).run()"%(name,','.join(repr(f) for f in fields),offset)
    batch=request('https://oldschool.runescape.wiki/api.php?'+urllib.parse.urlencode(dict(action='bucket',format='json',query=query)))['bucket']
    rows.extend(batch)
    if len(batch)<500:break
    time.sleep(.3)
   else:raise ValueError('Bucket pagination exceeded limit')
   (args.cache/(name+'.json')).write_text(json.dumps(rows),encoding='utf-8')
 def load(name):return json.loads((args.cache/(name+'.json')).read_text(encoding='utf-8'))
 stats,names=load('stats'),load('names');by_id={};styles=collections.defaultdict(set)
 for b in load('infobox_bonuses'):
  if b.get('combat_style'):styles[b['page_name']].add(b['combat_style'])
 for item in load('infobox_item'):
  for ident in item.get('item_id',[]):
   if str(ident).isdigit():by_id.setdefault(int(ident),[]).append(item)
 slots={0:'head',1:'cape',2:'neck',3:'weapon',4:'body',5:'shield',7:'legs',9:'hands',10:'feet',12:'ring',13:'ammo'}
 mapping={'astab':'stabAttack','aslash':'slashAttack','acrush':'crushAttack','amagic':'magicAttack','arange':'rangedAttack','dstab':'stabDefence','dslash':'slashDefence','dcrush':'crushDefence','dmagic':'magicDefence','drange':'rangedDefence','str':'meleeStrength','rstr':'rangedStrength','mdmg':'magicDamage','prayer':'prayerBonus'}
 equipment={}
 for key,fact in stats.items():
  e=fact.get('equipment');ident=int(key)
  if not e or e.get('slot') not in slots:continue
  candidates=by_id.get(ident,[]);categories=set()
  for c in candidates:categories.update(styles[c['page_name']])
  category=next(iter(categories)) if len(categories)==1 else ''
  item={'id':ident,'name':names.get(key,''),'slot':slots[e['slot']],'category':category,'twoHanded':e.get('is2h',False),'speed':e.get('aspeed',0),'version':candidates[0].get('version_anchor','') if len(candidates)==1 else '',
        'sourcePages':sorted({c['page_name'] for c in candidates}),'stats':{target:round(e.get(source,0)*10) if source=='mdmg' else e.get(source,0) for source,target in mapping.items()}}
  if not item['name']:continue
  equipment[key]=item
 targets=[]
 target_mapping={'attack_level':'attackLevel','strength_level':'strengthLevel','defence_level':'defenceLevel','magic_level':'magicLevel','ranged_level':'rangedLevel','stab_defence_bonus':'stabDefence','slash_defence_bonus':'slashDefence','crush_defence_bonus':'crushDefence','magic_defence_bonus':'magicDefence','light_range_defence_bonus':'lightRangedDefence','standard_range_defence_bonus':'standardRangedDefence','heavy_range_defence_bonus':'heavyRangedDefence','magic_attack_bonus':'offensiveMagic','flat_armour':'flatArmour','elemental_weakness_percent':'weaknessSeverity'}
 for raw in load('infobox_monster'):
  if not raw.get('hitpoints') or not raw.get('name'):continue
  # Missing level data is not equivalent to level zero; unmodelled NPCs are omitted.
  if any(raw.get(k) is None for k in ['defence_level','magic_level']):continue
  attrs=[]
  for s in raw.get('attribute',[]):
   name=s.upper().replace(' ','_').replace('VAMPYRE_TIER_','VAMPYRE_')
   if name in ['DEMON','DRAGON','FIERY','FLYING','GOLEM','KALPHITE','LEAFY','PENANCE','RAT','SHADE','SPECTRAL','UNDEAD','VAMPYRE','VAMPYRE_1','VAMPYRE_2','VAMPYRE_3','XERICIAN']:attrs.append(name)
  for ident in raw.get('id',[]):
   if not str(ident).isdigit():continue
   row={'id':int(ident),'name':raw['name'],'sourcePage':raw['page_name'],'version':raw.get('version_anchor',''),'defaultVersion':raw.get('default_version') is not None,'hitpoints':raw['hitpoints'],'size':raw.get('size') or 1,'speed':raw.get('attack_speed') or 4,'attributes':sorted(set(attrs))}
   for source,target in target_mapping.items():
    fallback=raw.get('range_defence_bonus',0) or 0 if 'RangedDefence' in target else 0
    row[target]=raw.get(source) if raw.get(source) is not None else fallback
   if raw.get('elemental_weakness','').upper() in ['AIR','WATER','EARTH','FIRE']:row['weaknessElement']=raw['elemental_weakness'].upper()
   targets.append(row)
 targets.sort(key=lambda r:(r['name'].lower(),r['id'],not r.get('defaultVersion',False),r['version']))
 out=PLUGIN/'src/main/resources/com/loadoutlab';out.mkdir(parents=True,exist_ok=True)
 for name,data in [('equipment-facts',equipment),('target-facts',targets)]:
  (out/(name+'.json')).write_text(json.dumps(data,sort_keys=True,separators=(',',':'),ensure_ascii=False)+'\n',encoding='utf-8')
 manifest={'acquired':'2026-09-09','sources':urls,'wikiApi':'https://oldschool.runescape.wiki/api.php','fields':FIELDS,'contentLicense':'Wiki factual inputs: separate attribution under the applicable Wiki content policy; game names/assets owned by Jagex. See NOTICE.md.','rawSha256':{name:hashlib.sha256((args.cache/(name+'.json')).read_bytes()).hexdigest() for name in list(urls)+list(FIELDS)},'equipmentCount':len(equipment),'targetVariantCount':len(targets)}
 (out/'facts-provenance.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
 print('Fresh source equipment:',len(equipment),'target variants:',len(targets))
if __name__=='__main__':main()
