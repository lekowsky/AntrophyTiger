package pl.skyrise.skyRiseJobs.features.windowcleaning.managers;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import pl.skyrise.skyRiseJobs.features.windowcleaning.WindowCleaningJobModule;
import pl.skyrise.skyRiseJobs.features.windowcleaning.session.JobSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ParticleManager {

    private final WindowCleaningJobModule plugin;
    private final Map<UUID, BukkitRunnable> activeTasks = new ConcurrentHashMap<>();

    public ParticleManager(WindowCleaningJobModule plugin) {
        this.plugin = plugin;
    }

    public List<Location> generateWindowsInRegion(World world) {
        List<Location> windows = new ArrayList<>();
        Location p1 = plugin.getConfigManager().getRegionPos1();
        Location p2 = plugin.getConfigManager().getRegionPos2();
        if (p1 == null || p2 == null || !p1.getWorld().equals(world)) return windows;

        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        List<String> allowedTypes = plugin.getConfigManager().getWindowBlockTypes();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (allowedTypes.contains(block.getType().name())) {
                        windows.add(block.getLocation());
                    }
                }
            }
        }
        return windows;
    }

    public void startParticles(Player player) {
        stopParticles(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !plugin.getJobManager().hasActiveSession(player)) {
                    stopParticles(player);
                    return;
                }

                JobSession session = plugin.getJobManager().getSession(player);
                if (session == null) return;

                List<Location> windows = session.getAllWindows();
                if (windows == null || windows.isEmpty()) return;

                // Pobierz ustawienia z configu
                Color color = plugin.getConfigManager().getParticleColor();
                double scale = plugin.getConfigManager().getParticleScale();
                Particle.DustOptions dustOptions = new Particle.DustOptions(color, (float) scale);

                int count = plugin.getConfigManager().getParticleCount();
                double offX = plugin.getConfigManager().getParticleOffsetX();
                double offY = plugin.getConfigManager().getParticleOffsetY();
                double offZ = plugin.getConfigManager().getParticleOffsetZ();

                for (Location loc : windows) {
                    if (!session.isWindowCleaned(loc)) {
                        // Środek bloku + konfigurowalne przesunięcie
                        Location emitLoc = loc.clone().add(0.5 + offX, 0.5 + offY, 0.5 + offZ);
                        player.spawnParticle(Particle.DUST, emitLoc, count, 0, 0, 0, 0, dustOptions);
                    }
                }
            }
        };

        long interval = plugin.getConfigManager().getParticleInterval();
        task.runTaskTimer(plugin.getPlugin(), 0L, interval);
        activeTasks.put(player.getUniqueId(), task);
    }

    public void stopParticles(Player player) {
        BukkitRunnable task = activeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    public void refreshParticles(Player player) {
        stopParticles(player);
        startParticles(player);
    }

    public void stopAll() {
        activeTasks.values().forEach(BukkitRunnable::cancel);
        activeTasks.clear();
    }
}