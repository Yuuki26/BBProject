# Battleship (roguelike)

A Swing battleship game. A run is a chain of stages against progressively larger enemy
fleets: clear one, pick an upgrade, and face a bigger one. Losing your whole fleet ends the
run.

## Running it

```
run.bat
```

Or from an IDE: open the `BB` folder as the project and run `com.bb.Main`. **No classpath
setup needed** — just Run.

Art loads through `com.bb.Assets.getResource(...)`, not a raw `getClass().getResource(...)`.
It checks the classpath first, and if that comes up empty (which it will for a plain "Run"
with no project configuration, since `lib` holds images, not classes, and nothing puts it on
the classpath automatically) it searches the filesystem instead: starting from the working
directory and from wherever the running code itself lives, walking upward a few levels from
each looking for a sibling `lib`. Between those two starting points, that covers both "IDE
default working directory = project root" (true of VS Code, IntelliJ and Eclipse out of the
box) and "run from somewhere unrelated entirely" (the code's own location still finds its way
back). If art still doesn't load, the error printed to stderr names the working directory it
searched from — open an issue with that line if it happens.

## Checking it still works

```
selftest.bat
```

`Test.SelfTest` builds the real window, drives the run logic, and prints a PASS/FAIL line per
behaviour. It exits non-zero if anything regressed.

## Controls

| Input | Action |
|---|---|
| Drag a ship from the roster onto the board | Deploy it |
| Drag a deployed ship | Move it |
| Drag a ship off the board and release | Send it back to port |
| **R** while dragging | Rotate the ship in your hand |
| **F** | Surface / dive the submarine under the cursor |
| Hover one of your ships | Its name, detection, size, cost and a small hull bar |
| Click one of your ships (during a battle) | Open its stats board; click it again, click water or press **Esc** to close |
| Right-click one of your ships (any time) | Open its stats board, on the board or in port |
| Auto-deploy | Spend the stage budget on the heaviest hulls that fit, at random |
| **Start** | Check the fleet against the budget, lock it in, go to the enemy board |
| **E** | Show your board |
| **Q** | Show the enemy board (once the battle has started) |
| Fire, then click tiles, then Confirm | Resolve a salvo |
| Click a yellow (crippled) ship's hit tile | Fire on it again, at half damage |

### Start

Each stage opens with deployment. The bar under the board reads **Your Board · Start ·
Pause**, and there is no way onto the enemy board yet — the Enemy Board button is not there,
and **Q** only reminds you to press Start.

Start refuses, with the reason on the status bar, if nothing is deployed or if the fleet on
the board costs more than the stage's budget. (Placement never lets you overspend, so the cost
half only trips on a fleet that arrived some other way, such as an old save — but Start is the
last point where it can be caught, so it checks.) Otherwise it locks the fleet in and takes
you to the enemy board, and the Start button becomes **Enemy Board (Q)** until the stage ends.

Locked in means locked in: until the stage is won, ships cannot be picked up, moved, rotated,
sent back to port or auto-deployed. Otherwise a hit ship could be dragged out of the line of
fire, or sent to port and brought back out at full hull. Surfacing and diving still work —
that is a battle action. The next stage reopens deployment and brings Start back.

Saves remember whether Start had been pressed. A save written before this existed is treated
as mid-battle if any shots had been fired, and as still deploying if none had.

**Rotation happens while you are dragging.** Pick a ship up, move it over the square you
want, press **R** until it points the right way, and release. The green/red preview updates
as you turn it, so you are deciding the orientation while looking at where the hull will
actually land. Ships on the board can be picked up and moved or rotated as often as you like.

Dragging is implemented by hand rather than with Swing's drag-and-drop, because Swing's DnD
runs a native modal loop that never delivers a keystroke — not even to a global
`AWTEventListener` — which made rotating mid-drag impossible.

### How your board looks

Your ships are drawn as **their own pictures**, across see-through tiles, turned upright
when a ship lies vertically. Tiles are not coloured for hits or detection any more — a
ship's condition is on its tooltip and stats board (below). Only two states show on the
picture itself:

- a **sunk** ship becomes a grey wreck, half see-through and listing, whole — not just the
  sections that were hit;
