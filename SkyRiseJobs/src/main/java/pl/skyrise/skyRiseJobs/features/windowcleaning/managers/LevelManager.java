package pl.skyrise.skyRiseJobs.features.windowcleaning.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;

public class LevelManager {

    private final WindowCleaningJobModule plugin;

    public LevelManager(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    public void addXP(Player player, int amount) {
        int currentXP = plugin.getDataManager().getPlayerXP(player);
        int currentLevel = plugin.getDataManager().getPlayerLevel(player);
        int maxLevel = plugin.getConfigManager().getMaxLevel();

        if (currentLevel >= maxLevel) return;

        int xpForNext = plugin.getConfigManager().getXpForLevel(currentLevel + 1);
        int newXP = currentXP + amount;

        while (newXP >= xpForNext && currentLevel < maxLevel) {
            newXP -= xpForNext;
            currentLevel++;
            player.sendMessage(plugin.formatJobMessage("§a▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.sendMessage(plugin.formatJobMessage("§6★ §eAwansowałeś na poziom §c" + currentLevel + " §ew pracy mycia okien! §6★"));
            player.sendMessage(plugin.formatJobMessage("§a▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // UWAGA: Punkty umiejętności są teraz przyznawane losowo po ukończeniu pracy,
            // a nie za awans. Dlatego usunięto linie dodające punkty tutaj.

            if (currentLevel < maxLevel) {
                xpForNext = plugin.getConfigManager().getXpForLevel(currentLevel + 1);
            }
        }

        plugin.getDataManager().setPlayerLevel(player, currentLevel);
        plugin.getDataManager().setPlayerXP(player, newXP);
    }

    public int getProgressPercent(Player player) {
        int currentLevel = plugin.getDataManager().getPlayerLevel(player);
        int maxLevel = plugin.getConfigManager().getMaxLevel();
        if (currentLevel >= maxLevel) return 100;

        int currentXP = plugin.getDataManager().getPlayerXP(player);
        int xpForNext = plugin.getConfigManager().getXpForLevel(currentLevel + 1);
        return Math.min(100, (currentXP * 100) / xpForNext);
    }
}