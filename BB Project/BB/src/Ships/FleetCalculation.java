package Ships;

import skills.ModifiedStats;

import java.util.List;

public class FleetCalculation implements Calculations{
    private final List<Ship_Placement> placements;
    private final ModifiedStats attackerSkills; // may be null
    private final ModifiedStats defenderSkills; // may be null

    public FleetCalculation(List<Ship_Placement> placements) {
        this(placements, null, null);
    }

    public FleetCalculation(List<Ship_Placement> placements, ModifiedStats attackerSkills, ModifiedStats defenderSkills) {
        this.placements = placements;
        this.attackerSkills = attackerSkills;
        this.defenderSkills = defenderSkills;
    }

    @Override
    public float DamageToShips() {
        if (placements == null || placements.isEmpty()) return 0f;

        // compute average attacker DMG (apply attacker dmg modifier)
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
        float attackerDmgMod = attackerSkills != null ? attackerSkills.dmgModifier() : 1f;
        avgDmg *= attackerDmgMod;

        // compute average defender shields (apply defender shield modifier)
        float avgShield = 0f;
        int dCount = 0;
        if (defenderSkills != null || placements != null) {
            // defender placements will be provided by caller when needed
            // if defenderSkills is null, modifier is 1
        }

        // NOTE: this method is intended to be used with attacker placements only.
        // The caller should provide defender placements if needed for more advanced rules.
        // For backward compatibility we keep the original simple penetration rule:
        // count how many attacker ships have penetration > defender average shield.
        // To compute defender average shield we need defender placements; if not provided,
        // we assume defender average shield = 0 (so penetration check uses ship's own shields).
        // For simplicity here we will set avgShield = 0; callers can use the static helper below
        // if they want defender placements included.

        float avgShieldUsed = 0f; // default if caller didn't provide defender placements

        // count penetrators relative to avgShieldUsed
        int penetrators = 0;
        for (Ship_Placement p : placements) {
            Ships_Type s = p.getShip();
            if (s == null) continue;
            float pen = s.getPenetration();
            // apply attacker dmg modifier does not change penetration; if you want skills to modify penetration,
            // you can add a method modify_penetration() to Skills and apply it here.
            if (pen > avgShieldUsed) penetrators++;
        }

        float penetratedFlag = (penetrators * 2 >= aCount) ? 1f : 0f; // at least half -> 1

        return avgDmg * penetratedFlag * 0.8f;
    }
    public static float DamageToShips(List<Ship_Placement> attacker, List<Ship_Placement> defender,
                                      ModifiedStats attackerSkills, ModifiedStats defenderSkills) {
        if (attacker == null || attacker.isEmpty()) return 0f;

        // avg attacker dmg
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
        if (attackerSkills != null) avgDmg *= attackerSkills.dmgModifier();

        // avg defender shields
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
            if (defenderSkills != null) avgShield *= defenderSkills.shieldModifier();
        }

        // count penetrators (attacker ships with penetration > avgShield)
        int penetrators = 0;
        for (Ship_Placement sp : attacker) {
            Ships_Type s = sp.getShip();
            if (s == null) continue;
            float pen = s.getPenetration();
            // if you want skills to modify penetration, apply attackerSkills here
            if (attackerSkills != null) {
                // if Skills later exposes a penetration modifier, apply it here
            }
            if (pen > avgShield) penetrators++;
        }
        float penetratedFlag = 1f;
        if(penetrators*3.33 <= aCount) {
            penetratedFlag =0.3f;}
        if(penetrators*2<= aCount) {
            penetratedFlag =0.5f;
        }





        return avgDmg * penetratedFlag*0.8f ;
    }

    // keep other methods if required by Calculations
    @Override
    public float DamageFromShips() { return 0f; }
    @Override
    public int DetectionRange() { return 0; }
};

