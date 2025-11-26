package com.bb;

import Ships.DataHandler;
import Ships.Ship_Placement;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;

/**
 * A reusable TransferHandler for board cells.
 * - Handles drop (canImport / importData) to show ghost and commit placement.
 * - Handles export (createTransferable / getSourceActions) to allow dragging a placed ship.
 *
 * This class is independent from the cell UI; it calls back into GameLayout for painting/validation.
 */
public class CellTransferHandler extends TransferHandler {
    private final GameLayout owner;

    public CellTransferHandler(GameLayout owner) {
        super();
        this.owner = owner;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDataFlavorSupported(DataHandler.SHIP_FLAVOR)) {
            return false;
        }
        try {
            Ship_Placement sp = (Ship_Placement) support.getTransferable()
                    .getTransferData(DataHandler.SHIP_FLAVOR);

            JComponent comp = (JComponent) support.getComponent();
            String coord = (String) comp.getClientProperty("coord");
            int col = coord.charAt(0) - 'A';
            int row = Integer.parseInt(coord.substring(1)) - 1;
            sp.setOrigin(new Point(col, row));

            // Delegate ghost painting to owner (GameLayout). Owner will dedupe repeated ghosts.
            owner.paintGhost(sp);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!support.isDataFlavorSupported(DataHandler.SHIP_FLAVOR)) return false;
        try {
            Ship_Placement sp = (Ship_Placement) support.getTransferable()
                    .getTransferData(DataHandler.SHIP_FLAVOR);

            JComponent comp = (JComponent) support.getComponent();
            String coord = (String) comp.getClientProperty("coord");
            int col = coord.charAt(0) - 'A';
            int row = Integer.parseInt(coord.substring(1)) - 1;
            sp.setOrigin(new Point(col, row));

            // Validate via owner and commit if valid
            if (owner.validatePlacement(sp)) {
                owner.commitPlacement(sp); // commit (paint + roster removal)
                owner.clearGhost();
                return true;
            } else {
                owner.clearGhost();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Export side: if this cell currently holds a ship, allow MOVE and create transferable
    @Override
    public int getSourceActions(JComponent c) {
        Object ship = c.getClientProperty("ship");
        return (ship instanceof Ship_Placement) ? TransferHandler.MOVE : NONE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        Object ship = c.getClientProperty("ship");
        if (ship instanceof Ship_Placement) {
            return new DataHandler((Ship_Placement) ship);
        }
        return null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // no-op: authoritative cleanup happens in drop target importData (owner.commitPlacement / owner.removeShipFromBoard)
    }
}
