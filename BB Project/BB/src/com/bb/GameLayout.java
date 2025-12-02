package com.bb;

import Ships.DefaultFleet;
import Ships.Fleet_Layout;
import Ships.Ship_Placement;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.TransferHandler;

public class GameLayout extends JPanel {
    private final Mode mode;
    private GameLayout enemyTargetBoard;
    private String lastGhostKey;
    private final int SIZE;
    private Fleet_Layout fleet_layout;
    private DefaultFleet fleet;
    private boolean[][] occupied;
    private final Map<Ship_Placement, List<Point>> placedMap;
    private boolean confirmLocked;
    private final int maxShots;
    private List<Point> selectedShots;
    private boolean placementLocked;
    private final Color shotColorMiss;
    private final Color shotColorHit;
    JPanel grid;
    private boolean[][] fired;

    public void setEnemyTargetBoard(GameLayout enemyTargetBoard) {
        this.enemyTargetBoard = enemyTargetBoard;
    }

    public GameLayout(Frame frame, Mode mode) {
        this.lastGhostKey = null;
        this.SIZE = 8;
        this.occupied = new boolean[8][8];
        this.placedMap = new HashMap();
        this.confirmLocked = false;
        this.maxShots = 3;
        this.selectedShots = new ArrayList();
        this.placementLocked = false;
        this.shotColorMiss = Color.RED;
        this.shotColorHit = Color.YELLOW;
        this.grid = new JPanel(new GridLayout(8, 8, 2, 2)) {
            public Dimension getPreferredSize() {
                return new Dimension(2560, 1280);
            }
        };
        this.fired = new boolean[8][8];
        this.mode = mode;
        this.setLayout(new BorderLayout());
        this.add(this.createBoardPanel(), "Center");
        if (mode == GameLayout.Mode.PLAYER) {
            this.fleet = new DefaultFleet();
            this.fleet_layout = new Fleet_Layout(this.fleet, this);
            this.add(this.fleet_layout, "East");
            JPanel bottom = new JPanel();
            JButton lockButton = new JButton("Lock ships");
            bottom.add(lockButton);
            this.add(bottom, "South");
            lockButton.addActionListener((e) -> this.lockPlacement());
        } else {
            this.fleet = null;
            this.fleet_layout = null;
            JPanel bottom = new JPanel();
            JButton confirmButton = new JButton("Confirm shots");
            bottom.add(confirmButton);
            this.add(bottom, "South");
            confirmButton.addActionListener((e) -> this.confirmShots());
        }

    }

    public GameLayout(Frame frame) {
        this(frame, GameLayout.Mode.OPPONENT);
    }

    public boolean isPlacementLocked() {
        return this.placementLocked;
    }

    public void setPlacementLocked(boolean placementLocked) {
        this.placementLocked = placementLocked;
    }

    private void lockPlacement() {
        if (!this.placementLocked) {
            this.placementLocked = true;

            for(int i = 0; i < this.grid.getComponentCount(); ++i) {
                JButton cell = (JButton)this.grid.getComponent(i);
                Object ship = cell.getClientProperty("ship");
                if (ship instanceof Ship_Placement) {
                    cell.setBackground(Color.BLACK);
                    cell.setTransferHandler((TransferHandler)null);
                }
            }

        }
    }

