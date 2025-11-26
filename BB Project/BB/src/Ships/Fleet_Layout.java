package Ships;

import com.bb.GameLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Fleet_Layout extends JPanel {
    private DefaultFleet fleet;
    private GameLayout game; // reference to GameLayout for callbacks

    public Fleet_Layout(DefaultFleet fleet, GameLayout game) {
        this.fleet = fleet;
        this.game = game;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(400, 160));

        // initial population
        for (Ship_Placement sp : fleet.getPlacements()) {
            addShipLabel(sp);
        }

        // Accept drops back into the fleet
        setTransferHandler(new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataHandler.SHIP_FLAVOR);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) return false;
                try {
                    Ship_Placement sp = (Ship_Placement) support.getTransferable()
                            .getTransferData(DataHandler.SHIP_FLAVOR);

                    // Remove from board (clears icons and occupied flags)
                    if (game != null) {
                        game.removeShipFromBoard(sp);
                    }

                    // Re-add to fleet UI
                    addShipBack(sp);
                    revalidate();
                    repaint();
                    return true;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }
            }
        });
    }

    // helper to create and add a label for a ship placement
    private void addShipLabel(Ship_Placement sp) {
        int tiles = 80;
        int width = tiles * sp.getShip().getSize();
        int height = Math.max(40, tiles * sp.getShip().getSize() / 2);

        String imagePath = sp.getShip().getImage();
        java.net.URL url = getClass().getResource(imagePath);
        ImageIcon image = new ImageIcon(url);
        Image scaled = image.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        JLabel label = new JLabel(new ImageIcon(scaled));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        // attach transfer handler and mouse press to start drag (MOVE)
        TransferHandlerShip handler = new TransferHandlerShip(sp);
        label.setTransferHandler(handler);

        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                label.requestFocusInWindow();
                JComponent comp = (JComponent) e.getSource();
                TransferHandler h = comp.getTransferHandler();
                h.exportAsDrag(comp, e, TransferHandler.MOVE); // use MOVE
            }
        });

        add(Box.createVerticalStrut(8));
        add(label);
    }

    // called when a ship is returned to the roster
    public void addShipBack(Ship_Placement sp) {
        // Avoid adding duplicates: check if a label for this ship already exists
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                TransferHandler th = ((JLabel) comp).getTransferHandler();
                if (th instanceof TransferHandlerShip) {
                    if (((TransferHandlerShip) th).getShip() == sp) {
                        return; // already present
                    }
                }
            }
        }
        addShipLabel(sp);
    }

    public void removeShip(Ship_Placement sp) {
        // find the JLabel associated with this placement and remove it
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                TransferHandler th = ((JLabel) comp).getTransferHandler();
                if (th instanceof TransferHandlerShip) {
                    if (((TransferHandlerShip) th).getShip() == sp) {
                        remove(comp);
                        break;
                    }
                }
            }
        }
    }
}
