package pl.skyrise.skyRiseJobs.api.job;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Generyczny silnik perków pracy: odczyt poziomu, koszty, ulepszanie.
 *
 * Lista perków jest definiowana per-praca ({@link BaseJobModule#getSkills()}),
 * a logika zakupu jest wspólna — zastępuje kopiowany feature SkillManager.
 *
 * Koszty / max-level / wymagany poziom można nadpisać w configu pracy:
 *   skills.<id>.name
 *   skills.<id>.max-level
 *   skills.<id>.cost-per-level.<n>
 *   skills.<id>.required-player-level
 */
public class JobSkillService {

    private final BaseJobModule job;

    public JobSkillService(BaseJobModule job) {
        this.job = job;
    }

    public int getLevel(Player player, String skillId) {
        return job.getDataStore().getSkillLevel(player, skillId);
    }

    public String getName(JobSkill skill) {
        return job.getJobConfig().getConfig().getString("skills." + skill.getId() + ".name", skill.getDefaultName());
    }

    public int getMaxLevel(JobSkill skill) {
        return job.getJobConfig().getConfig().getInt("skills." + skill.getId() + ".max-level", skill.getMaxLevel());
    }

    public int getRequiredJobLevel(JobSkill skill) {
        return job.getJobConfig().getConfig()
                .getInt("skills." + skill.getId() + ".required-player-level", skill.getRequiredJobLevel());
    }

    /** Koszt ulepszenia do podanego (1-indeksowanego) poziomu. */
    public int getCost(JobSkill skill, int level) {
        return job.getJobConfig().getConfig()
                .getInt("skills." + skill.getId() + ".cost-per-level." + level, skill.getDefaultCost(level));
    }

    public boolean upgrade(Player player, JobSkill skill) {
        JobData data = job.getDataStore().load(player.getUniqueId());
        int current = data.skillLevel(skill.getId());
        int max = getMaxLevel(skill);
        if (current >= max) {
            job.sendJobMessage(player, "&cOsiągnąłeś maksymalny poziom tej umiejętności!");
            return false;
        }
        int requiredLevel = getRequiredJobLevel(skill);
        if (data.level < requiredLevel) {
            job.sendJobMessage(player, "&cPotrzebujesz poziomu " + requiredLevel + " w pracy, aby odblokować tę umiejętność!");
            return false;
        }
        int cost = getCost(skill, current + 1);
        if (data.skillPoints < cost) {
            job.sendJobMessage(player, "&cNie masz wystarczającej liczby punktów! Potrzebujesz " + cost);
            return false;
        }
        data.skillPoints -= cost;
        job.getDataStore().savePlayer(player.getUniqueId(), data);
        job.getDataStore().setSkillLevel(player, skill.getId(), current + 1);
        job.sendJobMessage(player, "&aUlepszono &e" + getName(skill) + " &ado poziomu &e" + (current + 1));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        return true;
    }
}
