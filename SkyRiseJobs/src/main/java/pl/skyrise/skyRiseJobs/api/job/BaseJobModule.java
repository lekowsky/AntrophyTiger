package pl.skyrise.skyRiseJobs.api.job;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.AbstractJobModule;
import pl.skyrise.skyRiseJobs.utils.ColorUtil;

import java.util.Collections;
import java.util.List;

/**
 * Wspólna baza KAŻDEJ pracy.
 *
 * Spina ze sobą gotowy, współdzielony framework:
 *  - {@link JobDataStore}   — dane gracza (poziom, xp, zarobki, punkty, perki),
 *  - {@link JobLevels}      — progresja poziom/exp,
 *  - {@link JobSkillService}— silnik perków (zakup/koszty),
 *  - {@link JobGui}         — GUI pracy + drzewko umiejętności.
 *
 * Nowa praca dziedziczy to wszystko i dostarcza WYŁĄCZNIE:
 *  - metadane: {@code getDisplayName/getDescription/getIcon/getMenuColor},
 *  - listę perków: {@link #defineSkills()} (perki są osobne dla każdej pracy),
 *  - logikę pracy: {@link #startWork}, {@link #completeWork}, {@link #cancelWork},
 *    {@link #getWorkState} oraz opcjonalnie {@link #getStatsLines}/{@link #getHowToWork}.
 *
 * Dzięki temu kolejne prace nie powielają GUI/danych/perków — zmienia się tylko
 * to, „jak praca działa”.
 */
public abstract class BaseJobModule extends AbstractJobModule {

    private JobDataStore dataStore;
    private JobLevels levels;
    private JobSkillService skillService;
    private JobGui gui;
    private List<JobSkill> skills;

    protected BaseJobModule(SkyRiseJobs plugin, String jobId) {
        super(plugin, jobId);
    }

    @Override
    public void onEnable() {
        super.onEnable(); // tworzy folder + ładuje config
        this.dataStore = new JobDataStore(plugin, getJobId(), plugin.getJobDatabase());
        this.levels = new JobLevels(this);
        this.skillService = new JobSkillService(this);
        this.gui = new JobGui(this);
        this.skills = Collections.unmodifiableList(defineSkills());
    }

    // ---------- Dostęp do frameworku ----------

    public JobDataStore getDataStore() { return dataStore; }
    public JobLevels getLevels() { return levels; }
    public JobSkillService getSkillService() { return skillService; }
    public JobGui getGui() { return gui; }
    public List<JobSkill> getSkills() { return skills != null ? skills : List.of(); }

    @Override
    public void openMenu(Player player) {
        gui.openMain(player);
    }

    // ---------- Komunikaty (DRY, z globalnym prefixem PRACA) ----------

    public net.kyori.adventure.text.Component formatJobMessage(String legacyMessage) {
        return ColorUtil.mini(plugin.prefix() + ColorUtil.legacyToMini(legacyMessage));
    }

    public void sendJobMessage(CommandSender sender, String legacyMessage) {
        sender.sendMessage(formatJobMessage(legacyMessage));
    }

    // ---------- Integracja z globalnym poziomem postaci / boostami ----------

    /** Dodaje XP do GLOBALNego poziomu postaci (z boostem). Wywołuj z logiki pracy. */
    public void grantCharacterExp(Player player, double baseExp) {
        addCharacterExp(player, baseExp);
    }

    /** Przelicza wypłatę z globalnym boostem. Wywołuj przed wypłatą. */
    public double withMoneyBoost(double baseMoney) {
        return applyMoneyBoost(baseMoney);
    }

    // ---------- HOOKI DO NADPISANIA PRZEZ KONKRETNĄ PRACĘ ----------

    /** Lista perków tej pracy (definiowana osobno dla każdej pracy). */
    protected abstract List<JobSkill> defineSkills();

    /** Stan pracy gracza — steruje przyciskiem akcji w GUI. */
    public abstract WorkState getWorkState(Player player);

    /** Rozpoczęcie zlecenia. */
    public abstract void startWork(Player player);

    /** Zakończenie/odbiór wypłaty. */
    public abstract void completeWork(Player player);

    /** Rezygnacja z aktywnej pracy. */
    public abstract void cancelWork(Player player);

    /** Dodatkowe linie statystyk w głównym GUI (np. premia do zarobku). Domyślnie brak. */
    public List<String> getStatsLines(Player player, JobData data) {
        return List.of();
    }

    /** Instrukcja "Jak pracować?" w głównym GUI. Domyślnie brak (item się nie pojawia). */
    public List<String> getHowToWork() {
        return List.of();
    }

    /** Stany pracy używane przez wspólne GUI. */
    public enum WorkState { IDLE, IN_PROGRESS, READY_TO_FINISH }
}
