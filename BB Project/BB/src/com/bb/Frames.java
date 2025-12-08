package com.bb;

import javax.swing.*;
import java.awt.*;


public class Frames extends JFrame {
    private final CardLayout cl = new CardLayout();
    private final JPanel cards = new JPanel(cl);
    
    private EndScreenPanel endScreen;

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
        
        opponent.setMainFrame(this);
        opponent.setPlayerBoard(player);
        Skill_Dialogs selection = new Skill_Dialogs(cl, cards, player::setActiveSkills);

        endScreen = new EndScreenPanel(cl, cards);

        // Add the panels to the card holder
        cards.add(startMenu, "START_MENU");
        cards.add(pauseMenu, "PAUSE_MENU");
        cards.add(selection, "Skills");
        cards.add(wrapper(player), "PLAYER");
        cards.add(wrapper(opponent), "OPPONENT");
        cards.add(endScreen, "END_SCREEN");

        // Add the card holder to the main frame
        add(cards, BorderLayout.CENTER);

        // The game open to the start menu
        cl.show(cards, "START_MENU");
    }

    public void triggerGameOver(boolean playerWon) {
        endScreen.setVictory(playerWon);
        cl.show(cards, "END_SCREEN");
    }
}
