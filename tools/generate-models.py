#!/usr/bin/env python3
"""Generate plain compatibility DTOs from the persisted editor field contract.

Independently authored implementation, BSD-2-Clause, Copyright 2026 Tommy Lynch.
The property names preserve users' saved loadouts. No upstream method bodies,
calculations, datasets or fixture expectations are used by this generator.
"""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]/'src/main/java/com/loadoutlab'
HEADER='/* SPDX-License-Identifier: BSD-2-Clause\n * Copyright (c) 2026, Tommy Lynch. Independently authored; see PROVENANCE.md.\n */\n'
def write(package,name,body):
 p=ROOT/package/(name+'.java');p.parent.mkdir(parents=True,exist_ok=True)
 p.write_text(HEADER+'package com.loadoutlab.'+package+';\n\n'+body+'\n',encoding='utf-8')
def dto(name,fields,extra='',imports='',ctor=''):
 body=imports+'\npublic class '+name+' {\n'
 for field,kind,default in fields:
  body+='    private '+kind+' '+field+(' = '+default if default else '')+';\n'
 body+=ctor
 for field,kind,default in fields:
  cap=field[0].upper()+field[1:]
  body+='    public '+kind+' '+('is' if kind=='boolean' else 'get')+cap+'() { return '+field+'; }\n'
  body+='    public void set'+cap+'('+kind+' value) { '+field+' = value; }\n'
 body+=extra+'}\n';write('model',name,body)
stats=['stabAttack','slashAttack','crushAttack','magicAttack','rangedAttack','meleeStrength','rangedStrength','magicDamage','stabDefence','slashDefence','crushDefence','magicDefence','rangedDefence','prayerBonus']
dto('EquipmentStats',[(n,'int','0') for n in stats],''.join('    public void add'+n[0].upper()+n[1:]+'(int value) { '+n+' += value; }\n' for n in stats)+'''
    public int getAttackBonusForType(AttackType type) {
        switch(type) { case STAB:return stabAttack;case SLASH:return slashAttack;case CRUSH:return crushAttack;case MAGIC:return magicAttack;default:return rangedAttack; }
    }
''')
fields=[(n+'Level','int','99') for n in ['attack','strength','defence','ranged','magic','prayer','hitpoints']]+[(n+'Boost','int','0') for n in ['attack','strength','defence','ranged','magic']]
fields += [('currentHitpoints','int','99'),('equipmentStats','EquipmentStats','new EquipmentStats()'),('equippedItemIds','int[]','new int[14]')]
fields += [(n,'String[]','new String[14]') for n in ['equippedItemNames','equippedItemVersions','equippedItemCategories']]
fields += [('combatStyle','CombatStyle','CombatStyle.UNARMED_PUNCH'),('activePrayers','java.util.Set<Prayer>','java.util.EnumSet.noneOf(Prayer.class)'),('weaponSpeed','int','4'),('soulreaperStacks','int','0'),('spellMaxHit','int','0')]
fields += [(n,'boolean','false') for n in ['onSlayerTask','inWilderness','chargeSpellActive','kandarinDiary','forinthrySurgeActive','markOfDarknessActive','usingSunfireRunes']]
fields += [(n,'String','null') for n in ['spellName','spellElement']]+[('spellbook','String','"Standard"')]
fields += [('rawEquipmentLoadout','com.loadoutlab.equipment.EquipmentLoadout','null'),('ammoApplicability','com.loadoutlab.equipment.AmmoApplicability','com.loadoutlab.equipment.AmmoApplicability.ALLOWED')]
extra=''.join('    public int getBoosted'+n.capitalize()+'() { return Math.max(0, '+n+'Level + '+n+'Boost); }\n' for n in ['attack','strength','defence','ranged','magic'])
extra+='''
    public int getWeaponId() { return equippedItemIds[3]; }
    public String getWeaponName() { return equippedItemNames[3]; }
    public String getWeaponVersion() { return equippedItemVersions[3]; }
    public String getWeaponCategory() { return equippedItemCategories[3]; }
    public int getAmmoId() { return equippedItemIds[13]; }
    public String getAmmoName() { return equippedItemNames[13]; }
    public String getCapeVersion() { return equippedItemVersions[1]; }
    public String getShieldVersion() { return equippedItemVersions[5]; }
    public boolean isWearing(String name) { for(String item:equippedItemNames) if(name!=null && name.equalsIgnoreCase(item)) return true; return false; }
    public boolean isWearingAny(String... names) { for(String name:names) if(isWearing(name)) return true; return false; }
    public boolean isWearingAll(String... names) { for(String name:names) if(!isWearing(name)) return false; return true; }
    public boolean isWearingItemContaining(String text) { if(text==null)return false; for(String item:equippedItemNames) if(item!=null && item.toLowerCase(java.util.Locale.ROOT).contains(text.toLowerCase(java.util.Locale.ROOT))) return true;return false; }
'''
dto('PlayerState',fields,extra,ctor='    public PlayerState() { java.util.Arrays.fill(equippedItemIds, -1); }\n')
fields=[(n,'int','0') for n in ['toaInvocationLevel','toaPathLevel','monsterCurrentHp','partySumMiningLevel']]+[(n,'int',v) for n,v in [('partyMaxCombatLevel','126'),('partyMaxHpLevel','99'),('partySize','1'),('demonbaneVulnerability','100')]]
fields += [('phase','String','null'),('fromCoxCm','boolean','false'),('defenceReductions','DefenceReductions','new DefenceReductions()')]
reductions=['dwh','bgs','arclight','emberlight','tonalztic','elderMaul','vulnerability','accursedSceptre','seercull','ayak']
extra='    public static class DefenceReductions {\n'
for n in reductions:
 cap=n[0].upper()+n[1:];extra+='        private int '+n+';\n        public int get'+cap+'() { return '+n+'; }\n        public void set'+cap+'(int value) { '+n+'=value; }\n'
