package com.bb;

import Ships.*;
import skills.ModifiedStats;
import skills.Skills;
import skills.SkillsRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GameLayout extends JPanel {

    // Keep a local copy of selected skills (for reference) but ModifiedStats reads the registry.
    private List<Skills> activeSkills = new ArrayList<>();
    private final ModifiedStats playerMods = new ModifiedStats(); // reads SkillsRegistry internally

    public ModifiedStats getPlayerModifiers() { return playerMods; }

    // board constants / state
    private String lastGhostKey = null;
    private final int SIZE = 8;
    private Fleet_Layout fleet_layout;
    private DefaultFleet fleet;
    private boolean[][] occupied = new boolean[SIZE][SIZE];
    private Ship_Placement selected_ship_placement = null;
    private boolean isLocked = false; // locking ships

    // check for AI shots
    private final boolean[][] firedOnPlayer = new boolean[SIZE][SIZE];
    private final Map<Point, Ship_Placement> pointToShip = new HashMap<>();
    private final Map<Ship_Placement, Integer> shipHP = new HashMap<>();

    private final Color shotColorMiss = Color.DARK_GRAY;
    private final Color shotColorHit = Color.GREEN;
    private final Color shotColorPartial = Color.YELLOW;

    public boolean isLocked() { return isLocked; }

    // map to remember which tiles were painted for each committed Ship_Placement
    private final Map<Ship_Placement, List<Point>> placedMap = new HashMap<>();

    JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
        @Override public Dimension getPreferredSize() {
            return new Dimension(2560, 1280);
        }
    };

    public GameLayout(Frame frame) {
        setLayout(new BorderLayout());
        add(createBoardPanel(), BorderLayout.CENTER);
        fleet = new DefaultFleet();
        fleet_layout = new Fleet_Layout(fleet, this);
        add(fleet_layout, BorderLayout.EAST);
    }

    /**
     * Called by Skill_Dialogs (or Frames) when player confirms skills.
     * We register skills into the global SkillsRegistry so ModifiedStats can read them.
     */
    public void setActiveSkills(List<Skills> skills) {
        this.activeSkills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
        SkillsRegistry.setSelectedSkills(this.activeSkills);
        // playerMods reads registry dynamically, no need to re-create it
    }

    public List<Ship_Placement> getPlacements() {
        return new ArrayList<>(placedMap.keySet());
    }

    private JPanel createBoardPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel boardPanel = new JPanel(new BorderLayout());

        // Top column labels
        JPanel top = new JPanel(new GridLayout(1, SIZE + 1));
        top.add(new JLabel()); // top-left empty corner
        for (int c = 0; c < SIZE; c++) {
            JLabel lbl = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            top.add(lbl);
        }
        outer.add(top, BorderLayout.NORTH);

        // Left row labels + grid
        JPanel center = new JPanel(new BorderLayout());
        JPanel leftLabels = new JPanel(new GridLayout(SIZE, 1));
        for (int r = 1; r <= SIZE; r++) {
            JLabel lbl = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            leftLabels.add(lbl);
        }
        center.add(leftLabels, BorderLayout.WEST);

        // Create a single CellTransferHandler instance and attach to every cell
        CellTransferHandler cellHandler = new CellTransferHandler(this);

        // Grid of buttons (cells)
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));

                String coord = "" + (char) ('A' + c) + (r + 1);
                cell.putClientProperty("coord", coord);

                // Attach the unified handler (supports both drop and export)
                cell.setTransferHandler(cellHandler);

                // Attach a single mouse-drag listener once (guarded by presence of "ship")
                cell.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        JComponent comp = (JComponent) e.getSource();
                        if (comp.getClientProperty("ship") instanceof Ship_Placement) {
                            TransferHandler handler = comp.getTransferHandler();
                            handler.exportAsDrag(comp, e, TransferHandler.MOVE);
                        }
                    }
                });

                grid.add(cell);
            }
        }

        center.add(grid, BorderLayout.CENTER);
        boardPanel.add(center, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridBagLayout()); // centers its child
        Dimension fixed = new Dimension(800, 800);
        boardPanel.setPreferredSize(fixed);
        boardPanel.setMinimumSize(fixed);
        boardPanel.setMaximumSize(fixed);
        wrapper.add(boardPanel); // GridBagLayout centers by default

        outer.add(wrapper, BorderLayout.CENTER);

        // Ensure ghost clears when the drag leaves the grid
        new DropTarget(grid, new java.awt.dnd.DropTargetListener() {
            @Override public void dragEnter(java.awt.dnd.DropTargetDragEvent dtde) {}
            @Override public void dragOver(java.awt.dnd.DropTargetDragEvent dtde) {}
            @Override public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dtde) {}
            @Override public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                clearGhost();
                lastGhostKey = null;
            }
            @Override public void dragExit(java.awt.dnd.DropTargetEvent dte) {
                clearGhost();
                lastGhostKey = null;
            }
        });

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        JButton confirmBtn = new JButton("Confirm Positions");
        confirmBtn.addActionListener(e -> {
            if (!isLocked) {
                lockPlacements();
                confirmBtn.setEnabled(false); // prevent double-confirm
            }
        });
        controlRow.add(confirmBtn);

        outer.add(controlRow, BorderLayout.SOUTH);
        return outer;
    }

    /**
     * Public API used by CellTransferHandler
     */
    public boolean validatePlacement(Ship_Placement sp) {
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) {
                return false; // out of bounds
            }
            if (occupied[p.y][p.x]) {
                return false; // collision
            }
        }
        return true;
    }

    /**
     * Commit placement: paint ship on board and remove from fleet UI.
     * This is the authoritative commit point called by CellTransferHandler.importData.
     */
    public void commitPlacement(Ship_Placement sp) {
        paintShip(sp, false);
        if (fleet_layout != null) {
            fleet_layout.removeShip(sp);
            fleet_layout.revalidate();
            fleet_layout.repaint();
        }
    }

    /**
     * Paint ship tiles
     */
    public void paintShip(Ship_Placement sp, boolean ghost) {
        // calculate tile size dynamically based on the grid component size
        int w = grid.getComponent(0).getWidth();
        int h = grid.getComponent(0).getHeight();
        int tileSize = (w > 0 && h > 0) ? Math.min(w, h) : 90; // fallback to 90 if not visible yet

        // if committing, clear any previously painted tiles for this same ship
        if (!ghost) {
            List<Point> prev = placedMap.remove(sp);
            if (prev != null) {
                for (Point p : prev) {
                    if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
                    int idx = p.y * SIZE + p.x;
                    JButton cell = (JButton) grid.getComponent(idx);
                    if (cell.getClientProperty("ship") == sp) {
                        cell.setIcon(null);
                        cell.setBackground(Color.WHITE);
                        cell.putClientProperty("ghost", null);
                        cell.putClientProperty("ship", null);
                        occupied[p.y][p.x] = false;
                    }
                    cell.addActionListener(e -> {
                        Object ship = cell.getClientProperty("ship");
                        if (ship instanceof Ship_Placement) {
                            selected_ship_placement = (Ship_Placement) ship;
                        }
                    });
                }
            }
        }

        // Validate placement first (bounds + collision)
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) {
                return;
            }
            if (!ghost && occupied[p.y][p.x]) {
                return;
            }
        }

        // Load full image and scale to ship size
        ImageIcon fullIcon = new ImageIcon(getClass().getResource(sp.getShip().getImage()));
        Image fullImage = fullIcon.getImage();

        int tiles = sp.getShip().getSize();
        int shipWidth = sp.isHorizontal() ? tileSize * tiles : tileSize;
        int shipHeight = sp.isHorizontal() ? tileSize : tileSize * tiles;

        BufferedImage shipCanvas = new BufferedImage(shipWidth, shipHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = shipCanvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(fullImage, 0, 0, shipWidth, shipHeight, null);
        g.dispose();

        List<Point> tilesPoints = sp.getOccupiedTiles();
        for (int i = 0; i < tiles; i++) {
            int sx = sp.isHorizontal() ? i * tileSize : 0;
            int sy = sp.isHorizontal() ? 0 : i * tileSize;
            BufferedImage tileImg = shipCanvas.getSubimage(sx, sy, tileSize, tileSize);

            Point p = tilesPoints.get(i);
            int index = p.y * SIZE + p.x;
            JButton cell = (JButton) grid.getComponent(index);

            cell.setIcon(new ImageIcon(tileImg));
            cell.setBackground(ghost ? Color.LIGHT_GRAY : Color.CYAN);
            cell.putClientProperty("ghost", null);

            if (!ghost) {
                occupied[p.y][p.x] = true;
                cell.putClientProperty("ship", sp);
            }
        }

        // store committed tiles for this ship so we can clear them later
        if (!ghost) {
            placedMap.put(sp, new ArrayList<>(sp.getOccupiedTiles()));
            clearGhost(); // clear any lingering ghost after commit
            lastGhostKey = null;
        }
    }

    /**
     * Public ghost painting API used by CellTransferHandler.
     * This method deduplicates repeated ghost painting using lastGhostKey.
     */
    public void paintGhost(Ship_Placement sp) {
        try {
            String coord = null;
            // build a small key to detect changes: ship identity + origin + orientation
            String shipId = Integer.toHexString(System.identityHashCode(sp));
            Point origin = sp.getOrigin();
            if (origin != null) {
                coord = origin.x + ":" + origin.y;
            } else {
                coord = "null";
            }
            String key = shipId + ":" + coord + ":" + sp.isHorizontal();
            if (key.equals(lastGhostKey)) {
                return; // no change
            }
            lastGhostKey = key;
        } catch (Exception ignored) {
        }

        clearGhost(); // remove old preview

        // calculate tile size dynamically based on the grid component size
        int w = grid.getComponent(0).getWidth();
        int h = grid.getComponent(0).getHeight();
        int tileSize = (w > 0 && h > 0) ? Math.min(w, h) : 90; // fallback to 90 if not visible yet
        ImageIcon fullIcon = new ImageIcon(getClass().getResource(sp.getShip().getImage()));
        Image fullImage = fullIcon.getImage();

        int tiles = sp.getShip().getSize();
        int shipWidth = sp.isHorizontal() ? tileSize * tiles : tileSize;
        int shipHeight = sp.isHorizontal() ? tileSize : tileSize * tiles;

        BufferedImage shipCanvas = new BufferedImage(shipWidth, shipHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = shipCanvas.createGraphics();
        g.drawImage(fullImage, 0, 0, shipWidth, shipHeight, null);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, shipWidth, shipHeight);
        g.dispose();

        for (int i = 0; i < tiles; i++) {
            Point p = sp.getOccupiedTiles().get(i);
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
            if (occupied[p.y][p.x]) continue; // skip committed ships

            int sx = sp.isHorizontal() ? i * tileSize : 0;
            int sy = sp.isHorizontal() ? 0 : i * tileSize;
            BufferedImage tileImg = shipCanvas.getSubimage(sx, sy, tileSize, tileSize);

            int index = p.y * SIZE + p.x;
            JButton cell = (JButton) grid.getComponent(index);
            cell.setIcon(new ImageIcon(tileImg));
            cell.setBackground(Color.LIGHT_GRAY);
            cell.putClientProperty("ghost", Boolean.TRUE);
        }
    }

    public void clearGhost() {
        for (int i = 0; i < grid.getComponentCount(); i++) {
            JButton cell = (JButton) grid.getComponent(i);
            if (Boolean.TRUE.equals(cell.getClientProperty("ghost"))) {
                cell.setIcon(null);
                cell.setBackground(Color.WHITE);
                cell.putClientProperty("ghost", null);
            }
        }
        lastGhostKey = null;
    }

    /**
     * Remove ship from board (used when returning to roster).
     * Uses stored placedMap to clear the exact cells that were painted earlier.
     */
    public void removeShipFromBoard(Ship_Placement sp) {
        // Prefer stored tile list so we clear the exact cells that were painted earlier
        List<Point> prev = placedMap.remove(sp);
        if (prev == null) {
            // fallback: clear any cells that reference this ship object
            for (int i = 0; i < grid.getComponentCount(); i++) {
                JButton cell = (JButton) grid.getComponent(i);
                if (cell.getClientProperty("ship") == sp) {
                    cell.setIcon(null);
                    cell.setBackground(Color.WHITE);
                    cell.putClientProperty("ghost", null);
                    cell.putClientProperty("ship", null);
                    int r = i / SIZE, c = i % SIZE;
                    occupied[r][c] = false;
                }
            }
            return;
        }

        for (Point p : prev) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
            int index = p.y * SIZE + p.x;
            JButton cell = (JButton) grid.getComponent(index);
            if (cell.getClientProperty("ship") == sp) {
                cell.setIcon(null);
                cell.setBackground(Color.WHITE);
                cell.putClientProperty("ghost", null);
                cell.putClientProperty("ship", null);
                occupied[p.y][p.x] = false;
            }
        }
    }

    public DefaultFleet getFleet() {
        return fleet;
    }

    // if hit, our ship gets lit up also
    private void revealShipTiles(Ship_Placement ship, Color color, boolean markFired) {
        if (ship == null) return;
        List<Point> tiles = placedMap.getOrDefault(ship, ship.getOccupiedTiles());
        for (Point tile : tiles) {
            int tx = tile.x;
            int ty = tile.y;
            if (tx < 0 || tx >= SIZE || ty < 0 || ty >= SIZE) continue;
            JButton cell = (JButton) grid.getComponent(ty * SIZE + tx);
            cell.setBackground(color);
            cell.setEnabled(!markFired);
            if (markFired) {
                firedOnPlayer[ty][tx] = true;
            }
        }
    }

    // lock ships
    public void lockPlacements() {
        isLocked = true;
        clearGhost();
        lastGhostKey = null;

        // Disable drag on cells
        for (int i = 0; i < grid.getComponentCount(); i++) {
            JComponent jc = (JComponent) grid.getComponent(i);
            jc.setTransferHandler(null);
            for (java.awt.event.MouseMotionListener ml : jc.getMouseMotionListeners()) {
                jc.removeMouseMotionListener(ml);
            }
        }
        if (fleet_layout != null) {
            fleet_layout.setEnabled(false);
            fleet_layout.setVisible(false);
        }
        grid.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));

        // Build combat maps from placed ships
        pointToShip.clear();
        shipHP.clear();
        float hpMod = playerMods != null ? playerMods.hpModifier() : 1f;
        for (Map.Entry<Ship_Placement, List<Point>> entry : placedMap.entrySet()) {
            Ship_Placement sp = entry.getKey();
            int baseHp = sp.getShip().getHP();
            int modifiedHp = Math.max(1, Math.round(baseHp * hpMod));
            shipHP.put(sp, modifiedHp);
            for (Point p : entry.getValue()) {
                pointToShip.put(new Point(p.x, p.y), sp);
            }
        }
        for (int r = 0; r < SIZE; r++) Arrays.fill(firedOnPlayer[r], false);
    }

    /**
     * Apply a volley of shots using attacker fleet stats and optional attacker modifiers.
     * Uses FleetCalculation.DamageToShips(attacker, defender, attackerMods, defenderMods).
     *
     * Painting rules:
     *  - miss -> DARK_GRAY, permanently fired
     *  - hit and remaining HP > 50% -> color ALL ship tiles GREEN, keep tiles targetable
     *  - hit and remaining HP <= 50% -> color ALL ship tiles YELLOW,keep targetable
     *  - destroyed -> color ALL ship tiles RED, mark tiles permanently fired
     */
    public void applyShots(List<Point> shots, List<Ship_Placement> attackerFleet, ModifiedStats attackerMods) {
        if (shots == null || shots.isEmpty()) return;

        List<Ship_Placement> defenderPlacements = new ArrayList<>(placedMap.keySet());

        // Decide damage path:
        // - if attackerMods provided -> use player-aware DamageToShips (attacker is player)
        // - otherwise -> use default AI damage path DamageFromShips
        float dmgFloat;
        if (attackerMods != null) {
            // attacker provided modifiers (player attack scenario)
            Ships.FleetCalculation calc = new Ships.FleetCalculation(attackerFleet, attackerMods);
            dmgFloat = calc.DamageToShips();
        } else {
            // AI/default damage path (attacker is opponent)
            dmgFloat = Ships.FleetCalculation.DamageFromShips(attackerFleet, defenderPlacements);
        }

        int damagePerHit = dmgFloat > 0f ? Math.max(1, Math.round(dmgFloat)) : 0;

        for (Point p : shots) {
            int col = p.x, row = p.y;
            if (col < 0 || col >= SIZE || row < 0 || row >= SIZE) continue;

            // Skip permanently fired tiles
            if (firedOnPlayer[row][col]) continue;

            JButton cell = (JButton) grid.getComponent(row * SIZE + col);
            Ship_Placement hitShip = pointToShip.get(new Point(col, row));

            if (hitShip == null) {
                // miss -> permanently mark fired and disable
                firedOnPlayer[row][col] = true;
                cell.setBackground(shotColorMiss);
                cell.setEnabled(false);
                continue;
            }

            // hit: reduce HP and color according to remaining percent
            int remaining = shipHP.getOrDefault(hitShip, hitShip.getShip().getHP());
            int damage = Math.max(0, damagePerHit); // allow zero if penetration failed
            remaining -= damage;
            shipHP.put(hitShip, remaining);

            int initialHP = hitShip.getShip().getHP();
            float pct = (float) remaining / (float) initialHP;

            if (remaining <= 0) {
                // destroyed -> reveal whole ship red and mark all tiles fired
                revealShipTiles(hitShip, Color.RED, true);
            } else if (pct <= 0.5f) {
                // ≤50% -> reveal whole ship yellow and mark all tiles fired
                revealShipTiles(hitShip, shotColorPartial, false);
            } else {
                // >50% -> reveal whole ship green but keep tiles targetable
                revealShipTiles(hitShip, shotColorHit, false);
            }
        }
    }

    // Backwards-compatible overloads
    public void applyShots(List<Point> shots, List<Ship_Placement> attackerFleet) {
        applyShots(shots, attackerFleet, null);
    }

    public void applyShots(List<Point> shots) {
        applyShots(shots, (List<Ship_Placement>) null, null);
    }
}
