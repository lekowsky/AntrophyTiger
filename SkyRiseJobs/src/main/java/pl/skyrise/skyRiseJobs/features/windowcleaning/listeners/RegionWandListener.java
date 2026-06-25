package pl.skyrise.skyRiseJobs.features.windowcleaning.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;

public class RegionWandListener implements Listener {

    private final WindowCleaningJobModule plugin;

    public RegionWandListener(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        if (event.getItem() == null) return;
        if (event.getItem().getType() != Material.STICK) return;
        if (!event.getItem().hasItemMeta()) return;
        if (!event.getItem().getItemMeta().hasCustomModelData()) return;
        if (event.getItem().getItemMeta().getCustomModelData() != 9999) return;

        event.setCancelled(true);

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getConfigManager().setRegionPos1(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(plugin.formatJobMessage("§aUstawiono §ePIERWSZY §apunkt regionu."));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            plugin.getConfigManager().setRegionPos2(event.getClickedBlock().getLocation());
            event.getPlayer().sendMessage(plugin.formatJobMessage("§aUstawiono §eDRUGI §apunkt regionu."));
        }
    }
}