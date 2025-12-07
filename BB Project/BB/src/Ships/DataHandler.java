package Ships;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class DataHandler implements Transferable {
    public static final DataFlavor SHIP_FLAVOR = new DataFlavor(Ship_Placement.class, "Ship");
    private final Ship_Placement ship;

    public DataHandler(Ship_Placement ship) {
        this.ship = ship;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { SHIP_FLAVOR };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return SHIP_FLAVOR.equals(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        return ship;
    }
}

