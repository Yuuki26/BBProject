package com.bb;

import Ships.DataHandler;
import Ships.DefaultFleet;
import Ships.Fleet_Layout;
import Ships.Ship_Placement;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class GameLayout extends JPanel {
    private final int SIZE = 8;
    private Fleet_Layout fleet_layout;
    private DefaultFleet fleet;
    private boolean[][] occupied=new boolean[SIZE][SIZE];
    JPanel grid = new JPanel(new GridLayout(SIZE, SIZE, 2, 2)) {
        @Override
        public Dimension getPreferredSize() {

            return new Dimension(800, 800);
        }
    };


    public GameLayout(Frame frame) {
        setLayout(new BorderLayout());
        add(createBoardPanel(), BorderLayout.CENTER);
        fleet = new DefaultFleet();
        fleet_layout = new Fleet_Layout(fleet);
        add(fleet_layout, BorderLayout.EAST);

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

        // Grid of buttons (cells)

        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                JButton cell = new JButton();
                cell.setFocusable(false);
                cell.setBackground(Color.WHITE);
                cell.setBorder(BorderFactory.createLineBorder(new Color(180, 180, 180)));

                String coord = "" + (char) ('A' + c) + (r + 1);
                cell.putClientProperty("coord", coord);

                // Accept ship drops
                cell.setTransferHandler(new TransferHandler() {
                    @Override
                    public boolean canImport(TransferSupport support) {
                        return support.isDataFlavorSupported(DataHandler.SHIP_FLAVOR);
                    }

                    @Override
                    public boolean importData(TransferSupport support) {
                        try {
                            Ship_Placement sp = (Ship_Placement) support.getTransferable()
                                    .getTransferData(DataHandler.SHIP_FLAVOR);

                            // Get cell coordinates
                            String coord = (String) ((JComponent) support.getComponent()).getClientProperty("coord");
                            int col = coord.charAt(0) - 'A';
                            int row = Integer.parseInt(coord.substring(1)) - 1;

                            // Update ship origin
                            sp.setOrigin(new Point(col, row));
                            //collision checking


                            // Paint ship across its occupied tiles
                            paintShip(sp,true);

                            return true;
                        } catch (Exception e) {
                            e.printStackTrace();
                            return false;
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

            return wrapper;


        }
    private void paintShip(Ship_Placement sp,boolean ghost) {
        int tileSize = 100;

        // Validate placement first
        for (Point p : sp.getOccupiedTiles()) {
            if (p.x < 0 || p.x >= SIZE || p.y < 0 || p.y >= SIZE) {
                JOptionPane.showMessageDialog(this, "Ship out of bounds!");
                return;
            }
            if (!ghost && occupied[p.y][p.x]) {
                JOptionPane.showMessageDialog(this, "Collision detected!");
                return;
            }
        }

        // Load full image
        ImageIcon fullIcon = new ImageIcon(getClass().getResource(sp.getShip().getImage()));
        Image fullImage = fullIcon.getImage();

        // Scale ship to exact tile dimensions
        int tiles = sp.getShip().getSize();
        int shipWidth = sp.isHorizontal() ? tileSize * tiles : tileSize;
        int shipHeight = sp.isHorizontal() ? tileSize : tileSize * tiles;

        BufferedImage shipCanvas = new BufferedImage(shipWidth, shipHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = shipCanvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(fullImage, 0, 0, shipWidth, shipHeight, null);

        // If ghost preview, draw semi-transparent
        if (ghost) {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f));
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, shipWidth, shipHeight);
        }
        g.dispose();

        // Slice into tiles
        java.util.List<Point> tilesPoints = sp.getOccupiedTiles();
        for (int i = 0; i < tiles; i++) {
            int sx = sp.isHorizontal() ? i * tileSize : 0;
            int sy = sp.isHorizontal() ? 0 : i * tileSize;
            BufferedImage tileImg = shipCanvas.getSubimage(sx, sy, tileSize, tileSize);

            Point p = tilesPoints.get(i);
            int index = p.y * SIZE + p.x;
            JButton cell = (JButton) grid.getComponent(index);

            cell.setIcon(new ImageIcon(tileImg));
            cell.setBackground(ghost ? Color.LIGHT_GRAY : Color.CYAN);

            if (!ghost) {
                occupied[p.y][p.x] = true; // mark only if committed
            }
        }


    }
}

