package pl.skyrise.skyRiseJobs.features.windowcleaning.listeners;

import net.citizensnpcs.api.event.NPCRightClickEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;

public class NPCClickListener implements Listener {

    private final WindowCleaningJobModule plugin;

    public NPCClickListener(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onNPCClick(NPCRightClickEvent event) {
        NPC npc = event.getNPC();
        Player player = event.getClicker();

        if (npc.equals(plugin.getNPCManager().getBossNPC())) {
            plugin.getGUIManager().openMainJobGUI(player);
        }
        else if (npc.equals(plugin.getNPCManager().getElevatorUpNPC())) {
            if (!plugin.getJobManager().hasActiveSession(player)) {
                player.sendMessage(plugin.formatJobMessage("§cNie masz aktywnej pracy! Porozmawiaj z Szefem."));
                return;
            }
            Location platform = plugin.getConfigManager().getPlatformLocation();
            if (platform == null) {
                player.sendMessage(plugin.formatJobMessage("§cPlatforma nie została ustawiona!"));
                return;
            }
            startElevator(player, platform, "§aOperator windy zawiózł Cię na platformę.");
        }
        else if (npc.equals(plugin.getNPCManager().getElevatorDownNPC())) {
            if (!plugin.getJobManager().hasActiveSession(player)) {
                player.sendMessage(plugin.formatJobMessage("§cNie masz aktywnej pracy."));
                return;
            }
            JobSession session = plugin.getJobManager().getSession(player);
            if (!session.isJobCompleted()) {
                player.sendMessage(plugin.formatJobMessage("§cMusisz umyć wszystkie okna zanim wrócisz!"));
                return;
            }

            Location targetLoc = plugin.getConfigManager().getPlatform2Location();
            if (targetLoc == null) {
                NPC downNPC = plugin.getNPCManager().getElevatorUpNPC();
                if (downNPC != null) {
                    targetLoc = getLocationInFrontOfNPC(downNPC);
                } else {
                    targetLoc = plugin.getConfigManager().getElevatorUpLocation();
                }
            }
            if (targetLoc == null) {
                player.sendMessage(plugin.formatJobMessage("§cNie ustawiono miejsca powrotu!"));
                return;
            }
            startElevator(player, targetLoc, "§aOperator windy zawiózł Cię na dół. Możesz odebrać wypłatę u Szefa.");
        }
    }

    private Location getLocationInFrontOfNPC(NPC npc) {
        if (npc.getEntity() == null) return npc.getStoredLocation();
        Location npcLoc = npc.getEntity().getLocation();
        Vector direction = npcLoc.getDirection().normalize();
        return npcLoc.clone().add(direction.multiply(1.5));
    }

    private void startElevator(Player player, Location destination, String successMessage) {
        player.sendActionBar("§eWinda rusza za 5 sekund...");
        player.setWalkSpeed(0);
        new BukkitRunnable() {
            int countdown = 5;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (countdown > 0) {
                    player.sendActionBar("§eWinda rusza za " + countdown + "...");
                    countdown--;
                } else {
                    player.teleport(destination);
                    player.sendActionBar(successMessage);
                    player.setWalkSpeed(0.2f);
                    cancel();
                }
            }
        }.runTaskTimer(plugin.getPlugin(), 0L, 20L);
    }
}