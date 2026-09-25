package com.bb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * The bar under each battle screen: switch boards, start the battle, or pause.
 *
 * <p>While ships are being deployed the second button is <b>Start</b>. Pressing it checks the
 * fleet against the stage's cost budget, locks it in and goes to the enemy board; from then
 * until the stage ends that button is <b>Enemy Board</b> instead. There is no way onto the
 * enemy board before Start - not the button, and not <kbd>Q</kbd>.
 *
 * <p>Board switching is also on <kbd>E</kbd> (your fleet) and <kbd>Q</kbd> (the enemy).
 */
public class Navigator extends JPanel {

    private static final String SLOT_START = "start";
    private static final String SLOT_ENEMY = "enemy";

    public Navigator(CardLayout cl, JPanel cards, GameLayout player) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 12, 8));
        setOpaque(false);

        JButton toPlayer = new JButton("Your Board (E)");
        JButton start = new JButton("Start");
        JButton toOpponent = new JButton("Enemy Board (Q)");
        JButton pauseButton = new JButton("Pause");

        toPlayer.setFocusable(false);
        start.setFocusable(false);
        toOpponent.setFocusable(false);
        pauseButton.setFocusable(false);

        start.setFont(start.getFont().deriveFont(Font.BOLD));
        start.setToolTipText("Lock in your fleet and go to the enemy board");

        toPlayer.addActionListener(e -> cl.show(cards, "PLAYER"));
        start.addActionListener(e -> {
            if (player.startBattle()) {
                cl.show(cards, "OPPONENT");
            } else {
                // GameLayout has already put the reason on the status bar above this one.
                Toolkit.getDefaultToolkit().beep();
            }
        });
        toOpponent.addActionListener(e -> cl.show(cards, "OPPONENT"));
        pauseButton.addActionListener(e -> cl.show(cards, "PAUSE_MENU"));

        // Start and Enemy Board share one slot. A CardLayout sizes it to the wider of the
        // two, so the bar does not shift sideways when they swap.
        CardLayout slotLayout = new CardLayout();
        JPanel slot = new JPanel(slotLayout);
        slot.setOpaque(false);
        slot.add(start, SLOT_START);
        slot.add(toOpponent, SLOT_ENEMY);

        Runnable showCurrentPhase = () -> slotLayout.show(slot,
                player.isBattleStarted() ? SLOT_ENEMY : SLOT_START);
        showCurrentPhase.run();
        player.addPropertyChangeListener(GameLayout.PROP_BATTLE_STARTED,
                e -> showCurrentPhase.run());

        add(toPlayer);
        add(slot);
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
                if (!battleShowing(cards)) return;
                if (player.isBattleStarted()) {
                    cl.show(cards, "OPPONENT");
                } else {
                    player.setStatusText("Press Start to lock in your fleet and begin the battle.");
                    Toolkit.getDefaultToolkit().beep();
                }
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
