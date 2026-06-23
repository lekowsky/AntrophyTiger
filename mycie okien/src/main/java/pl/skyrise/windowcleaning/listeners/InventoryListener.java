package pl.skyrise.windowcleaning.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;
import pl.skyrise.windowcleaning.minigame.WindowMinigame;
import pl.skyrise.windowcleaning.session.JobSession;

public class InventoryListener implements Listener {

    private final WindowCleaningPlugin plugin;

    public InventoryListener(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        boolean isPluginGUI = title.equals("§9§lMycie Okien") ||
                title.equals("§9§lDrzewko umiejętności") ||
                title.contains("Mycie okna");

        if (!isPluginGUI) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemStack clicked = event.getCurrentItem();

        if (title.equals("§9§lMycie Okien")) {
            handleMainGUI(player, clicked);
        } else if (title.equals("§9§lDrzewko umiejętności")) {
            handleSkillTreeGUI(player, clicked);
        } else if (title.contains("Mycie okna")) {
            handleMinigameGUI(player, clicked);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.equals("§9§lMycie Okien") ||
                title.equals("§9§lDrzewko umiejętności") ||
                title.contains("Mycie okna")) {
            event.setCancelled(true);
        }
    }

    private void handleMainGUI(Player player, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();
        if (name.contains("Rozpocznij pracę")) {
            player.closeInventory();
            plugin.getJobManager().startJob(player);
        } else if (name.contains("Anuluj pracę")) {
            player.closeInventory();
            plugin.getJobManager().forceEndJob(player);
            player.sendMessage("§cZrezygnowałeś z pracy. Straciłeś wypłatę i doświadczenie.");
        } else if (name.contains("Zakończ pracę")) {
            JobSession session = plugin.getJobManager().getSession(player);
            if (session != null && session.isJobCompleted()) {
                player.closeInventory();
                plugin.getJobManager().completeJob(player);
            } else {
                player.closeInventory();
                player.sendMessage("§cNie umyłeś jeszcze wszystkich okien!");
            }
        } else if (name.contains("Drzewko umiejętności")) {
            plugin.getGUIManager().openSkillTreeGUI(player);
        }
    }

    private void handleSkillTreeGUI(Player player, ItemStack clicked) {
        String name = clicked.getItemMeta().getDisplayName();
        if (name.contains("Powrót")) {
            plugin.getGUIManager().openMainJobGUI(player);
        } else if (name.contains("Szybsze szorowanie")) {
            plugin.getSkillManager().upgradeSkill(player, "szybsze_szorowanie");
            plugin.getGUIManager().openSkillTreeGUI(player);
        } else if (name.contains("Wytrzymałość")) {
            plugin.getSkillManager().upgradeSkill(player, "wytrzymalosc");
            plugin.getGUIManager().openSkillTreeGUI(player);
        } else if (name.contains("Większa strefa")) {
            plugin.getSkillManager().upgradeSkill(player, "wieksza_strefa");
            plugin.getGUIManager().openSkillTreeGUI(player);
        } else if (name.contains("Czysta robota")) {
            plugin.getSkillManager().upgradeSkill(player, "czysta_robota");
            plugin.getGUIManager().openSkillTreeGUI(player);
        } else if (name.contains("Darmowe namaczanie")) {
            plugin.getSkillManager().upgradeSkill(player, "darmowe_namaczanie");
            plugin.getGUIManager().openSkillTreeGUI(player);
        } else if (name.contains("Bonus za komplet")) {
            plugin.getSkillManager().upgradeSkill(player, "bonus_za_komplet");
            plugin.getGUIManager().openSkillTreeGUI(player);
        }
    }

    private void handleMinigameGUI(Player player, ItemStack clicked) {
        if (clicked.getType() == Material.LIME_WOOL) {
            WindowMinigame game = plugin.getJobManager().getSession(player).getCurrentMinigame();
            if (game != null) game.handleScrub();
        }
    }
}