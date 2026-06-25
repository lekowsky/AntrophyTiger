package pl.skyrise.skyRiseJobs.core;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultHook {
    private static Economy economy;
    private static Chat chat;
    private static boolean enabled;

    private VaultHook() {}

    public static void setup(JavaPlugin plugin) {
        economy = null;
        chat = null;
        enabled = Bukkit.getPluginManager().getPlugin("Vault") != null;
        if (!enabled) return;
        RegisteredServiceProvider<Economy> eco = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (eco != null) economy = eco.getProvider();
        RegisteredServiceProvider<Chat> ch = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (ch != null) chat = ch.getProvider();
    }

    public static boolean isEnabled() { return enabled; }
    public static boolean hasEconomy() { return economy != null; }
    public static Economy economy() { return economy; }

    public static boolean has(OfflinePlayer player, double amount) {
        return amount <= 0 || (economy != null && economy.has(player, amount));
    }

    public static boolean withdraw(OfflinePlayer player, double amount) {
        return amount <= 0 || (economy != null && economy.withdrawPlayer(player, amount).transactionSuccess());
    }

    public static boolean deposit(OfflinePlayer player, double amount) {
        return amount <= 0 || (economy != null && economy.depositPlayer(player, amount).transactionSuccess());
    }

    public static String format(double amount) {
        return economy != null ? economy.format(amount) : String.format(java.util.Locale.US, "%.2f", amount);
    }

    public static String getPrefix(Player player) {
        if (chat == null || player == null) return "";
        String prefix = chat.getPlayerPrefix(player);
        return prefix != null ? prefix : "";
    }

    public static String getGroup(Player player) {
        if (chat == null || player == null) return "default";
        String group = chat.getPrimaryGroup(player);
        return group != null ? group : "default";
    }
}
