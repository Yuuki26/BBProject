package com.bb;

import Ships.DefaultFleet;
import Ships.FleetCalculation;
import Ships.Fleet_Layout;
import Ships.Ship_Placement;
import Ships.Ships_Type;
import Ships.Submarine;
import Ships.StealthMap;
import skills.ModifiedStats;
import skills.Skills;
import skills.SkillsRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The player's own board: where ships are deployed before a battle, and what the opponent
 * fires at during one.
 *
 * <p>This is the counterpart to {@link OpponentPanel}. It owns the player's fleet, the
 * per-ship hull points, and the record of where the enemy has already fired.
 *
 * <h2>Deployment</h2>
 * Press a ship in the {@link Fleet_Layout} roster or on the board, move it, press
 * <kbd>R</kbd> to turn it, and release to place. See {@link #beginCarry} for why this is a
 * hand-rolled carry rather than Swing drag-and-drop.
 */
public class GameLayout extends JPanel {

    public static final int SIZE = 8;

    // ---- cell colours -------------------------------------------------------------------
    private static final Color C_EMPTY     = Color.WHITE;
    private static final Color C_SHIP      = new Color(70, 110, 170);
    private static final Color C_SELECTED  = new Color(120, 170, 235);
    private static final Color C_GHOST_OK  = new Color(150, 220, 150);
    private static final Color C_GHOST_BAD = new Color(235, 150, 150);
    private static final Color C_MISS      = Color.DARK_GRAY;
    private static final Color C_HIT       = new Color(200, 60, 50);
    private static final Color C_SUNK      = new Color(110, 25, 20);

    private final DefaultFleet fleet = new DefaultFleet();

    private final JButton[][] cells = new JButton[SIZE][SIZE];
    /** Ship occupying each tile, or {@code null} when the tile is open water. */
    private final Ship_Placement[][] board = new Ship_Placement[SIZE][SIZE];

    /** Remaining hull points per deployed ship. Insertion-ordered so saves stay stable. */
    private final Map<Ship_Placement, Integer> shipHP = new LinkedHashMap<>();
    /** Hull points each ship started the battle with, after defensive skills. */
    private final Map<Ship_Placement, Integer> shipMaxHP = new LinkedHashMap<>();

    private final boolean[][] incomingShot = new boolean[SIZE][SIZE];
    private final boolean[][] incomingHit = new boolean[SIZE][SIZE];

    /**
     * Per-tile detection for this board, rebuilt from the fleet on every refresh.
     *
     * <p>Derived state, never edited directly: a tile's value is the detection of the ship
     * on it, so moving a ship or surfacing a submarine changes the map for free.
     */
    private StealthMap stealthMap = new StealthMap(SIZE);

    /** Tiles currently showing a drag preview, so they can be restored on clear. */
    private final List<Point> ghostTiles = new ArrayList<>();

    private final Fleet_Layout roster;
    private final Frames mainFrame;
    private final JLabel status = new JLabel("Drag your ships onto the board. Press R to rotate.");

    /** The ship <kbd>R</kbd> will turn. Set by clicking a ship in the roster or on the board. */
    private Ship_Placement selected;

    /** The ship under the mouse. */
    private Ship_Placement hovered;

    /** The grid of cells, kept so a drag can be mapped back to a tile from any component. */
    private JPanel gridPanel;

    // ---- carry state --------------------------------------------------------------------
    //
    // Placement is a hand-rolled carry rather than Swing drag-and-drop. Swing's DnD runs a
    // native modal loop that swallows every key event - verified: even a global
    // AWTEventListener never sees R while a drag is in flight - which makes rotating a ship
    // while moving it impossible. Doing the drag ourselves keeps the event queue normal, so
    // the R binding fires mid-carry exactly like any other keystroke.

    /** The ship currently being moved, or null. */
    private Ship_Placement carried;

    /** Tile the carried ship's origin would land on, or null when off the board. */
    private Point carryOrigin;

    /** Orientation of the carried ship while it is in the air. */
    private boolean carryHorizontal;

    /** Where the carried ship came from, so a cancelled carry puts it back. */
    private Point carryReturnOrigin;
    private boolean carryReturnHorizontal;

    public GameLayout(Frames mainFrame) {
        this.mainFrame = mainFrame;

        setLayout(new BorderLayout(10, 10));
        setOpaque(false);

        this.roster = new Fleet_Layout(fleet, this);

        add(buildBoardPanel(), BorderLayout.CENTER);
        add(buildRosterPanel(), BorderLayout.EAST);
        add(buildStatusBar(), BorderLayout.SOUTH);

        installRotateHotkey();
        applyStealthTooltips();
        refreshBoard();

        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "Your Fleet",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, null, Color.WHITE));
    }

    // =====================================================================================
    // UI construction
    // =====================================================================================

    private JComponent buildBoardPanel() {
        JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2));
        grid.setOpaque(false);
        this.gridPanel = grid;

        MouseAdapter carryMouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                Point tile = tileAt(e.getComponent(), e.getPoint());
                if (tile == null) return;

                Ship_Placement sp = board[tile.y][tile.x];
                if (sp == null) {
                    setSelected(null);
                    return;
                }
                beginCarry(sp, e.getComponent(), e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updateCarry(e.getComponent(), e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                endCarry(e.getComponent(), e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point tile = tileAt(e.getComponent(), e.getPoint());
                hovered = tile == null ? null : board[tile.y][tile.x];
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (carried == null) hovered = null;
            }
        };

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setOpaque(true);
                cell.setBackground(C_EMPTY);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
                cell.putClientProperty("coord", "" + (char) ('A' + c) + (r + 1));

                cell.addMouseListener(carryMouse);
                cell.addMouseMotionListener(carryMouse);

                cells[r][c] = cell;
                grid.add(cell);
            }
        }

        // Column letters across the top, row numbers down the left.
        JPanel top = new JPanel(new GridLayout(1, SIZE));
        top.setOpaque(false);
        for (int c = 0; c < SIZE; c++) {
            top.add(axisLabel(String.valueOf((char) ('A' + c))));
        }

        JPanel left = new JPanel(new GridLayout(SIZE, 1));
        left.setOpaque(false);
        for (int r = 1; r <= SIZE; r++) {
            left.add(axisLabel(String.valueOf(r)));
        }

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(Box.createHorizontalStrut(24), BorderLayout.WEST);
        topRow.add(top, BorderLayout.CENTER);

        JPanel labelled = new JPanel(new BorderLayout(4, 4));
        labelled.setOpaque(false);
        labelled.add(topRow, BorderLayout.NORTH);
        labelled.add(left, BorderLayout.WEST);
        labelled.add(grid, BorderLayout.CENTER);
        labelled.setPreferredSize(new Dimension(620, 620));

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(labelled);
        return wrapper;
    }

    private JLabel axisLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    private JComponent buildRosterPanel() {
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);

        JLabel title = new JLabel("Undeployed", SwingConstants.CENTER);
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        holder.add(title, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(roster);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(230, 560));
        holder.add(scroll, BorderLayout.CENTER);
        return holder;
    }

    private JComponent buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        bar.setOpaque(false);

        JButton rotateBtn = new JButton("Rotate (R)");
        rotateBtn.setFocusable(false);
        rotateBtn.addActionListener(e -> rotateSelected());

        JButton autoBtn = new JButton("Auto-deploy");
        autoBtn.setFocusable(false);
        autoBtn.addActionListener(e -> autoDeploy());

        JButton diveBtn = new JButton("Surface / Dive (F)");
        diveBtn.setFocusable(false);
        diveBtn.setToolTipText("Acts on the submarine under the cursor");
        diveBtn.addActionListener(e -> toggleSurfaceHovered());

        status.setForeground(Color.WHITE);

        bar.add(rotateBtn);
        bar.add(diveBtn);
        bar.add(autoBtn);
        bar.add(status);
        return bar;
    }

    /**
     * Binds <kbd>R</kbd> to rotation.
     *
     * <p>{@code WHEN_IN_FOCUSED_WINDOW} is deliberate: the grid cells are non-focusable, so a
     * focus-scoped binding would never fire. Swing only dispatches window-scoped bindings to
     * components that are showing, and {@code CardLayout} hides the cards it is not
     * displaying, so this stays inert while the opponent board is up.
     */
    private void installRotateHotkey() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "rotateShip");
        am.put("rotateShip", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rotateSelected();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0), "toggleSurface");
        am.put("toggleSurface", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSurfaceHovered();
            }
        });
    }

    // =====================================================================================
    // Selection and rotation
    // =====================================================================================

    /** Called by {@link Fleet_Layout} when a roster ship is clicked. */
    public void setSelected(Ship_Placement sp) {
        this.selected = sp;
        if (sp == null) {
            Ship_Placement next = nextUndeployed();
            status.setText(next == null
                    ? "Fleet deployed."
                    : "Hover a ship and press R to rotate, then drag it onto the board.");
        } else if (!isDeployed(sp)) {
            status.setText(describe(sp) + " - press R to rotate.");
        } else {
            status.setText(describe(sp) + " deployed.");
        }
        roster.setSelected(sp);
        refreshBoard();
    }

    public Ship_Placement getSelected() {
        return selected;
    }

    /**
     * Handles the <kbd>R</kbd> key: turns the ship currently being carried.
     *
     * <p>Rotation only happens mid-carry. Turning a ship sitting in the roster was removed
     * because it made the player decide an orientation before they could see where the hull
     * would actually land; rotating while it is in hand, over the square it is going to
     * occupy, is the gesture that reads naturally.
     */
    public void rotateSelected() {
        if (carried == null) {
            status.setText("Pick a ship up first - rotation happens while you are moving it.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        carryHorizontal = !carryHorizontal;
        renderCarry();

        String fit = carryOrigin != null && canPlaceAt(carried, carryOrigin, carryHorizontal)
                ? "" : " - will not fit here";
        status.setText(shortName(carried)
                + (carryHorizontal ? " horizontal" : " vertical") + fit);
    }

    /** The next ship still sitting in the roster, or null when the roster is empty. */
    private Ship_Placement nextUndeployed() {
        for (Ship_Placement sp : fleet.getPlacements()) {
            if (!isDeployed(sp)) return sp;
        }
        return null;
    }

    /** Records the ship under the cursor. Called by {@link Fleet_Layout} for roster ships. */
    public void setHovered(Ship_Placement sp) {
        this.hovered = sp;
    }

    public Ship_Placement getHovered() {
        return hovered;
    }

    // =====================================================================================
    // Carry - pick a ship up, move it, rotate it, put it down
    // =====================================================================================

    /** True while a ship is in hand. */
    public boolean isCarrying() {
        return carried != null;
    }

    public Ship_Placement getCarried() {
        return carried;
    }

    /**
     * Picks a ship up.
     *
     * <p>A ship already on the board is lifted off immediately, so its tiles free up and the
     * cost it was using is refunded while it is in the air. That is what lets it be dropped
     * back overlapping its old position, and what stops a half-finished move from leaving the
     * board and the accounting disagreeing.
     */
    public void beginCarry(Ship_Placement sp, Component source, Point pointInSource) {
        beginCarryAt(sp, tileAt(source, pointInSource));
    }

    /** Picks {@code sp} up with its origin over {@code tile}, which may be null. */
    public void beginCarryAt(Ship_Placement sp, Point tile) {
        if (sp == null || carried != null) return;

        carried = sp;
        carryHorizontal = sp.isHorizontal();
        carryReturnOrigin = isDeployed(sp) && sp.getOrigin() != null
                ? new Point(sp.getOrigin()) : null;
        carryReturnHorizontal = sp.isHorizontal();

        if (isDeployed(sp)) {
            clearTiles(sp);
        }
        selected = sp;

        updateCarryTo(tile);
        status.setText(shortName(sp) + " in hand - press R to rotate, release to place.");
    }

    /** Tracks the cursor while a ship is in hand. */
    public void updateCarry(Component source, Point pointInSource) {
        updateCarryTo(tileAt(source, pointInSource));
    }

    /** Moves the carried ship's origin to {@code tile}, or off the board when null. */
    public void updateCarryTo(Point tile) {
        if (carried == null) return;
        carryOrigin = tile;
        renderCarry();
    }

    public void endCarry(Component source, Point pointInSource) {
        endCarryAt(tileAt(source, pointInSource));
    }

    /**
     * Puts the carried ship down at {@code tile}.
     *
     * <p>Only a legal square commits. Anything else returns the ship to wherever it came
     * from, so a mis-drop never loses a hull and never leaves a stale origin behind.
     */
    public void endCarryAt(Point tile) {
        if (carried == null) return;

        Ship_Placement sp = carried;
        Point target = tile;
        boolean horizontal = carryHorizontal;

        // Clear carry state before committing, so validation sees a settled board.
        carried = null;
        carryOrigin = null;
        clearGhost();

        if (target != null && canPlaceAt(sp, target, horizontal)) {
            if (sp.isHorizontal() != horizontal) sp.rotate();
            sp.setOrigin(target);
            commitPlacement(sp);
            return;
        }

        if (target == null) {
            // Dropped off the board entirely: treat it as sending the ship back to port.
            returnToRoster(sp);
            return;
        }

        restoreCarried(sp);
        Toolkit.getDefaultToolkit().beep();
    }

    /** Abandons the carry and puts the ship back where it started. */
    public void cancelCarry() {
        if (carried == null) return;
        Ship_Placement sp = carried;
        carried = null;
        carryOrigin = null;
        clearGhost();
        restoreCarried(sp);
    }

    private void restoreCarried(Ship_Placement sp) {
        if (sp.isHorizontal() != carryReturnHorizontal) sp.rotate();

        if (carryReturnOrigin == null) {
            returnToRoster(sp);
            return;
        }

        sp.setOrigin(carryReturnOrigin);
        occupyTiles(sp);
        ensureHP(sp);
        refreshBoard();
        announceDeploymentProgress();
    }

    /** Sends a ship back to port: off the board, out of the cost, into the roster. */
    private void returnToRoster(Ship_Placement sp) {
        clearTiles(sp);
        sp.setOrigin(null);
        shipHP.remove(sp);
        shipMaxHP.remove(sp);
        roster.rebuild();
        refreshBoard();
        announceDeploymentProgress();
    }

    /** Paints the preview for the ship in hand. */
    private void renderCarry() {
        clearGhost();
        if (carried == null || carryOrigin == null) return;

        boolean ok = canPlaceAt(carried, carryOrigin, carryHorizontal);
        for (Point p : tilesFor(carried, carryOrigin, carryHorizontal)) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
            ghostTiles.add(new Point(p.x, p.y));
            cells[p.y][p.x].setBackground(ok ? C_GHOST_OK : C_GHOST_BAD);
        }
    }

    /**
     * Maps a point in any component to a board tile, or null when it is off the grid.
     *
     * <p>Needed because a carry that starts on a roster label keeps delivering its mouse
     * events to that label, in that label's coordinates, however far the cursor travels.
     */
    private Point tileAt(Component source, Point pointInSource) {
        if (gridPanel == null || source == null) return null;

        Point inGrid = SwingUtilities.convertPoint(source, pointInSource, gridPanel);
        if (inGrid.x < 0 || inGrid.y < 0
                || inGrid.x >= gridPanel.getWidth() || inGrid.y >= gridPanel.getHeight()) {
            return null;
        }

        Component hit = gridPanel.getComponentAt(inGrid);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (cells[r][c] == hit) return new Point(c, r);
            }
        }
        return null;
    }

    private String describe(Ship_Placement sp) {
        return shortName(sp) + " (" + (sp.isHorizontal() ? "horizontal" : "vertical") + ")";
    }

    private String shortName(Ship_Placement sp) {
        return sp.getShip().getClass().getSimpleName();
    }

    // =====================================================================================
    // Placement
    // =====================================================================================

    /** True when {@code sp} can sit at its own current origin and orientation. */
    public boolean validatePlacement(Ship_Placement sp) {
        if (sp == null || sp.getOrigin() == null) return false;
        return canPlaceAt(sp, sp.getOrigin(), sp.isHorizontal());
    }

    /**
     * True when {@code sp} could sit at {@code origin} facing {@code horizontal}: on the
     * board, clear of other ships, and inside the stage's cost budget.
     *
     * <p>Takes the position as arguments rather than reading it off the ship, so a preview
     * can be tested without moving anything. The old drag code mutated the ship's origin on
     * every dragover just to ask this question, which left a rejected drop with its origin
     * still pointing at the square it was refused from - and since "deployed" was decided by
     * that origin, the ship then counted against the budget without ever being on the board.
     */
    public boolean canPlaceAt(Ship_Placement sp, Point origin, boolean horizontal) {
        if (sp == null || sp.getShip() == null || origin == null) return false;

        for (Point p : tilesFor(sp, origin, horizontal)) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) return false;
            Ship_Placement occupant = board[p.y][p.x];
            if (occupant != null && occupant != sp) return false;
        }
        return canAfford(sp);
    }

    /** The tiles {@code sp} would cover at a given origin and orientation. */
    private List<Point> tilesFor(Ship_Placement sp, Point origin, boolean horizontal) {
        List<Point> tiles = new ArrayList<>();
        if (origin == null) return tiles;
        for (int i = 0; i < sp.getShip().getSize(); i++) {
            tiles.add(new Point(origin.x + (horizontal ? i : 0),
                                origin.y + (horizontal ? 0 : i)));
        }
        return tiles;
    }

    /**
     * True when the stage's cost budget covers {@code sp} on top of what is already deployed.
     *
     * <p>A ship already on the board is excluded from the running total, so moving it to a
     * new square is never blocked by its own cost.
     */
    public boolean canAfford(Ship_Placement sp) {
        if (sp == null || sp.getShip() == null) return false;
        return deployedCostExcluding(sp) + sp.getShip().getCost()
                <= RunState.current().getDeploymentBudget();
    }

    private int deployedCostExcluding(Ship_Placement exclude) {
        int spent = 0;
        for (Ship_Placement other : getDeployedPlacements()) {
            if (other != exclude) spent += other.getShip().getCost();
        }
        return spent;
    }

    /** Total hull cost currently on the board. */
    public int getDeployedCost() {
        return deployedCostExcluding(null);
    }

    /** Hull cost still available to spend this stage. */
    public int getRemainingBudget() {
        return RunState.current().getDeploymentBudget() - getDeployedCost();
    }

    /** Moves {@code sp} onto its current origin and takes it out of the roster. */
    public void commitPlacement(Ship_Placement sp) {
        if (sp == null) return;
        clearTiles(sp);
        occupyTiles(sp);
        ensureHP(sp);
        roster.removeShip(sp);
        roster.revalidate();
        roster.repaint();
        setSelected(sp);
        announceDeploymentProgress();
    }

    /** Lifts {@code sp} off the board and returns it to the roster. */
    public void removeShipFromBoard(Ship_Placement sp) {
        if (sp == null) return;
        clearTiles(sp);
        sp.setOrigin(null);
        shipHP.remove(sp);
        shipMaxHP.remove(sp);
        if (selected == sp) selected = null;
        refreshBoard();
        announceDeploymentProgress();
    }

    public void clearGhost() {
        for (Point p : ghostTiles) {
            paintCell(p.y, p.x);
        }
        ghostTiles.clear();
    }

    private void clearTiles(Ship_Placement sp) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == sp) board[r][c] = null;
            }
        }
    }

    private void occupyTiles(Ship_Placement sp) {
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
            board[p.y][p.x] = sp;
        }
    }

    private void announceDeploymentProgress() {
        int spent = getDeployedCost();
        int budget = RunState.current().getDeploymentBudget();
        int left = budget - spent;

        StringBuilder text = new StringBuilder("Cost " + spent + " / " + budget);
        if (deployedCount() == 0) {
            text.append(" - deploy at least one ship before you can fire.");
        } else if (left <= 0) {
            text.append(" - budget spent. Fire, or drag a ship back to swap it.");
        } else {
            text.append(" - ").append(left).append(" left");
            Ship_Placement next = cheapestUndeployed();
            if (next == null) {
                text.append(", nothing else in the roster.");
            } else if (next.getShip().getCost() > left) {
                text.append(", not enough for anything else in the roster.");
            } else {
                text.append(" to spend.");
            }
        }
        status.setText(text.toString());
    }

    /** The cheapest ship still in the roster, or null when the roster is empty. */
    private Ship_Placement cheapestUndeployed() {
        Ship_Placement best = null;
        for (Ship_Placement sp : fleet.getPlacements()) {
            if (isDeployed(sp)) continue;
            if (best == null || sp.getShip().getCost() < best.getShip().getCost()) best = sp;
        }
        return best;
    }

    /** Randomly places every ship still sitting in the roster. */
    public void autoDeploy() {
        Random rnd = new Random();

        // Spend the budget on the heaviest hulls that still fit, rather than whatever comes
        // first in the roster: filling it with destroyers would waste the stage's allowance.
        List<Ship_Placement> byCostDesc = new ArrayList<>(fleet.getPlacements());
        byCostDesc.sort((a, b) -> Integer.compare(b.getShip().getCost(), a.getShip().getCost()));

        for (Ship_Placement sp : byCostDesc) {
            if (isDeployed(sp)) continue;
            if (!canAfford(sp)) continue;
            for (int attempt = 0; attempt < 500; attempt++) {
                if (rnd.nextBoolean()) sp.rotate();
                int size = sp.getShip().getSize();
                int maxX = sp.isHorizontal() ? SIZE - size : SIZE - 1;
                int maxY = sp.isHorizontal() ? SIZE - 1 : SIZE - size;
                if (maxX < 0 || maxY < 0) continue;
                sp.setOrigin(new Point(rnd.nextInt(maxX + 1), rnd.nextInt(maxY + 1)));
                if (validatePlacement(sp)) {
                    occupyTiles(sp);
                    ensureHP(sp);
                    roster.removeShip(sp);
                    break;
                }
                sp.setOrigin(null);
            }
        }
        roster.revalidate();
        roster.repaint();
        refreshBoard();
        announceDeploymentProgress();
    }

    // =====================================================================================
    // Battle state
    // =====================================================================================

    public DefaultFleet getFleet() {
        return fleet;
    }

    public Frames getMainFrame() {
        return mainFrame;
    }

    /** Ships that have been dropped on the board. */
    /**
     * Ships actually occupying tiles.
     *
     * <p>Derived from {@code board}, not from whether a ship has an origin set. The board is
     * the only thing that knows what is really deployed; a stale origin left behind by a
     * refused placement used to count here, which is how the cost budget could be exceeded.
     * The ship in hand mid-carry is deliberately absent, so its cost is refunded while it
     * is in the air.
     */
    public List<Ship_Placement> getDeployedPlacements() {
        List<Ship_Placement> out = new ArrayList<>();
        for (Ship_Placement sp : fleet.getPlacements()) {
            if (isDeployed(sp)) out.add(sp);
        }
        return out;
    }

    /** True when {@code sp} currently occupies at least one tile. */
    public boolean isDeployed(Ship_Placement sp) {
        if (sp == null) return false;
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == sp) return true;
            }
        }
        return false;
    }

    public int deployedCount() {
        return getDeployedPlacements().size();
    }

    /**
     * True when the player is ready to fight.
     *
     * <p>Deliberately not "every ship is on the board": the roster is bigger than the stage's
     * cost budget can field, so leaving ships in port is the normal case rather than an
     * unfinished setup. One deployed ship is enough to start.
     */
    public boolean isReadyForBattle() {
        // Also refuses an over-budget board: a guard here means no future placement path can
        // quietly let a fleet sail that the stage was never allowed to field.
        return deployedCount() > 0 && getDeployedCost() <= RunState.current().getDeploymentBudget();
    }

    /** True when nothing left in the roster can be paid for out of this stage's budget. */
    public boolean isBudgetExhausted() {
        Ship_Placement cheapest = cheapestUndeployed();
        return cheapest == null || cheapest.getShip().getCost() > getRemainingBudget();
    }

    /** Ships that still have hull points left. */
    public List<Ship_Placement> getAlivePlacements() {
        List<Ship_Placement> out = new ArrayList<>();
        for (Ship_Placement sp : getDeployedPlacements()) {
            if (shipHP.getOrDefault(sp, 1) > 0) out.add(sp);
        }
        return out;
    }

    private void ensureHP(Ship_Placement sp) {
        if (shipHP.containsKey(sp)) return;
        int max = scaledMaxHP(sp);
        shipMaxHP.put(sp, max);
        shipHP.put(sp, max);
    }

    /** Base hull points with defensive skill multipliers folded in. */
    private int scaledMaxHP(Ship_Placement sp) {
        float mod = new ModifiedStats().hpModifier();
        return Math.max(1, Math.round(sp.getShip().getHP() * mod));
    }

    public int getShipHP(Ship_Placement sp) {
        return shipHP.getOrDefault(sp, 0);
    }

    public int getShipMaxHP(Ship_Placement sp) {
        return shipMaxHP.getOrDefault(sp, sp.getShip().getHP());
    }

    /**
     * How many shots the player gets this salvo.
     *
     * <p>This replaces the old hard-coded 3. The count starts at the run's base allowance
     * (3 by default, raised by rewards), drops by one for every ship that has been sunk, and
     * is finally scaled by offensive skills such as Rapid Fire. It never falls below one, so
     * a losing player can still shoot back.
     */
    public int getAvailableShots() {
        int base = RunState.current().getBaseShots();

        int sunk = deployedCount() - getAlivePlacements().size();
        int afterLosses = base - sunk;

        float mod = new ModifiedStats().shotsModifier();
        int total = Math.round(afterLosses * mod);

        return Math.max(1, total);
    }

    /** Hands the chosen loadout to the registry. Wired in from {@code Skill_Dialogs}. */
    public void setActiveSkills(List<Skills> selected) {
        SkillsRegistry.setSelectedSkills(selected);
        RunState.current().setLoadout(selected);
        // Hull points depend on defensive skills, so recompute anything already deployed.
        for (Ship_Placement sp : getDeployedPlacements()) {
            int max = scaledMaxHP(sp);
            shipMaxHP.put(sp, max);
            shipHP.put(sp, max);
        }
        refreshBoard();
    }

    /**
     * Applies the opponent's salvo to this board.
     *
     * @param shots              tiles the opponent fired at, in (x=col, y=row) form
     * @param opponentPlacements the firing fleet, used to size the incoming damage
     */
    public List<ShotOutcome> applyShots(List<Point> shots, List<Ship_Placement> opponentPlacements) {
        List<ShotOutcome> outcomes = new ArrayList<>();
        if (shots == null || shots.isEmpty()) return outcomes;

        float damage = FleetCalculation.damageBetween(
                opponentPlacements,
                getAlivePlacements(),
                1f,                                   // the AI carries no offensive skills
                new ModifiedStats().shieldModifier()  // the player's defensive skills do apply
        );
        int perHit = Math.max(1, Math.round(damage));

        int hits = 0;
        for (Point p : shots) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
            if (incomingShot[p.y][p.x]) continue;

            incomingShot[p.y][p.x] = true;
            Ship_Placement hit = board[p.y][p.x];
            if (hit == null) {
                outcomes.add(new ShotOutcome(p, false, false, 0));
                continue;
            }

            incomingHit[p.y][p.x] = true;
            hits++;
            int remaining = shipHP.getOrDefault(hit, getShipMaxHP(hit)) - perHit;
            shipHP.put(hit, Math.max(0, remaining));

            // Concealment is the hull's own stealth plus the water it is sitting in. It only
            // travels back for a tile that has just been revealed, so it gives nothing away.
            int concealment = hit.getShip().getDetection();
            outcomes.add(new ShotOutcome(p, true, remaining <= 0, concealment));
        }

        refreshBoard();
        status.setText("Enemy salvo: " + hits + " hit" + (hits == 1 ? "" : "s")
                + " of " + shots.size() + ".");

        // Skill durations are ticked once per exchange by OpponentPanel, which owns the turn
        // loop. Ticking here as well burned every timed skill twice as fast as its
        // description claimed.

        if (isDefeated() && mainFrame != null) {
            mainFrame.triggerGameOver(false);
        }
        return outcomes;
    }

    /** True once every deployed ship is out of hull points. */
    public boolean isDefeated() {
        if (shipHP.isEmpty()) return false;
        for (int hp : shipHP.values()) {
            if (hp > 0) return false;
        }
        return true;
    }

    // =====================================================================================
    // Rendering
    // =====================================================================================

    /**
     * Rebuilds the detection map from the current fleet, then repaints.
     *
     * <p>The map is derived, not stored: it is rebuilt here rather than edited in place, so a
     * ship moving, sinking or a submarine surfacing is reflected without every one of those
     * paths having to remember to update it.
     */
    private void refreshBoard() {
        stealthMap = StealthMap.fromBoard(board);
        applyStealthTooltips();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                paintCell(r, c);
            }
        }
        repaint();
    }

    private void paintCell(int r, int c) {
        JButton cell = cells[r][c];
        Ship_Placement sp = board[r][c];

        cell.setText("");
        cell.setForeground(Color.WHITE);

        if (incomingShot[r][c]) {
            if (incomingHit[r][c]) {
                boolean sunk = sp != null && shipHP.getOrDefault(sp, 1) <= 0;
                cell.setBackground(sunk ? C_SUNK : C_HIT);
                cell.setText(sunk ? "X" : "*");
            } else {
                cell.setBackground(C_MISS);
            }
            return;
        }

        if (sp == null) {
            cell.setBackground(C_EMPTY);
            cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
            return;
        }

        // Ship tiles are shaded by detection, so how visible each hull is can be read at a
        // glance - a submarine going dark when it dives, and lighting up when it surfaces.
        cell.setBackground(detectionColour(stealthMap.at(r, c)));
        cell.setBorder(sp == selected
                ? BorderFactory.createLineBorder(new Color(255, 215, 90), 2)
                : BorderFactory.createLineBorder(new Color(180, 180, 180)));
    }

    /**
     * Colour ramp for a ship tile's detection.
     *
     * <p>Dark navy is nearly invisible, pale grey-blue is obvious. Running low to high rather
     * than the other way round means the loudest ships stand out on screen exactly as they do
     * to the enemy search.
     */
    private Color detectionColour(int detection) {
        int d = Math.max(0, Math.min(StealthMap.MAX_DETECTION, detection));
        float t = d / (float) StealthMap.MAX_DETECTION;

        int red   = Math.round(24 + t * (198 - 24));
        int green = Math.round(44 + t * (214 - 44));
        int blue  = Math.round(86 + t * (232 - 86));
        return new Color(red, green, blue);
    }

    // =====================================================================================
    // Stealth
    // =====================================================================================

    /** The detection map for this board, derived from whatever is currently deployed. */
    public StealthMap getStealthMap() {
        return stealthMap;
    }

    public int getStealthAt(int row, int col) {
        return stealthMap.at(row, col);
    }

    /** Names each tile and, where a ship sits, its detection and any oxygen state. */
    private void applyStealthTooltips() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                String coord = "" + (char) ('A' + c) + (r + 1);
                Ship_Placement sp = board[r][c];

                if (sp == null) {
                    cells[r][c].setToolTipText(coord + " - open water");
                    continue;
                }

                Ships_Type ship = sp.getShip();
                StringBuilder tip = new StringBuilder(coord + " - " + ship.getName()
                        + " (" + ship.getHullCode() + ")"
                        + ", detection " + ship.getDetection()
                        + ", cost " + ship.getCost());

                if (ship instanceof Submarine) {
                    Submarine sub = (Submarine) ship;
                    tip.append(sub.isSurfaced()
                            ? " - SURFACED, oxygen " + sub.getOxygen() + "/" + sub.getMaxOxygen()
                            : " - submerged, oxygen " + sub.getOxygen() + "/" + sub.getMaxOxygen());
                }
                cells[r][c].setToolTipText(tip.toString());
            }
        }
    }

    // =====================================================================================
    // Submarines
    // =====================================================================================

    /**
     * Advances every deployed submarine by one turn.
     *
     * <p>Called once per exchange by {@link OpponentPanel}, which owns the turn loop.
     *
     * @return a line per boat that was forced to the surface this turn
     */
    public List<String> tickSubmarines() {
        List<String> events = new ArrayList<>();
        for (Ship_Placement sp : getAlivePlacements()) {
            if (!(sp.getShip() instanceof Submarine)) continue;
            Submarine sub = (Submarine) sp.getShip();
            if (sub.tickTurn()) {
                events.add(sub.getName() + " ran out of oxygen and surfaced.");
            }
        }
        refreshBoard();
        return events;
    }

    /**
     * Surfaces or dives the submarine under the cursor - the manual half of the oxygen
     * mechanic, so a boat does not have to wait for its air to run out.
     */
    public void toggleSurfaceHovered() {
        Ship_Placement target = hovered != null ? hovered : selected;
        if (target == null || !(target.getShip() instanceof Submarine)) {
            status.setText("Hover one of your submarines, then press F to surface or dive.");
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        Submarine sub = (Submarine) target.getShip();
        if (sub.isSurfaced()) {
            if (sub.dive()) {
                status.setText(sub.getName() + " dived - detection back down to "
                        + sub.getDetection() + ".");
            } else {
                status.setText(sub.getName() + " needs at least "
                        + Submarine.OXYGEN_TO_DIVE + " oxygen to dive (has "
                        + sub.getOxygen() + ").");
                Toolkit.getDefaultToolkit().beep();
            }
        } else {
            sub.surface();
            status.setText(sub.getName() + " surfaced - detection up to "
                    + sub.getDetection() + ".");
        }
        refreshBoard();
    }

    /** Refills every submarine's tank. Called when a stage begins. */
    public void resetSubmarines() {
        for (Ship_Placement sp : fleet.getPlacements()) {
            if (sp.getShip() instanceof Submarine) {
                ((Submarine) sp.getShip()).resetOxygen();
            }
        }
    }

    // =====================================================================================
    // Save / load support
    // =====================================================================================

    public boolean[][] getIncomingShots() {
        return incomingShot;
    }

    public boolean[][] getIncomingHits() {
        return incomingHit;
    }

    /** Puts one ship straight onto the board, bypassing drag and drop. Used by loads. */
    public void restoreShip(Ship_Placement sp, int currentHP, int maxHP) {
        occupyTiles(sp);
        shipMaxHP.put(sp, maxHP);
        shipHP.put(sp, currentHP);
        roster.removeShip(sp);
    }

    /** Rebuilds the roster panel from the current fleet. Used after a load swaps it out. */
    public void rebuildRosterFromFleet() {
        roster.rebuild();
    }

    /** Repaints everything once a load has finished writing state in. */
    public void refreshAfterLoad() {
        refreshBoard();
        announceDeploymentProgress();
    }

    /** Marks a tile the opponent has already fired at. Used by loads. */
    public void restoreIncoming(int row, int col, boolean wasHit) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return;
        incomingShot[row][col] = true;
        incomingHit[row][col] = wasHit;
    }

    /** Wipes the board back to an empty, undeployed state. */
    public void resetBoard() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                board[r][c] = null;
                incomingShot[r][c] = false;
                incomingHit[r][c] = false;
            }
        }
        shipHP.clear();
        shipMaxHP.clear();
        ghostTiles.clear();
        selected = null;
        hovered = null;
        fleet.reset();
        resetSubmarines();
        roster.rebuild();
        refreshBoard();
        announceDeploymentProgress();
    }

    /**
     * Prepares the board for the next stage of a run.
     *
     * <p>Ships stay where the player put them and keep their orientation; the enemy's shot
     * history is wiped so a fresh opponent starts from scratch. Hull points are only fully
     * restored when {@code healFleet} is set, which is what makes carrying damage forward
     * between stages a real cost.
     *
     * <p>The concealment map is rolled fresh, because each stage is new water. Ships are left
     * where they are rather than being forced back into the roster - the player can always
     * drag them, so they can reposition into the new deep water if they want to, and are
     * never made to re-deploy a fleet they were happy with.
     */
    public void prepareNextStage(boolean healFleet) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                incomingShot[r][c] = false;
                incomingHit[r][c] = false;
            }
        }
        for (Ship_Placement sp : getDeployedPlacements()) {
            int max = scaledMaxHP(sp);
            shipMaxHP.put(sp, max);
            if (healFleet) {
                shipHP.put(sp, max);
            } else {
                shipHP.put(sp, Math.max(1, Math.min(shipHP.getOrDefault(sp, max), max)));
            }
        }
        refreshBoard();
    }

    public void setStatusText(String text) {
        status.setText(text);
    }
}
