package com.bb;

import Ships.*;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class GameLayout extends JPanel {

    private static final int SIZE = 8;

    private Fleet_Layout fleet_layout;
    private DefaultFleet fleet;
    private boolean[][] occupied = new boolean[SIZE][SIZE];
    private final Map<Ship_Placement, List<Point>> placedMap = new HashMap<>();

    private boolean placementLocked = false;
    JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(2560, 1280);
        }
    };
    private final Color shotColorMiss = Color.RED;
    private final Color shotColorHit  = Color.YELLOW;
    private boolean[][] fired = new boolean[SIZE][SIZE];

    private String lastGhostKey = null;

    public GameLayout(Frame frame) {
        setLayout(new BorderLayout());
        add(createBoardPanel(), BorderLayout.CENTER);

        this.fleet = new DefaultFleet();
        this.fleet_layout = new Fleet_Layout(this.fleet, this);
        add(this.fleet_layout, BorderLayout.EAST);

        JPanel bottom = new JPanel();
        JButton lockButton = new JButton("Lock ships");
        bottom.add(lockButton);
        add(bottom, BorderLayout.SOUTH);

        lockButton.addActionListener(e -> lockPlacement());
    }

    public boolean isPlacementLocked() {
        return placementLocked;
    }

    public void setPlacementLocked(boolean placementLocked) {
        this.placementLocked = placementLocked;
    }

    private void lockPlacement() {
        if (placementLocked) return;

        placementLocked = true;
        for (int i = 0; i < grid.getComponentCount(); i++) {
            JButton cell = (JButton) grid.getComponent(i);
            Object ship = cell.getClientProperty("ship");
            if (ship instanceof Ship_Placement) {
                cell.setBackground(Color.BLACK);
                cell.setTransferHandler(null);
            }
        }
    }

    private JPanel createBoardPanel() {
        JPanel outer = new JPanel(new BorderLayout());
        JPanel boardPanel = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new GridLayout(1, SIZE + 1));
        top.add(new JLabel());
        for (int c = 0; c < SIZE; c++) {
            JLabel lbl = new JLabel(String.valueOf((char) ('A' + c)), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            top.add(lbl);
        }
        outer.add(top, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        JPanel leftLabels = new JPanel(new GridLayout(SIZE, 1));
        for (int r = 1; r <= SIZE; r++) {
            JLabel lbl = new JLabel(String.valueOf(r), SwingConstants.CENTER);
            lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 14f));
            leftLabels.add(lbl);
        }
        center.add(leftLabels, BorderLayout.WEST);
        CellTransferHandler cellHandler = new CellTransferHandler(this);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));

                String coord = "" + (char) ('A' + c) + (r + 1);
                cell.putClientProperty("coord", coord);

                cell.setTransferHandler(cellHandler);
                cell.addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        if (placementLocked) return;

                        JComponent comp = (JComponent) e.getSource();
                        if (comp.getClientProperty("ship") instanceof Ship_Placement) {
                            TransferHandler handler = comp.getTransferHandler();
                            if (handler != null) {
                                handler.exportAsDrag(comp, e, TransferHandler.MOVE);
                            }
                        }
                    }
                });

                grid.add(cell);
            }
        }

        center.add(grid, BorderLayout.CENTER);
        boardPanel.add(center, BorderLayout.CENTER);

        JPanel wrapper = new JPanel(new GridBagLayout());
        Dimension fixed = new Dimension(800, 800);
        boardPanel.setPreferredSize(fixed);
        boardPanel.setMinimumSize(fixed);
        boardPanel.setMaximumSize(fixed);
        wrapper.add(boardPanel);

        outer.add(wrapper, BorderLayout.CENTER);
        new DropTarget(grid, new java.awt.dnd.DropTargetListener() {
            @Override public void dragEnter(java.awt.dnd.DropTargetDragEvent dtde) { }
            @Override public void dragOver(java.awt.dnd.DropTargetDragEvent dtde) { }
            @Override public void dropActionChanged(java.awt.dnd.DropTargetDragEvent dtde) { }
            @Override
            public void drop(java.awt.dnd.DropTargetDropEvent dtde) {
                clearGhost();
                lastGhostKey = null;
            }
            @Override
            public void dragExit(java.awt.dnd.DropTargetEvent dte) {
                clearGhost();
                lastGhostKey = null;
            }
        });

        return outer;
    }

    private boolean validatePlacementInternal(Ship_Placement sp) {
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) {
                return false;
            }
            if (occupied[p.y][p.x]) {
                return false;
            }
        }
        return true;
    }

    public boolean validatePlacement(Ship_Placement sp) {
        if (placementLocked) return false;
        return validatePlacementInternal(sp);
    }

    public void commitPlacement(Ship_Placement sp) {
        if (placementLocked) return;
        paintShip(sp, false);
        if (fleet_layout != null) {
            fleet_layout.removeShip(sp);
            fleet_layout.revalidate();
            fleet_layout.repaint();
        }
    }

    public void paintShip(Ship_Placement sp, boolean ghost) {
        int tileSize = 150;

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
                }
            }
        }

        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) {
                return;
            }
            if (!ghost && occupied[p.y][p.x]) {
                return;
            }
        }

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

        if (!ghost) {
            placedMap.put(sp, new ArrayList<>(sp.getOccupiedTiles()));
            clearGhost();
            lastGhostKey = null;
        }
    }

    public void paintGhost(Ship_Placement sp) {
        try {
            String coord;
            String shipId = Integer.toHexString(System.identityHashCode(sp));
            Point origin = sp.getOrigin();
            if (origin != null) {
                coord = origin.x + ":" + origin.y;
            } else {
                coord = "null";
            }
            String key = shipId + ":" + coord + ":" + sp.isHorizontal();
            if (key.equals(lastGhostKey)) {
                return;
            }
            lastGhostKey = key;
        } catch (Exception ignored) { }

        clearGhost();

        int tileSize = 150;
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
            if (occupied[p.y][p.x]) continue;

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

    public void removeShipFromBoard(Ship_Placement sp) {
        List<Point> prev = placedMap.remove(sp);
        if (prev == null) {
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

    public void applyShots(List<Point> shots) {
        if (shots == null) return;

        for (Point p : shots) {
            int x = p.x;
            int y = p.y;
            if (x < 0 || x >= SIZE || y < 0 || y >= SIZE) continue;
            if (fired[y][x]) continue;

            fired[y][x] = true;

            JButton cell = (JButton) grid.getComponent(y * SIZE + x);
            boolean isHit = occupied[y][x];

            cell.setBackground(isHit ? shotColorHit : shotColorMiss);
        }
    }
}
