package pl.skyrise.skyRiseJobs.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.List;

public class CoreTabCompleter implements TabCompleter {
    private final TabRegistry registry;
    public CoreTabCompleter(TabRegistry registry) { this.registry = registry; }
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return registry.complete(command.getName(), sender, args);
    }
}
