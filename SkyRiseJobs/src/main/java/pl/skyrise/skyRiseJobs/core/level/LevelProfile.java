package pl.skyrise.skyRiseJobs.core.level;

public class LevelProfile {
    private int level;
    private double exp;

    public LevelProfile(int level, double exp) {
        this.level = Math.max(1, level);
        this.exp = Math.max(0.0, exp);
    }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }
    public double getExp() { return exp; }
    public void setExp(double exp) { this.exp = Math.max(0.0, exp); }
}
