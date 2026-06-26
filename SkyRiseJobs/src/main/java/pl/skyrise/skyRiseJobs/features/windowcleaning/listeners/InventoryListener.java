package pl.skyrise.skyRiseJobs.features.windowcleaning.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.minigame.WindowMinigame;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;

/**
 * Obsługa GUI minigry mycia okna.
 *
 * Główne menu pracy i drzewko umiejętności korzystają z wspólnego {@code GuiMenu}
 * (obsługiwanego przez {@code GuiListener}), więc tutaj zostaje tylko logika minigry.
 */
public class InventoryListener implements Listener {

    private static final String MINIGAME_TITLE = "Mycie okna";

    private final WindowCleaningJobModule plugin;

    public InventoryListener(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().contains(MINIGAME_TITLE)) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() != Material.LIME_WOOL) return;

        JobSession session = plugin.getJobManager().getSession(player);
        if (session == null) return;
        WindowMinigame game = session.getCurrentMinigame();
        if (game != null) game.handleScrub();
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().contains(MINIGAME_TITLE)) event.setCancelled(true);
    }
}
