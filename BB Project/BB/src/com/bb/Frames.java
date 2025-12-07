package com.bb;

import javax.swing.*;
import java.awt.*;


public class Frames extends JFrame {
    private final CardLayout cl = new CardLayout();
    private final JPanel cards = new JPanel(cl);

    public Frames() {
        super("Battleship");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();

        // Set a reasonable default size and allow the user to resize it.
        setSize(1200, 800);
        setLocationRelativeTo(null); // Center the frame on screen
        setResizable(true);
    }
    private JPanel wrapper(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.add(new Navigator(cl, cards), BorderLayout.SOUTH);
        return wrapper;
    }

    private void initUI() {
        // Create all the different panels for the card layout
        StartMenuPanel startMenu = new StartMenuPanel(cl, cards);
        PauseMenuPanel pauseMenu = new PauseMenuPanel(cl, cards);
        GameLayout player = new GameLayout(this);
        OpponentPanel opponent = new OpponentPanel();
        opponent.setPlayerBoard(player);
        Skill_Dialogs selection = new Skill_Dialogs(cl, cards, player::setActiveSkills);

        // Add the panels to the card holder
        cards.add(startMenu, "START_MENU");
        cards.add(pauseMenu, "PAUSE_MENU");
        cards.add(selection, "Skills");
        cards.add(wrapper(player), "PLAYER");
        cards.add(wrapper(opponent), "OPPONENT");

        // Add the card holder to the main frame
        getContentPane().add(cards, BorderLayout.CENTER);

        // The game open to the start menu
        cl.show(cards, "START_MENU");
    }
}
