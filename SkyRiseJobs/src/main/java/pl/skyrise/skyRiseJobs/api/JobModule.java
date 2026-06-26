package pl.skyrise.skyRiseJobs.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Bazowy kontrakt dla pojedynczej pracy w folderze features.
 */
public interface JobModule extends Module {
    String getJobId();
    String getDisplayName();
    List<String> getDescription();
    ItemStack getIcon(Player player);
    default String getMenuColor() { return "&#e36c1e"; }
    boolean isAvailable(Player player);
    void openMenu(Player player);
}
