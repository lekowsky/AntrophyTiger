package pl.skyrise.skyRiseJobs.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Wspólny budowniczy przedmiotów dla całego pluginu.
 *
 * Udostępnia zarówno nowe (MiniMessage/legacy aware) API: {@code name/lore/glow/customModelData},
 * jak i klasyczne {@code setX} używane przez moduły prac — dzięki temu nie trzeba duplikować klas.
 */
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

    // ---------- Nowe API (legacy '&' aware, Component) ----------

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

    // ---------- Klasyczne API (§ aware) — zgodność z modułami prac ----------

    public ItemBuilder setName(String name) {
        if (meta != null && name != null) meta.setDisplayName(name.replace('&', '§'));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (meta != null && lore != null) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) colored.add(line == null ? "" : line.replace('&', '§'));
            meta.setLore(colored);
        }
        return this;
    }

    public ItemBuilder addLore(String... lines) {
        if (meta != null && lines != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            for (String line : lines) lore.add(line == null ? "" : line.replace('&', '§'));
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (meta != null && data > 0) meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (meta != null) meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder setGlowing(boolean glowing) {
        if (meta != null && glowing) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        if (meta instanceof SkullMeta skullMeta && owner != null) skullMeta.setOwner(owner);
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }
}
