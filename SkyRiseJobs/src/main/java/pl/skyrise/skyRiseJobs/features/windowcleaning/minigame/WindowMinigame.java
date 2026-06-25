package pl.skyrise.skyRiseJobs.features.windowcleaning.minigame;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.utils.ItemBuilder;

import java.util.Random;

public class WindowMinigame {

    private final WindowCleaningJobModule plugin;
    private final Player player;
    private final Location windowLocation;
    private final Inventory gui;
    private int progress = 0;
    private boolean finished = false;
    private final Random random = new Random();

    private int pointerPosition = 0;
    private int pointerDirection = 1;
    private BukkitTask pointerTask;
    private boolean canClickInGreenZone = true;
    private boolean perfectWindow = true;

    private static final int PROGRESS_BAR_START = 9;
    private static final int SCRUB_BUTTON_SLOT = 22;

    public WindowMinigame(WindowCleaningJobModule plugin, Player player, Location windowLocation) {
        this.plugin = plugin;
        this.player = player;
        this.windowLocation = windowLocation;
        int size = plugin.getConfigManager().getMinigameGuiSize();
        this.gui = Bukkit.createInventory(null, size, "§8Mycie okna");
        this.perfectWindow = true;
        setupGUI();
        startPointerAnimation();
    }

    private void setupGUI() {
        ItemStack bg = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < gui.getSize(); i++) {
            if (i < 9 || i >= gui.getSize() - 9) gui.setItem(i, bg);
        }
        gui.setItem(SCRUB_BUTTON_SLOT, new ItemBuilder(Material.LIME_WOOL)
                .setName("§a§lSzoruj okno")
                .setLore(java.util.Arrays.asList("§7Kliknij, gdy wskaźnik jest w §a§lZIELONEJ §7strefie!"))
                .build());
        updatePointerDisplay();
    }

    private void startPointerAnimation() {
        int baseSpeed = plugin.getConfigManager().getPointerBaseSpeed();
        int skillLevel = plugin.getSkillManager().getSkillLevel(player, "szybsze_szorowanie");
        int speedReduction = skillLevel * plugin.getConfigManager().getPointerSpeedReductionPerLevel();
        long interval = Math.max(2, baseSpeed - speedReduction);

        pointerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (finished || !player.isOnline()) {
                    cancel();
                    return;
                }
                boolean wasInGreen = (pointerPosition >= 3 && pointerPosition <= 5);
                pointerPosition += pointerDirection;
                if (pointerPosition >= 8) { pointerPosition = 8; pointerDirection = -1; }
                else if (pointerPosition <= 0) { pointerPosition = 0; pointerDirection = 1; }

                boolean nowInGreen = (pointerPosition >= 3 && pointerPosition <= 5);
                if (!wasInGreen && nowInGreen) canClickInGreenZone = true;
                if (wasInGreen && !nowInGreen) canClickInGreenZone = false;
                updatePointerDisplay();
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, interval);
    }

    private void updatePointerDisplay() {
        int filledSlots = (progress * 9) / 100;
        for (int i = 0; i < 9; i++) {
            int slot = PROGRESS_BAR_START + i;
            Material mat;
            String name;
            if (i < filledSlots) {
                mat = Material.LIME_STAINED_GLASS_PANE;
                name = "§aPostęp: " + progress + "%";
            } else {
                mat = Material.WHITE_STAINED_GLASS_PANE;
                name = "§7Postęp: " + progress + "%";
            }
            if (i == pointerPosition) {
                mat = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
                name = "§b▼ Wskaźnik ▼";
            }
            gui.setItem(slot, new ItemBuilder(mat).setName(name).build());
        }
    }

    public void open() { player.openInventory(gui); }

    public void handleScrub() {
        if (finished) return;

        int scrubLevel = plugin.getSkillManager().getSkillLevel(player, "szybsze_szorowanie");
        int enduranceLevel = plugin.getSkillManager().getSkillLevel(player, "wytrzymalosc");
        int widerZoneLevel = plugin.getSkillManager().getSkillLevel(player, "wieksza_strefa");

        int greenStart = 3 - widerZoneLevel * plugin.getConfigManager().getZoneExpandPerLevel();
        int greenEnd = 5 + widerZoneLevel * plugin.getConfigManager().getZoneExpandPerLevel();
        boolean inGreenZone = (pointerPosition >= greenStart && pointerPosition <= greenEnd);

        if (inGreenZone && !canClickInGreenZone) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            player.sendMessage(plugin.formatJobMessage("§cZbyt szybko! Poczekaj na kolejny przejazd wskaźnika."));
            return;
        }

        int baseScrub = plugin.getConfigManager().getBaseScrubPower();
        int scrubBonus = scrubLevel * plugin.getConfigManager().getScrubPowerPerLevel();
        int failPenalty = plugin.getConfigManager().getScrubFailPenalty();
        int reducedPenalty = Math.max(0, failPenalty - enduranceLevel * plugin.getConfigManager().getPenaltyReductionPerLevel());

        if (inGreenZone) {
            progress = Math.min(100, progress + baseScrub + scrubBonus);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.8f);
            canClickInGreenZone = false;
        } else {
            progress = Math.max(0, progress - reducedPenalty);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.8f);
            player.sendMessage(plugin.formatJobMessage("§cChybiłeś! Postęp zmalał."));
            perfectWindow = false;
        }

        if (progress >= 100) {
            progress = 100;
            finished = true;
            if (pointerTask != null) pointerTask.cancel();

            plugin.getJobManager().getSession(player).markWindowCleaned(windowLocation, perfectWindow);
            plugin.getJobManager().getSession(player).setBrushDipped(false);

            int freeDipLevel = plugin.getSkillManager().getSkillLevel(player, "darmowe_namaczanie");
            if (freeDipLevel > 0 && random.nextInt(100) < freeDipLevel * plugin.getConfigManager().getFreeDipChancePerLevel()) {
                plugin.getJobManager().getSession(player).setBrushDipped(true);
                player.sendMessage(plugin.formatJobMessage("§b★ Oszczędzasz namaczanie! Możesz myć dalej."));
            }

            int cleaned = plugin.getJobManager().getSession(player).getCleanedCount();
            int total = plugin.getJobManager().getSession(player).getTotalRequired();
            player.sendMessage(plugin.formatJobMessage("§a✔ Okno umyte! (" + cleaned + "/" + total + ")"));

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            plugin.getParticleManager().refreshParticles(player);
            player.closeInventory();
        } else {
            updatePointerDisplay();
        }
    }

    public boolean isFinished() { return finished; }
    public void cancel() { if (pointerTask != null) pointerTask.cancel(); }
}