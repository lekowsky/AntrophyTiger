package pl.skyrise.skyRiseJobs.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.core.TabRegistry;

import java.util.List;

public class BoostCommand implements CommandExecutor {
    private final SkyRiseJobs plugin;
    public BoostCommand(SkyRiseJobs plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
            plugin.getBoostManager().sendStatus(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "buy", "kup" -> {
                if (!(sender instanceof Player player)) {
                    plugin.getBoostManager().send(sender, "only-player");
                    return true;
                }
                plugin.getBoostManager().purchase(player);
            }
            case "start" -> {
                long minutes = args.length >= 2 ? parseLong(args[1], 60L) : 60L;
                plugin.getBoostManager().forceStart(sender, minutes);
            }
            case "stop" -> plugin.getBoostManager().forceStop(sender);
            case "reload" -> {
                if (!sender.hasPermission(plugin.getBoostManager().getAdminPermission())) {
                    plugin.getBoostManager().send(sender, "admin-no-permission");
                    return true;
                }
                plugin.getBoostManager().reload();
                plugin.getBoostManager().send(sender, "reloaded");
            }
            default -> plugin.getBoostManager().sendStatus(sender);
        }
        return true;
    }

    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission(plugin.getBoostManager().getAdminPermission())) {
                return TabRegistry.filter(List.of("status", "buy", "start", "stop", "reload"), args[0]);
            }
            return TabRegistry.filter(List.of("status", "buy"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start") && sender.hasPermission(plugin.getBoostManager().getAdminPermission())) {
            return TabRegistry.filter(List.of("15", "30", "60", "120"), args[1]);
        }
        return List.of();
    }

    private long parseLong(String raw, long fallback) {
        try { return Long.parseLong(raw); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
