package com.bb;

import Ships.DataHandler;
import Ships.Ship_Placement;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Transferable;

public class CellTransferHandler extends TransferHandler {
    private final GameLayout owner;

    public CellTransferHandler(GameLayout owner) {
        this.owner = owner;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDataFlavorSupported(DataHandler.SHIP_FLAVOR)) {
            return false;
        } else {
            try {
                Ship_Placement sp = (Ship_Placement) support.getTransferable().getTransferData(DataHandler.SHIP_FLAVOR);
                JComponent comp = (JComponent) support.getComponent();
                String coord = (String) comp.getClientProperty("coord");
                
                if (coord != null) {
                    int col = coord.charAt(0) - 'A';
                    int row = Integer.parseInt(coord.substring(1)) - 1;
                    sp.setOrigin(new Point(col, row));
                    this.owner.paintGhost(sp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!support.isDataFlavorSupported(DataHandler.SHIP_FLAVOR)) {
            return false;
        } else {
            try {
                Ship_Placement sp = (Ship_Placement) support.getTransferable().getTransferData(DataHandler.SHIP_FLAVOR);
                JComponent comp = (JComponent) support.getComponent();
                String coord = (String) comp.getClientProperty("coord");
                
                if (coord != null) {
                    int col = coord.charAt(0) - 'A';
                    int row = Integer.parseInt(coord.substring(1)) - 1;
                    sp.setOrigin(new Point(col, row));
                    
                    if (this.owner.validatePlacement(sp)) {
                        this.owner.commitPlacement(sp);
                        this.owner.clearGhost();
                        return true;
                    }
                }
                // If validation fails or coord is null, clear ghost and return false
                this.owner.clearGhost();
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    public int getSourceActions(JComponent c) {
        Object ship = c.getClientProperty("ship");
        return ship instanceof Ship_Placement ? MOVE : NONE;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        Object ship = c.getClientProperty("ship");
        return ship instanceof Ship_Placement ? new DataHandler((Ship_Placement) ship) : null;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // Logic for after export is complete (optional)
    }
}