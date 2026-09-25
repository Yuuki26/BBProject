package skills;

import java.awt.Color;

/**
 * How rare a skill is: how often the reward roll offers it, and how far into a run the player
 * has to get before it is offered at all.
 *
 * <p>The two halves do different jobs. The {@linkplain #getBaseWeight() weight} makes a strong
 * skill uncommon without ruling it out. The {@linkplain #getUnlockStage() unlock stage} rules
 * it out entirely until the run reaches that stage, however lucky the roll - and that includes
 * the two skills picked at the very start of a run, which is what stops an overpowered skill
 * from simply being chosen on turn one.
 *
 * <p>Tiers are listed from most to least common. Retuning a tier here retunes every skill in it;
 * a single skill can still be nudged on its own by overriding {@link Skills#weight()}.
 */
public enum Rarity {

    COMMON   ("Common",    100, 1, new Color(200, 200, 200)),
    UNCOMMON ("Uncommon",   60, 1, new Color(110, 205, 110)),
    RARE     ("Rare",       30, 2, new Color(90, 155, 245)),
    EPIC     ("Epic",       12, 4, new Color(185, 115, 240)),
    LEGENDARY("Legendary",   5, 6, new Color(250, 170, 55));

    private final String label;
    private final int baseWeight;
    private final int unlockStage;
    private final Color color;

    Rarity(String label, int baseWeight, int unlockStage, Color color) {
        this.label = label;
        this.baseWeight = baseWeight;
        this.unlockStage = unlockStage;
        this.color = color;
    }

    /** Name shown to the player, e.g. "Rare". */
    public String getLabel() {
        return label;
    }

    /**
     * Relative odds of a skill of this tier being offered, before any upgrade bonus.
     *
     * <p>Only the ratios matter: a common skill (100) comes up twenty times as often as a
     * legendary one (5) when both are in the pool.
     */
    public int getBaseWeight() {
        return baseWeight;
    }

    /** First stage at which skills of this tier can be offered or picked. */
    public int getUnlockStage() {
        return unlockStage;
    }

    /** True when a run on {@code stage} is far enough along for this tier. */
    public boolean isUnlockedAt(int stage) {
        return stage >= unlockStage;
    }

    /** Colour used for this tier's border and label on screen. */
    public Color getColor() {
        return color;
    }
}
