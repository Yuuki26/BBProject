package Ships;

import skills.ModifiedStats;

import java.util.List;

/**
 * Turns two fleets into a per-hit damage number.
 *
 * <p>The model is: take the attacker's average gun, scale it by the attacker's offensive
 * skills, then ask how much of that average gets through the defender's average shield.
 * The more of the attacking fleet that out-guns the shield, the less the salvo is blunted.
 *
 * <p>The previous version compared penetration against a hard-coded shield of {@code 0},
 * so every ship with any penetration at all counted as a penetrator and the defender's
 * shields - and therefore every defensive skill - had no effect on incoming damage.
 */
public class FleetCalculation implements Calculations {

    /** Flat scaling so a single hit does not delete a ship outright. */
    private static final float GLOBAL_SCALE = 0.8f;

    private final List<Ship_Placement> attacker;
    private final List<Ship_Placement> defender;
    private final ModifiedStats attackerStats;

    public FleetCalculation(List<Ship_Placement> attacker) {
        this(attacker, null, null);
    }

    public FleetCalculation(List<Ship_Placement> attacker, List<Ship_Placement> defender) {
        this(attacker, defender, null);
    }

    public FleetCalculation(List<Ship_Placement> attacker,
                            List<Ship_Placement> defender,
                            ModifiedStats attackerStats) {
        this.attacker = attacker;
        this.defender = defender;
        this.attackerStats = attackerStats;
    }

    /** Damage this fleet deals to the fleet it was constructed against. */
    @Override
    public float DamageToShips() {
        ModifiedStats stats = attackerStats != null ? attackerStats : new ModifiedStats();
        return damageBetween(attacker, defender, stats.dmgModifier(), 1f);
    }

    /** Damage this fleet takes from the fleet it was constructed against. */
    @Override
    public float DamageFromShips() {
        return damageBetween(defender, attacker, 1f, new ModifiedStats().shieldModifier());
    }


    /**
     * Core damage formula, shared by both directions of fire.
     *
     * @param attacker      the firing fleet
     * @param defender      the fleet being fired on; may be null or empty, which means an
     *                      unshielded target
     * @param dmgModifier   multiplier from the attacker's offensive skills
     * @param shieldModifier multiplier from the defender's defensive skills
     * @return damage applied per hit, never negative
     */
    public static float damageBetween(List<Ship_Placement> attacker,
                                      List<Ship_Placement> defender,
                                      float dmgModifier,
                                      float shieldModifier) {
        if (attacker == null || attacker.isEmpty()) return 0f;

        float sumDmg = 0f;
        int aCount = 0;
        for (Ship_Placement sp : attacker) {
            Ships_Type s = sp.getShip();
            if (s == null) continue;
            sumDmg += s.getDMG();
            aCount++;
        }
        if (aCount == 0) return 0f;

        float avgDmg = (sumDmg / aCount) * dmgModifier;
        float avgShield = averageShield(defender) * shieldModifier;

        // How much of the attacking fleet can actually out-gun that shield?
        int penetrators = 0;
        for (Ship_Placement sp : attacker) {
            Ships_Type s = sp.getShip();
            if (s == null) continue;
            if (s.getPenetration() > avgShield) penetrators++;
        }

        float penetratedFlag = 1f;
        if (penetrators * 3.33f <= aCount) {
            penetratedFlag = 0.3f;
        } else if (penetrators * 2f <= aCount) {
            penetratedFlag = 0.5f;
        }

        return Math.max(0f, avgDmg * penetratedFlag * GLOBAL_SCALE);
    }

    /** Backwards-compatible two-fleet entry point with no skill modifiers applied. */
    public static float DamageFromShips(List<Ship_Placement> attacker,
                                        List<Ship_Placement> defender) {
        return damageBetween(attacker, defender, 1f, 1f);
    }

    private static float averageShield(List<Ship_Placement> defender) {
        if (defender == null || defender.isEmpty()) return 0f;
        float sum = 0f;
        int count = 0;
        for (Ship_Placement sp : defender) {
            Ships_Type s = sp.getShip();
            if (s == null) continue;
            sum += s.getShields();
            count++;
        }
        return count == 0 ? 0f : sum / count;
    }
}
