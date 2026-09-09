#!/usr/bin/env python3
# SPDX-License-Identifier: BSD-2-Clause
"""Check migration packaging boundaries. This is not a copyright or formula-parity proof."""
import argparse,hashlib,json,pathlib,re,zipfile
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--jar',type=pathlib.Path);p.add_argument('--output',type=pathlib.Path);a=p.parse_args()
problems=[];files=[]
for tree in ('src/main','src/test','src/independentTest'):
 for file in sorted((root/tree).rglob('*')):
  if not file.is_file():continue
  path=file.relative_to(root).as_posix();raw=file.read_bytes()
  if file.suffix=='.java':
   text=raw.decode('utf-8-sig')
   if re.search(r'com\.dpscalc\.(?:calc|state|equipment|combat|fixture|parity)\b',text):problems.append(path+': legacy engine reference')
   if re.search(r'com\.dpscalc\.data\b|\bDpsCalcPlugin\b',text):problems.append(path+': legacy runtime contract')
   if 'GNU General Public License' in text:problems.append(path+': GPL source notice')
   if '/com/dpscalc/' in text:problems.append(path+': legacy resource path')
   if 'com/loadoutlab/' in path and 'SPDX-License-Identifier: BSD-2-Clause' not in text:problems.append(path+': missing authored-source notice')
  files.append({'path':path,'sha256':hashlib.sha256(raw).hexdigest()})
legacy=root/'src/main/java/com/dpscalc'
for folder in ['calc','state','equipment','data','combat','fixture','parity']:
 if any((legacy/folder).rglob('*.java')):problems.append('legacy runtime sources remain: '+folder)
if any((root/'src/main/resources/com/dpscalc').rglob('*.*')):problems.append('legacy resource bundle remains')
for f in ['LICENSE','src/main/resources/META-INF/LICENSE']:
 text=(root/f).read_text(encoding='utf-8-sig')
 if 'BSD 2-Clause License' not in text or 'GNU GENERAL PUBLIC LICENSE' in text:problems.append(f+': unexpected current code license')
if (root/'LICENSE').read_bytes()!=(root/'src/main/resources/META-INF/LICENSE').read_bytes():problems.append('source and binary licenses differ')
if (root/'NOTICE.md').read_bytes()!=(root/'src/main/resources/META-INF/NOTICE.md').read_bytes():problems.append('source and binary notices differ')
if 'Jiimbones' not in (root/'src/main/resources/META-INF/LICENSE-wiki-gear-setups').read_text():problems.append('missing BSD third-party attribution')
jar_summary=None
if a.jar:
 with zipfile.ZipFile(a.jar) as jar:
  names=jar.namelist()
  for name in names:
   if re.match(r'com/dpscalc/(?:calc|state|equipment|data|combat|fixture|parity)/',name):problems.append('legacy JAR entry: '+name)
   if name.startswith('net/runelite/'):problems.append('reserved namespace in JAR: '+name)
   if name.endswith('.class') and int.from_bytes(jar.read(name)[6:8],'big')>55:problems.append('not Java 11: '+name)
  for name in ['com/loadoutlab/DpsLoadoutLabPlugin.class','com/loadoutlab/engine/DamagePmf.class','com/loadoutlab/calculation/DpsCalculator.class','com/loadoutlab/equipment-facts.json','META-INF/LICENSE','META-INF/NOTICE.md','META-INF/LICENSE-wiki-gear-setups']:
   if name not in names:problems.append('missing JAR entry: '+name)
  if len(names)!=len(set(names)):problems.append('duplicate JAR entries')
  if jar.read('META-INF/LICENSE')!=(root/'LICENSE').read_bytes():problems.append('JAR license does not match source')
  jar_summary={'sha256':hashlib.sha256(a.jar.read_bytes()).hexdigest(),'classes':sum(n.endswith('.class') for n in names),'entries':len(names)}
report={'scope':'source/resource/binary migration boundary, not legal certification or gameplay parity','files':files,'jar':jar_summary,'problems':problems}
if a.output:a.output.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'checkedFiles':len(files),'jar':jar_summary,'problems':problems},indent=2))
raise SystemExit(bool(problems))
