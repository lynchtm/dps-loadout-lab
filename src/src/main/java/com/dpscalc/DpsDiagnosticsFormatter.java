package com.dpscalc;

import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterInputs;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.CombatStyle;
import com.dpscalc.state.EquipmentSlot;
import com.dpscalc.state.EquipmentStats;
import com.dpscalc.state.PlayerState;

import java.util.stream.Collectors;

public final class DpsDiagnosticsFormatter {

    private DpsDiagnosticsFormatter() {}

    public static Snapshot snapshot() {
        return new Snapshot();
    }

    public static String format(Snapshot snapshot) {
        Snapshot safeSnapshot = snapshot == null ? snapshot() : snapshot;
        StringBuilder builder = new StringBuilder(2048);
        appendTarget(builder, safeSnapshot.target);
        appendMonster(builder, safeSnapshot.monster);
        appendPlayer(builder, safeSnapshot.player);
        appendSettings(builder, safeSnapshot.settings);
        appendResults(builder, safeSnapshot.panelResult, safeSnapshot.liveResult, safeSnapshot.specResult);
        return builder.toString().trim();
    }

    private static void appendTarget(StringBuilder builder, TargetInfo target) {
        line(builder, "== Target ==");
        if (target == null) {
            line(builder, "NPC: - | - | idx - | none");
            return;
        }
        line(builder, "NPC: %s | %s | idx %s | %s", id(target.id), value(target.name), id(target.index), value(target.source));
    }

    private static void appendMonster(StringBuilder builder, MonsterStats monster) {
        line(builder, "");
        line(builder, "== MonsterStats ==");
        if (monster == null) {
            line(builder, "-");
            return;
        }
        line(builder, "id/name/version: %d | %s | %s", monster.getId(), value(monster.getName()), value(monster.getVersion()));
        line(builder, "size/speed: %d | %d", monster.getSize(), monster.getSpeed());
        line(builder, "levels: atk %d | str %d | def %d | hp %d | mage %d | range %d",
            monster.getAttackLevel(), monster.getStrengthLevel(), monster.getDefenceLevel(), monster.getHitpoints(),
            monster.getMagicLevel(), monster.getRangedLevel());
        line(builder, "def: stab %d | slash %d | crush %d | magic %d | light %d | std %d | heavy %d | flat %d",
            monster.getStabDefence(), monster.getSlashDefence(), monster.getCrushDefence(), monster.getMagicDefence(),
            monster.getLightRangedDefence(), monster.getStandardRangedDefence(), monster.getHeavyRangedDefence(), monster.getFlatArmour());
        line(builder, "weakness: %s %d", monster.getWeaknessElement() == null ? "-" : monster.getWeaknessElement().getJsonName(), monster.getWeaknessSeverity());
        line(builder, "attributes: %s", monster.getAttributes() == null || monster.getAttributes().isEmpty()
            ? "-"
            : monster.getAttributes().stream().map(MonsterAttribute::name).sorted().collect(Collectors.joining(", ")));
        appendInputs(builder, monster.getInputs());
    }

    private static void appendInputs(StringBuilder builder, MonsterInputs inputs) {
        line(builder, "");
        line(builder, "== MonsterInputs ==");
        if (inputs == null) {
            line(builder, "-");
            return;
        }
        line(builder, "toa: inv %d | path %d", inputs.getToaInvocationLevel(), inputs.getToaPathLevel());
        line(builder, "cox CM: %s | party combat %d | mining %d | hp %d | size %d",
            inputs.isFromCoxCm(), inputs.getPartyMaxCombatLevel(), inputs.getPartySumMiningLevel(), inputs.getPartyMaxHpLevel(), inputs.getPartySize());
        line(builder, "hp/phase/demonbane: %d | %s | %d", inputs.getMonsterCurrentHp(), value(inputs.getPhase()), inputs.getDemonbaneVulnerability());
        MonsterInputs.DefenceReductions reductions = inputs.getDefenceReductions();
        if (reductions == null) {
            line(builder, "reductions: -");
            return;
        }
        line(builder, "reductions: dwh %d | bgs %d | arclight %d | emberlight %d | tonalztic %d | elder maul %d | vuln %d | accursed %d | seercull %d | ayak %d",
            reductions.getDwh(), reductions.getBgs(), reductions.getArclight(), reductions.getEmberlight(), reductions.getTonalztic(),
            reductions.getElderMaul(), reductions.getVulnerability(), reductions.getAccursedSceptre(), reductions.getSeercull(), reductions.getAyak());
    }

