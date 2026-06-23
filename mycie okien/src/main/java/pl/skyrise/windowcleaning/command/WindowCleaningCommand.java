package pl.skyrise.windowcleaning.commands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;
import pl.skyrise.windowcleaning.utils.ItemBuilder;

public class WindowCleaningCommand implements CommandExecutor {

    private final WindowCleaningPlugin plugin;

    public WindowCleaningCommand(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("windowcleaning.admin")) {
            sender.sendMessage("§cNie masz uprawnień!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> { plugin.reloadConfig(); plugin.getConfigManager().reload(); sender.sendMessage("§aKonfiguracja przeładowana!"); }
            case "setboss" -> setLocation(sender, "boss");
            case "setelevatorup" -> setLocation(sender, "elevatorup");
            case "setelevatordown" -> setLocation(sender, "elevatordown");
            case "setplatform" -> setLocation(sender, "platform");
            case "setplatform2" -> setLocation(sender, "platform2");
            case "wand" -> giveWand(sender);
            case "setregion" -> setRegion(sender);
            case "reset" -> resetPlayer(sender, args);
            case "forceend" -> forceEnd(sender, args);
            case "resetall" -> resetAll(sender, args);
            case "setlevel" -> setLevel(sender, args);
            case "setxp" -> setXp(sender, args);
            case "setskillpoints" -> setSkillPoints(sender, args);
            case "addskillpoints" -> addSkillPoints(sender, args);
            case "setskill" -> setSkill(sender, args);
            default -> sender.sendMessage("§cNieznana komenda. Użyj /windowcleaning");
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§eDostępne komendy:");
        sender.sendMessage("§7/windowcleaning reload §8- Przeładowuje konfigurację");
        sender.sendMessage("§7/windowcleaning setboss §8- Ustawia NPC Szefa");
        sender.sendMessage("§7/windowcleaning setelevatorup §8- Ustawia NPC Operatora (góra)");
        sender.sendMessage("§7/windowcleaning setelevatordown §8- Ustawia NPC Operatora (dół)");
        sender.sendMessage("§7/windowcleaning setplatform §8- Ustawia platformę startową");
        sender.sendMessage("§7/windowcleaning setplatform2 §8- Ustawia miejsce powrotu po pracy");
        sender.sendMessage("§7/windowcleaning wand §8- Daje różdżkę do zaznaczania regionu");
        sender.sendMessage("§7/windowcleaning setregion §8- Zapisuje zaznaczony region");
        sender.sendMessage("§7/windowcleaning reset <gracz> §8- Resetuje postępy gracza");
        sender.sendMessage("§7/windowcleaning forceend <gracz> §8- Wymusza zakończenie pracy");
        sender.sendMessage("§7/windowcleaning resetall <gracz> confirm §8- Całkowicie kasuje dane gracza");
        sender.sendMessage("§7/windowcleaning setlevel <gracz> <poziom> §8- Ustawia poziom pracy");
        sender.sendMessage("§7/windowcleaning setxp <gracz> <xp> §8- Ustawia doświadczenie");
        sender.sendMessage("§7/windowcleaning setskillpoints <gracz> <punkty> §8- Ustawia punkty umiejętności");
        sender.sendMessage("§7/windowcleaning addskillpoints <gracz> <punkty> §8- Dodaje punkty umiejętności");
        sender.sendMessage("§7/windowcleaning setskill <gracz> <umiejętność> <poziom> §8- Ustawia poziom umiejętności");
    }

    private void setLocation(CommandSender sender, String type) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cTylko gracz!"); return; }
        Location loc = player.getLocation();
        switch (type) {
            case "boss" -> plugin.getConfigManager().setBossLocation(loc);
            case "elevatorup" -> plugin.getConfigManager().setElevatorUpLocation(loc);
            case "elevatordown" -> plugin.getConfigManager().setElevatorDownLocation(loc);
            case "platform" -> plugin.getConfigManager().setPlatformLocation(loc);
            case "platform2" -> plugin.getConfigManager().setPlatform2Location(loc);
        }
        plugin.getNPCManager().createNPCs();
        sender.sendMessage("§aUstawiono lokalizację " + type + "!");
    }