extra+='        public DefenceReductions() {}\n        public DefenceReductions(DefenceReductions other) {\n'+''.join('            '+n+'=other.'+n+';\n' for n in reductions)+'        }\n    }\n'
ctor='    public MonsterInputs() {}\n    public MonsterInputs(MonsterInputs other) {\n'+''.join('        '+n+'='+('new DefenceReductions(other.defenceReductions)' if n=='defenceReductions' else 'other.'+n)+';\n' for n,t,v in fields)+'    }\n'
dto('MonsterInputs',fields,extra,ctor=ctor)
fields=[('id','int','-1'),('name','String','"Custom target"'),('version','String','""'),('sourcePage','String','""'),('size','int','1'),('speed','int','4'),('hitpoints','int','100')]
fields += [(n+'Level','int','1') for n in ['attack','strength','defence','ranged','magic']]
fields += [(n,'int','0') for n in ['stabDefence','slashDefence','crushDefence','magicDefence','lightRangedDefence','standardRangedDefence','heavyRangedDefence','flatArmour','offensiveMagic','weaknessSeverity']]
fields += [('attributes','java.util.Set<MonsterAttribute>','java.util.EnumSet.noneOf(MonsterAttribute.class)'),('weaknessElement','WeaknessElement','null'),('inputs','MonsterInputs','new MonsterInputs()')]
ctor='    public MonsterStats() {}\n    public MonsterStats(MonsterStats other) {\n'
ctor+=''.join('        '+n+'='+('new MonsterInputs(other.inputs)' if n=='inputs' else 'new java.util.HashSet<>(other.attributes)' if n=='attributes' else 'other.'+n)+';\n' for n,t,v in fields)+'    }\n'
extra='''
    public MonsterStats copy() { return new MonsterStats(this); }
    public void addAttribute(MonsterAttribute attribute) { attributes.add(attribute); }
    public boolean hasAttribute(MonsterAttribute attribute) { return attributes.contains(attribute); }
    public boolean usesDefenceForMagicDefence() { return name.equalsIgnoreCase("Verzik Vitur") || name.equalsIgnoreCase("Ice demon"); }
    public int getDefenceLevelForMagic() { return usesDefenceForMagicDefence() ? defenceLevel : magicLevel; }
    public int getDefenceForStyle(String key) { switch(key.toLowerCase(java.util.Locale.ROOT)) {case "stab":return stabDefence;case "slash":return slashDefence;case "crush":return crushDefence;case "magic":return magicDefence;case "ranged_light":case "light":return lightRangedDefence;case "ranged_heavy":case "heavy":return heavyRangedDefence;default:return standardRangedDefence;} }
    public boolean isToaMonster() { return java.util.Arrays.asList("Akkha","Ba-Ba","Kephri","Zebak","Elidinis' Warden","Tumeken's Warden","Warden core").contains(name); }
    public boolean isToaPathMonster() { return isToaMonster() && !name.contains("Warden") && !name.contains("core"); }
    public String[] getAvailablePhases() { return new String[0]; }
    @Override public String toString() { return name + (version==null || version.isEmpty() ? "" : " ("+version+")"); }
'''
dto('MonsterStats',fields,extra,ctor=ctor)
write('model','AttackType','''public enum AttackType {
    STAB, SLASH, CRUSH, RANGED_LIGHT, RANGED_STANDARD, RANGED_HEAVY, MAGIC;
    public String getKey() { return name().toLowerCase(java.util.Locale.ROOT); }
    public boolean isMelee() { return this==STAB || this==SLASH || this==CRUSH; }
    public boolean isRanged() { return name().startsWith("RANGED_"); }
    public boolean isMagic() { return this==MAGIC; }
}''')
write('model','MonsterAttribute','''public enum MonsterAttribute {
    DEMON, DRAGON, FIERY, FLYING, GOLEM, KALPHITE, LEAFY, PENANCE, RAT, SHADE, SPECTRAL, UNDEAD, VAMPYRE, VAMPYRE_1, VAMPYRE_2, VAMPYRE_3, XERICIAN;
    public String getJsonName() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static MonsterAttribute fromJson(String value) { return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT).replace(' ','_')); }
    public boolean isVampyre() { return name().startsWith("VAMPYRE"); }
}''')
write('model','WeaknessElement','''public enum WeaknessElement {
    AIR, WATER, EARTH, FIRE;
    public String getJsonName() { return name().toLowerCase(java.util.Locale.ROOT); }
    public static WeaknessElement fromJson(String value) { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
}''')
print('Generated independently implemented saved-state models.')
