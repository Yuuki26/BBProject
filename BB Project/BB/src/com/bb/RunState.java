package com.bb;

import skills.SkillPool;
import skills.Skills;
import skills.SkillsRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything that survives from one battle to the next inside a single run.
 *
 * <p>A run is a chain of stages against progressively larger enemy fleets. Winning a stage
 * grants a {@link Reward} and advances the chain; losing ends the run. This holds the state
 * that has to outlive an individual battle - the stage counter, the score, the shot
 * allowance and the skill loadout - so no single battle screen owns it.
 *
 * <p>There is one run in flight at a time, reached through {@link #current()}.
 */
public class RunState {

    /** Shots per salvo at the start of a run, before losses and skills are applied. */
    public static final int STARTING_SHOTS = 3;

    /**
     * Total hull cost that can be on the board at stage 1.
     *
     * <p>Ten buys one battleship, or a carrier and a destroyer, or a heavy and a light
     * cruiser. That choice is the point: the roster is bigger than the budget, so deploying
     * is about what to leave in port.
     */
    public static final int STARTING_DEPLOYMENT_BUDGET = 10;

    /** Extra hull cost allowed for each stage cleared. */
    public static final int BUDGET_GAIN_PER_STAGE = 4;

    /**
     * Gold for clearing a stage: {@code CURRENCY_BASE + CURRENCY_PER_STAGE * stage}. That is
     * 40, 50 and 60 for the first three, so the first shop (before stage 4) opens with 150.
     */
    public static final int CURRENCY_BASE = 30;
    public static final int CURRENCY_PER_STAGE = 10;

    private static RunState instance = new RunState();

    private int stage = 1;
    private int score = 0;
    private int baseShots = STARTING_SHOTS;
    private int stagesCleared = 0;
    private List<Skills> loadout = new ArrayList<>();

    /** Gold on hand, spent in the shop. */
    private int currency = 0;

    /** Fleet cost bought in the shop ("Expand your fleet"), on top of the stage's budget. */
    private int budgetBonus = 0;

    /** The run currently in progress. */
    public static RunState current() {
        return instance;
    }

    /** Throws away the current run and starts a fresh one. */
    public static void startNewRun() {
        instance = new RunState();
        SkillsRegistry.reset();
    }

    /** Installs a run rebuilt from a save file. */
    public static void restore(RunState restored) {
        if (restored != null) instance = restored;
    }

    // ---- stage ---------------------------------------------------------------------------

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = Math.max(1, stage);
    }

    public int getStagesCleared() {
        return stagesCleared;
    }

    public void setStagesCleared(int stagesCleared) {
        this.stagesCleared = Math.max(0, stagesCleared);
    }

    /** Records a stage win and moves the run forward. */
    public void advanceStage(int stageScore) {
        stagesCleared++;
        score += stageScore;
        stage++;
    }

    // ---- score ---------------------------------------------------------------------------

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = Math.max(0, score);
    }

    public void addScore(int delta) {
        this.score = Math.max(0, this.score + delta);
    }

    // ---- shots ---------------------------------------------------------------------------

    /**
     * The salvo size before ship losses and skill multipliers.
     *
     * <p>Starts at {@link #STARTING_SHOTS} and is raised by rewards, which is the
     * "modified in later game" half of making the shot count dynamic.
     */
    public int getBaseShots() {
        return baseShots;
    }

    public void setBaseShots(int baseShots) {
        this.baseShots = Math.max(1, baseShots);
    }

    public void addBaseShots(int delta) {
        setBaseShots(this.baseShots + delta);
    }

    // ---- loadout -------------------------------------------------------------------------

    public List<Skills> getLoadout() {
        return new ArrayList<>(loadout);
    }

    public void setLoadout(List<Skills> loadout) {
        this.loadout = loadout == null ? new ArrayList<>() : new ArrayList<>(loadout);
    }

    /**
     * Adds a skill to the loadout and pushes the whole thing back to the registry.
     *
     * <p>An upgrade takes the place of what it upgrades instead of sitting beside it, so
     * taking ERA Silver while holding ERA Bronze leaves one ERA in the loadout, not two.
     */
    public void addSkill(Skills skill) {
        if (skill == null) return;
        loadout.removeAll(SkillPool.replacedBy(skill, loadout));
        loadout.add(skill);
        SkillsRegistry.setSelectedSkills(loadout);
    }

    /** True when the loadout already contains a skill of the same type. */
    public boolean hasSkill(Class<?> skillClass) {
        for (Skills s : loadout) {
            if (s.getClass() == skillClass) return true;
        }
        return false;
    }

    // ---- deployment budget ---------------------------------------------------------------

    /**
     * Total hull cost that may be deployed this stage.
     *
     * <p>This is what {@link Ships.Ships_Type#getCost()} is for: it caps what can be on the
     * board, not how big the roster is. The budget grows as the run goes on, so a fleet the
     * player could only field one ship of at stage 1 comes out in full later.
     */
    public int getDeploymentBudget() {
        return deploymentBudgetForStage(stage) + budgetBonus;
    }

    /**
     * The budget a given stage allows before any fleet expansions.
     *
     * <p>This is also what the enemy is bought with. Expansions deliberately stay out of it:
     * if buying fleet cost grew the enemy too, it would buy the player nothing.
     */
    public static int deploymentBudgetForStage(int stage) {
        return STARTING_DEPLOYMENT_BUDGET + (Math.max(1, stage) - 1) * BUDGET_GAIN_PER_STAGE;
    }

    /** Fleet cost bought in the shop so far. */
    public int getBudgetBonus() {
        return budgetBonus;
    }

    public void setBudgetBonus(int budgetBonus) {
        this.budgetBonus = Math.max(0, budgetBonus);
    }

    /** Permanently raises the deployment budget, as "Expand your fleet" does. */
    public void addBudgetBonus(int delta) {
        setBudgetBonus(this.budgetBonus + delta);
    }

    // ---- currency ------------------------------------------------------------------------

    public int getCurrency() {
        return currency;
    }

    public void setCurrency(int currency) {
        this.currency = Math.max(0, currency);
    }

    public void addCurrency(int delta) {
        setCurrency(this.currency + delta);
    }

    /**
     * Pays {@code price} if the player has it.
     *
     * @return false, with nothing spent, when they cannot afford it
     */
    public boolean spendCurrency(int price) {
        if (price < 0 || price > currency) return false;
        currency -= price;
        return true;
    }

    /** Gold paid out for clearing {@code stage}. */
    public static int currencyForStage(int stage) {
        return CURRENCY_BASE + CURRENCY_PER_STAGE * Math.max(1, stage);
    }

    // ---- difficulty ----------------------------------------------------------------------

    /**
     * Multiplier applied to the opponent's fleet size for the current stage.
     *
     * <p>Stage 1 fields roughly what the player fields; each further stage adds 25%, so the
     * run gets harder without the board filling up immediately.
     */
    public float enemyScale() {
        return 1f + (stage - 1) * 0.25f;
    }

    /** Points awarded for clearing the current stage with {@code hullRemaining} hull left. */
    public int stageScore(int hullRemaining) {
        return 100 * stage + 10 * Math.max(0, hullRemaining);
    }

    @Override
    public String toString() {
        return "Stage " + stage + "  |  Score " + score + "  |  Shots " + baseShots
                + "  |  Gold " + currency;
    }
}
