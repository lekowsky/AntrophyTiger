package pl.skyrise.skyRiseJobs.features.windowcleaning.managers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;
import pl.skyrise.skyRiseJobs.features.windowcleaning.utils.ItemBuilder;

import java.util.*;

public class JobManager {

    private final WindowCleaningJobModule plugin;
    private final Map<UUID, JobSession> activeSessions = new HashMap<>();

    public JobManager(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    public void startJob(Player player) {
        if (activeSessions.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.formatJobMessage("§cMasz już aktywną pracę!"));
            return;
        }
        // Sprawdzenie miejsca w ekwipunku
        int freeSlots = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (item == null || item.getType() == Material.AIR) freeSlots++;
        }
        if (freeSlots < 2) {
            player.sendMessage(plugin.formatJobMessage("§cNie masz wystarczająco wolnego miejsca w ekwipunku! Potrzebujesz 2 slotów."));
            return;
        }

        int level = plugin.getDataManager().getPlayerLevel(player);
        double earningsMultiplier = 1.0 + (level * plugin.getConfigManager().getEarningsPerLevel());

        List<Location> allWindows = plugin.getParticleManager().generateWindowsInRegion(player.getWorld());
        int totalRequired = plugin.getConfigManager().getTotalWindowsToClean();
        if (allWindows.size() < totalRequired) {
            player.sendMessage(plugin.formatJobMessage("§cW regionie jest za mało szyb! Skontaktuj się z administratorem."));
            return;
        }
        Collections.shuffle(allWindows);
        List<Location> selectedWindows = allWindows.subList(0, totalRequired);

        JobSession session = new JobSession(player.getUniqueId(), level, earningsMultiplier, selectedWindows, totalRequired);
        activeSessions.put(player.getUniqueId(), session);
        giveJobItems(player);
        plugin.getParticleManager().startParticles(player);
        player.sendMessage(plugin.formatJobMessage("§aRozpocząłeś pracę! Udaj się do §bOperatora windy §a(▲§b), aby wjechać na platformę."));
    }

    public void completeJob(Player player) {
        JobSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) {
            player.sendMessage(plugin.formatJobMessage("§cNie masz aktywnej pracy."));
            return;
        }
        double baseEarnings = plugin.getConfigManager().getBaseEarnings();
        double total = baseEarnings * session.getEarningsMultiplier();

        int completionLevel = plugin.getSkillManager().getSkillLevel(player, "bonus_za_komplet");
        if (completionLevel > 0 && session.getPerfectWindows() == session.getTotalRequired()) {
            double mult = 1.0 + completionLevel * plugin.getConfigManager().getCompletionBonusMultiplierPerLevel();
            total *= mult;
            player.sendMessage(plugin.formatJobMessage("§d★ Bonus za perfekcyjny komplet! ×" + String.format("%.1f", mult) + " ★"));
        }
        int perfectLevel = plugin.getSkillManager().getSkillLevel(player, "czysta_robota");
        if (perfectLevel > 0 && session.getPerfectWindows() > 0) {
            int bonus = session.getPerfectWindows() * perfectLevel * plugin.getConfigManager().getPerfectBonusPerLevel();
            total += bonus;
            player.sendMessage(plugin.formatJobMessage("§e★ Czysta robota: +" + bonus + " $ za " + session.getPerfectWindows() + " perfekcyjnych okien ★"));
        }

        total = plugin.applyGlobalMoneyBoost(total);
        plugin.getEconomy().depositPlayer(player, total);
        int xpGained = plugin.getConfigManager().getBaseXP();
        plugin.getLevelManager().addXP(player, xpGained);
        plugin.addGlobalCharacterExp(player, xpGained);
        plugin.getDataManager().addCompletedJob(player, total);

        double chance = plugin.getConfigManager().getSkillPointDropChance();
        if (Math.random() < chance) {
            int points = plugin.getDataManager().getSkillPoints(player);
            plugin.getDataManager().setSkillPoints(player, points + 1);
            player.sendMessage(plugin.formatJobMessage("§b★ Zdobyłeś punkt umiejętności! ★"));
        }
        removeJobItems(player);
        plugin.getParticleManager().stopParticles(player);
        player.sendMessage(plugin.formatJobMessage("§aPraca zakończona! Zarobiłeś: §e" + String.format("%.2f", total) + " $"));
    }

    public void forceEndJob(Player player) {
        JobSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            removeJobItems(player);
            plugin.getParticleManager().stopParticles(player);
            player.sendMessage(plugin.formatJobMessage("§cTwoja praca została anulowana."));
        }
    }

    public void resetAllProgress(Player player) {
        forceEndJob(player);
        plugin.getDataManager().resetPlayer(player);
        player.sendMessage(plugin.formatJobMessage("§cTwoje postępy w pracy mycia okien zostały całkowicie zresetowane."));
    }

    private void giveJobItems(Player player) {
        ItemStack brush = new ItemBuilder(Material.STICK)
                .setName(plugin.getConfigManager().getBrushName())
                .setCustomModelData(plugin.getConfigManager().getBrushModelData())
                .addLore("§7Narzędzie do mycia okien")
                .setUnbreakable(true)
                .build();
        ItemStack bucket = new ItemBuilder(Material.WATER_BUCKET)
                .setName(plugin.getConfigManager().getBucketName())
                .setCustomModelData(plugin.getConfigManager().getBucketModelData())
                .addLore("§7Płyn do mycia")
                .setUnbreakable(true)
                .build();
        player.getInventory().addItem(brush, bucket);
    }

    private void removeJobItems(Player player) {
        player.getInventory().remove(Material.STICK);
        player.getInventory().remove(Material.WATER_BUCKET);
    }

    public JobSession getSession(Player player) {
        return activeSessions.get(player.getUniqueId());
    }
    public boolean hasActiveSession(Player player) {
        return activeSessions.containsKey(player.getUniqueId());
    }
}