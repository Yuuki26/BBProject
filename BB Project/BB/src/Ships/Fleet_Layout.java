package Ships;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Fleet_Layout extends JPanel {
    private DefaultFleet fleet;


    public Fleet_Layout(DefaultFleet fleet) {
        this.fleet=fleet;

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(400,450));
        for(Ship_Placement i : fleet.getPlacements()){
            int tiles=100;
            int width=tiles*i.getShip().getSize();
            int height=tiles*i.getShip().getSize()/2;

            String imagePath = i.getShip().getImage();
            java.net.URL url = getClass().getResource(imagePath);
            ImageIcon image = new ImageIcon(url);
            Image sacle=image.getImage().getScaledInstance(width,height,Image.SCALE_SMOOTH);
            JLabel label = new JLabel(new ImageIcon(sacle));
            label.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("r"),"rotate ship");
            label.getActionMap().put("rotate ship",new AbstractAction("rotate ship") {
                public void actionPerformed(ActionEvent e) {
                    i.rotate();
                    int tiles=100;
                    int width = i.isHorizontal() ? tiles * i.getShip().getSize() : tiles;
                    int height = i.isHorizontal() ? tiles : tiles * i.getShip().getSize();
                    ImageIcon image = new ImageIcon(getClass().getResource(i.getShip().getImage()));
                    Image scaled = image.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(scaled));
                }
            });

            TransferHandlerShip handler = new TransferHandlerShip(i);
            label.setTransferHandler(handler);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            label.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    label.requestFocusInWindow();
                    JComponent comp = (JComponent) e.getSource();
                    TransferHandler handler = comp.getTransferHandler();
                    handler.exportAsDrag(comp, e, TransferHandler.COPY);
            }
        });
        add(Box.createVerticalGlue());
        add(label);}

    }

}
