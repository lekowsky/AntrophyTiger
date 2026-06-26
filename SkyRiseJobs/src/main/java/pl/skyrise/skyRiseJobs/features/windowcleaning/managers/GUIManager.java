package pl.skyrise.skyRiseJobs.features.windowcleaning.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;
import pl.skyrise.skyRiseJobs.features.windowcleaning.utils.ItemBuilder;

import java.util.Arrays;

public class GUIManager {

    private final WindowCleaningJobModule plugin;

    public GUIManager(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    public void openMainJobGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§9§lMycie Okien");

        ItemStack bg = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, bg);
            }
        }

        int level = plugin.getDataManager().getPlayerLevel(player);
        int xp = plugin.getDataManager().getPlayerXP(player);
        int nextLevelXp = (level < plugin.getConfigManager().getMaxLevel())
                ? plugin.getConfigManager().getXpForLevel(level + 1) : 0;
        double totalEarned = plugin.getDataManager().getTotalEarned(player);
        int skillPoints = plugin.getDataManager().getSkillPoints(player);
        int progress = plugin.getLevelManager().getProgressPercent(player);
        double earningsBonus = plugin.getConfigManager().getEarningsPerLevel() * level * 100.0;

        ItemStack stats = new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullOwner(player.getName())
                .setName("§b§lTwoje statystyki")
                .setLore(Arrays.asList(
                        " ",
                        "§7Poziom: §f" + level + " §7/ §f" + plugin.getConfigManager().getMaxLevel(),
                        "§7Doświadczenie: §f" + xp + "§7/§f" + nextLevelXp + " §7(" + progress + "%)",
                        "§7Łącznie zarobione: §6" + String.format("%.2f", totalEarned) + " $",
                        "§7Punkty umiejętności: §b" + skillPoints,
                        "§7Premia do zarobku: §a+" + String.format("%.0f", earningsBonus) + "%",
                        " "
                ))
                .build();
        gui.setItem(11, stats);

        ItemStack actionButton;
        if (!plugin.getJobManager().hasActiveSession(player)) {
            actionButton = new ItemBuilder(Material.LIME_WOOL)
                    .setName("§a§lRozpocznij pracę")
                    .setLore(Arrays.asList("§7Kliknij, aby rozpocząć zlecenie."))
                    .build();
        } else {
            JobSession session = plugin.getJobManager().getSession(player);
            if (session.isJobCompleted()) {
                actionButton = new ItemBuilder(Material.GOLD_INGOT)
                        .setName("§6§lZakończ pracę")
                        .setLore(Arrays.asList("§7Kliknij, aby odebrać wypłatę."))
                        .build();
            } else {
                actionButton = new ItemBuilder(Material.RED_WOOL)
                        .setName("§c§lAnuluj pracę")
                        .setLore(Arrays.asList("§7Kliknij, aby zrezygnować.", "§cUtrata wypłaty i doświadczenia."))
                        .build();
            }
        }
        gui.setItem(13, actionButton);

        gui.setItem(14, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName("§b§lDrzewko umiejętności")
                .setLore(Arrays.asList("§7Wydaj punkty, aby ulepszyć swoje zdolności."))
                .build());

        gui.setItem(15, new ItemBuilder(Material.BOOK)
                .setName("§e§lJak pracować?")
                .setLore(Arrays.asList(
                        "§71. Rozpocznij pracę.",
                        "§72. Zamocz szczotkę (PPM w powietrze).",
                        "§73. Wjedź windą ▲.",
                        "§74. Myj szare szyby.",
                        "§75. Wróć windą ▼ i odbierz wypłatę."
                ))
                .setGlowing(true)
                .build());

        player.openInventory(gui);
    }

    public void openSkillTreeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, "§9§lDrzewko umiejętności");

        ItemStack bg = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 36; i++) {
            if (i < 9 || i >= 27 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, bg);
            }
        }

        int points = plugin.getDataManager().getSkillPoints(player);
        gui.setItem(4, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName("§bDostępne punkty: §f" + points)
                .build());

        gui.setItem(11, createSkillIcon(player, "szybsze_szorowanie", Material.DIAMOND_PICKAXE, "§b§lSzybsze szorowanie"));
        gui.setItem(13, createSkillIcon(player, "wytrzymalosc", Material.IRON_CHESTPLATE, "§7§lWytrzymałość"));
        gui.setItem(15, createSkillIcon(player, "wieksza_strefa", Material.TARGET, "§e§lWiększa strefa"));

        gui.setItem(20, createSkillIcon(player, "czysta_robota", Material.NETHER_STAR, "§d§lCzysta robota"));
        gui.setItem(22, createSkillIcon(player, "darmowe_namaczanie", Material.WATER_BUCKET, "§3§lDarmowe namaczanie"));
        gui.setItem(24, createSkillIcon(player, "bonus_za_komplet", Material.DIAMOND, "§6§lBonus za komplet"));

        gui.setItem(31, new ItemBuilder(Material.ARROW).setName("§c§lPowrót").build());

        player.openInventory(gui);
    }

    private ItemStack createSkillIcon(Player player, String skillId, Material material, String name) {
        int level = plugin.getSkillManager().getSkillLevel(player, skillId);
        int max = plugin.getSkillManager().getMaxSkillLevel(skillId);
        int playerLevel = plugin.getDataManager().getPlayerLevel(player);
        int reqLevel = plugin.getConfigManager().getRequiredPlayerLevel(skillId);

        String statusLine;
        if (playerLevel < reqLevel) {
            statusLine = "§cWymagany poziom: " + reqLevel;
        } else if (level >= max) {
            statusLine = "§cMaksymalny poziom";
        } else {
            int cost = plugin.getSkillManager().getUpgradeCost(skillId, level + 1);
            statusLine = "§eKoszt ulepszenia: §f" + cost + " punktów";
        }

        return new ItemBuilder(material)
                .setName(name + " §7[" + level + "/" + max + "]")
                .setLore(Arrays.asList("§7" + getSkillDescription(skillId), " ", statusLine))
                .build();
    }

    private String getSkillDescription(String skillId) {
        return switch (skillId) {
            case "szybsze_szorowanie" -> "Zwiększa siłę szorowania.";
            case "wytrzymalosc" -> "Zmniejsza karę za chybienie.";
            case "wieksza_strefa" -> "Poszerza zieloną strefę.";
            case "czysta_robota" -> "Premia za każde perfekcyjne okno.";
            case "darmowe_namaczanie" -> "Szansa na darmowe namoczenie.";
            case "bonus_za_komplet" -> "Mnożnik za 100% perfekcyjnych okien.";
            default -> "";
        };
    }
}