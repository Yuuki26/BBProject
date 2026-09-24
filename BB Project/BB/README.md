# Battleship (roguelike)

A Swing battleship game. A run is a chain of stages against progressively larger enemy
fleets: clear one, pick an upgrade, and face a bigger one. Losing your whole fleet ends the
run.

## Running it

```
run.bat
```

Or from an IDE, run `com.bb.Main`.

**`lib` has to be on the classpath.** Every image is loaded through
`getResource("/ships/...")`, `getResource("/background.png")` and similar, so `lib` is a
resource root, not just a dependency folder. `run.bat` and the supplied VS Code launch
configuration both handle this; a hand-rolled run configuration that omits it will start the
game with no art.

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
| **E** | Show your board |
| **Q** | Show the enemy board |
| Auto-deploy | Spend the stage budget on the heaviest hulls that fit, at random |
| Fire, then click tiles, then Confirm | Resolve a salvo |

**Rotation happens while you are dragging.** Pick a ship up, move it over the square you
want, press **R** until it points the right way, and release. The green/red preview updates
as you turn it, so you are deciding the orientation while looking at where the hull will
actually land. Ships on the board can be picked up and moved or rotated as often as you like.

Dragging is implemented by hand rather than with Swing's drag-and-drop, because Swing's DnD
runs a native modal loop that never delivers a keystroke — not even to a global
`AWTEventListener` — which made rotating mid-drag impossible.

## How a run works

1. Pick two starting skills.
2. Deploy what the stage budget allows, then trade salvos with the enemy.
3. Clear the enemy fleet and you reach the reward screen: choose one of three upgrades
   (an extra shot, a repair, a new skill, or a new hull).
4. The next stage raises the deployment budget and fields a larger enemy fleet. Damage
   carries over unless you took the repair, so attrition is real.
5. Lose every ship and the run ends, with the stage you reached and your score.

Heavier enemy hulls unlock as the run goes on - see Hulls below.

## Hulls

Six hull classes, two named vessels each. Every class is an abstract class carrying the whole
stat block plus its own fixed **cost**; the named vessels differ only in their numbers.

| Class | Code | Cost | Size | Detection | Vessels |
|---|---|---|---|---|---|
| Battleship | BB | 10 | 4 | 7 | Nagato, Montana |
| Aircraft Carrier | CV | 8 | 4 | 6 | Essex, Enterprise |
| Heavy Cruiser | CB | 6 | 3 | 5 | Baltimore, Des Moines |
| Light Cruiser | CL | 4 | 3 | 4 | Helena, Cleveland |
| Submarine | SS | 3 | 2 | 1 (5 surfaced) | Balao, Archerfish |
| Destroyer | DD | 2 | 2 | 4 | I-556, I-141 |

**Cost is fixed per class and cannot be changed.** `getCost()` is declared `final` on each
abstract class, so no vessel, skill or reward can override it.

Adding a vessel means writing the class and adding one line to
`Ships.vessels.VesselRegistry`; nothing else in the game enumerates ship types.

### The deployment budget

Cost caps **what can be on the board at once**, not how big your roster is. Each stage has a
budget, and the roster is deliberately worth more than it:

| Stage | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 |
|---|---|---|---|---|---|---|---|---|
| Budget | 10 | 14 | 18 | 22 | 26 | 30 | 34 | 38 |

At stage 1 a budget of 10 buys **one battleship**, or **a carrier and a destroyer**, or
**the two cruisers**. The starting roster is worth 33, so most of it stays in port and
deploying is a decision about what to leave behind. Drag a ship back to the roster to free
its cost and swap in something else; the status bar under your board tracks `Cost 8 / 10`
and the roster reddens anything you can no longer afford. A placement the budget cannot pay
for previews red and is refused on release, so the cap cannot be exceeded.

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
it — and it drives both the tile colour on your board and the weight a searching opponent
pays there. Ship tiles are shaded from dark navy (quiet) to pale blue (loud), so a submarine
visibly lights up the moment it surfaces. Hover any tile for the exact figures.

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

A save captures the run *and* the battle in progress — both boards, both sets of hull points,
every tile already fired at, and each submarine's oxygen and whether it was surfaced — so
loading puts you back exactly where you stopped. The format is plain text rather than Java
serialization, so saves survive edits to the ship and skill classes.

The detection map is not saved: it is derived from the fleet, so rebuilding it from the
restored ships gives the same grid without a second source of truth that could drift.

## Layout

- `src/com/bb` — screens, run loop, save/load, enemy generation and AI
  (`EnemyAI`, `NormalEnenmy`, `EliteEnemy`, `ShotOutcome`)
- `src/Ships` — hull classes (`AbstractShip`, `Battleship`, `AircraftCarrier`, …), placement,
  fleet, roster panel, damage model, `StealthMap`
- `src/Ships/vessels` — the twelve named ships and `VesselRegistry`
- `src/skills` — skill interfaces, implementations, registries
- `src/Test` — the self-check
- `lib` — images (a classpath resource root)
