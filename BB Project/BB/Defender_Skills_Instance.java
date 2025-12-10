package skills;

//used for counting remaining turns of skills and usages
public class Defender_Skills_Instance {
    private final Defender_Skills template;
    private int remainingUses;
    private int remainingTurns;

    public Defender_Skills_Instance(Defender_Skills template) {
        this.template = template;
        this.remainingUses = template.usage();
        this.remainingTurns = template.turns();
    }
    public boolean isUnlimitedUses() { return template.usage() == -1; }
    public boolean isUnlimitedDuration() { return template.turns() == -1; }
    public Defender_Skills getTemplate() { return template; }
    public boolean isActive() {
        boolean usesOk = remainingUses > 0 || template.usage() == -1;
        boolean turnsOk = remainingTurns > 0 || template.turns() == -1;
        return usesOk && turnsOk;
    }

    public boolean consumeUse() {
        if (isUnlimitedUses()) return true;
        if (remainingUses > 0) {
            remainingUses--;
            return true;
        }
        return false;
    }

    public void tickTurn() {
        if (remainingTurns > 0) remainingTurns--;
    }
}
