package pl.skyrise.skyRiseJobs.features.windowcleaning.listeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.minigame.WindowMinigame;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;

import java.util.List;

public class WindowCleaningListener implements Listener {

    private final WindowCleaningJobModule plugin;

    public WindowCleaningListener(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWindowClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        JobSession session = plugin.getJobManager().getSession(player);

        // Blokada wylewania wody z wiaderka pracy
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
            ItemStack item = event.getItem();
            if (item != null && isBucket(item)) {
                event.setCancelled(true);
                if (session != null) {
                    player.sendMessage(plugin.formatJobMessage("§cNie możesz wylać płynu z wiaderka! Użyj go do namaczania szczotki."));
                }
                return;
            }
        }

        if (session == null) return;

        ItemStack hand = player.getInventory().getItemInMainHand();

        // Zamaczanie szczotki
        if (event.getAction() == Action.RIGHT_CLICK_AIR ||
                (event.getAction() == Action.RIGHT_CLICK_BLOCK && !isWindowBlock(event.getClickedBlock()))) {

            if (isBrush(hand) && hasBucket(player)) {
                session.setBrushDipped(true);
                player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1f, 1.2f);
                player.sendMessage(plugin.formatJobMessage("§a✔ Szczotka zamoczona w płynie. Możesz myć okna."));
                event.setCancelled(true);
                return;
            }
        }

        // Mycie okna
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!isWindowBlock(block)) return;

        if (!session.getAllWindows().contains(block.getLocation())) {
            player.sendMessage(plugin.formatJobMessage("§cTo okno nie jest przeznaczone do mycia w tym zleceniu!"));
            return;
        }
        if (session.isWindowCleaned(block.getLocation())) {
            player.sendMessage(plugin.formatJobMessage("§cTo okno zostało już umyte!"));
            return;
        }

        if (!isBrush(hand)) {
            player.sendMessage(plugin.formatJobMessage("§cMusisz trzymać szczotkę do okien!"));
            return;
        }

        if (!session.isBrushDipped()) {
            player.sendMessage(plugin.formatJobMessage("§cNajpierw musisz zamoczyć szczotkę! Kliknij PPM w powietrze trzymając szczotkę i mając wiaderko."));
            return;
        }

        if (!plugin.getConfigManager().isInRegion(block.getLocation())) {
            player.sendMessage(plugin.formatJobMessage("§cTo okno nie znajduje się w wyznaczonym obszarze pracy!"));
            return;
        }

        WindowMinigame game = new WindowMinigame(plugin, player, block.getLocation());
        session.setCurrentMinigame(game);
        game.open();
        event.setCancelled(true);
    }

    private boolean isBrush(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().hasCustomModelData() &&
                item.getItemMeta().getCustomModelData() == plugin.getConfigManager().getBrushModelData();
    }

    private boolean isBucket(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().hasCustomModelData() &&
                item.getItemMeta().getCustomModelData() == plugin.getConfigManager().getBucketModelData();
    }

    private boolean hasBucket(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isBucket(item)) return true;
        }
        return false;
    }

    private boolean isWindowBlock(Block block) {
        if (block == null) return false;
        List<String> allowedTypes = plugin.getConfigManager().getWindowBlockTypes();
        return allowedTypes.contains(block.getType().name());
    }
}