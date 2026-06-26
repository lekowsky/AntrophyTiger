package pl.skyrise.skyRiseJobs.features._template;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseJobs.SkyRiseJobs;
import pl.skyrise.skyRiseJobs.api.job.BaseJobModule;
import pl.skyrise.skyRiseJobs.api.job.JobData;
import pl.skyrise.skyRiseJobs.api.job.JobSkill;
import pl.skyrise.skyRiseJobs.utils.ItemBuilder;

import java.util.List;

/**
 * SZABLON NOWEJ PRACY — kopiuj ten plik tworząc kolejną pracę.
 *
 * Pokazuje MINIMUM, jakie musi dostarczyć praca. Całe GUI pracy, drzewko umiejętności,
 * system punktów, poziomy i baza danych pochodzą z {@link BaseJobModule} — nie kopiujesz ich.
 * Zmienia się TYLKO to, "jak praca działa" (metody startWork/completeWork/cancelWork).
 *
 * Aby aktywować: zarejestruj w {@code SkyRiseJobs#registerModules()}:
 *     moduleManager.register(new ExampleJobModule(this));
 * oraz dodaj config: src/main/resources/config/example/config.yml
 *
 * Domyślnie NIE jest rejestrowany (to tylko wzorzec).
 */
public class ExampleJobModule extends BaseJobModule {

    public ExampleJobModule(SkyRiseJobs plugin) {
        super(plugin, "example");
    }

    // 1) METADANE -----------------------------------------------------------

    @Override
    public String getDisplayName() {
        return getJobConfig().getConfig().getString("job-display-name", "Przykładowa Praca");
    }

    @Override
    public List<String> getDescription() {
        return List.of("&7Przykładowa praca oparta na wspólnym szkielecie.");
    }

    @Override
    public ItemStack getIcon(Player player) {
        return new ItemBuilder(Material.GOLDEN_PICKAXE).name(getMenuColor() + "&l" + getDisplayName()).build();
    }

    @Override
    public String getMenuColor() {
        return getJobConfig().getConfig().getString("job-menu-color", "&6");
    }

    @Override
    protected boolean hasJobAccess(Player player) {
        return player.hasPermission("skyrisejobs.job.example");
    }

    // 2) PERKI (osobne dla tej pracy) --------------------------------------

    @Override
    protected List<JobSkill> defineSkills() {
        return List.of(
                JobSkill.of("wydajnosc", Material.EXPERIENCE_BOTTLE)
                        .slot(11).name("&a&lWydajność")
                        .description("Więcej zarobku za zlecenie.")
                        .maxLevel(5).cost(1, 2, 3, 4, 5).requiredJobLevel(0).build(),
                JobSkill.of("szczescie", Material.GOLD_NUGGET)
                        .slot(13).name("&e&lSzczęście")
                        .description("Większa szansa na punkt umiejętności.")
                        .maxLevel(3).cost(2, 3, 4).requiredJobLevel(2).build()
        );
    }

    // 3) OPCJONALNE: dodatkowe statystyki / instrukcja ---------------------

    @Override
    public List<String> getStatsLines(Player player, JobData data) {
        return List.of("&7Bonus wydajności: &a+" + (getDataStore().getSkillLevel(player, "wydajnosc") * 10) + "%");
    }

    @Override
    public List<String> getHowToWork() {
        return List.of("&71. Kliknij Rozpocznij pracę.", "&72. Wykonaj zadanie.", "&73. Odbierz wypłatę.");
    }

    // 4) LOGIKA PRACY — TO JEDYNE, CO NAPRAWDĘ SIĘ ZMIENIA ----------------

    @Override
    public WorkState getWorkState(Player player) {
        // Tu zwykle sprawdzasz własny stan sesji pracy. Demo: zawsze IDLE.
        return WorkState.IDLE;
    }

    @Override
    public void startWork(Player player) {
        sendJobMessage(player, "&aRozpocząłeś przykładową pracę!");
        // ... uruchom własną mechanikę (minigra, region, zadania itd.)
    }

    @Override
    public void completeWork(Player player) {
        double pay = withMoneyBoost(50.0);                 // boost ekonomiczny gratis
        getLevels().addXp(player, 100);                    // poziom pracy
        grantCharacterExp(player, 100);                    // globalny poziom postaci
        getDataStore().addTotalEarned(player, pay);
        sendJobMessage(player, "&aZakończono! Zarobek: &e" + pay + " $");
    }

    @Override
    public void cancelWork(Player player) {
        sendJobMessage(player, "&cAnulowano przykładową pracę.");
    }
}
