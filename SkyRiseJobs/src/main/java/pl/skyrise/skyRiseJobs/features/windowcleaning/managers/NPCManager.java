package pl.skyrise.skyRiseJobs.features.windowcleaning.managers;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class NPCManager {

    private final WindowCleaningJobModule plugin;
    private final NPCRegistry npcRegistry;
    private final List<TextDisplay> nameLabels = new ArrayList<>();
    private NPC bossNPC;
    private NPC elevatorUpNPC;
    private NPC elevatorDownNPC;

    public NPCManager(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
        this.npcRegistry = CitizensAPI.getNPCRegistry();
    }

    public void createNPCs() {
        removeNPCs();

        Location bossLoc = plugin.getConfigManager().getBossLocation();
        if (bossLoc != null) {
            bossNPC = createNpc(bossLoc, "Praca: Mycie Okien", "&7Praca: &1&lMycie Okien");
        }

        Location upLoc = plugin.getConfigManager().getElevatorUpLocation();
        if (upLoc != null) {
            elevatorUpNPC = createNpc(upLoc, "Operator windy ▲", "&bOperator windy (&a▲&b)");
        }

        Location downLoc = plugin.getConfigManager().getElevatorDownLocation();
        if (downLoc != null) {
            elevatorDownNPC = createNpc(downLoc, "Operator windy ▼", "&bOperator windy (&c▼&b)");
        }
    }

    private NPC createNpc(Location location, String citizensName, String label) {
        NPC npc = npcRegistry.createNPC(EntityType.PLAYER, citizensName);
        npc.spawn(location);
        npc.setProtected(true);
        Entity entity = npc.getEntity();
        if (entity != null) {
            entity.setCustomNameVisible(false);
            entity.customName(null);
            createNameLabel(entity, label);
        }
        return npc;
    }

    private void createNameLabel(Entity parent, String label) {
        if (parent == null || parent.getWorld() == null) return;
        TextDisplay display = parent.getWorld().spawn(parent.getLocation(), TextDisplay.class, text -> {
            text.text(ColorUtil.legacy(label));
            text.setBillboard(Billboard.CENTER);
            text.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            text.setSeeThrough(false);
            text.setShadowed(false);
            text.setPersistent(false);
            Transformation transformation = text.getTransformation();
            transformation.getTranslation().set(0.0f, 0.75f, 0.0f);
            text.setTransformation(transformation);
        });
        parent.addPassenger(display);
        nameLabels.add(display);
    }

    public void removeNPCs() {
        for (TextDisplay label : nameLabels) {
            if (label != null && !label.isDead()) label.remove();
        }
        nameLabels.clear();
        if (bossNPC != null) bossNPC.destroy();
        if (elevatorUpNPC != null) elevatorUpNPC.destroy();
        if (elevatorDownNPC != null) elevatorDownNPC.destroy();
        bossNPC = null;
        elevatorUpNPC = null;
        elevatorDownNPC = null;
    }

    public NPC getBossNPC() { return bossNPC; }
    public NPC getElevatorUpNPC() { return elevatorUpNPC; }
    public NPC getElevatorDownNPC() { return elevatorDownNPC; }
}
