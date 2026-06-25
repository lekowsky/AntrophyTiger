package pl.skyrise.skyRiseJobs.features.windowcleaning.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;

public class PlayerStateListener implements Listener {

    private final WindowCleaningJobModule plugin;

    public PlayerStateListener(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getJobManager().hasActiveSession(player)) {
            plugin.getJobManager().forceEndJob(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (plugin.getJobManager().hasActiveSession(player)) {
            // Usuwamy przedmioty pracy z listy dropów, aby nie wypadły
            event.getDrops().removeIf(item ->
                    item.hasItemMeta() &&
                            item.getItemMeta().hasCustomModelData() &&
                            (item.getItemMeta().getCustomModelData() == plugin.getConfigManager().getBrushModelData() ||
                                    item.getItemMeta().getCustomModelData() == plugin.getConfigManager().getBucketModelData())
            );
            // Następnie usuwamy je z ekwipunku (dla pewności)
            plugin.getJobManager().forceEndJob(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (plugin.getJobManager().hasActiveSession(player)) {
            plugin.getJobManager().forceEndJob(player);
        }
    }
}