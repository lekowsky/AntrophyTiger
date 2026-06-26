package pl.skyrise.skyRiseJobs.api.job;

import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * Generyczna progresja poziom/exp pracy.
 *
 * Czyta z configu pracy:
 *   max-level
 *   xp-per-level.<n>     (domyślnie 100 * n)
 *
 * Zastępuje kopiowany per-praca feature LevelManager.
 */
public class JobLevels {

    private final BaseJobModule job;

    public JobLevels(BaseJobModule job) {
        this.job = job;
    }

    private FileConfiguration cfg() { return job.getJobConfig().getConfig(); }

    public int getMaxLevel() { return cfg().getInt("max-level", 5); }

    public int getXpForLevel(int level) { return cfg().getInt("xp-per-level." + level, 100 * level); }

    /** Dodaje XP pracy, obsługuje awanse i komunikaty. */
    public void addXp(Player player, int amount) {
        JobDataStore store = job.getDataStore();
        JobData data = store.load(player.getUniqueId());
        int maxLevel = getMaxLevel();
        if (data.level >= maxLevel) return;

        int newXP = data.xp + amount;
        int xpForNext = getXpForLevel(data.level + 1);
        while (newXP >= xpForNext && data.level < maxLevel) {
            newXP -= xpForNext;
            data.level++;
            job.sendJobMessage(player, "&a▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            job.sendJobMessage(player, "&6★ &eAwansowałeś na poziom &c" + data.level
                    + " &ew pracy &f" + job.getDisplayName() + "&e! &6★");
            job.sendJobMessage(player, "&a▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            if (data.level < maxLevel) xpForNext = getXpForLevel(data.level + 1);
        }
        data.xp = newXP;
        store.savePlayer(player.getUniqueId(), data);
    }

    public int getProgressPercent(Player player) {
        JobData data = job.getDataStore().load(player.getUniqueId());
        int maxLevel = getMaxLevel();
        if (data.level >= maxLevel) return 100;
        int xpForNext = getXpForLevel(data.level + 1);
        if (xpForNext <= 0) return 0;
        return Math.min(100, (data.xp * 100) / xpForNext);
    }
}
