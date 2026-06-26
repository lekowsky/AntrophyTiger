package pl.skyrise.skyRiseJobs.api.job;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.gui.GuiMenu;
import pl.skyrise.skyRiseJobs.utils.ItemBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Wspólne GUI każdej pracy:
 *  - główne menu pracy (statystyki + przycisk akcji + drzewko + instrukcja),
 *  - drzewko umiejętności (perki definiowane przez pracę).
 *
 * Renderowane przez {@link GuiMenu} (akcje przypięte do slotów), więc nie ma kruchego
 * porównywania tytułów/nazw itemów. To jest "jak ma prezentować się każde GUI pracy".
 *
 * Każda praca dziedziczy to GUI bez kopiowania kodu — różnice (statystyki, opis pracy,
 * stan przycisku akcji, instrukcja) pochodzą z {@link BaseJobModule}.
 */
public class JobGui {

    private final BaseJobModule job;

    public JobGui(BaseJobModule job) {
        this.job = job;
    }

    // ---------- Główne menu pracy ----------

    public void openMain(Player player) {
        String title = job.getMenuColor() + "&l" + job.getDisplayName();
        GuiMenu menu = new GuiMenu(title, 27);
        fillBorder(menu, 27);

        menu.setItem(11, buildStatsIcon(player));
        menu.setItem(13, buildActionButton(player), e -> {
            BaseJobModule.WorkState state = job.getWorkState(player);
            player.closeInventory();
            switch (state) {
                case IDLE -> job.startWork(player);
                case READY_TO_FINISH -> job.completeWork(player);
                case IN_PROGRESS -> job.cancelWork(player);
            }
        });

        if (!job.getSkills().isEmpty()) {
            menu.setItem(14, new ItemBuilder(Material.ENCHANTED_BOOK)
                    .setName("&b&lDrzewko umiejętności")
                    .setLore(List.of("&7Wydaj punkty, aby ulepszyć swoje zdolności."))
                    .build(), e -> openSkillTree(player));
        }

        List<String> howTo = job.getHowToWork();
        if (howTo != null && !howTo.isEmpty()) {
            menu.setItem(15, new ItemBuilder(Material.BOOK)
                    .setName("&e&lJak pracować?")
                    .setLore(howTo)
                    .setGlowing(true)
                    .build());
        }

        menu.open(player);
    }

    private ItemStack buildStatsIcon(Player player) {
        JobData data = job.getDataStore().load(player.getUniqueId());
        int maxLevel = job.getLevels().getMaxLevel();
        int nextLevelXp = data.level < maxLevel ? job.getLevels().getXpForLevel(data.level + 1) : 0;
        int progress = job.getLevels().getProgressPercent(player);

        List<String> lore = new ArrayList<>();
        lore.add(" ");
        lore.add("&7Poziom: &f" + data.level + " &7/ &f" + maxLevel);
        lore.add("&7Doświadczenie: &f" + data.xp + "&7/&f" + nextLevelXp + " &7(" + progress + "%)");
        lore.add("&7Łącznie zarobione: &6" + String.format("%.2f", data.totalEarned) + " $");
        if (!job.getSkills().isEmpty()) lore.add("&7Punkty umiejętności: &b" + data.skillPoints);
        List<String> extra = job.getStatsLines(player, data);
        if (extra != null) lore.addAll(extra);
        lore.add(" ");

        return new ItemBuilder(Material.PLAYER_HEAD)
                .setSkullOwner(player.getName())
                .setName("&b&lTwoje statystyki")
                .setLore(lore)
                .build();
    }

    private ItemStack buildActionButton(Player player) {
        return switch (job.getWorkState(player)) {
            case IDLE -> new ItemBuilder(Material.LIME_WOOL)
                    .setName("&a&lRozpocznij pracę")
                    .setLore(List.of("&7Kliknij, aby rozpocząć zlecenie."))
                    .build();
            case READY_TO_FINISH -> new ItemBuilder(Material.GOLD_INGOT)
                    .setName("&6&lZakończ pracę")
                    .setLore(List.of("&7Kliknij, aby odebrać wypłatę."))
                    .build();
            case IN_PROGRESS -> new ItemBuilder(Material.RED_WOOL)
                    .setName("&c&lAnuluj pracę")
                    .setLore(List.of("&7Kliknij, aby zrezygnować.", "&cUtrata wypłaty i doświadczenia."))
                    .build();
        };
    }

    // ---------- Drzewko umiejętności ----------

    public void openSkillTree(Player player) {
        String title = job.getMenuColor() + "&lDrzewko umiejętności";
        GuiMenu menu = new GuiMenu(title, 36);
        fillBorder(menu, 36);

        JobData data = job.getDataStore().load(player.getUniqueId());
        menu.setItem(4, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName("&bDostępne punkty: &f" + data.skillPoints)
                .build());

        int autoSlot = 10;
        for (JobSkill skill : job.getSkills()) {
            int slot = skill.getSlot() >= 0 ? skill.getSlot() : nextFreeSlot(menu, autoSlot);
            if (skill.getSlot() < 0) autoSlot = slot + 1;
            menu.setItem(slot, buildSkillIcon(player, data, skill), e -> {
                job.getSkillService().upgrade(player, skill);
                openSkillTree(player);
            });
        }

        menu.setItem(menu.getInventory().getSize() - 5, new ItemBuilder(Material.ARROW)
                .setName("&c&lPowrót").build(), e -> openMain(player));

        menu.open(player);
    }

    private ItemStack buildSkillIcon(Player player, JobData data, JobSkill skill) {
        JobSkillService service = job.getSkillService();
        int level = data.skillLevel(skill.getId());
        int max = service.getMaxLevel(skill);
        int reqLevel = service.getRequiredJobLevel(skill);

        String statusLine;
        if (data.level < reqLevel) statusLine = "&cWymagany poziom: " + reqLevel;
        else if (level >= max) statusLine = "&cMaksymalny poziom";
        else statusLine = "&eKoszt ulepszenia: &f" + service.getCost(skill, level + 1) + " punktów";

        List<String> lore = new ArrayList<>();
        for (String line : skill.getDescription()) lore.add("&7" + line);
        lore.add(" ");
        lore.add(statusLine);

        return new ItemBuilder(skill.getIcon())
                .setName(service.getName(skill) + " &7[" + level + "/" + max + "]")
                .setLore(lore)
                .setGlowing(level > 0)
                .build();
    }

    // ---------- Pomocnicze ----------

    private void fillBorder(GuiMenu menu, int size) {
        ItemStack bg = new ItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < size; i++) {
            if (i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8) menu.setItem(i, bg);
        }
    }

    private int nextFreeSlot(GuiMenu menu, int from) {
        int size = menu.getInventory().getSize();
        int slot = from;
        while (slot < size && (slot % 9 == 0 || slot % 9 == 8 || menu.getInventory().getItem(slot) != null)) slot++;
        return Math.min(slot, size - 1);
    }
}
