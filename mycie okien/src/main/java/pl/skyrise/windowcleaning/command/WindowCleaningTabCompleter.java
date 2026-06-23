package pl.skyrise.windowcleaning.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class WindowCleaningTabCompleter implements TabCompleter {

    private final WindowCleaningPlugin plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "reload", "setboss", "setelevatorup", "setelevatordown", "setplatform", "setplatform2",
            "wand", "setregion", "reset", "forceend", "resetall",
            "setlevel", "setxp", "setskillpoints", "addskillpoints", "setskill"
    );

    public WindowCleaningTabCompleter(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("windowcleaning.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && (
                args[0].equalsIgnoreCase("reset") ||
                        args[0].equalsIgnoreCase("forceend") ||
                        args[0].equalsIgnoreCase("resetall") ||
                        args[0].equalsIgnoreCase("setlevel") ||
                        args[0].equalsIgnoreCase("setxp") ||
                        args[0].equalsIgnoreCase("setskillpoints") ||
                        args[0].equalsIgnoreCase("addskillpoints") ||
                        args[0].equalsIgnoreCase("setskill"))) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("setskill")) {
            return plugin.getConfig().getConfigurationSection("skills").getKeys(false).stream()
                    .filter(s -> s.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("resetall") && !args[2].equalsIgnoreCase("confirm")) {
            return Arrays.asList("confirm");
        }

        return Collections.emptyList();
    }
}