    private void giveWand(CommandSender sender) {
        if (!(sender instanceof Player player)) { sender.sendMessage("§cTylko gracz!"); return; }
        ItemStack wand = new ItemBuilder(Material.STICK)
                .setName("§6Różdżka regionu")
                .setCustomModelData(9999)
                .addLore("§7Kliknij §eLPM §7aby ustawić punkt 1", "§7Kliknij §ePPM §7aby ustawić punkt 2")
                .build();
        player.getInventory().addItem(wand);
        player.sendMessage("§aOtrzymałeś różdżkę.");
    }

    private void setRegion(CommandSender sender) {
        if (!(sender instanceof Player)) { sender.sendMessage("§cTylko gracz!"); return; }
        if (plugin.getConfigManager().getRegionPos1() == null || plugin.getConfigManager().getRegionPos2() == null) {
            sender.sendMessage("§cNajpierw zaznacz oba punkty różdżką!"); return;
        }
        sender.sendMessage("§aRegion zapisany!");
    }

    private void resetPlayer(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUżycie: /windowcleaning reset <gracz>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        plugin.getDataManager().resetPlayer(target);
        plugin.getJobManager().forceEndJob(target);
        sender.sendMessage("§aZresetowano postępy gracza " + target.getName());
    }

    private void forceEnd(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUżycie: /windowcleaning forceend <gracz>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        plugin.getJobManager().forceEndJob(target);
        sender.sendMessage("§aZakończono pracę gracza " + target.getName());
    }

    private void resetAll(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            sender.sendMessage("§c§lUWAGA! §cTa komenda usuwa wszystkie dane gracza.");
            sender.sendMessage("§cAby potwierdzić: §e/windowcleaning resetall " + (args.length > 1 ? args[1] : "<gracz>") + " confirm");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        plugin.getJobManager().resetAllProgress(target);
        sender.sendMessage("§aUsunięto dane gracza " + target.getName());
    }

    private void setLevel(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§cUżycie: /windowcleaning setlevel <gracz> <poziom>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        int level = Integer.parseInt(args[2]);
        plugin.getDataManager().setPlayerLevel(target, level);
        sender.sendMessage("§aUstawiono poziom " + level + " graczowi " + target.getName());
    }

    private void setXp(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§cUżycie: /windowcleaning setxp <gracz> <xp>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        int xp = Integer.parseInt(args[2]);
        plugin.getDataManager().setPlayerXP(target, xp);
        sender.sendMessage("§aUstawiono " + xp + " XP graczowi " + target.getName());
    }

    private void setSkillPoints(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§cUżycie: /windowcleaning setskillpoints <gracz> <punkty>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        int points = Integer.parseInt(args[2]);
        plugin.getDataManager().setSkillPoints(target, points);
        sender.sendMessage("§aUstawiono " + points + " punktów umiejętności graczowi " + target.getName());
    }

    private void addSkillPoints(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§cUżycie: /windowcleaning addskillpoints <gracz> <punkty>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        int toAdd = Integer.parseInt(args[2]);
        int current = plugin.getDataManager().getSkillPoints(target);
        plugin.getDataManager().setSkillPoints(target, current + toAdd);
        sender.sendMessage("§aDodano " + toAdd + " punktów umiejętności graczowi " + target.getName());
    }

    private void setSkill(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUżycie: /windowcleaning setskill <gracz> <umiejętność> <poziom>");
            sender.sendMessage("§cDostępne umiejętności: szybsze_szorowanie, wytrzymalosc, wieksza_strefa, czysta_robota, darmowe_namaczanie, bonus_za_komplet");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage("§cGracz nie online!"); return; }
        String skillId = args[2].toLowerCase();
        int skillLevel = Integer.parseInt(args[3]);
        plugin.getDataManager().setSkillLevel(target, skillId, skillLevel);
        sender.sendMessage("§aUstawiono umiejętność " + skillId + " na poziom " + skillLevel + " graczowi " + target.getName());
    }
}