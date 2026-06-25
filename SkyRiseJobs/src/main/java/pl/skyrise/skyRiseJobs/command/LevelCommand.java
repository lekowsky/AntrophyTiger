package pl.skyrise.skyRiseJobs.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.core.TabRegistry;
import pl.skyrise.skyRiseJobs.core.level.LevelProfile;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class LevelCommand implements CommandExecutor {

    private final SkyRiseJobs plugin;

    public LevelCommand(SkyRiseJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.getLevelManager().send(sender, "only-player");
                return true;
            }
            plugin.getLevelManager().send(player, "status");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "status" -> status(sender, args);
            case "addexp" -> addExp(sender, args);
            case "set" -> set(sender, args);
            case "reload" -> reload(sender);
            default -> help(sender, label);
        }
        return true;
    }

    private void status(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (!sender.hasPermission("skyrisejobs.level.admin")) {
                plugin.getLevelManager().send(sender, "no-permission");
                return;
            }
            OfflinePlayer target = offline(args[1]);
            String raw = plugin.getLevelManager().applyPlaceholders(target.getPlayer(), plugin.getLevelManager().getProfile(target).getLevel() + "");
            LevelProfile profile = plugin.getLevelManager().getProfile(target);
            sender.sendMessage(ColorUtil.mini(plugin.getLevelManager().applyPlaceholders(target.getPlayer(),
                    plugin.prefix() + "<white>Poziom gracza <#36d1ff>" + (target.getName() != null ? target.getName() : args[1])
                            + "</#36d1ff><white>: <#36d1ff>" + profile.getLevel()
                            + " (" + String.format(java.util.Locale.US, "%.1f", plugin.getLevelManager().getProgress(target.getUniqueId())) + "%)</#36d1ff><white>.")));
            return;
        }
        if (!(sender instanceof Player player)) {
            plugin.getLevelManager().send(sender, "only-player");
            return;
        }
        plugin.getLevelManager().send(player, "status");
    }

    private void addExp(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skyrisejobs.level.admin")) {
            plugin.getLevelManager().send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            help(sender, "joblevel");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<red>Gracz musi być online, aby dodać exp z boostem.</red>"));
            return;
        }
        double amount = parseDouble(args[2], 0.0);
        plugin.getLevelManager().addExperience(target, amount);
        String msg = plugin.getLevelManager().applyPlaceholders(target, plugin.getLevelManager().getProfile(target).getLevel() + "");
        sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<white>Dodano <#36d1ff>" + amount + "</#36d1ff><white> exp graczowi <#36d1ff>" + target.getName() + "</#36d1ff><white>."));
    }

    private void set(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skyrisejobs.level.admin")) {
            plugin.getLevelManager().send(sender, "no-permission");
            return;
        }
        if (args.length < 3) {
            help(sender, "joblevel");
            return;
        }
        OfflinePlayer target = offline(args[1]);
        int level = (int) parseDouble(args[2], 1);
        double exp = args.length >= 4 ? parseDouble(args[3], 0.0) : 0.0;
        plugin.getLevelManager().setLevel(target, level, exp);
        sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<white>Ustawiono poziom gracza <#36d1ff>" + (target.getName() != null ? target.getName() : args[1]) + "</#36d1ff><white> na <#36d1ff>" + level + "</#36d1ff><white>."));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission("skyrisejobs.level.admin")) {
            plugin.getLevelManager().send(sender, "no-permission");
            return;
        }
        plugin.getLevelManager().reload();
        sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<white>Przeładowano system poziomu postaci.</white>"));
    }

    private void help(CommandSender sender, String label) {
        sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<#36d1ff>/" + label + "</#36d1ff><gray> - <white>Twój poziom postaci"));
        if (sender.hasPermission("skyrisejobs.level.admin")) {
            sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<#36d1ff>/" + label + " status <gracz></#36d1ff><gray> - <white>Status gracza"));
            sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<#36d1ff>/" + label + " addexp <gracz> <exp></#36d1ff><gray> - <white>Dodaj exp"));
            sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<#36d1ff>/" + label + " set <gracz> <poziom> [exp]</#36d1ff><gray> - <white>Ustaw poziom"));
            sender.sendMessage(ColorUtil.mini(plugin.prefix() + "<#36d1ff>/" + label + " reload</#36d1ff><gray> - <white>Reload poziomów"));
        }
    }

    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("skyrisejobs.level.admin")) return TabRegistry.filter(List.of("status", "addexp", "set", "reload"), args[0]);
            return List.of();
        }
        if (args.length == 2 && sender.hasPermission("skyrisejobs.level.admin") && List.of("status", "addexp", "set").contains(args[0].toLowerCase())) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(player -> names.add(player.getName()));
            return TabRegistry.filter(names, args[1]);
        }
        if (args.length == 3 && sender.hasPermission("skyrisejobs.level.admin")) {
            if (args[0].equalsIgnoreCase("addexp")) return TabRegistry.filter(List.of("10", "25", "50", "100"), args[2]);
            if (args[0].equalsIgnoreCase("set")) return TabRegistry.filter(List.of("1", "2", "3", "5", "10"), args[2]);
        }
        return List.of();
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer offline(String name) {
        Player online = Bukkit.getPlayerExact(name);
        return online != null ? online : Bukkit.getOfflinePlayer(name);
    }

    private double parseDouble(String raw, double fallback) {
        try { return Double.parseDouble(raw); }
        catch (NumberFormatException ignored) { return fallback; }
    }
}
