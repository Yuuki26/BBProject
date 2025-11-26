package Ships;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;

public class TransferHandlerShip extends TransferHandler {
    private final Ship_Placement ship;

    public TransferHandlerShip(Ship_Placement ship) {
        super("icon");
        this.ship = ship;

        // Try to set a drag image so the user sees the ship while dragging.
        // This is optional and will silently fail if the resource can't be loaded.
        try {
            String path = ship.getShip().getImage();
            java.net.URL url = getClass().getResource(path);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage();

                // scale to a reasonable drag preview size
                int max = 128;
                int w = img.getWidth(null);
                int h = img.getHeight(null);
                if (w <= 0 || h <= 0) {
                    // fallback size
                    w = max;
                    h = max;
                } else {
                    float scale = Math.min((float) max / w, (float) max / h);
                    w = Math.max(1, Math.round(w * scale));
                    h = Math.max(1, Math.round(h * scale));
                }
                Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                setDragImage(toBufferedImage(scaled));
            }
        } catch (Exception ignored) {
            // don't fail construction if drag image can't be created
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new DataHandler(ship);
    }

    @Override
    public int getSourceActions(JComponent c) {
        // Use MOVE so the intent is to move the ship (not copy).
        return TransferHandler.MOVE;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // Keep this intentionally minimal:
        // cleanup (removing icons / occupied flags / labels) is handled by the drop target's importData.
        // If you prefer automatic removal of the source component on successful MOVE,
        // implement that logic here (careful to coordinate with importData to avoid duplicates).
    }

    public Ship_Placement getShip() {
        return ship;
    }

    // Utility: convert Image to BufferedImage for setDragImage
    private static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        int w = Math.max(1, img.getWidth(null));
        int h = Math.max(1, img.getHeight(null));
        BufferedImage bimage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bimage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return bimage;
    }
}
