package com.bb;

import javax.swing.*;
import java.awt.*;


public class Frames extends JFrame {
    private final CardLayout cl = new CardLayout();
    private final JPanel cards = new JPanel(cl);

    public Frames() {
        super("Battleship Cards Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(2560, 1280));
    }
    private JPanel wrapper(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.add(new Navigator(cl, cards), BorderLayout.SOUTH);
        return wrapper;
    }

    private void initUI() {

        GameLayout player = new GameLayout(this);
        OpponentPanel opponent = new OpponentPanel();
        Skill_Dialogs selection=new Skill_Dialogs(cl,cards);
        cards.add(selection,"Skills");
        cards.add(wrapper(player), "PLAYER");
        cards.add(wrapper(opponent), "OPPONENT");


        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(cards, BorderLayout.CENTER);
        getContentPane().add(cards, BorderLayout.SOUTH);

    }
}
