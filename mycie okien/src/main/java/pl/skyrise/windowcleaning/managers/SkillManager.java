package pl.skyrise.windowcleaning.managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;

public class SkillManager {
    private final WindowCleaningPlugin plugin;
    public SkillManager(WindowCleaningPlugin plugin) { this.plugin = plugin; }

    public int getSkillLevel(Player player, String skillId) {
        return plugin.getDataManager().getSkillLevel(player, skillId);
    }

    public void upgradeSkill(Player player, String skillId) {
        int current = getSkillLevel(player, skillId);
        int max = plugin.getConfigManager().getMaxSkillLevel(skillId);
        if (current >= max) {
            player.sendMessage("§cOsiągnąłeś maksymalny poziom tej umiejętności!");
            return;
        }
        int playerLevel = plugin.getDataManager().getPlayerLevel(player);
        int requiredLevel = plugin.getConfigManager().getRequiredPlayerLevel(skillId);
        if (playerLevel < requiredLevel) {
            player.sendMessage("§cPotrzebujesz poziomu " + requiredLevel + " w pracy, aby odblokować tę umiejętność!");
            return;
        }
        int points = plugin.getDataManager().getSkillPoints(player);
        int cost = plugin.getConfigManager().getSkillCost(skillId, current + 1);
        if (points < cost) {
            player.sendMessage("§cNie masz wystarczającej liczby punktów! Potrzebujesz " + cost);
            return;
        }
        plugin.getDataManager().setSkillPoints(player, points - cost);
        plugin.getDataManager().setSkillLevel(player, skillId, current + 1);
        player.sendMessage("§aUlepszono §e" + getSkillName(skillId) + " §ado poziomu §e" + (current + 1));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    public String getSkillName(String skillId) {
        return plugin.getConfig().getString("skills." + skillId + ".name", skillId);
    }
    public int getMaxSkillLevel(String skillId) {
        return plugin.getConfigManager().getMaxSkillLevel(skillId);
    }
    public int getUpgradeCost(String skillId, int nextLevel) {
        return plugin.getConfigManager().getSkillCost(skillId, nextLevel);
    }
}