package com.bb;

import Ships.PlayerFleet;
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
import java.awt.event.HierarchyEvent;
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
 *
 * <h2>Starting the battle</h2>
 * Deployment ends when the player presses Start ({@link #startBattle()}), which checks the
 * fleet against the stage's cost budget and then locks it in until the stage is over.
 *
 * <h2>How ships are drawn</h2>
 * A ship's tiles are see-through and its own picture is drawn across them
 * ({@link #paintShips}). Hits are not painted onto tiles; a ship's condition is on its
 * tooltip and stats board, and only two states show on the picture itself: a sunk ship is a
 * grey wreck, and a submerged submarine is drawn faint. Open water is white, and dark grey
 * where the enemy has fired and missed.
 *
 * <h2>Ship stats</h2>
 * Hovering a ship shows a tooltip with its basics and a hull bar. Clicking one during a
 * battle - or right-clicking one at any time, on the board or in port - opens a
 * {@link ShipStatsCard} with everything about it; see {@link #showStats}.
 */
public class GameLayout extends JPanel {

    public static final int SIZE = 8;

    /**
     * Property fired when the battle starts, and again when a new stage reopens deployment.
     * {@link Navigator} listens for it to swap the Start button for Enemy Board.
     */
    public static final String PROP_BATTLE_STARTED = "battleStarted";

    private static final String LOCKED_MESSAGE =
            "The battle has started - your fleet is locked in until this stage ends.";

    // ---- cell colours -------------------------------------------------------------------
    private static final Color C_EMPTY     = Color.WHITE;
    private static final Color C_GHOST_OK  = new Color(150, 220, 150);
    private static final Color C_GHOST_BAD = new Color(235, 150, 150);
    private static final Color C_MISS      = Color.DARK_GRAY;
    private static final Color C_OUTLINE   = new Color(255, 215, 90);

    private static final javax.swing.border.Border WATER_BORDER =
            BorderFactory.createLineBorder(new Color(180, 180, 180));
    /** Same insets as the water border, drawing nothing. */
    private static final javax.swing.border.Border SHIP_BORDER =
            BorderFactory.createEmptyBorder(1, 1, 1, 1);

    /** How faint a submerged submarine is drawn: it is under water. */
    private static final float SUBMERGED_ALPHA = 0.45f;
    /** How faint the ship in hand is previewed where it would land. */
    private static final float CARRY_ALPHA = 0.6f;
    /** A sunk ship: grey, half see-through, and listing a few degrees. */
    private static final float WRECK_ALPHA = 0.6f;
    private static final double WRECK_LIST = Math.toRadians(5);

    private final PlayerFleet fleet = new PlayerFleet();

    private final JButton[][] cells = new JButton[SIZE][SIZE];
    /** Ship occupying each tile, or {@code null} when the tile is open water. */
    private final Ship_Placement[][] board = new Ship_Placement[SIZE][SIZE];

    /**
     * Remaining hull points per ship that has been deployed. Kept when a ship goes back to
     * port, so damage sticks until it is repaired or the ship is sold. Insertion-ordered so
     * saves stay stable.
     */
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

    /** The stats board, and the ship it is showing (null while it is closed). */
    private final ShipStatsCard statsCard = new ShipStatsCard(this::hideStats);
    private Ship_Placement statsShip;

    /** Tiles currently showing a drag preview, so they can be restored on clear. */
    private final List<Point> ghostTiles = new ArrayList<>();

    private final Fleet_Layout roster;
    private final Frames mainFrame;
    private final JLabel status = new JLabel(
            "Drag your ships onto the board (R rotates while dragging), then press Start.");

    /**
     * True once Start has been pressed for this stage. From then until the stage ends the
     * fleet is locked in: nothing can be picked up, moved or auto-deployed.
     */
    private boolean battleStarted;

    /** Deployment-only controls, disabled while the battle is on. */
    private JButton rotateBtn;
    private JButton autoBtn;

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

        // The stats board floats over the window, so it has to be put away by hand when this
        // board stops showing - on a switch to the enemy board, a menu, the shop.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing()) {
                hideStats();
            }
        });

        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.WHITE), "Your Fleet",
                javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
                javax.swing.border.TitledBorder.DEFAULT_POSITION, null, Color.WHITE));
    }

    // =====================================================================================
    // UI construction
    // =====================================================================================

    private JComponent buildBoardPanel() {
        JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
            @Override
            protected void paintChildren(Graphics g) {
                super.paintChildren(g);
                paintShips(g);   // over the cells, so one picture can span several tiles
            }
        };
        grid.setOpaque(false);
        this.gridPanel = grid;

        MouseAdapter carryMouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point tile = tileAt(e.getComponent(), e.getPoint());
                if (tile == null) return;
                Ship_Placement sp = board[tile.y][tile.x];

                // Right-click opens the stats board at any time; so does a plain click once
                // the fleet is locked in, when there is nothing left to pick up.
                if (SwingUtilities.isRightMouseButton(e)) {
                    toggleStats(sp, e.getComponent());
                    return;
                }
                if (!SwingUtilities.isLeftMouseButton(e)) return;

                if (sp == null) {
                    hideStats();
                    setSelected(null);
                    return;
                }
                if (battleStarted) {
                    setSelected(sp);   // so Surface / Dive acts on the ship being looked at
                    toggleStats(sp, e.getComponent());
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
                // A flat fill from the background alone: no pressed-button grey flashing up
                // under the cursor when a ship is picked up.
                cell.setContentAreaFilled(false);
                cell.setBackground(C_EMPTY);
                cell.setBorder(WATER_BORDER);
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

        rotateBtn = new JButton("Rotate (R)");
        rotateBtn.setFocusable(false);
        rotateBtn.addActionListener(e -> rotateSelected());

        autoBtn = new JButton("Auto-deploy");
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

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeStats");
        am.put("closeStats", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hideStats();
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
        // Deployment hints would only mislead once the fleet is locked in.
        if (!battleStarted) status.setText(selectionHint(sp));
        roster.setSelected(sp);
        refreshBoard();
    }

    private String selectionHint(Ship_Placement sp) {
        if (sp == null) {
            return nextUndeployed() == null
                    ? "Fleet deployed - press Start."
                    : "Drag a ship onto the board - press R while dragging to rotate it.";
        }
        return isDeployed(sp)
                ? describe(sp) + " deployed."
                : describe(sp) + " - drag it onto the board, R rotates it on the way.";
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
            status.setText(battleStarted
                    ? LOCKED_MESSAGE
                    : "Pick a ship up first - rotation happens while you are moving it.");
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

    /**
     * Picks {@code sp} up with its origin over {@code tile}, which may be null.
     *
     * <p>Refused once the battle has started: a ship that could still be moved could dodge a
     * hit it had already taken, or go back to port and come out again at full hull.
     */
    public void beginCarryAt(Ship_Placement sp, Point tile) {
        if (sp == null || carried != null) return;
        if (battleStarted) {
            status.setText(LOCKED_MESSAGE);
            Toolkit.getDefaultToolkit().beep();
            return;
        }

        hideStats();
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
    /**
     * Sends a ship back to port: off the board, out of the cost, into the roster.
     *
     * <p>Its damage goes with it. Forgetting the hull points here used to mean a battered ship
     * could be dragged to port and straight back out at full strength - a free repair that
     * would make the shop's paid one pointless.
     */
    private void returnToRoster(Ship_Placement sp) {
        clearTiles(sp);
        sp.setOrigin(null);
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
            // Filled even over another ship's see-through tiles, so an overlap shows red.
            JButton cell = cells[p.y][p.x];
            cell.setOpaque(true);
            cell.setBorder(WATER_BORDER);
            cell.setBackground(ok ? C_GHOST_OK : C_GHOST_BAD);
        }
        if (gridPanel != null) gridPanel.repaint();   // moves the ship's picture along
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
        if (selected == sp) selected = null;
        roster.rebuild();
        refreshBoard();
        announceDeploymentProgress();
    }

    // =====================================================================================
    // Roster changes: starter pick, shop
    // =====================================================================================

    /**
     * Installs the starter fleet as the roster, undeployed.
     *
     * <p>Each ship has to fit the fleet cost on its own, so every one of them can be put on
     * the board. Together they may cost more than that; deploying is then a matter of
     * choosing which to leave in port.
     *
     * @return null when installed, or why it was refused: no ships, or a ship that costs
     *         more than the whole fleet cost and so could never be deployed
     */
    public String setStarterFleet(List<Ships_Type> picks) {
        if (picks == null || picks.isEmpty()) return "Choose at least one ship.";

        int budget = RunState.current().getDeploymentBudget();
        for (Ships_Type t : picks) {
            if (t.getCost() > budget) {
                return t.getName() + " costs " + t.getCost() + ", more than your fleet cost of "
                        + budget + " - it could never be deployed.";
            }
        }

        resetBoard();
        for (Ships_Type t : picks) fleet.addShip(t);
        roster.rebuild();
        refreshBoard();
        announceDeploymentProgress();
        return null;
    }

    /** Adds a newly bought ship to the roster, undeployed and at full hull. */
    public Ship_Placement addToRoster(Ships_Type type) {
        Ship_Placement sp = fleet.addShip(type);
        roster.rebuild();
        announceDeploymentProgress();
        return sp;
    }

    /** Takes a ship out of the fleet for good, lifting it off the board first if need be. */
    public void sellShip(Ship_Placement sp) {
        if (sp == null) return;
        if (carried == sp) cancelCarry();
        clearTiles(sp);
        sp.setOrigin(null);
        fleet.removeShip(sp);
        shipHP.remove(sp);
        shipMaxHP.remove(sp);
        if (selected == sp) selected = null;
        if (hovered == sp) hovered = null;
        roster.rebuild();
        refreshBoard();
        announceDeploymentProgress();
    }

    /**
     * Hull points right now, whether the ship is deployed or in port. A ship that has never
     * been damaged has no record and is at full hull.
     */
    public int currentHP(Ship_Placement sp) {
        return shipHP.containsKey(sp) ? shipHP.get(sp) : getShipMaxHP(sp);
    }

    /** True when the ship has lost hull points that a repair would restore. */
    public boolean isDamaged(Ship_Placement sp) {
        return currentHP(sp) < getShipMaxHP(sp);
    }

    /** True when this board is keeping hull points for the ship. Used by saves. */
    public boolean hasHullRecord(Ship_Placement sp) {
        return shipHP.containsKey(sp);
    }

    /** Restores a ship to full hull. */
    public void repairShip(Ship_Placement sp) {
        if (sp == null || !shipHP.containsKey(sp)) return;
        shipHP.put(sp, getShipMaxHP(sp));
        refreshBoard();
    }

    /** Restores every ship the player owns to full hull, on the board or in port. */
    public void repairAll() {
        for (Ship_Placement sp : fleet.getPlacements()) {
            if (shipHP.containsKey(sp)) shipHP.put(sp, getShipMaxHP(sp));
        }
        refreshBoard();
    }

    /** Puts a ship's hull points back from a save, without deploying it. */
    public void restoreHull(Ship_Placement sp, int currentHP, int maxHP) {
        if (sp == null || maxHP <= 0) return;
        shipMaxHP.put(sp, maxHP);
        shipHP.put(sp, Math.max(0, Math.min(currentHP, maxHP)));
    }

    public void clearGhost() {
        for (Point p : ghostTiles) {
            paintCell(p.y, p.x);
        }
        ghostTiles.clear();
        if (gridPanel != null) gridPanel.repaint();
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
            text.append(" - deploy at least one ship before you can start.");
        } else if (spent > budget) {
            text.append(" - over budget. Drag a ship back to port before you can start.");
        } else if (left == 0) {
            text.append(" - budget spent. Press Start, or drag a ship back to swap it.");
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
        if (battleStarted) {
            status.setText(LOCKED_MESSAGE);
            return;
        }
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

    public PlayerFleet getFleet() {
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
        return deploymentProblem() == null;
    }

    /**
     * Why the fleet cannot sail yet, or null when it can.
     *
     * <p>Also refuses an over-budget board. Placement already refuses anything the budget
     * cannot pay for, so this only trips on a fleet that reached the board some other way -
     * a save written before the budget existed, say - but checking here means no path,
     * present or future, can quietly let a fleet sail that the stage never allowed.
     */
    private String deploymentProblem() {
        if (deployedCount() == 0) {
            return "deploy at least one ship first.";
        }
        int spent = getDeployedCost();
        int budget = RunState.current().getDeploymentBudget();
        if (spent > budget) {
            return "your fleet costs " + spent + " but this stage's budget is " + budget
                    + " - drag a ship back to port.";
        }
        return null;
    }

    // =====================================================================================
    // Starting the battle
    // =====================================================================================

    /** True once Start has been pressed this stage, and the fleet is locked in. */
    public boolean isBattleStarted() {
        return battleStarted;
    }

    /**
     * The Start button: checks the deployment, locks the fleet in, and begins the battle.
     *
     * <p>Refuses, and says why on the status bar, when nothing is deployed or the fleet on
     * the board costs more than this stage's budget.
     *
     * @return true when the battle is now under way
     */
    public boolean startBattle() {
        if (battleStarted) return true;
        cancelCarry();

        String problem = deploymentProblem();
        if (problem != null) {
            status.setText("Can't start: " + problem);
            return false;
        }

        setBattleStarted(true);
        setSelected(null);   // a deployment pick; in battle the outline follows clicks
        status.setText("Battle started with " + deployedCount() + " ship"
                + (deployedCount() == 1 ? "" : "s") + " (cost " + getDeployedCost() + " / "
                + RunState.current().getDeploymentBudget() + "). Your fleet is locked in.");
        return true;
    }

    /** Puts a loaded game back in the phase it was saved in, without re-checking it. */
    public void restoreBattleStarted(boolean started) {
        setBattleStarted(started);
    }

    private void setBattleStarted(boolean started) {
        boolean was = battleStarted;
        battleStarted = started;

        if (rotateBtn != null) rotateBtn.setEnabled(!started);
        if (autoBtn != null) autoBtn.setEnabled(!started);
        roster.rebuild();

        firePropertyChange(PROP_BATTLE_STARTED, was, started);
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

    /** Hands a skill loadout to the registry and the run, and rescales deployed hulls. */
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
        int halfHit = Math.max(1, Math.round(damage / 2f));

        int hits = 0;
        java.util.Set<Point> thisSalvo = new java.util.HashSet<>();
        for (Point p : shots) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
            if (!thisSalvo.add(new Point(p))) continue;

            // Same rule the player fires under: a tile already hit can be hit again, at half
            // damage, once its ship is crippled or has had every tile hit. Anything else
            // already fired on is a wasted shot.
            boolean refire = incomingShot[p.y][p.x];
            if (refire && !isRefireTarget(p.y, p.x)) continue;

            incomingShot[p.y][p.x] = true;
            Ship_Placement hit = board[p.y][p.x];
            if (hit == null) {
                outcomes.add(new ShotOutcome(p, false, false, 0));
                continue;
            }

            incomingHit[p.y][p.x] = true;
            hits++;
            int remaining = shipHP.getOrDefault(hit, getShipMaxHP(hit)) - (refire ? halfHit : perHit);
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

    /**
     * Tiles the enemy has already hit and may fire on again, at half damage: hits on a ship
     * that is still afloat and is either at or below half hull, or has had every tile hit.
     *
     * <p>Handed to the opponent each turn. It only ever names tiles the opponent has already
     * hit itself, so it gives away nothing the player could not see on the enemy's side.
     */
    public List<Point> refireTargets() {
        List<Point> out = new ArrayList<>();
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (isRefireTarget(r, c)) out.add(new Point(c, r));
            }
        }
        return out;
    }

    private boolean isRefireTarget(int row, int col) {
        if (!incomingHit[row][col]) return false;
        Ship_Placement sp = board[row][col];
        if (sp == null) return false;
        int hp = shipHP.getOrDefault(sp, getShipMaxHP(sp));
        if (hp <= 0) return false;
        if (hp <= getShipMaxHP(sp) * 0.5f) return true;
        for (Point tile : sp.getOccupiedTiles()) {
            if (!incomingHit[tile.y][tile.x]) return false;
        }
        return true;   // every tile hit, still above half: nowhere left to find it
    }

    /** True once every deployed ship is out of hull points. */
    public boolean isDefeated() {
        // Only what is on the board counts: ships in port keep their hull records now, and a
        // healthy one sitting there must not keep a sunk fleet in the fight.
        return deployedCount() > 0 && getAlivePlacements().isEmpty();
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
        if (statsShip != null) {
            // Keep an open stats board in step - a repair, a dive, a ship sold from under it.
            if (fleet.getPlacements().contains(statsShip)) statsCard.show(statsShip, this);
            else hideStats();
        }
        repaint();
    }

    private void paintCell(int r, int c) {
        JButton cell = cells[r][c];
        cell.setText("");

        if (board[r][c] != null) {
            // See-through: the ship's picture is drawn across its tiles by paintShips. Hits
            // are not painted on - a ship's condition is on its tooltip and stats board.
            cell.setOpaque(false);
            cell.setBorder(SHIP_BORDER);
            return;
        }

        // Open water: white, or dark grey where the enemy fired and missed.
        cell.setOpaque(true);
        cell.setBorder(WATER_BORDER);
        cell.setBackground(incomingShot[r][c] ? C_MISS : C_EMPTY);
    }

    /**
     * Draws each deployed ship's picture across its tiles, over the see-through cells.
     *
     * <p>A sunk ship is drawn as a grey wreck, listing and half see-through, and a submerged
     * submarine faintly, since it is under water. The listing is what tells a wreck apart
     * whatever the art's own colours - a dark submarine, a grey carrier. The ship the stats
     * board is showing, or the one last clicked, gets a gold outline, and the ship in hand is
     * previewed faintly where it would land.
     */
    private void paintShips(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        for (Ship_Placement sp : getDeployedPlacements()) {
            Rectangle area = footprint(sp.getOccupiedTiles());
            boolean sunk = shipHP.getOrDefault(sp, 1) <= 0;
            boolean submerged = sp.getShip() instanceof Submarine
                    && !((Submarine) sp.getShip()).isSurfaced();
            drawShip(g, sp.getShip().getImage(), area, sp.isHorizontal(), sunk,
                    sunk ? WRECK_ALPHA : submerged ? SUBMERGED_ALPHA : 1f);

            if (sp == selected || sp == statsShip) {
                g.setColor(C_OUTLINE);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(area.x + 1, area.y + 1, area.width - 3, area.height - 3, 12, 12);
            }
        }

        if (carried != null && carryOrigin != null) {
            drawShip(g, carried.getShip().getImage(),
                    footprint(tilesFor(carried, carryOrigin, carryHorizontal)),
                    carryHorizontal, false, CARRY_ALPHA);
        }
        g.dispose();
    }

    /**
     * Draws one ship's picture centred in {@code area}, turned upright for a vertical ship.
     * The art is scaled for the screen's own pixel density, so it stays sharp on a scaled
     * display.
     */
    private void drawShip(Graphics2D g, String image, Rectangle area, boolean horizontal,
                          boolean wreck, float alpha) {
        if (area == null) return;
        int pad = 3;
        int along = (horizontal ? area.width : area.height) - 2 * pad;
        int across = (horizontal ? area.height : area.width) - 2 * pad;
        if (along <= 0 || across <= 0) return;

        double density = g.getTransform().getScaleX();
        if (density <= 0) density = 1;
        java.awt.image.BufferedImage art = Icons.art(image,
                (int) (along * density), (int) (across * density), wreck);
        if (art == null) return;

        double w = art.getWidth() / density;
        double h = art.getHeight() / density;
        Graphics2D gg = (Graphics2D) g.create();
        gg.translate(area.getCenterX(), area.getCenterY());
        if (!horizontal) gg.rotate(Math.PI / 2);
        if (wreck) {
            gg.rotate(WRECK_LIST);
            gg.scale(0.9, 0.9);   // so the list stays inside its own tiles
        }
        if (alpha < 1f) gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gg.drawImage(art, (int) Math.round(-w / 2), (int) Math.round(-h / 2),
                (int) Math.round(w), (int) Math.round(h), null);
        gg.dispose();
    }

    /**
     * The grid area {@code tiles} cover, in the grid's coordinates. Worked out from the first
     * cell's size and spacing, so tiles hanging off the board (a ship being dragged in) still
     * get a place.
     */
    private Rectangle footprint(List<Point> tiles) {
        if (tiles == null || tiles.isEmpty()) return null;
        Rectangle first = cells[0][0].getBounds();
        int stepX = cells[0][1].getX() - first.x;
        int stepY = cells[1][0].getY() - first.y;
        Rectangle area = null;
        for (Point p : tiles) {
            Rectangle r = new Rectangle(first.x + p.x * stepX, first.y + p.y * stepY,
                    first.width, first.height);
            area = area == null ? r : area.union(r);
        }
        return area;
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

    /** Names each tile and, where a ship sits, its basics and a hull bar. */
    private void applyStealthTooltips() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                String coord = "" + (char) ('A' + c) + (r + 1);
                Ship_Placement sp = board[r][c];
                cells[r][c].setToolTipText(sp == null ? coord + " - open water"
                        : shipTooltip(sp, coord, null));
            }
        }
    }

    /**
     * The hover text for one of the player's ships: name, detection, size and cost, a small
     * hull bar, and a submarine's oxygen. Shared by the board and the roster.
     *
     * @param where shown after the name, e.g. the tile; may be null
     * @param note  an extra line at the end; may be null
     */
    public String shipTooltip(Ship_Placement sp, String where, String note) {
        Ships_Type ship = sp.getShip();
        int hp = currentHP(sp);
        int max = getShipMaxHP(sp);

        StringBuilder tip = new StringBuilder("<html><b>").append(ship.getName())
                .append("</b> (").append(ship.getHullCode()).append(")");
        if (where != null) tip.append(" &middot; ").append(where);
        tip.append("<br>Detection ").append(ship.getDetection())
                .append(" &middot; Size ").append(ship.getSize())
                .append(" &middot; Cost ").append(ship.getCost());

        // Two table cells make the bar: Swing's HTML draws their backgrounds reliably.
        int barWidth = 120;
        int filled = max <= 0 ? 0 : Math.round(barWidth * Math.max(0, Math.min(hp, max)) / (float) max);
        tip.append("<table cellpadding=0 cellspacing=0 border=0><tr>");
        if (filled > 0) {
            tip.append("<td bgcolor=").append(hex(ShipStatsCard.hullColour(hp, max)))
                    .append(" width=").append(filled).append(" height=7></td>");
        }
        if (filled < barWidth) {
            tip.append("<td bgcolor=#50555f width=").append(barWidth - filled)
                    .append(" height=7></td>");
        }
        tip.append("</tr></table>").append(hp <= 0 ? "Sunk" : hp + " / " + max + " HP");

        if (ship instanceof Submarine) {
            Submarine sub = (Submarine) ship;
            tip.append("<br>").append(sub.isSurfaced() ? "Surfaced" : "Submerged")
                    .append(", oxygen ").append(sub.getOxygen()).append("/").append(sub.getMaxOxygen());
        }
        if (note != null) tip.append("<br><i>").append(note).append("</i>");
        tip.append("<br><font color=#5a6272>")
                .append(battleStarted ? "Click" : "Right-click").append(" for full stats</font>");
        return tip.append("</html>").toString();
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // =====================================================================================
    // Ship stats board
    // =====================================================================================

    /**
     * Opens the stats board for {@code sp}, next to it.
     *
     * <p>The board floats on the window's layered pane, beside the ship's tiles when it is
     * deployed and beside {@code anchor} (the roster entry, say) when it is not, flipping
     * to the other side when it would run off the window.
     */
    public void showStats(Ship_Placement sp, Component anchor) {
        if (sp == null) {
            hideStats();
            return;
        }
        statsShip = sp;
        statsCard.show(sp, this);

        JRootPane root = getRootPane();
        if (root != null) {
            JLayeredPane layer = root.getLayeredPane();
            if (statsCard.getParent() != layer) layer.add(statsCard, JLayeredPane.POPUP_LAYER);

            Rectangle near = anchorBounds(sp, anchor, layer);
            Dimension d = statsCard.getPreferredSize();
            int x = near.x + near.width + 12;
            if (x + d.width > layer.getWidth() - 8) x = near.x - d.width - 12;
            x = Math.max(8, Math.min(x, layer.getWidth() - d.width - 8));
            int y = Math.max(8, Math.min(near.y - 8, layer.getHeight() - d.height - 8));
            statsCard.setBounds(x, y, d.width, d.height);
            layer.repaint();
        }
        statsCard.setVisible(true);
        refreshBoard();   // outlines the ship the board is about
    }

    /** Opens the stats board for {@code sp}, or closes it if it is already showing that ship. */
    public void toggleStats(Ship_Placement sp, Component anchor) {
        if (sp == null || sp == statsShip) hideStats();
        else showStats(sp, anchor);
    }

    /** Closes the stats board. */
    public void hideStats() {
        if (statsShip == null && !statsCard.isVisible()) return;
        statsShip = null;
        statsCard.setVisible(false);
        if (statsCard.getParent() != null) statsCard.getParent().repaint();
        refreshBoard();
    }

    /** The ship the stats board is showing, or null when it is closed. */
    public Ship_Placement getStatsShip() {
        return statsShip;
    }

    public ShipStatsCard getStatsCard() {
        return statsCard;
    }

    /** How many of {@code sp}'s sections the enemy has hit. */
    public int sectionsHit(Ship_Placement sp) {
        int n = 0;
        if (!isDeployed(sp)) return 0;
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x >= 0 && p.x < SIZE && p.y >= 0 && p.y < SIZE && incomingHit[p.y][p.x]) n++;
        }
        return n;
    }

    /** The screen area the stats board should sit beside, in {@code layer}'s coordinates. */
    private Rectangle anchorBounds(Ship_Placement sp, Component anchor, JLayeredPane layer) {
        Rectangle area = null;
        if (isDeployed(sp)) {
            for (Point p : sp.getOccupiedTiles()) {
                if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) continue;
                JButton cell = cells[p.y][p.x];
                if (cell.getParent() == null) continue;
                Rectangle r = SwingUtilities.convertRectangle(cell.getParent(), cell.getBounds(), layer);
                area = area == null ? r : area.union(r);
            }
        }
        if (area == null && anchor != null && anchor.getParent() != null) {
            area = SwingUtilities.convertRectangle(anchor.getParent(), anchor.getBounds(), layer);
        }
        return area == null ? new Rectangle(0, 0, 0, 0) : area;
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
        statsShip = null;
        statsCard.setVisible(false);
        fleet.reset();
        resetSubmarines();
        setBattleStarted(false);
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
     *
     * <p>A new stage opens a new deployment phase, so the fleet is unlocked again and the
     * Start button comes back.
     */
    public void prepareNextStage(boolean healFleet) {
        setBattleStarted(false);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                incomingShot[r][c] = false;
                incomingHit[r][c] = false;
            }
        }
        for (Ship_Placement sp : getDeployedPlacements()) {
            int max = scaledMaxHP(sp);
            shipMaxHP.put(sp, max);
            shipHP.put(sp, Math.max(1, Math.min(shipHP.getOrDefault(sp, max), max)));
        }
        if (healFleet) repairAll();
        refreshBoard();
    }

    public void setStatusText(String text) {
        status.setText(text);
    }
}