    private JPanel createBoardPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel boardPanel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(1, 9));
        top.add(new JLabel());

        for(int c = 0; c < 8; ++c) {
            JLabel lbl = new JLabel(String.valueOf((char)(65 + c)), 0);
            lbl.setFont(lbl.getFont().deriveFont(1, 14.0F));
            top.add(lbl);
        }

        outer.add(top, "North");
        JPanel center = new JPanel(new BorderLayout());
        JPanel leftLabels = new JPanel(new GridLayout(8, 1));

        for(int r = 1; r <= 8; ++r) {
            JLabel lbl = new JLabel(String.valueOf(r), 0);
            lbl.setFont(lbl.getFont().deriveFont(1, 14.0F));
            leftLabels.add(lbl);
        }

        center.add(leftLabels, "West");
        CellTransferHandler cellHandler = new CellTransferHandler(this);

        for(int r = 0; r < 8; ++r) {
            for(int c = 0; c < 8; ++c) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));
                String coord = "" + (char)(65 + c) + (r + 1);
                cell.putClientProperty("coord", coord);
                if (this.mode == GameLayout.Mode.PLAYER) {
                    cell.setTransferHandler(cellHandler);
                    cell.addMouseMotionListener(new MouseMotionAdapter() {
                        public void mouseDragged(MouseEvent e) {
                            if (!GameLayout.this.placementLocked) {
                                JComponent comp = (JComponent)e.getSource();
                                if (comp.getClientProperty("ship") instanceof Ship_Placement) {
                                    TransferHandler handler = comp.getTransferHandler();
                                    if (handler != null) {
                                        handler.exportAsDrag(comp, e, 2);
                                    }
                                }

                            }
                        }
                    });
                }

                cell.addActionListener((e) -> this.onCellClicked(r, c));
                this.grid.add(cell);
            }
        }

        center.add(this.grid, "Center");
        boardPanel.add(center, "Center");
        JPanel wrapper = new JPanel(new GridBagLayout());
        Dimension fixed = new Dimension(800, 800);
        boardPanel.setPreferredSize(fixed);
        boardPanel.setMinimumSize(fixed);
        boardPanel.setMaximumSize(fixed);
        wrapper.add(boardPanel);
        outer.add(wrapper, "Center");
        new DropTarget(this.grid, new DropTargetListener() {
            public void dragEnter(DropTargetDragEvent dtde) {
            }

            public void dragOver(DropTargetDragEvent dtde) {
            }

            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            public void drop(DropTargetDropEvent dtde) {
                GameLayout.this.clearGhost();
                GameLayout.this.lastGhostKey = null;
            }

            public void dragExit(DropTargetEvent dte) {
                GameLayout.this.clearGhost();
                GameLayout.this.lastGhostKey = null;
            }
        });
        return outer;
    }

    private void onCellClicked(int row, int col) {
        if (this.mode == GameLayout.Mode.OPPONENT) {
            if (!this.confirmLocked) {
                if (!this.fired[row][col]) {
                    if (this.selectedShots.size() < 3) {
                        for(Point p : this.selectedShots) {
                            if (p.x == col && p.y == row) {
                                return;
                            }
                        }

                        JButton cell = (JButton)this.grid.getComponent(row * 8 + col);
                        cell.setBackground(Color.LIGHT_GRAY);
                        this.selectedShots.add(new Point(col, row));
                    }
                }
            }
        }
    }

    private void confirmShots() {
        if (!this.selectedShots.isEmpty()) {
            for(Point p : this.selectedShots) {
                int col = p.x;
                int row = p.y;
                this.fired[row][col] = true;
                JButton cell = (JButton)this.grid.getComponent(row * 8 + col);
                boolean isHit = this.occupied[row][col];
                cell.setBackground(isHit ? this.shotColorHit : this.shotColorMiss);
            }

            this.confirmLocked = true;
            this.enemyShootBack();
            this.selectedShots.clear();
            this.confirmLocked = false;
        }
    }

    private void enemyShootBack() {
        GameLayout target = this.enemyTargetBoard != null ? this.enemyTargetBoard : this;
        List<Point> candidates = new ArrayList();

        for(int r = 0; r < 8; ++r) {
            for(int c = 0; c < 8; ++c) {
                if (!target.fired[r][c]) {
                    candidates.add(new Point(c, r));
                }
            }
        }

        if (!candidates.isEmpty()) {
            Collections.shuffle(candidates);
            int shots = Math.min(3, candidates.size());

            for(int i = 0; i < shots; ++i) {
                Point p = (Point)candidates.get(i);
                int col = p.x;
                int row = p.y;
                target.fired[row][col] = true;
                JButton cell = (JButton)target.grid.getComponent(row * 8 + col);
                boolean isHit = target.occupied[row][col];
                cell.setBackground(isHit ? this.shotColorHit : this.shotColorMiss);
            }

        }
    }

    private boolean validatePlacementInternal(Ship_Placement sp) {
        for(Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= 8 || p.y < 0 || p.y >= 8) {
                return false;
            }

            if (this.occupied[p.y][p.x]) {
                return false;
            }
        }

        return true;
    }

    public boolean validatePlacement(Ship_Placement sp) {
        return !this.placementLocked && this.mode == GameLayout.Mode.PLAYER ? this.validatePlacementInternal(sp) : false;
    }

    public void commitPlacement(Ship_Placement sp) {
        if (!this.placementLocked && this.mode == GameLayout.Mode.PLAYER) {
            this.paintShip(sp, false);
            if (this.fleet_layout != null) {
                this.fleet_layout.removeShip(sp);
                this.fleet_layout.revalidate();
                this.fleet_layout.repaint();
            }

        }
    }

    public void randomizeFleet(DefaultFleet templateFleet) {
        if (this.mode == GameLayout.Mode.OPPONENT) {
            Random rand = new Random();

            for(Ship_Placement baseSp : templateFleet.getPlacements()) {
                boolean placed = false;

                while(!placed) {
                    boolean horizontal = rand.nextBoolean();
                    int size = baseSp.getShip().getSize();
                    int maxX = horizontal ? 8 - size : 7;
                    int maxY = horizontal ? 7 : 8 - size;
                    int x = rand.nextInt(maxX + 1);
                    int y = rand.nextInt(maxY + 1);
                    Ship_Placement sp = new Ship_Placement(baseSp.getShip(), new Point(x, y), horizontal);
                    if (this.validatePlacementInternal(sp)) {
                        List<Point> occ = sp.getOccupiedTiles();

                        for(Point p : occ) {
                            this.occupied[p.y][p.x] = true;
                        }

                        this.placedMap.put(sp, new ArrayList(occ));
                        placed = true;
                    }
                }
            }

        }
    }

    public void paintShip(Ship_Placement sp, boolean ghost) {
        int tileSize = 150;
        if (!ghost) {
            List<Point> prev = (List)this.placedMap.remove(sp);
            if (prev != null) {
                for(Point p : prev) {
                    if (p.x >= 0 && p.x < 8 && p.y >= 0 && p.y < 8) {
                        int idx = p.y * 8 + p.x;
                        JButton cell = (JButton)this.grid.getComponent(idx);
                        if (cell.getClientProperty("ship") == sp) {
                            cell.setIcon((Icon)null);
                            cell.setBackground(Color.WHITE);
                            cell.putClientProperty("ghost", (Object)null);
                            cell.putClientProperty("ship", (Object)null);
                            this.occupied[p.y][p.x] = false;
                        }
                    }
                }
            }
        }

        for(Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= 8 || p.y < 0 || p.y >= 8) {
                return;
            }

            if (!ghost && this.occupied[p.y][p.x]) {
                return;
            }
        }

        ImageIcon fullIcon = new ImageIcon(this.getClass().getResource(sp.getShip().getImage()));
        Image fullImage = fullIcon.getImage();
        int tiles = sp.getShip().getSize();
        int shipWidth = sp.isHorizontal() ? tileSize * tiles : tileSize;
        int shipHeight = sp.isHorizontal() ? tileSize : tileSize * tiles;
        BufferedImage shipCanvas = new BufferedImage(shipWidth, shipHeight, 2);
        Graphics2D g = shipCanvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(fullImage, 0, 0, shipWidth, shipHeight, (ImageObserver)null);
        g.dispose();
        List<Point> tilesPoints = sp.getOccupiedTiles();

        for(int i = 0; i < tiles; ++i) {
            int sx = sp.isHorizontal() ? i * tileSize : 0;
            int sy = sp.isHorizontal() ? 0 : i * tileSize;
            BufferedImage tileImg = shipCanvas.getSubimage(sx, sy, tileSize, tileSize);
            Point p = (Point)tilesPoints.get(i);
            int index = p.y * 8 + p.x;
            JButton cell = (JButton)this.grid.getComponent(index);
            cell.setIcon(new ImageIcon(tileImg));
            cell.setBackground(ghost ? Color.LIGHT_GRAY : Color.CYAN);
            cell.putClientProperty("ghost", (Object)null);
            if (!ghost) {
                this.occupied[p.y][p.x] = true;
                cell.putClientProperty("ship", sp);
            }
        }

        if (!ghost) {
            this.placedMap.put(sp, new ArrayList(sp.getOccupiedTiles()));
            this.clearGhost();
            this.lastGhostKey = null;
        }

    }

    public void paintGhost(Ship_Placement sp) {
        try {
            String shipId = Integer.toHexString(System.identityHashCode(sp));
            Point origin = sp.getOrigin();
            String coord;
            if (origin != null) {
                coord = origin.x + ":" + origin.y;
            } else {
                coord = "null";
            }

            String key = shipId + ":" + coord + ":" + sp.isHorizontal();
            if (key.equals(this.lastGhostKey)) {
                return;
            }

            this.lastGhostKey = key;
        } catch (Exception var17) {
        }

        this.clearGhost();
        int tileSize = 150;
        ImageIcon fullIcon = new ImageIcon(this.getClass().getResource(sp.getShip().getImage()));
        Image fullImage = fullIcon.getImage();
        int tiles = sp.getShip().getSize();
        int shipWidth = sp.isHorizontal() ? tileSize * tiles : tileSize;
        int shipHeight = sp.isHorizontal() ? tileSize : tileSize * tiles;
        BufferedImage shipCanvas = new BufferedImage(shipWidth, shipHeight, 2);
        Graphics2D g = shipCanvas.createGraphics();
        g.drawImage(fullImage, 0, 0, shipWidth, shipHeight, (ImageObserver)null);
        g.setComposite(AlphaComposite.getInstance(3, 0.4F));
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, shipWidth, shipHeight);
        g.dispose();

        for(int i = 0; i < tiles; ++i) {
            Point p = (Point)sp.getOccupiedTiles().get(i);
            if (p.x >= 0 && p.x < 8 && p.y >= 0 && p.y < 8 && !this.occupied[p.y][p.x]) {
                int sx = sp.isHorizontal() ? i * tileSize : 0;
                int sy = sp.isHorizontal() ? 0 : i * tileSize;
                BufferedImage tileImg = shipCanvas.getSubimage(sx, sy, tileSize, tileSize);
                int index = p.y * 8 + p.x;
                JButton cell = (JButton)this.grid.getComponent(index);
                cell.setIcon(new ImageIcon(tileImg));
                cell.setBackground(Color.LIGHT_GRAY);
                cell.putClientProperty("ghost", Boolean.TRUE);
            }
        }

    }

    public void clearGhost() {
        for(int i = 0; i < this.grid.getComponentCount(); ++i) {
            JButton cell = (JButton)this.grid.getComponent(i);
            if (Boolean.TRUE.equals(cell.getClientProperty("ghost"))) {
                cell.setIcon((Icon)null);
                cell.setBackground(Color.WHITE);
                cell.putClientProperty("ghost", (Object)null);
            }
        }

        this.lastGhostKey = null;
    }

    public void removeShipFromBoard(Ship_Placement sp) {
        List<Point> prev = (List)this.placedMap.remove(sp);
        if (prev == null) {
            for(int i = 0; i < this.grid.getComponentCount(); ++i) {
                JButton cell = (JButton)this.grid.getComponent(i);
                if (cell.getClientProperty("ship") == sp) {
                    cell.setIcon((Icon)null);
                    cell.setBackground(Color.WHITE);
                    cell.putClientProperty("ghost", (Object)null);
                    cell.putClientProperty("ship", (Object)null);
                    int r = i / 8;
                    int c = i % 8;
                    this.occupied[r][c] = false;
                }
            }

        } else {
            for(Point p : prev) {
                if (p.x >= 0 && p.x < 8 && p.y >= 0 && p.y < 8) {
                    int index = p.y * 8 + p.x;
                    JButton cell = (JButton)this.grid.getComponent(index);
                    if (cell.getClientProperty("ship") == sp) {
                        cell.setIcon((Icon)null);
                        cell.setBackground(Color.WHITE);
                        cell.putClientProperty("ghost", (Object)null);
                        cell.putClientProperty("ship", (Object)null);
                        this.occupied[p.y][p.x] = false;
                    }
                }
            }

        }
    }

    public static enum Mode {
        PLAYER,
        OPPONENT;
    }
}
