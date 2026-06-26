package pl.skyrise.skyRiseJobs.core;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

public class DisabledCommandExecutor implements CommandExecutor {
    private final String module;
    public DisabledCommandExecutor(String module) { this.module = module == null ? "moduł" : module; }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String raw = SkyRiseJobs.getInstance().getConfig().getString("messages.module-disabled",
                "{prefix}<red>Ta komenda jest obecnie wyłączona <gray>(moduł: <#36d1ff>{module}</#36d1ff>)</gray>.</red>");
        raw = raw.replace("{prefix}", SkyRiseJobs.getInstance().prefix()).replace("{module}", module);
        sender.sendMessage(ColorUtil.mini(raw));
        return true;
    }
}
