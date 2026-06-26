package pl.skyrise.skyRiseJobs.core;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseJobs.core.npc.NpcRegistry;

import java.util.UUID;
import java.util.logging.Level;

public final class CitizensHook {
    private static JavaPlugin plugin;
    private static NpcRegistry npcRegistry;
    private static boolean enabled;

    private CitizensHook() {}

    public static void setup(JavaPlugin javaPlugin, NpcRegistry registry) {
        plugin = javaPlugin;
        npcRegistry = registry;
        enabled = javaPlugin != null && javaPlugin.getServer().getPluginManager().getPlugin("Citizens") != null;
    }

    public static boolean isEnabled() { return enabled; }

    public static CreatedNpc createPlayerNpc(String moduleId, String npcId, Location location, String rawName, String skin, boolean glowing) {
        if (!enabled || location == null || location.getWorld() == null) return null;
        try {
            Object registry = citizensRegistry();
            Object npc = registry.getClass().getMethod("createNPC", EntityType.class, String.class)
                    .invoke(registry, EntityType.PLAYER, rawName == null ? npcId : rawName);
            try { npc.getClass().getMethod("setName", String.class).invoke(npc, rawName == null ? npcId : rawName); } catch (Throwable ignored) {}
            try { npc.getClass().getMethod("setProtected", boolean.class).invoke(npc, true); } catch (Throwable ignored) {}
            applySkin(npc, skin);
            boolean spawned = Boolean.TRUE.equals(npc.getClass().getMethod("spawn", Location.class).invoke(npc, location));
            if (!spawned) return null;
            int citizensId = ((Number) npc.getClass().getMethod("getId").invoke(npc)).intValue();
            Entity entity = null;
            Object rawEntity = npc.getClass().getMethod("getEntity").invoke(npc);
            if (rawEntity instanceof Entity bukkitEntity) {
                entity = bukkitEntity;
                entity.setInvulnerable(true);
                entity.setGlowing(glowing);
                if (npcRegistry != null) npcRegistry.markNpc(entity, moduleId, npcId);
            }
            if (npcRegistry != null) npcRegistry.markCitizensNpc(citizensId, moduleId, npcId);
            return new CreatedNpc(citizensId, entity != null ? entity.getUniqueId() : null, entity);
        } catch (Throwable throwable) {
            if (plugin != null) plugin.getLogger().log(Level.WARNING, "Nie udało się utworzyć NPC Citizens: " + throwable.getMessage(), throwable);
            return null;
        }
    }

    public static void destroyNpc(int citizensId) {
        if (!enabled) return;
        if (npcRegistry != null) npcRegistry.unmarkCitizensNpc(citizensId);
        try {
            Object registry = citizensRegistry();
            Object npc = registry.getClass().getMethod("getById", int.class).invoke(registry, citizensId);
            if (npc != null) npc.getClass().getMethod("destroy").invoke(npc);
        } catch (Throwable ignored) {}
    }

    private static Object citizensRegistry() throws ReflectiveOperationException {
        Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
        return api.getMethod("getNPCRegistry").invoke(null);
    }

    private static void applySkin(Object npc, String skin) {
        if (skin == null || skin.isBlank()) return;
        try {
            Class<?> skinTrait = Class.forName("net.citizensnpcs.trait.SkinTrait");
            Object trait = npc.getClass().getMethod("getOrAddTrait", Class.class).invoke(npc, skinTrait);
            try { trait.getClass().getMethod("setSkinName", String.class, boolean.class).invoke(trait, skin, true); }
            catch (NoSuchMethodException ignored) { trait.getClass().getMethod("setSkinName", String.class).invoke(trait, skin); }
        } catch (Throwable ignored) {}
    }

    public record CreatedNpc(int citizensId, UUID entityUuid, Entity entity) {}
}
