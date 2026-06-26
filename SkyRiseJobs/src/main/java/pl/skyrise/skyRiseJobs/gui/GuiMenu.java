package pl.skyrise.skyRiseJobs.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class GuiMenu implements InventoryHolder {
    private final Inventory inventory;
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    public GuiMenu(String titleLegacy, int size) {
        this.inventory = Bukkit.createInventory(this, normalizeSize(size), ColorUtil.auto(titleLegacy));
    }

    public void setItem(int slot, ItemStack item) { setItem(slot, item, null); }

    public void setItem(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, item);
        if (action != null) actions.put(slot, action);
        else actions.remove(slot);
    }

    public void open(Player player) { player.openInventory(inventory); }

    public void handle(InventoryClickEvent event) {
        Consumer<InventoryClickEvent> action = actions.get(event.getRawSlot());
        if (action != null) action.accept(event);
    }

    @Override public @NotNull Inventory getInventory() { return inventory; }

    private static int normalizeSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        return ((normalized + 8) / 9) * 9;
    }
}
