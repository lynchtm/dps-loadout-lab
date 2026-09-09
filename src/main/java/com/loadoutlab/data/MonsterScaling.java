// SPDX-License-Identifier: BSD-2-Clause
// Copyright (c) 2026, Tommy Lynch.
package com.loadoutlab.data;

import com.loadoutlab.model.*;

/** Explicit snapshot transforms; never mutates the selected target or catalog. */
public final class MonsterScaling {
    private MonsterScaling() {}

    public static MonsterStats scale(MonsterStats original) {
        MonsterStats m = original.copy();
        MonsterInputs input = m.getInputs();
        if (m.isToaMonster()) {
            double raid = 1 + input.getToaInvocationLevel() / 5 * 0.02;
            double party = 1 + Math.max(0, input.getPartySize() - 1) * 0.9;
            double path =
                    m.isToaPathMonster() && input.getToaPathLevel() > 0
                            ? 1.03 + 0.05 * input.getToaPathLevel()
                            : 1;
            m.setHitpoints((int) Math.floor(m.getHitpoints() * raid * party * path));
            m.setDefenceLevel((int) Math.floor(m.getDefenceLevel() * raid));
        }
        MonsterInputs.DefenceReductions r = input.getDefenceReductions();
        int baseDef = m.getDefenceLevel(),
                baseAtk = m.getAttackLevel(),
                baseStr = m.getStrengthLevel();
        int def = baseDef, magic = m.getMagicLevel();
        if (r.getVulnerability() > 0) def = Math.min(def, baseDef - baseDef / 10);
        if (r.getAccursedSceptre() > 0) {
            def = Math.min(def, baseDef - baseDef * 15 / 100);
            magic -= magic * 15 / 100;
        }
        for (int n = 0; n < r.getDwh(); n++) def -= def * 30 / 100;
        for (int n = 0; n < r.getElderMaul(); n++) def -= def * 35 / 100;
        boolean demon = m.hasAttribute(MonsterAttribute.DEMON);
        int arc = demon ? 10 : 5, ember = demon ? 15 : 5;
        def -=
                r.getArclight() * (baseDef * arc / 100 + 1)
                        + r.getEmberlight() * (baseDef * ember / 100 + 1);
        m.setAttackLevel(
                Math.max(
                        0,
                        baseAtk
                                - r.getArclight() * (baseAtk * arc / 100 + 1)
                                - r.getEmberlight() * (baseAtk * ember / 100 + 1)));
        m.setStrengthLevel(
                Math.max(
                        0,
                        baseStr
                                - r.getArclight() * (baseStr * arc / 100 + 1)
                                - r.getEmberlight() * (baseStr * ember / 100 + 1)));
        def -= r.getTonalztic() * (magic / 8);
        // BGS spill follows Defence, Strength, Prayer (NPCs have none), Attack, Magic, Ranged.
        int spill = Math.max(0, r.getBgs() - Math.max(0, def));
        def = Math.max(0, def - r.getBgs());
        int drain = Math.min(spill, m.getStrengthLevel());
        m.setStrengthLevel(m.getStrengthLevel() - drain);
        spill -= drain;
        drain = Math.min(spill, m.getAttackLevel());
        m.setAttackLevel(m.getAttackLevel() - drain);
        spill -= drain;
        drain = Math.min(spill, magic);
        magic -= drain;
        spill -= drain;
        m.setRangedLevel(Math.max(0, m.getRangedLevel() - spill));
        magic = Math.max(0, magic - r.getSeercull());
        if (r.getAyak() > 0)
            throw new IllegalArgumentException(
                    "Ayak drain is not yet supported; enter the reduced target stats directly");
        int floor = 0;
        if (m.isToaMonster()) {
            int cap =
                    m.getName().equals("Akkha") || m.getName().equals("Kephri")
                            ? 20
                            : m.getName().equals("Ba-Ba") || m.getName().equals("Zebak") ? 30 : 30;
            floor = Math.max(0, baseDef - cap);
        }
        m.setDefenceLevel(Math.max(floor, def));
        m.setMagicLevel(magic);
        return m;
    }
}
