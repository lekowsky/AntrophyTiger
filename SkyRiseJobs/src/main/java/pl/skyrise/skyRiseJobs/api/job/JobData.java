package pl.skyrise.skyRiseJobs.api.job;

import java.util.HashMap;
import java.util.Map;

/**
 * Rekord danych gracza w obrębie jednej pracy.
 * Pobierany jednym zapytaniem z {@link JobDataStore} (mniej operacji = mniej lagów).
 */
public class JobData {
    public int level;
    public int xp;
    public double totalEarned;
    public int skillPoints;
    public final Map<String, Integer> skills = new HashMap<>();

    public int skillLevel(String skillId) {
        return skills.getOrDefault(skillId, 0);
    }
}