    private static void appendPlayer(StringBuilder builder, PlayerState player) {
        line(builder, "");
        line(builder, "== PlayerState ==");
        if (player == null) {
            line(builder, "-");
            return;
        }
        line(builder, "levels: atk %d%+d | str %d%+d | def %d%+d | range %d%+d | mage %d%+d | hp %d/%d",
            player.getAttackLevel(), player.getAttackBoost(), player.getStrengthLevel(), player.getStrengthBoost(),
            player.getDefenceLevel(), player.getDefenceBoost(), player.getRangedLevel(), player.getRangedBoost(),
            player.getMagicLevel(), player.getMagicBoost(), player.getHitpointsLevel(), player.getCurrentHitpoints());
        appendEquipment(builder, player);
        appendCombat(builder, player);
    }

    private static void appendEquipment(StringBuilder builder, PlayerState player) {
        line(builder, "");
        line(builder, "== Equipment ==");
        int[] ids = player.getEquippedItemIds();
        String[] names = player.getEquippedItemNames();
        String[] versions = player.getEquippedItemVersions();
        String[] categories = player.getEquippedItemCategories();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            int index = slot.getIndex();
            line(builder, "%s: %s | %s | %s | %s", slot.name(), itemId(ids, index), item(names, index), item(versions, index), item(categories, index));
        }
        EquipmentStats stats = player.getEquipmentStats();
        if (stats == null) {
            line(builder, "stats: -");
            return;
        }
        line(builder, "attack: stab %d | slash %d | crush %d | magic %d | range %d",
            stats.getStabAttack(), stats.getSlashAttack(), stats.getCrushAttack(), stats.getMagicAttack(), stats.getRangedAttack());
        line(builder, "strength: melee %d | range %d | magic %d%%", stats.getMeleeStrength(), stats.getRangedStrength(), stats.getMagicDamage());
        line(builder, "defence: stab %d | slash %d | crush %d | magic %d | range %d | prayer %d",
            stats.getStabDefence(), stats.getSlashDefence(), stats.getCrushDefence(), stats.getMagicDefence(), stats.getRangedDefence(), stats.getPrayerBonus());
    }

    private static void appendCombat(StringBuilder builder, PlayerState player) {
        line(builder, "");
        line(builder, "== Combat ==");
        CombatStyle style = player.getCombatStyle();
        if (style == null) {
            line(builder, "style: - | - | - | weapon speed %d", player.getWeaponSpeed());
        } else {
            line(builder, "style: %s | %s | %s | weapon speed %d", style.getName(), style.getAttackType(), style.getStance(), player.getWeaponSpeed());
        }
        line(builder, "prayers: %s", player.getActivePrayers() == null || player.getActivePrayers().isEmpty()
            ? "-"
            : player.getActivePrayers().stream().map(Enum::name).sorted().collect(Collectors.joining(", ")));
        line(builder, "buffs: slayer %s | wild %s | charge %s | kandarin %s | forinthry %s | soulreaper %d | mark %s | sunfire %s",
            player.isOnSlayerTask(), player.isInWilderness(), player.isChargeSpellActive(), player.isKandarinDiary(),
            player.isForinthrySurgeActive(), player.getSoulreaperStacks(), player.isMarkOfDarknessActive(), player.isUsingSunfireRunes());
        line(builder, "spell: %s | %s | %s | max %d", value(player.getSpellName()), value(player.getSpellbook()), value(player.getSpellElement()), player.getSpellMaxHit());
    }

    private static void appendSettings(StringBuilder builder, SettingsInfo settings) {
        line(builder, "");
        line(builder, "== Settings ==");
        if (settings == null) {
            line(builder, "settings: -");
            return;
        }
        line(builder, "settings: best prayer %s | max boosts %s | slayer %s | charge %s",
            settings.useBestPrayer, settings.assumeMaxBoosts, settings.onSlayerTask, settings.chargeSpell);
    }

    private static void appendResults(StringBuilder builder, DpsResult panelResult, DpsResult liveResult, DpsResult specResult) {
        line(builder, "");
        line(builder, "== Results ==");
        line(builder, "panel: %s", result(panelResult));
        line(builder, "cached live: %s", result(liveResult));
        line(builder, "cached spec: %s", result(specResult));
    }

    private static String result(DpsResult result) {
        if (result == null) {
            return "-";
        }
        return String.format("dps %.2f | max %d | acc %.1f%% | atk %d | def %d | speed %d | avg %.1f | dpt %.2f | hp %d | hits %s | ttk %s | kph %s",
            result.getDps(), result.getMaxHit(), result.getAccuracy() * 100, result.getAttackRoll(), result.getDefenceRoll(), result.getAttackSpeed(),
            result.getAverageHit(), result.getDamagePerTick(), result.getMonsterHp(), result.getFormattedExpectedHits(), result.getFormattedTimeToKill(), result.getFormattedKillsPerHour());
    }

    private static String item(String[] items, int index) {
        if (items == null || index < 0 || index >= items.length) {
            return "-";
        }
        return value(items[index]);
    }

    private static String itemId(int[] ids, int index) {
        if (ids == null || index < 0 || index >= ids.length || ids[index] <= 0) {
            return "-";
        }
        return String.valueOf(ids[index]);
    }

    private static String id(int id) {
        return id < 0 ? "-" : String.valueOf(id);
    }

    private static String value(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

    private static void line(StringBuilder builder, String format, Object... args) {
        builder.append(args.length == 0 ? format : String.format(format, args)).append('\n');
    }

    public static final class Snapshot {
        private TargetInfo target;
        private MonsterStats monster;
        private PlayerState player;
        private SettingsInfo settings;
        private DpsResult panelResult;
        private DpsResult liveResult;
        private DpsResult specResult;

        private Snapshot() {}

        public Snapshot withTarget(TargetInfo target) { this.target = target; return this; }
        public Snapshot withMonster(MonsterStats monster) { this.monster = monster; return this; }
        public Snapshot withPlayer(PlayerState player) { this.player = player; return this; }
        public Snapshot withSettings(SettingsInfo settings) { this.settings = settings; return this; }
        public Snapshot withPanelResult(DpsResult panelResult) { this.panelResult = panelResult; return this; }
        public Snapshot withLiveResult(DpsResult liveResult) { this.liveResult = liveResult; return this; }
        public Snapshot withSpecResult(DpsResult specResult) { this.specResult = specResult; return this; }
    }

    public static final class TargetInfo {
        private final int id;
        private final String name;
        private final int index;
        private final String source;

        public TargetInfo(int id, String name, int index, String source) {
            this.id = id;
            this.name = name;
            this.index = index;
            this.source = source;
        }
    }

    public static final class SettingsInfo {
        private final boolean useBestPrayer;
        private final boolean assumeMaxBoosts;
        private final boolean onSlayerTask;
        private final boolean chargeSpell;

        public SettingsInfo(boolean useBestPrayer, boolean assumeMaxBoosts, boolean onSlayerTask, boolean chargeSpell) {
            this.useBestPrayer = useBestPrayer;
            this.assumeMaxBoosts = assumeMaxBoosts;
            this.onSlayerTask = onSlayerTask;
            this.chargeSpell = chargeSpell;
        }
    }
}
