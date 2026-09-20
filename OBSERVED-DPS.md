# Observed DPS

Enable **Show observed DPS** in DPS Loadout Lab's RuneLite settings. Optionally enable
**Kill summary in chat**, or use **Toggle kill summaries** in the overlay's right-click
menu. Chat summaries appear locally; nothing is sent to other players. Either option
enables recording. Disabling both, disabling the plugin, logout, hopping, a character
change or **Reset observed DPS** clears the tracker. Calculator selections, the NPC
**Calculate DPS** action and comparison loadouts do not change the observed fight.

## Measurement

`DPS = your recorded boss and relevant add damage / ((end tick - first hit tick) × 0.6)`

- The first player-owned hitsplat starts recording, including zero-damage hits. An
  add can start a supported encounter only when its live boss is visible nearby.
- Damage is taken from RuneLite's `Hitsplat.isMine()` events on encounter members.
  Other players' hits and unrelated NPCs do not contribute. Unattributed damage
  (including damage-over-time or indirect sources without a player-owned hitsplat)
  cannot be inferred and is not credited. Special hitsplats may represent mechanics
  such as shields; this is observed hitsplat damage, not a reconstructed HP ledger.
- There is one active observation. Explicit boss definitions join forms, paired
  bosses and listed adds. A new NPC object during a supported phase does not reset
  the timer. Switching an ordinary NPC target starts a fresh NPC observation.
- Eating, movement, missed attacks and transitions remain part of the fight. There
  is no subtraction of gaps between hits and no extension beyond the completion tick.
- First-hit timing omits the initial attack windup/travel. Starting late measures only
  the observed portion. It is not synchronized to the game's official kill-duration
  message. Very short fights have greater timing bias; a same-tick kill shows **—**
  in the overlay and **N/A (too short to measure)** in chat.
- Results use nominal 0.6-second game ticks, not wall-clock/server-lag compensation.
- A confirmed completion freezes damage and elapsed time. Killing hitsplats delivered
  later in the same tick are included before the single summary is emitted.
- Death, connection loss, leaving the local encounter, or a lost target interrupts
  recording. A supported boss can disappear for up to 30 seconds for a phase change.
  Two minutes without one of your qualifying hits abandons a stale observation.
  These are interruption guards, never reasons to claim a kill or shorten a completed
  fight. Known reset forms are handled separately (currently Vorkath returning to sleep).
- Completed/interrupted results remain visible for **Target timeout** seconds, or
  until replaced/reset when **Keep overlay visible** is enabled. An interrupted result
  shows its stop reason and no numeric DPS or successful-kill chat message.

## Explicit encounter definitions

These definitions target regular post-quest encounters. They are implemented and
covered by synthetic event tests; in-game confirmation is still required.

| Encounter | Grouped damage / completion |
| --- | --- |
| Vorkath | Boss and zombified spawn; boss death ends the encounter. |
| Zulrah | All three forms and snakelings; boss death, not a submerge/despawn. |
| Alchemical Hydra | All forms; final phase death/dead form only. |
| Kalphite Queen | Both forms; flying form death only. |
| Vet'ion | Both phases and skeletal hellhounds; second phase death only. |
| Calvar'ion | Its phases and hellhounds, separately from Vet'ion. |
| Grotesque Guardians | Dawn and Dusk as boss damage; final Dusk death/dead form only. |
| Hespori | Boss and flowers; Hespori's death only. |
| Abyssal Sire | Boss forms, respiratory systems, spawns/scions; final boss phase death only. |
| Nex | Boss forms, four mages and encounter blood reavers; boss death/dead form only. |
| Phantom Muspah | Boss forms, shield and teleport phases; final phase death only. |
| Sarachnis | Boss and its spider adds; boss death only. |
| General Graardor | Boss and three bodyguards; boss death only. |
| Commander Zilyana | Boss and three bodyguards; boss death only. |
| Kree'arra | Boss and three bodyguards; boss death only. |
| K'ril Tsutsaroth | Boss and three bodyguards; boss death only. |

Bodyguard cleanup after the boss's death does not extend that encounter. Adds alone
cannot start another observation while the boss is absent/dead. NPC membership uses
explicit RuneLite IDs and a local world-view/distance boundary, not matching names.

Unlisted NPCs use the **Observed NPC** fallback. That is an individual NPC estimate,
not a claim of complete boss/raid support. In particular, raids, Nightmare, Royal
Titans and other unlisted encounters do not yet have full encounter definitions.
Quest/league variants are not silently combined with regular bosses. Multiple
simultaneous ordinary targets (for example a barrage stack) are not aggregated.

## In-game checks

1. Enable both observed options. Kill an ordinary NPC, then wait before the next
   kill. Check that each result starts fresh and the completed number never decays.
2. At a supported boss, switch to a listed add and back, and change a calculator
   comparison using **Calculate DPS**. The encounter totals should remain intact;
   the existing menu action should still select/update your current-player comparison.
3. Test Kalphite Queen/Vet'ion/Hydra phases or both Guardians: only final completion
   should emit a summary. Compare the damage breakdown with your own hitsplats.
4. Eat or move during a fight: the elapsed time should continue. Wait after the kill:
   it should stop. Enable **Keep overlay visible** to inspect the frozen result.
5. Teleport out, die, disconnect or hop during an unfinished fight. No successful-kill
   summary should appear. A subsequent fight should contain only its own damage.
6. Turn chat off and complete another fight. Confirm silence. Hide the overlay and
   turn chat on: summaries should still work. Disable both and re-enable to verify reset.

Record the boss, phase and observed behavior for any discrepancy. Automated replays
exercise event ordering and state transitions, but cannot prove live encounter coverage.

## Implementation sources

The tracker and tests are newly authored. Public RuneLite contracts and factual IDs
were checked against the September 20, 2026 API:

- [NPC IDs](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/gameval/NpcID.java)
- [Hitsplat attribution](https://github.com/runelite/runelite/blob/master/runelite-api/src/main/java/net/runelite/api/Hitsplat.java)
- [NPC death utility and transformation exceptions](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/game/NpcUtil.java)

Existing public encounter trackers were reviewed for design tradeoffs during research;
their implementation source was not copied into this plugin.
