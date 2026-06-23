package pl.skyrise.windowcleaning.managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;

public class NPCManager {

    private final WindowCleaningPlugin plugin;
    private final NPCRegistry npcRegistry;
    private NPC bossNPC;
    private NPC elevatorUpNPC;
    private NPC elevatorDownNPC;

    public NPCManager(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
    }

    public void createNPCs() {
        removeNPCs();

        Location bossLoc = plugin.getConfigManager().getBossLocation();
        if (bossLoc != null) {
            bossNPC = npcRegistry.createNPC(EntityType.PLAYER, "§7Praca: §9§lMycie Okien");
            bossNPC.spawn(bossLoc);
            bossNPC.setProtected(true);
        }

        Location upLoc = plugin.getConfigManager().getElevatorUpLocation();
        if (upLoc != null) {
            elevatorUpNPC = npcRegistry.createNPC(EntityType.PLAYER, "§bOperator windy (§a▲§b)");
            elevatorUpNPC.spawn(upLoc);
            elevatorUpNPC.setProtected(true);
        }

        Location downLoc = plugin.getConfigManager().getElevatorDownLocation();
        if (downLoc != null) {
            elevatorDownNPC = npcRegistry.createNPC(EntityType.PLAYER, "§bOperator windy (§c▼§b)");
            elevatorDownNPC.spawn(downLoc);
            elevatorDownNPC.setProtected(true);
        }
    }

    public void removeNPCs() {
        if (bossNPC != null) bossNPC.destroy();
        if (elevatorUpNPC != null) elevatorUpNPC.destroy();
        if (elevatorDownNPC != null) elevatorDownNPC.destroy();
    }

    public NPC getBossNPC() { return bossNPC; }
    public NPC getElevatorUpNPC() { return elevatorUpNPC; }
    public NPC getElevatorDownNPC() { return elevatorDownNPC; }
}