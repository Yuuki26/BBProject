package Ships;

import skills.ModifiedStats;

import java.util.List;

public class FleetCalculation implements Calculations{
    private final List<Ship_Placement> placements;
    private final ModifiedStats attackerStats; // may be null

    public FleetCalculation(List<Ship_Placement> placements) {
        this(placements, null);
    }

    public FleetCalculation(List<Ship_Placement> placements, ModifiedStats attackerStats) {
        this.placements = placements;
        this.attackerStats = attackerStats;
    }

    @Override
    public float DamageToShips() {
        if (placements == null || placements.isEmpty()) return 0f;

        float sumDmg = 0f;
        int aCount = 0;
        for (Ship_Placement p : placements) {
            Ships_Type s = p.getShip();
            if (s == null) continue;
            sumDmg += s.getDMG();
            aCount++;
        }
        if (aCount == 0) return 0f;
        float avgDmg = sumDmg / aCount;

        // apply player modifier: prefer injected attackerStats, otherwise read registry
        float attackerDmgMod = attackerStats != null ? attackerStats.dmgModifier()
                : new ModifiedStats().dmgModifier();
        avgDmg *= attackerDmgMod;

        // penetration logic (unchanged)
        float avgShieldUsed = 0f; // no defender list here
        int penetrators = 0;
        for (Ship_Placement p : placements) {
            Ships_Type s = p.getShip();
            if (s == null) continue;
            if (s.getPenetration() > avgShieldUsed) penetrators++;
        }

        float penetratedFlag = 1f;
        if (penetrators * 3.33f <= aCount) penetratedFlag = 0.3f;
        else if (penetrators * 2f <= aCount) penetratedFlag = 0.5f;

        return avgDmg * penetratedFlag * 0.8f;
    }
    public static float DamageFromShips(List<Ship_Placement> attacker, List<Ship_Placement> defender) {
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
        float avgDmg = sumDmg / aCount;

        // compute defender average shield (no skill modifiers applied here)
        float avgShield = 0f;
        int dCount = 0;
        if (defender != null && !defender.isEmpty()) {
            float sumShield = 0f;
            for (Ship_Placement sp : defender) {
                Ships_Type s = sp.getShip();
                if (s == null) continue;
                sumShield += s.getShields();
                dCount++;
            }
            if (dCount > 0) avgShield = sumShield / dCount;
        }

        int penetrators = 0;
        for (Ship_Placement sp : attacker) {
            Ships_Type s = sp.getShip();
            if (s == null) continue;
            if (s.getPenetration() > avgShield) penetrators++;
        }

        float penetratedFlag = 1f;
        if (penetrators * 3.33f <= aCount) penetratedFlag = 0.3f;
        else if (penetrators * 2f <= aCount) penetratedFlag = 0.5f;

        return avgDmg * penetratedFlag * 0.8f;}

    // keep other methods if required by Calculations
    @Override
    public float DamageFromShips() { return 0f; }
    @Override
    public int DetectionRange() { return 0; }
};

