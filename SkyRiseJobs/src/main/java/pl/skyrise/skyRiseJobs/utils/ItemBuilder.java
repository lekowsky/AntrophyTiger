package pl.skyrise.skyRiseJobs.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material != null ? material : Material.STONE, Math.max(1, Math.min(64, amount)));
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(String legacyText) {
        if (meta != null) meta.displayName(ColorUtil.legacy(legacyText));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        if (meta != null && lines != null) meta.lore(lines.stream().map(ColorUtil::legacy).toList());
        return this;
    }

    public ItemBuilder lore(String... lines) {
        if (lines == null) return this;
        return lore(List.of(lines));
    }

    public ItemBuilder addLore(String legacyText) {
        if (meta != null) {
            List<net.kyori.adventure.text.Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(ColorUtil.legacy(legacyText));
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder hideFlags() {
        if (meta != null) meta.addItemFlags(ItemFlag.values());
        return this;
    }

    public ItemBuilder customModelData(int data) {
        if (meta != null && data > 0) meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder pdc(JavaPlugin plugin, String key, String value) {
        if (meta != null && plugin != null && key != null && value != null) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, key), PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
