package com.dpscalc;

import com.dpscalc.calc.DpsCalculator;
import com.dpscalc.calc.DpsResult;
import com.dpscalc.data.MonsterAttribute;
import com.dpscalc.data.MonsterConstants;
import com.dpscalc.data.MonsterStats;
import com.dpscalc.state.*;

import java.util.*;

public class DpsCalcTester {

    public static void main(String[] args) {
        DpsCalcTester tester = new DpsCalcTester();

        if (args.length > 0) {
            tester.runScenario(args[0]);
        } else {
            tester.runInteractive();
        }
    }

    private void runInteractive() {
        Scanner scanner = new Scanner(System.in);

        printHeader("DPS Calculator Tester");
        printScenarios();

        while (true) {
            System.out.print("\nEnter scenario number (or 'q' to quit): ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("q")) break;

            runScenario(input);
        }

        scanner.close();
    }

    private void printScenarios() {
        System.out.println("\nAvailable scenarios:");
        System.out.println("  1  - Basic melee (99 stats, no gear)");
        System.out.println("  2  - Basic ranged (99 stats, no gear)");
        System.out.println("  3  - Basic magic (Trident)");
        System.out.println("  4  - Mokhaoitl Burrowing (100% accuracy)");
        System.out.println("  5  - Mokhaoitl Normal (normal accuracy)");
        System.out.println("  6  - Mokhaoitl Shielded (immune)");
        System.out.println("  7  - Slayer helm on task");
        System.out.println("  8  - Salve amulet vs undead");
        System.out.println("  9  - Dragon hunter lance vs dragon");
        System.out.println("  10 - Twisted bow vs high magic");
        System.out.println("  11 - Scythe vs 3x3 monster");
        System.out.println("  12 - Fang vs ToA monster");
        System.out.println("  13 - Custom (enter your own values)");
        System.out.println("  all - Run all scenarios");
    }

    private void runScenario(String input) {
        switch (input.toLowerCase()) {
            case "1": basicMelee(); break;
            case "2": basicRanged(); break;
            case "3": basicMagic(); break;
            case "4": mokhaoitlBurrowing(); break;
            case "5": mokhaoitlNormal(); break;
            case "6": mokhaoitlShielded(); break;
            case "7": slayerHelmOnTask(); break;
            case "8": salveVsUndead(); break;
            case "9": dhLanceVsDragon(); break;
            case "10": twistedBowHighMagic(); break;
            case "11": scytheVsBigMonster(); break;
            case "12": fangVsToA(); break;
            case "13": customScenario(); break;
            case "all": runAll(); break;
            default: System.out.println("Unknown scenario: " + input);
        }
    }

    private void runAll() {
        basicMelee();
        basicRanged();
        basicMagic();
        mokhaoitlBurrowing();
        mokhaoitlNormal();
        mokhaoitlShielded();
        slayerHelmOnTask();
        salveVsUndead();
        dhLanceVsDragon();
        twistedBowHighMagic();
        scytheVsBigMonster();
        fangVsToA();
    }

    private void basicMelee() {
        printHeader("Basic Melee (99 stats, unarmed)");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Test Monster")
            .defenceLevel(100)
            .slashDefence(50)
            .hitpoints(200)
            .build();

        printResult(player, monster);
    }

    private void basicRanged() {
        printHeader("Basic Ranged (99 stats, +100 ranged bonus)");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .rangedLevel(99)
            .rangedAttack(100)
            .rangedStrength(100)
            .combatStyle(new CombatStyle("Rapid", AttackType.RANGED_STANDARD, "Rapid", 0, 0, 0, 0, 0))
            .weaponSpeed(3)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Test Monster")
            .defenceLevel(100)
            .standardRangedDefence(50)
            .hitpoints(200)
            .build();

        printResult(player, monster);
    }

    private void basicMagic() {
        printHeader("Basic Magic (Trident of the seas)");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .magicLevel(99)
            .magicAttack(100)
            .magicDamage(150)
            .weapon("Trident of the seas")
            .combatStyle(new CombatStyle("Accurate", AttackType.MAGIC, "Accurate", 0, 0, 0, 0, 0))
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Test Monster")
            .defenceLevel(100)
            .magicLevel(100)
            .magicDefence(50)
            .hitpoints(200)
            .build();

        printResult(player, monster);
    }

    private void mokhaoitlBurrowing() {
        printHeader("Doom of Mokhaoitl (Burrowing) - 100% Accuracy");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .id(MonsterConstants.DOOM_OF_MOKHAIOTL_IDS[0])
            .name("Doom of Mokhaoitl")
            .defenceLevel(140)
            .crushDefence(100)
            .hitpoints(1000)
            .build();
        monster.getInputs().setPhase("Burrowing");

        printResult(player, monster);
        System.out.println("  [Expected: 100% accuracy due to Burrowing phase]");
    }

    private void mokhaoitlNormal() {
        printHeader("Doom of Mokhaoitl (Normal) - Normal Accuracy");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .id(MonsterConstants.DOOM_OF_MOKHAIOTL_IDS[0])
            .name("Doom of Mokhaoitl")
            .defenceLevel(140)
            .crushDefence(100)
            .hitpoints(1000)
            .build();
        monster.getInputs().setPhase("Normal");

        printResult(player, monster);
        System.out.println("  [Expected: <100% accuracy, Normal phase has no special accuracy]");
    }

    private void mokhaoitlShielded() {
        printHeader("Doom of Mokhaoitl (Shielded) - Immune to non-demonbane");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .combatStyle(new CombatStyle("Kick", AttackType.CRUSH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .id(MonsterConstants.DOOM_OF_MOKHAIOTL_IDS[0])
            .name("Doom of Mokhaoitl")
            .defenceLevel(140)
            .crushDefence(100)
            .hitpoints(1000)
            .build();
        monster.getInputs().setPhase("Shielded");

        printResult(player, monster);
        System.out.println("  [Expected: 0 max hit - immune to non-demonbane]");
    }

    private void slayerHelmOnTask() {
        printHeader("Slayer Helm (i) on Task");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .slashAttack(150)
            .meleeStrength(120)
            .head("Slayer helmet (i)")
            .weapon("Abyssal whip")
            .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
            .weaponSpeed(4)
            .onSlayerTask(true)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Abyssal demon")
            .defenceLevel(135)
            .slashDefence(20)
            .hitpoints(150)
            .build();

        printResult(player, monster);

        player.setOnSlayerTask(false);
        DpsResult offTask = new DpsCalculator(player, monster).calculate();
        System.out.println("  [Off-task comparison: " + String.format("%.3f DPS", offTask.getDps()) + "]");
    }

    private void salveVsUndead() {
        printHeader("Salve Amulet (ei) vs Undead");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .slashAttack(150)
            .meleeStrength(120)
            .neck("Salve amulet(ei)")
            .weapon("Abyssal whip")
            .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Vorkath")
            .defenceLevel(214)
            .slashDefence(26)
            .hitpoints(750)
            .attribute(MonsterAttribute.UNDEAD)
            .build();

        printResult(player, monster);
    }

    private void dhLanceVsDragon() {
        printHeader("Dragon Hunter Lance vs Dragon");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .stabAttack(150)
            .meleeStrength(120)
            .weapon("Dragon hunter lance")
            .combatStyle(new CombatStyle("Lunge", AttackType.STAB, "Controlled", 1, 1, 1, 0, 0))
            .weaponSpeed(4)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Vorkath")
            .defenceLevel(214)
            .stabDefence(26)
            .hitpoints(750)
            .attribute(MonsterAttribute.DRAGON)
            .build();

        printResult(player, monster);
    }

    private void twistedBowHighMagic() {
        printHeader("Twisted Bow vs High Magic Monster");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .rangedLevel(99)
            .rangedAttack(70)
            .rangedStrength(20)
            .weapon("Twisted bow")
            .combatStyle(new CombatStyle("Rapid", AttackType.RANGED_STANDARD, "Rapid", 0, 0, 0, 0, 0))
            .weaponSpeed(5)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Commander Zilyana")
            .defenceLevel(300)
            .magicLevel(300)
            .standardRangedDefence(80)
            .hitpoints(500)
            .build();

        printResult(player, monster);
        System.out.println("  [Tbow scales with target magic level: " + monster.getMagicLevel() + "]");
    }

    private void scytheVsBigMonster() {
        printHeader("Scythe of Vitur vs 3x3 Monster");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .slashAttack(150)
            .meleeStrength(120)
            .weapon("Scythe of vitur")
            .combatStyle(new CombatStyle("Chop", AttackType.SLASH, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(5)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Verzik P3")
            .defenceLevel(200)
            .slashDefence(100)
            .hitpoints(2500)
            .size(3)
            .build();

        printResult(player, monster);
        System.out.println("  [Scythe hits 3 times on 3x3: 100%, 50%, 25% of max hit]");
    }

    private void fangVsToA() {
        printHeader("Osmumten's Fang vs ToA Monster");

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(99)
            .strengthLevel(99)
            .stabAttack(150)
            .meleeStrength(120)
            .weapon("Osmumten's fang")
            .combatStyle(new CombatStyle("Lunge", AttackType.STAB, "Aggressive", 0, 3, 0, 0, 0))
            .weaponSpeed(5)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .id(MonsterConstants.BABA_IDS[0])
            .name("Ba-Ba")
            .defenceLevel(150)
            .stabDefence(80)
            .hitpoints(400)
            .build();
        monster.getInputs().setToaInvocationLevel(300);

        printResult(player, monster);
        System.out.println("  [Fang has double accuracy roll inside ToA]");
    }

    private void customScenario() {
        Scanner scanner = new Scanner(System.in);
        printHeader("Custom Scenario");

        System.out.print("Attack level (default 99): ");
        int attack = readInt(scanner, 99);

        System.out.print("Strength level (default 99): ");
        int strength = readInt(scanner, 99);

        System.out.print("Attack bonus (default 100): ");
        int attackBonus = readInt(scanner, 100);

        System.out.print("Strength bonus (default 100): ");
        int strBonus = readInt(scanner, 100);

        System.out.print("Weapon speed in ticks (default 4): ");
        int speed = readInt(scanner, 4);

        System.out.print("Monster defence level (default 100): ");
        int monsterDef = readInt(scanner, 100);

        System.out.print("Monster defence bonus (default 50): ");
        int monsterDefBonus = readInt(scanner, 50);

        System.out.print("Monster HP (default 200): ");
        int monsterHp = readInt(scanner, 200);

        PlayerState player = new TestUtils.PlayerStateBuilder()
            .attackLevel(attack)
            .strengthLevel(strength)
            .slashAttack(attackBonus)
            .meleeStrength(strBonus)
            .combatStyle(CombatStyle.MELEE_ACCURATE_SLASH)
            .weaponSpeed(speed)
            .build();

        MonsterStats monster = new TestUtils.MonsterStatsBuilder()
            .name("Custom Monster")
            .defenceLevel(monsterDef)
            .slashDefence(monsterDefBonus)
            .hitpoints(monsterHp)
            .build();

        printResult(player, monster);
    }

    private int readInt(Scanner scanner, int defaultValue) {
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void printHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));
    }

    private void printResult(PlayerState player, MonsterStats monster) {
        DpsCalculator calc = new DpsCalculator(player, monster);
        DpsResult result = calc.calculate();

        System.out.println("\n  Player:");
        System.out.println("    Combat Style: " + player.getCombatStyle().getName() +
                          " (" + player.getCombatStyle().getAttackType() + ")");
        if (player.getWeaponName() != null) {
            System.out.println("    Weapon: " + player.getWeaponName());
        }
        System.out.println("    Attack Speed: " + result.getAttackSpeed() + " ticks (" +
                          String.format("%.1f", result.getAttackSpeed() * 0.6) + "s)");

        System.out.println("\n  Monster:");
        System.out.println("    Name: " + monster.getName());
        if (monster.getInputs().getPhase() != null) {
            System.out.println("    Phase: " + monster.getInputs().getPhase());
        }
        System.out.println("    HP: " + monster.getHitpoints());

        System.out.println("\n  Results:");
        System.out.println("    Max Hit:      " + result.getMaxHit());
        System.out.println("    Attack Roll:  " + String.format("%,d", result.getAttackRoll()));
        System.out.println("    Defence Roll: " + String.format("%,d", result.getDefenceRoll()));
        System.out.println("    Accuracy:     " + String.format("%.2f%%", result.getAccuracy() * 100));
        System.out.println("    DPS:          " + String.format("%.3f", result.getDps()));

        if (monster.getHitpoints() > 0 && result.getDps() > 0) {
            double ttk = monster.getHitpoints() / result.getDps();
            System.out.println("    Est. TTK:     " + String.format("%.1fs", ttk) +
                              " (" + String.format("%.0f", ttk / 0.6) + " ticks)");
        }
    }
}
