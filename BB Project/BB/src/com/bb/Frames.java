package com.bb;

import javax.swing.*;
import java.awt.*;

/**
 * The application window and the card stack every screen lives on.
 *
 * <p>It also owns the run loop. A won battle goes to {@link #triggerStageWon(int)}, which
 * shows the reward screen and then rolls the next stage; only a loss reaches
 * {@link #triggerGameOver(boolean)} and ends the run.
 */
public class Frames extends JFrame {

    private final CardLayout cl = new CardLayout();

    private final JPanel cards = new JPanel(cl) {
        private Image bgImage;
        {
            try {
                java.net.URL url = Assets.getResource("/background.png");
                if (url != null) {
                    bgImage = javax.imageio.ImageIO.read(url);
                } else {
                    System.err.println("Background image not found: /background.png");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (bgImage != null) {
                g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    };

    private EndScreenPanel endScreen;
    private RewardPanel rewardScreen;
    private GameLayout player;
    private OpponentPanel opponent;
    private Skill_Dialogs skillSelect;

    public Frames() {
        super("Battleship");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();

        setSize(1280, 860);
        setLocationRelativeTo(null);
        setResizable(true);
    }

    private JPanel wrapper(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        content.setOpaque(false);
        wrapper.add(content, BorderLayout.CENTER);
        wrapper.add(new Navigator(cl, cards), BorderLayout.SOUTH);
        return wrapper;
    }

    private void initUI() {
        StartMenuPanel startMenu = new StartMenuPanel(cl, cards, this);
        PauseMenuPanel pauseMenu = new PauseMenuPanel(cl, cards, this);

        player = new GameLayout(this);
        opponent = new OpponentPanel();
        opponent.setMainFrame(this);
        opponent.setPlayerBoard(player);

        skillSelect = new Skill_Dialogs(cl, cards, player::setActiveSkills);
        rewardScreen = new RewardPanel(this::onRewardPicked);
        endScreen = new EndScreenPanel(cl, cards, this);

        cards.add(startMenu, "START_MENU");
        cards.add(pauseMenu, "PAUSE_MENU");
        cards.add(skillSelect, "Skills");
        cards.add(wrapper(player), "PLAYER");
        cards.add(wrapper(opponent), "OPPONENT");
        cards.add(rewardScreen, "REWARD");
        cards.add(endScreen, "END_SCREEN");

        add(cards, BorderLayout.CENTER);
        cl.show(cards, "START_MENU");
    }

    // =====================================================================================
    // Run loop
    // =====================================================================================

    /**
     * Called when the player clears a stage.
     *
     * <p>This is the change that turns a single win into a run: instead of ending the game,
     * it banks the score and hands the player a choice of upgrade for the next stage.
     *
     * @param hullRemaining total hull points left in the player's fleet, used for scoring
     */
    public void triggerStageWon(int hullRemaining) {
        RunState run = RunState.current();
        run.advanceStage(run.stageScore(hullRemaining));
        rewardScreen.present(player);
        cl.show(cards, "REWARD");
    }

    /** Applies the chosen reward and starts the next stage. */
    private void onRewardPicked(Reward reward) {
        reward.apply(player);

        // A new hull is added to the fleet undeployed, so the roster has to be rebuilt or it
        // would never appear - leaving deployment permanently incomplete and blocking Fire.
        if (reward.getKind() == Reward.Kind.SHIP) {
            player.rebuildRosterFromFleet();
        }

        // Damage carries forward unless the player specifically took the repair.
        player.prepareNextStage(reward.getKind() == Reward.Kind.REPAIR);
        opponent.startStage();

        player.setStatusText("Stage " + RunState.current().getStage()
                + " - deploy any new ships, then fire. Press R to rotate.");
        cl.show(cards, "PLAYER");
    }

    /** Ends the run. Only a loss gets here; wins go through {@link #triggerStageWon(int)}. */
    public void triggerGameOver(boolean playerWon) {
        endScreen.setVictory(playerWon);
        cl.show(cards, "END_SCREEN");
    }

    /** Clears the run and returns to skill selection for a fresh start. */
    public void startNewRun() {
        RunState.startNewRun();
        player.resetBoard();
        opponent.startStage();
        skillSelect.clearSelection();
        cl.show(cards, "Skills");
    }

    // =====================================================================================
    // Save / load
    // =====================================================================================

    /** Prompts for a slot and writes the run to it. */
    public void promptSave() {
        Integer slot = promptForSlot("Save game", "Save to which slot?");
        if (slot == null) return;

        String error = SaveManager.save(slot, player, opponent);
        if (error == null) {
            JOptionPane.showMessageDialog(this, "Saved to slot " + slot + ".",
                    "Save game", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, error, "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Prompts for a slot and restores the run from it. */
    public void promptLoad() {
        Integer slot = promptForSlot("Load game", "Load which slot?");
        if (slot == null) return;

        String error = SaveManager.load(slot, player, opponent);
        if (error == null) {
            cl.show(cards, "PLAYER");
            player.setStatusText("Loaded slot " + slot + " - stage "
                    + RunState.current().getStage() + ".");
        } else {
            JOptionPane.showMessageDialog(this, error, "Load failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** True when at least one save slot has something in it. */
    public boolean hasAnySave() {
        for (int i = 1; i <= SaveManager.slotCount(); i++) {
            if (SaveManager.slotExists(i)) return true;
        }
        return false;
    }

    private Integer promptForSlot(String title, String message) {
        String[] options = new String[SaveManager.slotCount()];
        for (int i = 0; i < options.length; i++) {
            options[i] = SaveManager.describeSlot(i + 1);
        }

        String choice = (String) JOptionPane.showInputDialog(this, message, title,
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (choice == null) return null;

        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(choice)) return i + 1;
        }
        return null;
    }

    public GameLayout getPlayerBoard() {
        return player;
    }

    public OpponentPanel getOpponentPanel() {
        return opponent;
    }
}
