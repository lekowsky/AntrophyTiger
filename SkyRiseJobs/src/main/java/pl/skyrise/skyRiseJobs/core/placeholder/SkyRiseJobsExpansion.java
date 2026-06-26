package pl.skyrise.skyRiseJobs.core.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;

public class SkyRiseJobsExpansion extends PlaceholderExpansion {

    private final SkyRiseJobs plugin;

    public SkyRiseJobsExpansion(SkyRiseJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "skyrisejobs";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SkyRise";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || plugin.getLevelManager() == null) return "";
        return switch (params.toLowerCase()) {
            case "level", "character_level" -> String.valueOf(plugin.getLevelManager().getLevel(player.getUniqueId()));
            case "exp", "character_exp" -> String.format(java.util.Locale.US, "%.0f", plugin.getLevelManager().getExp(player.getUniqueId()));
            case "exp_required", "character_exp_required" -> String.format(java.util.Locale.US, "%.0f", plugin.getLevelManager().getRequiredExp(plugin.getLevelManager().getLevel(player.getUniqueId())));
            case "exp_percent", "character_exp_percent" -> String.format(java.util.Locale.US, "%.1f", plugin.getLevelManager().getProgress(player.getUniqueId()));
            case "level_progress", "character_level_progress" -> plugin.getLevelManager().getLevelProgressPlaceholder(player.getUniqueId());
            case "boost_money" -> String.format(java.util.Locale.US, "%.2f", plugin.getBoostManager().getMoneyMultiplier());
            case "boost_exp" -> String.format(java.util.Locale.US, "%.2f", plugin.getBoostManager().getExpMultiplier());
            default -> null;
        };
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return onRequest(player, params);
    }
}
