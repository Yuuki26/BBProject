package Ships;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class TransferHandlerShip extends TransferHandler {
    private final Ship_Placement ship;

    public TransferHandlerShip(Ship_Placement ship) {
        super("icon");
        this.ship = ship;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        return new DataHandler(ship);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    public Ship_Placement getShip() {
        return ship;
    }
}
