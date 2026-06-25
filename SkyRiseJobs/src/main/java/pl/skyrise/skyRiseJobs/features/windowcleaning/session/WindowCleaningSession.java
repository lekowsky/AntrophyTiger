package pl.skyrise.skyRiseJobs.features.windowcleaning.session;

import org.bukkit.Location;

public class WindowCleaningSession {
    private final Location windowLocation;
    private boolean cleaned = false;

    public WindowCleaningSession(Location windowLocation) {
        this.windowLocation = windowLocation;
    }

    public Location getWindowLocation() { return windowLocation; }
    public boolean isCleaned() { return cleaned; }
    public void setCleaned(boolean cleaned) { this.cleaned = cleaned; }
}