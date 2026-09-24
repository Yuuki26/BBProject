package Ships;

import com.bb.GameLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The undeployed-ship roster beside the player's board.
 *
 * <p>Press a ship here and move onto the board to deploy it; release off the board to send
 * it back to port. Rotation happens with <kbd>R</kbd> while the ship is in hand, not here -
 * see {@link GameLayout#rotateSelected()}.
 */
public class Fleet_Layout extends JPanel {

    /** Pixels per hull tile in the roster preview. */
    private static final int TILE = 34;

    private final DefaultFleet fleet;
    private final GameLayout game;

    /** Ships currently shown here, parallel to the label components. */
    private final List<Ship_Placement> shown = new ArrayList<>();

    private Ship_Placement selected;

    public Fleet_Layout(DefaultFleet fleet, GameLayout game) {
        this.fleet = fleet;
        this.game = game;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);

        rebuild();

        // Dropping a carried ship anywhere off the board sends it back here; GameLayout
        // handles that, so this panel needs no drop target of its own.
    }

    /** Rebuilds the roster from scratch, showing every ship that is not on the board. */
    public final void rebuild() {
        removeAll();
        shown.clear();
        for (Ship_Placement sp : fleet.getPlacements()) {
            if (game == null || !game.isDeployed(sp)) addShipLabel(sp);
        }
        revalidate();
        repaint();
    }

    /** Redraws the existing entries, picking up orientation and selection changes. */
    public void refresh() {
        rebuild();
    }

    /** Highlights {@code sp} if it is in the roster; clears the highlight otherwise. */
    public void setSelected(Ship_Placement sp) {
        this.selected = sp;
        for (Component comp : getComponents()) {
            if (comp instanceof JLabel) {
                JLabel lbl = (JLabel) comp;
                Object owner = lbl.getClientProperty("ship");
                boolean isSel = owner == sp && sp != null;
                lbl.setBorder(isSel
                        ? BorderFactory.createLineBorder(new Color(120, 200, 255), 3)
                        : BorderFactory.createEmptyBorder(3, 3, 3, 3));
            }
        }
        repaint();
    }

    private void addShipLabel(Ship_Placement sp) {
        int size = sp.getShip().getSize();
        int width = sp.isHorizontal() ? TILE * size : TILE;
        int height = sp.isHorizontal() ? TILE : TILE * size;

        JLabel label = new JLabel();
        label.putClientProperty("ship", sp);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        label.setHorizontalAlignment(SwingConstants.CENTER);

        ImageIcon icon = loadScaled(sp.getShip().getImage(), width, height);
        if (icon != null) {
            label.setIcon(icon);
        } else {
            // Missing art should not make the ship unpickable.
            label.setText(sp.getShip().getName());
            label.setForeground(Color.WHITE);
            label.setOpaque(true);
            label.setBackground(new Color(70, 110, 170));
            label.setPreferredSize(new Dimension(width, height));
        }

        boolean affordable = game == null || game.canAfford(sp);
        label.setToolTipText(sp.getShip().getName()
                + " (" + sp.getShip().getHullCode() + ")"
                + " - cost " + sp.getShip().getCost()
                + ", detection " + sp.getShip().getDetection()
                + (affordable ? ", drag onto the board and press R to rotate"
                              : " - over budget for this stage"));


        // Picking a ship up here starts the same carry the board uses. Press, move onto the
        // board, press R to turn it, release to place - all without Swing's drag-and-drop,
        // whose native loop never delivers the keystroke.
        MouseAdapter carry = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (game == null || !SwingUtilities.isLeftMouseButton(e)) return;
                game.setSelected(sp);
                game.beginCarry(sp, e.getComponent(), e.getPoint());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (game != null) game.updateCarry(e.getComponent(), e.getPoint());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (game == null || !SwingUtilities.isLeftMouseButton(e)) return;
                game.endCarry(e.getComponent(), e.getPoint());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (game != null) game.setHovered(sp);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (game != null) game.setHovered(null);
            }
        };
        label.addMouseListener(carry);
        label.addMouseMotionListener(carry);

        // A caption under each hull, so cost is visible without hunting for a tooltip.
        JLabel caption = new JLabel(
                sp.getShip().getName() + "  " + sp.getShip().getCost(), SwingConstants.CENTER);
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        caption.setFont(caption.getFont().deriveFont(Font.BOLD, 11f));
        caption.setForeground(affordable ? Color.WHITE : new Color(230, 130, 120));
        caption.setToolTipText(label.getToolTipText());

        add(Box.createVerticalStrut(8));
        add(label);
        add(caption);
        shown.add(sp);

        if (sp == selected) setSelected(sp);
    }

    /** Puts a ship back in the roster, unless it is already listed. */
    public void addShipBack(Ship_Placement sp) {
        if (shown.contains(sp)) return;
        rebuild();
    }

    /**
     * Drops a ship from the roster, typically because it was just deployed.
     *
     * <p>Rebuilds rather than unhooking one component: each entry is a group (spacer, image,
     * caption), and picking a single label out of the middle would leave its caption and
     * spacer orphaned. A rebuild also refreshes which remaining hulls are still affordable,
     * which has just changed by definition.
     */
    public void removeShip(Ship_Placement sp) {
        if (!shown.contains(sp)) return;
        rebuild();
    }

    private ImageIcon loadScaled(String path, int width, int height) {
        java.net.URL url = getClass().getResource(path);
        if (url == null) {
            System.err.println("Ship image not found on the classpath: " + path);
            return null;
        }
        Image scaled = new ImageIcon(url).getImage()
                .getScaledInstance(Math.max(1, width), Math.max(1, height), Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }
}
