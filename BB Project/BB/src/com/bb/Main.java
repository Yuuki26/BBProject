package com.bb;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> (new Frames()).setVisible(true));
    }
}
