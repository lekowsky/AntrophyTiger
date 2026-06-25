package pl.skyrise.skyRiseJobs.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.ModuleManager;
import pl.skyrise.skyRiseJobs.core.TabRegistry;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

import java.util.List;

public class SkyRiseJobsCommand implements CommandExecutor {
    private final SkyRiseJobs plugin;
    public SkyRiseJobsCommand(SkyRiseJobs plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("version")) { info(sender); return true; }
        switch (args[0].toLowerCase()) {
            case "reload" -> reload(sender, args);
            case "list" -> list(sender);
            case "enable" -> enable(sender, args);
            case "disable" -> disable(sender, args);
            default -> help(sender, label);
        }
        return true;
    }

    private void reload(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            if (plugin.getModuleManager().reload(args[1])) msg(sender, "<green>Przeładowano moduł <#36d1ff>" + args[1] + "</#36d1ff>.");
            else msg(sender, "<red>Nie udało się przeładować modułu <#36d1ff>" + args[1] + "</#36d1ff>.");
            return;
        }
        plugin.reloadConfig();
        plugin.getBoostManager().reload();
        plugin.getLevelManager().reload();
        plugin.getModuleManager().reloadAll();
        msg(sender, "<green>Przeładowano SkyRiseJobs.");
    }

    private void list(CommandSender sender) {
        ModuleManager mm = plugin.getModuleManager();
        msg(sender, "<white>Moduły <gray>(<#36d1ff>" + mm.getModuleCount() + "</#36d1ff>)<white>:");
        for (String name : mm.getModuleNames()) {
            msg(sender, (mm.isEnabled(name) ? "<green>✔ " : "<red>✘ ") + "<white>" + name);
        }
    }

    private void enable(CommandSender sender, String[] args) {
        if (args.length < 2) { msg(sender, "<red>Użycie: /skyrisejobs enable <moduł>"); return; }
        msg(sender, plugin.getModuleManager().enable(args[1]) ? "<green>Włączono moduł." : "<red>Nie udało się włączyć modułu.");
    }

    private void disable(CommandSender sender, String[] args) {
        if (args.length < 2) { msg(sender, "<red>Użycie: /skyrisejobs disable <moduł>"); return; }
        msg(sender, plugin.getModuleManager().disable(args[1]) ? "<green>Wyłączono moduł." : "<red>Nie udało się wyłączyć modułu.");
    }

    private void info(CommandSender sender) {
        msg(sender, "<white>SkyRiseJobs <gray>v" + plugin.getDescription().getVersion());
        msg(sender, "<white>Moduły: <#36d1ff>" + plugin.getModuleManager().getModuleCount());
    }

    private void help(CommandSender sender, String label) {
        msg(sender, "<#36d1ff>/" + label + " list <gray>- <white>Lista modułów");
        msg(sender, "<#36d1ff>/" + label + " reload [moduł] <gray>- <white>Reload");
        msg(sender, "<#36d1ff>/" + label + " enable <moduł> <gray>- <white>Włącz moduł");
        msg(sender, "<#36d1ff>/" + label + " disable <moduł> <gray>- <white>Wyłącz moduł");
    }

    private void msg(CommandSender sender, String mini) {
        sender.sendMessage(ColorUtil.mini(plugin.prefix() + mini));
    }

    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) return TabRegistry.filter(List.of("reload", "list", "enable", "disable", "version"), args[0]);
        if (args.length == 2 && List.of("reload", "enable", "disable").contains(args[0].toLowerCase())) {
            return TabRegistry.filter(plugin.getModuleManager().getModuleNames(), args[1]);
        }
        return List.of();
    }
}