- a **submerged** submarine is drawn faint, as it is under water; surfaced, it is solid.

Open water is white, and dark grey where the enemy has fired and missed. While you drag a
ship, its picture is previewed faintly over the green (fits) or red (doesn't) landing tiles.
The ship whose stats board is open, or the one you last clicked, has a gold outline.

### Ship stats

Hovering one of your ships shows a tooltip with its name, detection, size and cost, and a
small **hull bar**: green above half, yellow at half or below, red at a quarter or below.

Clicking a ship opens its **stats board** beside it: the ship's picture, a full hull bar with
the numbers on it, where it lies and how many of its sections have been hit, whether it is
crippled or sunk, and its shields, damage, penetration, shots, detection and size. Shields
and damage include your skills, marked with the bonus (e.g. `144 (+60%)` with Ablative
Plating); a submarine adds its oxygen and whether it is submerged. The board stays up to date
while it is open (a repair, a dive), and closes when you click it, click the ship again, click
open water, press **Esc**, or leave your board.

During deployment a left click picks the ship up as before, so there the board is on the
**right** mouse button; right-click works in battle too, and on ships in port.

## How a run works

1. You are dealt a **starter fleet** of three ships, each costing no more than your fleet cost
   of 10. Each one can be rerolled once. There are no starting skills.
2. Deploy what fits and press **Start**, then trade salvos with the enemy.
3. Clear the enemy fleet: you earn **gold**, then choose one of three upgrades (an extra shot,
   a free repair, or a skill), drawn by weight. Each reward screen has one free reroll.
4. Before stages **4, 7, 10, …** the **shop** opens — the only place to get more ships.
5. The next stage raises the fleet cost and fields a larger enemy fleet. Damage carries over
   unless you repair it, so attrition is real.
6. Lose every ship on the board and the run ends, with the stage you reached and your score.

Heavier enemy hulls unlock as the run goes on - see Hulls below.

### Firing again on a crippled ship

A ship at or below half its hull turns **yellow**, and its whole hull shows. From then on its
tiles can be targeted even if they were hit before — but a tile you already hit only takes
**half damage**. Tiles of it you have not hit yet still take full damage.

The same goes for a ship whose every tile has been hit while it is still above half (a tough
hull shrugging off weak guns). There is nowhere left to find it, and without this it could
never be sunk. A hit on a healthy ship that still has hidden sections is refused, with a
reminder to go and find the rest of it.

**The enemy plays by the same rule.** Each turn your board tells the opponent which of its
own earlier hits it may fire on again - hits on a ship of yours that is crippled or has had
every tile hit - and those shots do half damage. It is never told about tiles it has not hit.
The Standard opponent mixes them into its random picks; the Elite one takes a shot that
extends a line of hits first, then a sure half-damage re-fire, then anything else. So a
battered ship of yours can be finished off, where before it became untouchable once every
tile had been hit.

Targets are previewed with a **crosshair and frame**: red for a full-damage shot, amber with
"1/2" for a half-damage one. The tile's own colour stays visible underneath, so a targeted
yellow tile still reads as yellow.

### Starter fleet

A new run deals three different ships, like reward cards, and those three are your starter
fleet. A run starts with **no skills**; every skill is earned on a reward screen.

- **Each** ship costs at most your fleet cost (10 at the start), so any one of them can be
  deployed. Together they can cost more — 8 + 4 + 6 is fine — and then you deploy what fits
  and the rest wait in port. A ship dearer than 10 on its own is never dealt; it can turn up
  in the shop later.
- Each ship can be **rerolled once**, into another ship of its hull class (see Rerolls below).

The roster (`PlayerFleet`) starts empty and holds only what you were dealt and what you buy.

### Gold and the shop

Clearing a stage pays `30 + 10 × stage` gold — 40, 50, 60 for the first three, so the first
shop opens with 150. The shop opens before stage 4 and every third stage after (7, 10, 13 …),
right after the reward. It sells:

| Item | Price | Notes |
|---|---|---|
| Ships (4 per visit, random) | cost × 12 | Join the roster undeployed. Only ships that fit in your fleet cost are offered. |
| Expand your fleet (1 per visit) | +2 → 30, +4 → 60, +6 → 90, +10 → 150 | Size rolled per visit: Uncommon +2 (50%), Rare +4 (30%), Epic +6 (15%), Legendary +10 (5%). Permanent. |
| Sell a ship | +60% of its price | Deployed or in port. You can't sell your last ship. |
| Repair a ship | up to half its price | Proportional to the damage: half-damaged costs a quarter of its price. |
| Repair all | sum of the above | Covers ships in port too. |
| Reroll an offer | 5, then 10, 15, 20 … | The price climbs with each reroll and resets next visit. See Rerolls below. |

Bought fleet cost raises **your** budget only. The enemy is still bought with the base
budget for the stage; otherwise every expansion would grow the enemy too and buy nothing.

**Damage stays with a ship in port.** Sending a damaged ship back to the roster no longer
wipes its damage, so dragging it to port and back is not a free repair — paying the shop is.

Tuning is all constants: `RunState.CURRENCY_BASE` / `CURRENCY_PER_STAGE`, and in `Shop`:
`PRICE_PER_COST`, `SELL_BACK_PERCENT`, `EXPANSION_PRICE_PER_COST`, `REPAIR_SHARE`,
`SHIPS_ON_SALE`, `FIRST_STAGE`, `EVERY`, and the `Expansion` table.

### Rerolls

The starter fleet, the reward screen and the shop all have rerolls. A reroll never changes
what **kind** of offer a slot is: a light cruiser rerolls into a light cruiser, and an
uncommon skill into an uncommon skill.

| Where | Price | How many | Rolls into | Can it land on something already seen? |
|---|---|---|---|---|
| Starter fleet | free | once per ship | another ship of the same hull class that fits your fleet cost | No: never a ship this screen has shown, including ones rerolled away |
| Reward screen | free | once per screen, on any card | another card of the same kind and rarity | No: never a card this screen has shown. The next screen starts fresh, so a skill rerolled away can come up again there |
| Shop: a ship | 5, then 10, 15, 20 … per visit | as many as you can pay for | another ship of the same hull class that fits your fleet cost | Yes: rerolling Helena can give Helena again, or a ship another slot already shows |
| Shop: Expand your fleet | the same climbing price | as many as you can pay for | its size rolled again, 50 / 30 / 15 / 5 | Yes |

- When there is nothing to roll into, the reroll is refused and costs nothing, and the button
  says so. That covers Extra Salvo and Drydock Repair (there is one of each), a rarity with a
  single skill (Epic and Legendary today), and a hull class with nothing else that fits.
- A slot you have already bought from can't be rerolled.
- Tuning: `Shop.REROLL_BASE_PRICE` and `REROLL_PRICE_STEP`, `RewardPanel.REROLLS_PER_SCREEN`,
  and `StarterFleetPanel.STARTER_SHIPS` and `REROLLS_PER_SHIP`.

## Skills: rarity, weight and upgrades

Every skill has a **rarity**. It does two things: sets how often the skill is offered, and
sets the first stage it can appear at all.

| Rarity | Weight | Unlocks at stage | Skills |
|---|---|---|---|
| Common | 100 | 1 | ERA Bronze, Heavy Caliber |
| Uncommon | 60 | 1 | Ablative Plating, Rapid Fire |
| Rare | 30 | 2 | Enhance, ERA Silver |
| Epic | 12 | 4 | Reinforced Hull |
| Legendary | 5 | 6 | ERA Gold |

There is no opening skill pick: a run starts with no skills and earns them on reward screens,
so the unlock stage is a real gate. "Unlocks at stage 6" means from the reward screen you get
for clearing stage 5 onwards.

Each skill's **weight** is its relative odds in the reward roll. It defaults to its rarity's
weight; override `weight()` on a skill to make that one skill more or less common without
moving it to another tier. The fixed rewards weigh in alongside (`Reward.SHOTS_WEIGHT` and
`REPAIR_WEIGHT`, both 60), and the three cards are drawn in proportion to weight, without
repeats.

**Upgrades.** A skill can name the skill it upgrades with `upgradeOf()`: ERA Silver upgrades
ERA Bronze, ERA Gold upgrades ERA Silver. Then:

- owning a skill makes its upgrades **4× as likely** to be offered
  (`SkillPool.UPGRADE_WEIGHT_MULTIPLIER`) — chains are followed, so owning Bronze boosts Gold
  as well as Silver;
- taking an upgrade **replaces** what it upgrades, rather than sitting beside it;
- a skill is never offered if you already own it or an upgrade of it.

The card says so: an upgrade shows `RARE · UPGRADE` and "Upgrades your ERA Bronze."

Measured over 20,000 reward screens:

| Situation | Offered on |
|---|---|
| ERA Silver, stage 2, holding Bronze | 72% of screens |
| ERA Silver, stage 2, no ERA | 24% |
| ERA Gold, stage 6, holding Silver | 21% |
| ERA Gold, stage 6, no ERA | 4% |

To add a skill: implement `Attacker_Skills` or `Defender_Skills`, give it a `rarity()` (the
compiler insists), optionally an `upgradeOf()`, and register it in `Skills_Register`. Tuning
is all in `skills/Rarity.java` and `SkillPool`.

## Hulls

Six hull classes, each an abstract class with its artwork and any special mechanic. The
named vessels under them carry their own stat block — **including their own cost**, so two
ships of one class can be priced apart.

| Class | Code | Size | Detection | Vessels (cost) |
|---|---|---|---|---|
| Battleship | BB | 4 | 7 (Monarch 6) | Nagato (10), Montana (12), Monarch (7) |
| Aircraft Carrier | CV | 4 | 6 | Essex (14), Enterprise (15) |
| Heavy Cruiser | CB | 3 | 5 | Baltimore (5), Des Moines (5) |
| Light Cruiser | CL | 3 | 4 (Drake 5) | Helena (4), Cleveland (4), Drake (5) |
| Submarine | SS | 2 | 1 (5 surfaced) | Balao (5), Archerfish (5) |
| Destroyer | DD | 2 | 2 | I-556 (2), I-141 (2) |

Cost is set in the vessel's `ShipStats`, next to the rest of its numbers:

```java
super("Monarch", new ShipStats()
        .hp(1000).shields(65).dmg(210).shots(2)
        .penetration(75).detection(6).size(4).cost(7)
        .image("/ships/vessels/Monarch.png"));
```

**Each ship has its own picture.** `.image(...)` names the file, under `lib`. Until that file
exists the hull class's picture stands in, so giving a ship its art is just dropping the file
into `lib/ships/vessels/` with the name from its class - no code change. The expected names
are listed in `lib/ships/vessels/README.txt`.

**Once a ship is built its cost cannot be changed.** It is copied out of the stats at
construction and `getCost()` is `final`, so no skill, reward or subclass can reprice a ship.
A vessel that leaves `.cost(...)` out (or sets it below 1) refuses to be built, rather than
sailing for free.

Adding a vessel means writing the class and adding one line to
`Ships.vessels.VesselRegistry`; nothing else in the game enumerates ship types.

### The deployment budget

Cost caps **what can be on the board at once** — your fleet cost. It grows each stage, plus
whatever you buy with "Expand your fleet":

| Stage | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| Fleet cost (before expansions) | 10 | 14 | 18 | 22 | 26 | 30 | 34 | 38 |

At stage 1, 10 buys **Nagato alone**, or **Monarch and a light cruiser**, or **a heavy and a
light cruiser**. Your three starter ships usually cost more than that between them, so from the
first stage deploying is a decision about what to leave in port: drag a ship back to free its cost and swap in something
else. The status bar under your board tracks `Cost 8 / 10` and the roster reddens anything you
can no longer afford. A placement the budget cannot pay for previews red and is refused on
release, so the cap cannot be exceeded.

You only need **one** ship on the board to fire — a full roster is never required.

The enemy is bought out of the **same budget**, so both sides field equal tonnage each stage
and the difference is which hulls and where. Heavier classes unlock as a run goes on:
submarines from stage 2, heavy cruisers from 3, carriers from 5, battleships from 6.

### Submarines and oxygen

A submarine is the quietest thing on the board, and it is on a clock. Submerged, it spends
**one oxygen per turn**. At zero it is forced to the surface, and a surfaced boat has its
detection raised by 4 — from 1 to 5, which is louder than a light cruiser.

Surfacing is also how the air comes back: a surfaced boat recovers 2 oxygen per turn and can
dive again once it has 2 in the tank. Hover a boat and press **F** (or use the Surface / Dive
button) to do it by choice rather than waiting to be forced up.

*The refill rule is the one piece not dictated by the spec — without it, running out would
permanently cost the hull its whole point.*

### Detection and the enemy search

**Detection** is how easily the enemy finds a ship: 1 for a submerged submarine, 7 for a
battleship. The stealth map is built from it — each tile takes the detection of the ship on
it — and it sets the weight a searching opponent pays there. Your board no longer shades
tiles by it; a ship's detection is on its tooltip and stats board, and a submarine shows
whether it is submerged by being drawn faint.

The map is derived, never stored: it is rebuilt from the fleet on every refresh, so moving a
ship or surfacing a boat updates it for free.

`EliteEnemy` treats the board as a weighted graph — tiles are nodes, adjacent tiles are
edges, entering a tile costs its detection weight — and target selection is a shortest-path
problem solved with Dijkstra over a priority queue. Two maps hold all the state:
`detectionWeight` (sweep cost per tile) and `intel` (what firing at a tile revealed). Two
modes:

- **Sweep** — no live contact. One colour of a checkerboard, since no ship is shorter than
  two tiles.
- **Hunt** — a hit that is not yet sunk. Multi-source Dijkstra outward from every live
  contact, strongly favouring tiles that continue a line of two or more hits, because ships
  are straight.

**It never reads a tile it has not fired at.** Detection comes from ships, so a full copy of
the map *is* the fleet layout; letting the opponent read it would just make it shoot every
non-zero tile. Detection enters the search only once a shot has revealed it, which is where
it matters — a quiet hull stays expensive to finish off after it is found.

The Elite opponent takes over from **stage 3**; the status bar names which one you face.
Measured over 60 simulated battles:

| Opponent | Shots to clear a 4-ship fleet |
|---|---|
| Standard (random) | 60.6 |
| Elite (search) | 34.0 |

### Shots per salvo

The salvo size is computed, not fixed:

```
shots = max(1, round((base - shipsSunk) * skillMultiplier))
```

`base` starts at 3 (`RunState.STARTING_SHOTS`) and rises with every "Extra Salvo" reward.
Each ship you lose costs you a shot, and offensive skills such as Rapid Fire multiply the
result.

## Saving

Three slots, reachable from the pause menu (Save Game / Load Game) and from the main menu
(Load Game). Saves live in `%USERPROFILE%\.battleship-bb\`.

A save captures the run *and* the battle in progress — both boards, both sets of hull points
(including damage on ships in port), every tile already fired at, each submarine's oxygen and
whether it was surfaced, your gold and bought fleet cost — so loading puts you back exactly
where you stopped. The format is plain text rather than Java
serialization, so saves survive edits to the ship and skill classes.

The detection map is not saved: it is derived from the fleet, so rebuilding it from the
restored ships gives the same grid without a second source of truth that could drift.

## Layout

- `src/com/bb` — screens, run loop, save/load, enemy generation and AI
  (`EnemyAI`, `NormalEnenmy`, `EliteEnemy`, `ShotOutcome`), the starter picker
  (`StarterFleetPanel`), the shop (`Shop` for the rules, `ShopPanel` for the screen) and the
  ship stats board (`ShipStatsCard`)
- `src/Ships` — hull classes (`AbstractShip`, `Battleship`, `AircraftCarrier`, …), placement,
  the roster (`PlayerFleet`), roster panel, damage model, `StealthMap`
- `src/Ships/vessels` — the named ships and `VesselRegistry`
- `src/skills` — skill interfaces, implementations, registries, `Rarity` and `SkillPool`
  (the reward odds)
- `src/Test` — the self-check
- `lib` — images (a classpath resource root), including `Currency.png`, the gold coin
