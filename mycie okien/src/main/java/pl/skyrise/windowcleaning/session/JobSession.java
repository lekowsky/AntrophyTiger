package pl.skyrise.windowcleaning.session;

import org.bukkit.Location;
import pl.skyrise.windowcleaning.minigame.WindowMinigame;
import java.util.*;

public class JobSession {
    private final UUID playerId;
    private final int playerLevel;
    private final double earningsMultiplier;
    private WindowMinigame currentMinigame;
    private final Set<Location> cleanedWindows = new HashSet<>();
    private final List<Location> allWindows;
    private final int totalRequired;
    private boolean brushDipped = false;
    private int perfectWindows = 0;

    public JobSession(UUID playerId, int playerLevel, double earningsMultiplier, List<Location> allWindows, int totalRequired) {
        this.playerId = playerId;
        this.playerLevel = playerLevel;
        this.earningsMultiplier = earningsMultiplier;
        this.allWindows = allWindows;
        this.totalRequired = totalRequired;
    }

    public UUID getPlayerId() { return playerId; }
    public int getPlayerLevel() { return playerLevel; }
    public double getEarningsMultiplier() { return earningsMultiplier; }

    public void markWindowCleaned(Location loc, boolean perfect) {
        cleanedWindows.add(loc);
        if (perfect) perfectWindows++;
    }
    public boolean isWindowCleaned(Location loc) { return cleanedWindows.contains(loc); }
    public boolean isJobCompleted() { return cleanedWindows.size() >= totalRequired; }
    public int getCleanedCount() { return cleanedWindows.size(); }
    public int getTotalRequired() { return totalRequired; }
    public List<Location> getAllWindows() { return allWindows; }
    public int getPerfectWindows() { return perfectWindows; }
    public boolean isBrushDipped() { return brushDipped; }
    public void setBrushDipped(boolean dipped) { this.brushDipped = dipped; }
    public WindowMinigame getCurrentMinigame() { return currentMinigame; }
    public void setCurrentMinigame(WindowMinigame minigame) { this.currentMinigame = minigame; }
}