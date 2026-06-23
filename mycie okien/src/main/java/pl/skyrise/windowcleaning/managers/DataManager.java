package pl.skyrise.windowcleaning.managers;

import org.bukkit.entity.Player;
import pl.skyrise.windowcleaning.WindowCleaningPlugin;
import java.sql.*;
import java.util.UUID;

public class DataManager {

    private final WindowCleaningPlugin plugin;
    private Connection connection;

    public DataManager(WindowCleaningPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/data.db");
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "level INT DEFAULT 0, " +
                        "xp INT DEFAULT 0, " +
                        "total_earned DOUBLE DEFAULT 0.0, " +
                        "skill_points INT DEFAULT 0)");
                stmt.execute("CREATE TABLE IF NOT EXISTS skills (" +
                        "uuid TEXT, skill_id TEXT, level INT DEFAULT 0, PRIMARY KEY (uuid, skill_id))");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Błąd bazy danych: " + e.getMessage());
        }
    }

    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getPlayerLevel(Player player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT level FROM players WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("level");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getPlayerXP(Player player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT xp FROM players WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("xp");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void setPlayerXP(Player player, int xp) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, level, xp, total_earned, skill_points) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, getPlayerLevel(player));
            ps.setInt(3, xp);
            ps.setDouble(4, getTotalEarned(player));
            ps.setInt(5, getSkillPoints(player));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void setPlayerLevel(Player player, int level) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, level, xp, total_earned, skill_points) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, level);
            ps.setInt(3, getPlayerXP(player));
            ps.setDouble(4, getTotalEarned(player));
            ps.setInt(5, getSkillPoints(player));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public double getTotalEarned(Player player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT total_earned FROM players WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("total_earned");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public void addCompletedJob(Player player, double earned) {
        double current = getTotalEarned(player);
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, level, xp, total_earned, skill_points) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, getPlayerLevel(player));
            ps.setInt(3, getPlayerXP(player));
            ps.setDouble(4, current + earned);
            ps.setInt(5, getSkillPoints(player));
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getSkillPoints(Player player) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT skill_points FROM players WHERE uuid = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("skill_points");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void setSkillPoints(Player player, int points) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, level, xp, total_earned, skill_points) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setInt(2, getPlayerLevel(player));
            ps.setInt(3, getPlayerXP(player));
            ps.setDouble(4, getTotalEarned(player));
            ps.setInt(5, points);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public int getSkillLevel(Player player, String skillId) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT level FROM skills WHERE uuid = ? AND skill_id = ?")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, skillId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("level");
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public void setSkillLevel(Player player, String skillId, int level) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO skills (uuid, skill_id, level) VALUES (?, ?, ?)")) {
            ps.setString(1, player.getUniqueId().toString());
            ps.setString(2, skillId);
            ps.setInt(3, level);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void resetPlayer(Player player) {
        try (PreparedStatement ps1 = connection.prepareStatement("DELETE FROM players WHERE uuid = ?");
             PreparedStatement ps2 = connection.prepareStatement("DELETE FROM skills WHERE uuid = ?")) {
            ps1.setString(1, player.getUniqueId().toString());
            ps2.setString(1, player.getUniqueId().toString());
            ps1.executeUpdate();
            ps2.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}