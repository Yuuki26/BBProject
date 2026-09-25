package com.bb;

import javax.swing.*;
import java.awt.*;

/**
 * The application window and the card stack every screen lives on.
 *
 * <p>It also owns the run loop. A won battle goes to {@link #triggerStageWon(int)}, which pays
 * out gold, shows the reward screen, opens the {@link ShopPanel shop} when one is due, and
 * then rolls the next stage; only a loss reaches {@link #triggerGameOver(boolean)} and ends
 * the run. A new run goes straight to the starter fleet, then the board; skills only come
 * from rewards, so every run starts with none.
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
    private StarterFleetPanel starterFleet;
    private ShopPanel shopScreen;

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
        wrapper.add(new Navigator(cl, cards, player), BorderLayout.SOUTH);
        return wrapper;
    }

    private void initUI() {
        StartMenuPanel startMenu = new StartMenuPanel(cl, cards, this);
        PauseMenuPanel pauseMenu = new PauseMenuPanel(cl, cards, this);

        player = new GameLayout(this);
        opponent = new OpponentPanel();
        opponent.setMainFrame(this);
        opponent.setPlayerBoard(player);

        starterFleet = new StarterFleetPanel(player, this::onStarterFleetChosen);
        rewardScreen = new RewardPanel(this::onRewardPicked);
        shopScreen = new ShopPanel(player, this::beginStage);
        endScreen = new EndScreenPanel(cl, cards, this);

        cards.add(startMenu, "START_MENU");
        cards.add(pauseMenu, "PAUSE_MENU");
        cards.add(starterFleet, "STARTER_FLEET");
        cards.add(wrapper(player), "PLAYER");
        cards.add(wrapper(opponent), "OPPONENT");
        cards.add(rewardScreen, "REWARD");
        cards.add(shopScreen, "SHOP");
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
        int cleared = run.getStage();
        run.advanceStage(run.stageScore(hullRemaining));

        int gold = RunState.currencyForStage(cleared);
        run.addCurrency(gold);

        rewardScreen.present(player, gold);
        cl.show(cards, "REWARD");
    }

    /**
     * Applies the chosen reward, then either opens the shop or starts the next stage.
     *
     * <p>The board is settled for the next stage before the shop opens - shot marks cleared,
     * fleet unlocked, sunk ships afloat again at 1 hull - so the shop repairs and sells from
     * the state the player will actually sail with.
     */
    private void onRewardPicked(Reward reward) {
        reward.apply(player);

        // Damage carries forward unless the player took the repair (applied just above).
        player.prepareNextStage(false);

        if (Shop.opensBefore(RunState.current().getStage())) {
            shopScreen.open();
            cl.show(cards, "SHOP");
        } else {
            beginStage();
        }
    }

    /** Rolls the enemy for the stage the run is now on and hands the player the board. */
    private void beginStage() {
        opponent.startStage();
        player.setStatusText("Stage " + RunState.current().getStage()
                + " - fleet cost is now " + RunState.current().getDeploymentBudget()
                + ". Deploy any new ships, then press Start.");
        cl.show(cards, "PLAYER");
    }

    /** The starter fleet is on the roster: on to deploying it. */
    private void onStarterFleetChosen() {
        int total = player.getFleet().totalCost();
        int budget = RunState.current().getDeploymentBudget();
        player.setStatusText("Drag your ships onto the board (R rotates while dragging), "
                + "then press Start." + (total > budget
                        ? " Your fleet cost is " + budget + ", so not every ship fits - "
                                + "the rest wait in port." : ""));
        cl.show(cards, "PLAYER");
    }

    /** Ends the run. Only a loss gets here; wins go through {@link #triggerStageWon(int)}. */
    public void triggerGameOver(boolean playerWon) {
        endScreen.setVictory(playerWon);
        cl.show(cards, "END_SCREEN");
    }

    /**
     * Clears the run and deals a new starter fleet. The run begins with no skills; they are
     * earned on the reward screens.
     */
    public void startNewRun() {
        RunState.startNewRun();
        player.resetBoard();
        opponent.startStage();
        starterFleet.reset();
        cl.show(cards, "STARTER_FLEET");
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

    public ShopPanel getShopPanel() {
        return shopScreen;
    }

    public StarterFleetPanel getStarterFleetPanel() {
        return starterFleet;
    }
}
