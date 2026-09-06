package com.dpscalc.scenario;

import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.*;
import com.google.gson.*;
import java.util.*;

/** Versioned, portable scenario. Overrides are explicit leaf paths, never live-state mutations. */
public final class Scenario {
    // RuneLiteModule binds this shared instance as the client Gson; retain portable static model helpers.
    public static final Gson JSON = net.runelite.http.api.RuneLiteAPI.GSON.newBuilder().serializeNulls().setPrettyPrinting().create();
    public int schemaVersion = 1;
    public String name = "Comparison";
    public List<Loadout> loadouts = new ArrayList<>();
    public MonsterStats target;
    public Encounter encounter = new Encounter();
    public Map<String, MonsterStats> targets = new LinkedHashMap<>();
    public Map<String, String> targetSources = new LinkedHashMap<>();
    public int selected;

    public Scenario() { loadouts.add(new Loadout()); }

    public static final class Loadout {
        public String name = "Loadout 1";
        public String description = "";
        public PlayerState player = defaults();
        public Set<String> overrides = new LinkedHashSet<>();
        public Map<String, String> sources = new LinkedHashMap<>();
        public int[] inventory = new int[0];
        public CarryPlan carry = new CarryPlan();
        public String wikiSource = "";
        public String wikiRevision = "";
        public List<String> wikiNotes = new ArrayList<>();
        public List<int[]> wikiSeeds = new ArrayList<>();
        public boolean manualEquipmentStats;
        public int blowpipeDartId;
        public boolean specialAttack;
        public int specials = 1;
        public int specialEnergy = 100;
        public int specialCost = 50;
        public int rotationTicks = 100;
        public String boost = "None";
        public Set<String> potions = new LinkedHashSet<>();
        public int elapsedSeconds;
        public boolean divine;
        public boolean protectionPrayer;
        public String incomingStyle = "Unavailable";
        public int incomingMaxHit;
        public int incomingAttackRoll;
        public int incomingSpeed = 4;
        public double prayerDrainPerMinute;
        public Loadout copy() { return JSON.fromJson(JSON.toJson(this), Loadout.class); }
        @Override public String toString() { return name; }
    }

    public static PlayerState defaults() {
        PlayerState p = new PlayerState();
        p.setAttackLevel(99); p.setStrengthLevel(99); p.setDefenceLevel(99);
        p.setRangedLevel(99); p.setMagicLevel(99); p.setPrayerLevel(99);
        p.setHitpointsLevel(99); p.setCurrentHitpoints(99);
        p.setCombatStyle(CombatStyle.UNARMED_PUNCH); p.setEquipmentStats(new EquipmentStats());
        p.setWeaponSpeed(4); p.setKandarinDiary(false); p.setOnSlayerTask(true);
        Arrays.fill(p.getEquippedItemIds(), -1);
        return p;
    }

    public static PlayerState copy(PlayerState p) { return JSON.fromJson(JSON.toJson(p), PlayerState.class); }

    public static void mergeLive(Loadout l, PlayerState live) {
        JsonObject current = JSON.toJsonTree(l.player).getAsJsonObject();
        JsonObject next = JSON.toJsonTree(live).getAsJsonObject();
        Map<String, JsonElement> leaves = flatten(next);
        for (Map.Entry<String, JsonElement> field : leaves.entrySet()) {
            String path = field.getKey();
            if (!l.overrides.contains(path) && isLive(path)) {
                put(current, path, field.getValue());
                l.sources.put(path, "Live client");
            }
        }
        l.player = JSON.fromJson(current, PlayerState.class);
    }

    public static boolean isLive(String path) {
        return path.matches("(attack|strength|defence|ranged|magic|prayer|hitpoints)Level")
            || path.matches("(attack|strength|defence|ranged|magic)Boost")
            || path.equals("currentHitpoints") || path.startsWith("equippedItemIds.")
            || path.startsWith("equippedItemNames.") || path.equals("activePrayers")
            || path.startsWith("combatStyle.") || path.equals("inWilderness") || path.equals("soulreaperStacks") || path.equals("spellbook") || path.equals("kandarinDiary");
    }

