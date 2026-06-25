package pl.skyrise.skyRiseJobs.features.windowcleaning.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) { meta.setDisplayName(name.replace('&', '§')); return this; }

    public ItemBuilder setLore(List<String> lore) {
        List<String> colored = new ArrayList<>();
        for (String line : lore) colored.add(line.replace('&', '§'));
        meta.setLore(colored);
        return this;
    }

    public ItemBuilder addLore(String... lines) {
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        for (String line : lines) lore.add(line.replace('&', '§'));
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) { meta.setCustomModelData(data); return this; }

    public ItemBuilder setUnbreakable(boolean unbreakable) { meta.setUnbreakable(unbreakable); return this; }

    public ItemBuilder setGlowing(boolean glowing) {
        if (glowing) { meta.addEnchant(Enchantment.UNBREAKING, 1, true); meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); }
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        if (meta instanceof SkullMeta skullMeta) skullMeta.setOwner(owner);
        return this;
    }

    public ItemStack build() { item.setItemMeta(meta); return item; }
}