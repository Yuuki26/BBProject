package com.bb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * The bar under each battle screen: switch boards, or pause.
 *
 * <p>Board switching is also on <kbd>E</kbd> (your fleet) and <kbd>Q</kbd> (the enemy).
 */
public class Navigator extends JPanel {

    public Navigator(CardLayout cl, JPanel cards) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 12, 8));
        setOpaque(false);

        JButton toPlayer = new JButton("Your Board (E)");
        JButton toOpponent = new JButton("Enemy Board (Q)");
        JButton pauseButton = new JButton("Pause");

        toPlayer.setFocusable(false);
        toOpponent.setFocusable(false);
        pauseButton.setFocusable(false);

        toPlayer.addActionListener(e -> cl.show(cards, "PLAYER"));
        toOpponent.addActionListener(e -> cl.show(cards, "OPPONENT"));
        pauseButton.addActionListener(e -> cl.show(cards, "PAUSE_MENU"));

        add(toPlayer);
        add(toOpponent);
        add(pauseButton);

        // WHEN_IN_FOCUSED_WINDOW, not WHEN_ANCESTOR_OF_FOCUSED_COMPONENT: the board cells
        // are deliberately non-focusable, so nothing inside `cards` normally holds focus and
        // an ancestor-scoped binding would never fire.
        InputMap im = cards.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "showPlayer");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), "showOpponent");

        cards.getActionMap().put("showPlayer", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (battleShowing(cards)) cl.show(cards, "PLAYER");
            }
        });
        cards.getActionMap().put("showOpponent", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (battleShowing(cards)) cl.show(cards, "OPPONENT");
            }
        });
    }

    /**
     * True when the visible card is one of the two battle screens.
     *
     * <p>The bindings live on the shared card container, which is always showing, so without
     * this guard E and Q would jump into a battle from the main menu or the end screen.
     * A battle screen is exactly one that carries a Navigator bar, so that is what we look
     * for - which keeps this correct no matter which Navigator instance registered the action.
     */
    private static boolean battleShowing(JPanel cards) {
        for (Component child : cards.getComponents()) {
            if (child.isVisible() && child instanceof Container) {
                return containsNavigator((Container) child);
            }
        }
        return false;
    }

    private static boolean containsNavigator(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof Navigator) return true;
            if (child instanceof Container && containsNavigator((Container) child)) return true;
        }
        return false;
    }
}