    public static Map<String, JsonElement> flatten(JsonObject object) {
        Map<String, JsonElement> leaves = new LinkedHashMap<>();
        flatten("", object, leaves); return leaves;
    }
    private static void flatten(String path, JsonElement value, Map<String, JsonElement> out) {
        if (value.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : value.getAsJsonObject().entrySet())
                flatten(path.isEmpty() ? e.getKey() : path + "." + e.getKey(), e.getValue(), out);
        } else if (value.isJsonArray() && !path.equals("activePrayers") && !path.equals("attributes")) {
            for (int i = 0; i < value.getAsJsonArray().size(); i++) flatten(path + "." + i, value.getAsJsonArray().get(i), out);
        } else out.put(path, value);
    }
    public static void put(JsonObject root, String path, JsonElement value) {
        String[] parts = path.split("\\."); JsonElement parent = root;
        for (int i = 0; i < parts.length - 1; i++) parent = parent.isJsonArray()
            ? parent.getAsJsonArray().get(Integer.parseInt(parts[i])) : parent.getAsJsonObject().get(parts[i]);
        if (parent.isJsonArray()) parent.getAsJsonArray().set(Integer.parseInt(parts[parts.length - 1]), value);
        else parent.getAsJsonObject().add(parts[parts.length - 1], value);
    }

    public static Scenario parse(String text) {
        if (text == null || text.length() > 1_000_000) throw new IllegalArgumentException("Scenario exceeds 1 MB");
        int depth=0; boolean quoted=false,escape=false;
        for(char c:text.toCharArray()) {
            if(escape){escape=false;continue;} if(quoted&&c=='\\'){escape=true;continue;}
            if(c=='"'){quoted=!quoted;continue;}if(quoted)continue;
            if(c=='{'||c=='['){if(++depth>32)throw new IllegalArgumentException("Scenario nesting exceeds 32 levels");}
            if(c=='}'||c==']')depth--;
        }
        Scenario s;
        try { s = JSON.fromJson(text, Scenario.class); }
        catch (RuntimeException ex) { throw new IllegalArgumentException("Invalid scenario JSON", ex); }
        if (s == null || s.schemaVersion != 1) throw new IllegalArgumentException("Unsupported scenario version");
        if (s.loadouts == null || s.loadouts.isEmpty() || s.loadouts.size() > 32) throw new IllegalArgumentException("Use 1–32 loadouts");
        if (s.targets == null || s.targets.size() > 200 || s.targetSources == null) throw new IllegalArgumentException("Invalid target presets");
        if (s.selected < 0 || s.selected >= s.loadouts.size()) throw new IllegalArgumentException("Invalid selected loadout");
        for (Loadout l : s.loadouts) validate(l);
        if(s.encounter==null)throw new IllegalArgumentException("Missing encounter settings");s.encounter.validate();
        if (s.target != null) validate(s.target);
        for (MonsterStats m : s.targets.values()) validate(m);
        return s;
    }
    public static void validate(Loadout l) {
        if (l == null || l.player == null || l.overrides == null || l.sources == null || l.name == null || l.name.length() > 200)
            throw new IllegalArgumentException("Invalid loadout");
        if(l.carry==null||l.wikiSource==null||l.wikiSource.length()>1000||l.wikiRevision==null||l.wikiRevision.length()>100||l.wikiNotes==null||l.wikiNotes.size()>200||l.wikiSeeds==null||l.wikiSeeds.size()>16)throw new IllegalArgumentException("Invalid Wiki source or inventory plan");
        l.carry.validate();for(String note:l.wikiNotes)if(note==null||note.length()>4000)throw new IllegalArgumentException("Invalid Wiki note");
        for(int[] seed:l.wikiSeeds)if(seed==null||seed.length!=14)throw new IllegalArgumentException("Invalid Wiki seed");
        if(l.potions==null || !PotionSelection.OPTIONS.containsAll(l.potions))throw new IllegalArgumentException("Unknown potion selection");
        PlayerState p = l.player;
        if (p.getCombatStyle() == null || p.getCombatStyle().getAttackType() == null || p.getEquipmentStats() == null
            || p.getActivePrayers() == null || p.getActivePrayers().contains(null)) throw new IllegalArgumentException("Invalid style, prayer or equipment");
        for(java.util.function.ToIntFunction<Prayer> bonus:java.util.Arrays.<java.util.function.ToIntFunction<Prayer>>asList(Prayer::getAttackBonus,Prayer::getStrengthBonus,Prayer::getRangedBonus,Prayer::getMagicBonus,Prayer::getDefenceBonus))
            if(p.getActivePrayers().stream().filter(prayer->bonus.applyAsInt(prayer)>0).count()>1)throw new IllegalArgumentException("Conflicting combat prayers; select compatible prayers");
        if (p.getEquippedItemIds() == null || p.getEquippedItemIds().length != 14 || p.getEquippedItemNames() == null || p.getEquippedItemNames().length != 14)
            throw new IllegalArgumentException("Equipment requires 14 slots");
        if(p.getEquippedItemVersions()==null||p.getEquippedItemVersions().length!=14||p.getEquippedItemCategories()==null||p.getEquippedItemCategories().length!=14)
            throw new IllegalArgumentException("Equipment metadata requires 14 slots");
        if(l.inventory==null||l.inventory.length>1000)throw new IllegalArgumentException("Invalid inventory");
        if(p.getSpellMaxHit()<0||p.getSpellMaxHit()>1000)throw new IllegalArgumentException("Spell max hit must be 0–1000");
        for(Map.Entry<String,JsonElement> e:flatten(JSON.toJsonTree(p.getEquipmentStats()).getAsJsonObject()).entrySet()) {
            double n=e.getValue().getAsDouble();if(!Double.isFinite(n)||n < -1000||n>10000)throw new IllegalArgumentException("Equipment bonus out of range: "+e.getKey());
        }
        Map<String,JsonElement> paths=flatten(JSON.toJsonTree(p).getAsJsonObject());
        for(String path:l.overrides)if(!paths.containsKey(path))throw new IllegalArgumentException("Unknown override: "+path);
        for (int v : new int[]{p.getAttackLevel(),p.getStrengthLevel(),p.getDefenceLevel(),p.getRangedLevel(),p.getMagicLevel(),p.getHitpointsLevel(),p.getPrayerLevel()})
            if (v < 1 || v > 126) throw new IllegalArgumentException("Base levels must be 1–126");
        for (int v : new int[]{p.getBoostedAttack(),p.getBoostedStrength(),p.getBoostedDefence(),p.getBoostedRanged(),p.getBoostedMagic()})
            if (v < 0 || v > 255) throw new IllegalArgumentException("Boosted levels must be 0–255");
        if (l.rotationTicks < 1 || l.rotationTicks > 10000 || l.specials < 0 || l.specials > 100 || l.specialCost < 1 || l.specialCost > 100
            || l.specialEnergy < 0 || l.specialEnergy > 100 || l.elapsedSeconds < 0 || l.elapsedSeconds > 86400)
            throw new IllegalArgumentException("Invalid rotation, energy or elapsed time");
        if (p.getCurrentHitpoints() < 0 || p.getCurrentHitpoints() > 255 || p.getWeaponSpeed() < 1 || p.getWeaponSpeed() > 20)
            throw new IllegalArgumentException("Invalid HP or weapon speed");
        if (l.incomingSpeed < 1 || l.incomingSpeed > 20 || l.incomingMaxHit < 0 || l.incomingMaxHit > 1000 || l.incomingAttackRoll < 0
            || !Double.isFinite(l.prayerDrainPerMinute) || l.prayerDrainPerMinute < 0) throw new IllegalArgumentException("Invalid incoming attack or prayer drain");
    }
    public static void validate(MonsterStats m) {
        if (m == null || m.getName() == null || m.getInputs() == null || m.getAttributes() == null || m.getAttributes().contains(null)
            || m.getInputs().getDefenceReductions() == null) throw new IllegalArgumentException("Invalid target");
        if (m.getHitpoints() < 1 || m.getHitpoints() > 100000 || m.getSize() < 1 || m.getSize() > 10)
            throw new IllegalArgumentException("Target HP must be 1–100000 and size 1–10");
        for (int v : new int[]{m.getAttackLevel(),m.getStrengthLevel(),m.getDefenceLevel(),m.getMagicLevel(),m.getRangedLevel()})
            if (v < 0 || v > 10000) throw new IllegalArgumentException("Target levels must be 0–10000");
    }
}
