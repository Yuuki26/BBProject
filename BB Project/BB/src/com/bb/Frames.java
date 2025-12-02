package com.bb;

import Ships.DefaultFleet;
import com.bb.GameLayout.Mode;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class Frames extends JFrame {
    private final CardLayout cl = new CardLayout();
    private final JPanel cards;

    public Frames() {
        super("Battleship Cards Demo");
        this.cards = new JPanel(this.cl);
        this.setDefaultCloseOperation(3);
        this.initUI();
        this.pack();
        this.setLocationRelativeTo((Component)null);
        this.setMinimumSize(new Dimension(2560, 1280));
    }

    private JPanel wrapper(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(content, "Center");
        wrapper.add(new Navigator(this.cl, this.cards), "South");
        return wrapper;
    }

    private void initUI() {
        GameLayout player = new GameLayout(this, Mode.PLAYER);
        GameLayout opponent = new GameLayout(this, Mode.OPPONENT);
        player.setEnemyTargetBoard(opponent);
        opponent.setEnemyTargetBoard(player);
        DefaultFleet templateFleet = new DefaultFleet();
        opponent.randomizeFleet(templateFleet);
        Skill_Dialogs selection = new Skill_Dialogs(this.cl, this.cards);
        MainMenuPanel mainMenu = new MainMenuPanel(this.cl, this.cards);
        this.cards.add(mainMenu, "MAIN_MENU");
        this.cards.add(selection, "Skills");
        this.cards.add(this.wrapper(player), "PLAYER");
        this.cards.add(this.wrapper(opponent), "OPPONENT");
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(this.cards, "Center");
        this.cl.show(this.cards, "MAIN_MENU");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> (new Frames()).setVisible(true));
    }
}
