package com.bb;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


public class Navigator extends JPanel  {
    public Navigator(CardLayout cl, JPanel cards) {
        setLayout(new FlowLayout(FlowLayout.CENTER, 12, 8));

        JButton toPlayer = new JButton("Show Player");
        JButton toOpponent = new JButton("Show Opponent");

        toPlayer.addActionListener(e -> cl.show(cards, "PLAYER"));
        toOpponent.addActionListener(e -> cl.show(cards, "OPPONENT"));

        add(toPlayer);
        add(toOpponent);
        cards.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("E"), "showPlayer");
        cards.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("Q"), "showOpponent");

        cards.getActionMap().put("showPlayer", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                cl.show(cards, "PLAYER");
            }
        });
        cards.getActionMap().put("showOpponent", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                cl.show(cards, "OPPONENT");
            }
        });

    }


}
