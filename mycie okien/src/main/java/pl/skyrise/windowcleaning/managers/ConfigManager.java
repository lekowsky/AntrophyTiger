package pl.skyrise.windowcleaning.managers;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;

import java.util.List;

public class ConfigManager {

    private final WindowCleaningPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // ---------- Lokalizacje ----------
    public Location getBossLocation() { return parseLocation(config.getString("boss-location")); }
    public void setBossLocation(Location loc) { config.set("boss-location", serializeLocation(loc)); plugin.saveConfig(); }
    public Location getElevatorUpLocation() { return parseLocation(config.getString("elevator-up-location")); }
    public void setElevatorUpLocation(Location loc) { config.set("elevator-up-location", serializeLocation(loc)); plugin.saveConfig(); }
    public Location getElevatorDownLocation() { return parseLocation(config.getString("elevator-down-location")); }
    public void setElevatorDownLocation(Location loc) { config.set("elevator-down-location", serializeLocation(loc)); plugin.saveConfig(); }
    public Location getPlatformLocation() { return parseLocation(config.getString("platform-location")); }
    public void setPlatformLocation(Location loc) { config.set("platform-location", serializeLocation(loc)); plugin.saveConfig(); }
    public Location getPlatform2Location() { return parseLocation(config.getString("platform2-location")); }
    public void setPlatform2Location(Location loc) { config.set("platform2-location", serializeLocation(loc)); plugin.saveConfig(); }

    public Location getRegionPos1() { return parseLocation(config.getString("region.pos1")); }
    public void setRegionPos1(Location loc) { config.set("region.pos1", serializeLocation(loc)); plugin.saveConfig(); }
    public Location getRegionPos2() { return parseLocation(config.getString("region.pos2")); }
    public void setRegionPos2(Location loc) { config.set("region.pos2", serializeLocation(loc)); plugin.saveConfig(); }
    public boolean isInRegion(Location loc) {
        Location p1 = getRegionPos1();
        Location p2 = getRegionPos2();
        if (p1 == null || p2 == null || loc.getWorld() != p1.getWorld()) return false;
        double minX = Math.min(p1.getX(), p2.getX());
        double maxX = Math.max(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double maxY = Math.max(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxZ = Math.max(p1.getZ(), p2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX &&
                loc.getY() >= minY && loc.getY() <= maxY &&
                loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    // ---------- Zarobki / XP ----------
    public double getBaseEarnings() { return config.getDouble("base-earnings", 50.0); }
    public int getBaseXP() { return config.getInt("base-xp", 100); }
    public double getEarningsPerLevel() { return config.getDouble("earnings-per-level", 0.1); }

    // ---------- Przedmioty ----------
    public String getBrushName() { return config.getString("brush.name", "&aSzczotka do okien").replace('&', '§'); }
    public int getBrushModelData() { return config.getInt("brush.model-data", 1001); }
    public String getBucketName() { return config.getString("bucket.name", "&bWiaderko z płynem").replace('&', '§'); }
    public int getBucketModelData() { return config.getInt("bucket.model-data", 1002); }

    // ---------- Poziomy ----------
    public int getMaxLevel() { return config.getInt("max-level", 5); }
    public int getXpForLevel(int level) { return config.getInt("xp-per-level." + level, 100 * level); }

    // ---------- Umiejętności ----------
    public int getMaxSkillLevel(String skillId) { return config.getInt("skills." + skillId + ".max-level", 5); }
    public int getSkillCost(String skillId, int level) { return config.getInt("skills." + skillId + ".cost-per-level." + level, 1); }
    public double getSkillPointDropChance() { return config.getDouble("skill-point-drop-chance", 0.3); }
    public int getRequiredPlayerLevel(String skillId) { return config.getInt("skills." + skillId + ".required-player-level", 0); }

    // Perki – dodatkowe dane (polskie ID)
    public int getScrubPowerPerLevel() { return config.getInt("skills.szybsze_szorowanie.power-per-level", 1); }
    public int getPointerSpeedReductionPerLevel() { return config.getInt("skills.szybsze_szorowanie.pointer-speed-reduction-per-level", 1); }
    public int getPenaltyReductionPerLevel() { return config.getInt("skills.wytrzymalosc.penalty-reduction-per-level", 5); }
    public int getZoneExpandPerLevel() { return config.getInt("skills.wieksza_strefa.zone-expand-per-level", 1); }
    public int getPerfectBonusPerLevel() { return config.getInt("skills.czysta_robota.bonus-per-level", 8); }
    public int getFreeDipChancePerLevel() { return config.getInt("skills.darmowe_namaczanie.chance-per-level", 15); }
    public double getCompletionBonusMultiplierPerLevel() { return config.getDouble("skills.bonus_za_komplet.multiplier-per-level", 0.2); }

    // ---------- Praca ----------
    public int getTotalWindowsToClean() { return config.getInt("job.total-windows-to-clean", 5); }
    public List<String> getWindowBlockTypes() { return config.getStringList("job.window-block-types"); }
    public int getBaseScrubPower() { return config.getInt("job.base-scrub-power", 10); }
    public int getMinigameGuiSize() { return config.getInt("job.minigame-gui-size", 27); }

    // ---------- Particle ----------
    public boolean isParticlesEnabled() { return config.getBoolean("particles.enabled", true); }
    public long getParticleInterval() { return config.getLong("particles.interval", 10L); }
    public Color getParticleColor() {
        String rgb = config.getString("particles.color", "158,158,158");
        String[] parts = rgb.split(",");
        if (parts.length == 3) {
            try {
                int r = Integer.parseInt(parts[0].trim());
                int g = Integer.parseInt(parts[1].trim());
                int b = Integer.parseInt(parts[2].trim());
                return Color.fromRGB(r, g, b);
            } catch (NumberFormatException ignored) {}
        }
        return Color.fromRGB(158, 158, 158);
    }
    public double getParticleScale() { return config.getDouble("particles.scale", 1.0); }
    public int getParticleCount() { return config.getInt("particles.count", 1); }
    public double getParticleOffsetX() { return config.getDouble("particles.offset-x", 0.0); }
    public double getParticleOffsetY() { return config.getDouble("particles.offset-y", 0.5); }
    public double getParticleOffsetZ() { return config.getDouble("particles.offset-z", 0.7); }

    // ---------- Minigra ----------
    public int getPointerBaseSpeed() { return config.getInt("minigame.pointer-base-speed", 6); }
    public int getScrubFailPenalty() { return config.getInt("minigame.scrub-fail-penalty", 15); }

    // ---------- Pomocnicze ----------
    private Location parseLocation(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.split(",");
        if (parts.length < 6) return null;
        try {
            return new Location(Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
        } catch (Exception e) { return null; }
    }
    private String serializeLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
    }
}