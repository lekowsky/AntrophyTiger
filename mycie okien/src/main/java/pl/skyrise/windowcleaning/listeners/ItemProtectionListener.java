package pl.skyrise.windowcleaning.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;

public class ItemProtectionListener implements Listener {

    private final WindowCleaningPlugin plugin;

    public ItemProtectionListener(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!plugin.getJobManager().hasActiveSession(event.getPlayer())) return;
        if (isJobItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNie możesz wyrzucić przedmiotów do pracy!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getJobManager().hasActiveSession(player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean currentIsJob = isJobItem(current);
        boolean cursorIsJob = isJobItem(cursor);

        // Normalne kliknięcie – przenoszenie do innego pojemnika
        if (event.getClickedInventory() != null && event.getClickedInventory().getType() != InventoryType.PLAYER) {
            if (currentIsJob || cursorIsJob) {
                event.setCancelled(true);
                player.sendMessage("§cNie możesz przenosić przedmiotów pracy do innych pojemników!");
            }
        }

        // Shift + klik – próba szybkiego przeniesienia
        if (event.isShiftClick() && currentIsJob) {
            Inventory topInv = event.getView().getTopInventory();
            if (topInv != null && topInv.getType() != InventoryType.PLAYER) {
                event.setCancelled(true);
                player.sendMessage("§cNie możesz przenosić przedmiotów pracy!");
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getJobManager().hasActiveSession(player)) return;

        boolean involvesOutside = event.getInventorySlots().stream()
                .anyMatch(slot -> {
                    Inventory inv = event.getView().getInventory(slot);
                    return inv != null && inv.getType() != InventoryType.PLAYER;
                });

        if (!involvesOutside) return;

        for (ItemStack item : event.getNewItems().values()) {
            if (isJobItem(item)) {
                event.setCancelled(true);
                player.sendMessage("§cNie możesz przenosić przedmiotów pracy do innych pojemników!");
                return;
            }
        }
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (!plugin.getJobManager().hasActiveSession(event.getPlayer())) return;
        if (isJobItem(event.getMainHandItem()) || isJobItem(event.getOffHandItem())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cNie możesz zamieniać przedmiotów pracy w drugą rękę!");
        }
    }

    private boolean isJobItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        if (item.hasItemMeta() && item.getItemMeta().hasCustomModelData()) {
            int model = item.getItemMeta().getCustomModelData();
            return model == plugin.getConfigManager().getBrushModelData() ||
                    model == plugin.getConfigManager().getBucketModelData();
        }
        return false;
    }
}