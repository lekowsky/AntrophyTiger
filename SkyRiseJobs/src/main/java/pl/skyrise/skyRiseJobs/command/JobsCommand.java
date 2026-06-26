package pl.skyrise.skyRiseJobs.command;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.JobModule;
import pl.skyrise.skyRiseJobs.core.TabRegistry;
import pl.skyrise.skyRiseJobs.gui.GuiMenu;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;
import pl.skyrise.skyRiseJobs.utils.ItemBuilder;

import java.util.List;

public class JobsCommand implements CommandExecutor {
    private final SkyRiseJobs plugin;
    public JobsCommand(SkyRiseJobs plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendPrefixed(sender, "only-player");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("boost")) {
            if (args.length >= 2 && (args[1].equalsIgnoreCase("buy") || args[1].equalsIgnoreCase("kup"))) {
                plugin.getBoostManager().purchase(player);
            } else {
                plugin.getBoostManager().sendStatus(player);
            }
            return true;
        }

        if (args.length > 0) {
            JobModule job = findJob(args[0]);
            if (job == null || !job.isAvailable(player)) {
                plugin.sendPrefixed(player, "job-unavailable");
                return true;
            }
            if (!plugin.getLevelManager().canAccessJob(player, job)) {
                plugin.getLevelManager().sendLockedMessage(player, job);
                return true;
            }
            job.openMenu(player);
            return true;
        }

        // Globalne GUI z listą wszystkich prac jest narzędziem administracyjnym.
        // Zgodnie z założeniem ma być dostępne wyłącznie dla OP.
        if (!player.isOp()) {
            plugin.sendPrefixed(player, "no-permission");
            return true;
        }
        openJobsMenu(player);
        return true;
    }

    public void openJobsMenu(Player player) {
        List<JobModule> jobs = plugin.getModuleManager().getJobModules();
        if (jobs.isEmpty()) {
            plugin.sendPrefixed(player, "no-jobs");
            return;
        }
        int size = plugin.getConfig().getInt("gui.jobs-size", 54);
        GuiMenu menu = new GuiMenu(plugin.getConfig().getString("gui.jobs-title", "&e&lPrace"), size);

        // GUI /jobs ma być puste — bez wypełniania szybami.
        int slot = 10;
        for (JobModule job : jobs) {
            while (slot < menu.getInventory().getSize() && isBorderSlot(slot, menu.getInventory().getSize())) slot++;
            if (slot >= menu.getInventory().getSize()) break;
            ItemStack icon = job.getIcon(player);
            if (icon == null) {
                icon = new ItemBuilder(Material.PAPER)
                        .name(job.getMenuColor() + "&l" + job.getDisplayName())
                        .build();
            }
            boolean levelAllowed = plugin.getLevelManager().canAccessJob(player, job);
            if (!levelAllowed) {
                icon = new ItemBuilder(Material.BARRIER)
                        .name("&c&l" + job.getDisplayName())
                        .build();
            }
            menu.setItem(slot, icon, event -> {
                if (!job.isAvailable(player)) {
                    plugin.sendPrefixed(player, "job-unavailable");
                    return;
                }
                if (!plugin.getLevelManager().canAccessJob(player, job)) {
                    plugin.getLevelManager().sendLockedMessage(player, job);
                    return;
                }
                job.openMenu(player);
            });
            slot++;
        }
        menu.open(player);
    }

    private JobModule findJob(String idOrName) {
        for (JobModule job : plugin.getModuleManager().getJobModules()) {
            if (job.getJobId().equalsIgnoreCase(idOrName) || job.getName().equalsIgnoreCase(idOrName)) return job;
        }
        return null;
    }

    private boolean isBorderSlot(int slot, int size) {
        int row = slot / 9, col = slot % 9, rows = size / 9;
        return row == 0 || row == rows - 1 || col == 0 || col == 8;
    }

    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (!sender.isOp()) return TabRegistry.filter(List.of("boost"), args[0]);
            java.util.ArrayList<String> options = new java.util.ArrayList<>(plugin.getModuleManager().getJobModules().stream().map(JobModule::getJobId).toList());
            options.add("boost");
            return TabRegistry.filter(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("boost")) return TabRegistry.filter(List.of("status", "buy"), args[1]);
        return List.of();
    }
}
