package pl.skyrise.skyRiseJobs.api.job;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Wspólny, generyczny magazyn danych prac.
 *
 * Wszystkie prace współdzielą JEDNĄ bazę SQLite (plugins/SkyRiseJobs/data.db),
 * a rekordy są rozróżniane kolumną {@code job}. Dzięki temu:
 * - plugin nie tworzy wielu plików/baz (mniejszy, prostszy backup),
 * - jedno połączenie = brak narzutu i lagów przy wielu pracach.
 *
 * Każda praca dostaje własny widok przez {@code jobId} przekazany w konstruktorze —
 * nie trzeba kopiować klasy DataManager dla każdej nowej pracy.
 */
public class JobDataStore {

    private final JavaPlugin plugin;
    private final String jobId;
    private final Connection connection;

    public JobDataStore(JavaPlugin plugin, String jobId, Connection sharedConnection) {
        this.plugin = plugin;
        this.jobId = jobId;
        this.connection = sharedConnection;
    }

    /** Otwiera (lub współdzieli) jedno połączenie do data.db i tworzy schemat. */
    public static Connection openSharedConnection(JavaPlugin plugin) {
        try {
            File db = new File(plugin.getDataFolder(), "data.db");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("CREATE TABLE IF NOT EXISTS job_players (" +
                        "job TEXT NOT NULL, uuid TEXT NOT NULL, " +
                        "level INT DEFAULT 0, xp INT DEFAULT 0, " +
                        "total_earned DOUBLE DEFAULT 0.0, skill_points INT DEFAULT 0, " +
                        "PRIMARY KEY (job, uuid))");
                stmt.execute("CREATE TABLE IF NOT EXISTS job_skills (" +
                        "job TEXT NOT NULL, uuid TEXT NOT NULL, skill_id TEXT NOT NULL, level INT DEFAULT 0, " +
                        "PRIMARY KEY (job, uuid, skill_id))");
            }
            return connection;
        } catch (SQLException e) {
            plugin.getLogger().severe("Nie można otworzyć bazy danych prac: " + e.getMessage());
            return null;
        }
    }

    public static void closeSharedConnection(Connection connection) {
        try { if (connection != null && !connection.isClosed()) connection.close(); }
        catch (SQLException ignored) {}
    }

    // ---------- Odczyt całego rekordu (mniej zapytań = mniej lagów) ----------

    public JobData load(UUID uuid) {
        JobData data = new JobData();
        if (connection == null) return data;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT level, xp, total_earned, skill_points FROM job_players WHERE job = ? AND uuid = ?")) {
            ps.setString(1, jobId);
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    data.level = rs.getInt("level");
                    data.xp = rs.getInt("xp");
                    data.totalEarned = rs.getDouble("total_earned");
                    data.skillPoints = rs.getInt("skill_points");
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd odczytu danych pracy " + jobId + ": " + e.getMessage());
        }
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT skill_id, level FROM job_skills WHERE job = ? AND uuid = ?")) {
            ps.setString(1, jobId);
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) data.skills.put(rs.getString("skill_id"), rs.getInt("level"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd odczytu perków pracy " + jobId + ": " + e.getMessage());
        }
        return data;
    }

    public void savePlayer(UUID uuid, JobData data) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO job_players (job, uuid, level, xp, total_earned, skill_points) " +
                        "VALUES (?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, jobId);
            ps.setString(2, uuid.toString());
            ps.setInt(3, data.level);
            ps.setInt(4, data.xp);
            ps.setDouble(5, data.totalEarned);
            ps.setInt(6, data.skillPoints);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd zapisu danych pracy " + jobId + ": " + e.getMessage());
        }
    }

    public void saveSkill(UUID uuid, String skillId, int level) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO job_skills (job, uuid, skill_id, level) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, jobId);
            ps.setString(2, uuid.toString());
            ps.setString(3, skillId);
            ps.setInt(4, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd zapisu perka pracy " + jobId + ": " + e.getMessage());
        }
    }

    public void reset(UUID uuid) {
        if (connection == null) return;
        try (PreparedStatement p1 = connection.prepareStatement("DELETE FROM job_players WHERE job = ? AND uuid = ?");
             PreparedStatement p2 = connection.prepareStatement("DELETE FROM job_skills WHERE job = ? AND uuid = ?")) {
            p1.setString(1, jobId); p1.setString(2, uuid.toString()); p1.executeUpdate();
            p2.setString(1, jobId); p2.setString(2, uuid.toString()); p2.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd resetu danych pracy " + jobId + ": " + e.getMessage());
        }
    }

    // ---------- Wygodne skróty per-pole (dla zgodności / prostych użyć) ----------

    public int getLevel(Player player) { return load(player.getUniqueId()).level; }
    public int getXp(Player player) { return load(player.getUniqueId()).xp; }
    public double getTotalEarned(Player player) { return load(player.getUniqueId()).totalEarned; }
    public int getSkillPoints(Player player) { return load(player.getUniqueId()).skillPoints; }
    public int getSkillLevel(Player player, String skillId) {
        return load(player.getUniqueId()).skills.getOrDefault(skillId, 0);
    }

    public void setLevel(Player player, int level) {
        JobData d = load(player.getUniqueId()); d.level = level; savePlayer(player.getUniqueId(), d);
    }
    public void setXp(Player player, int xp) {
        JobData d = load(player.getUniqueId()); d.xp = xp; savePlayer(player.getUniqueId(), d);
    }
    public void setSkillPoints(Player player, int points) {
        JobData d = load(player.getUniqueId()); d.skillPoints = points; savePlayer(player.getUniqueId(), d);
    }
    public void addTotalEarned(Player player, double earned) {
        JobData d = load(player.getUniqueId()); d.totalEarned += earned; savePlayer(player.getUniqueId(), d);
    }
    public void setSkillLevel(Player player, String skillId, int level) {
        saveSkill(player.getUniqueId(), skillId, level);
    }
    public void reset(Player player) { reset(player.getUniqueId()); }

    public String getJobId() { return jobId; }
}
