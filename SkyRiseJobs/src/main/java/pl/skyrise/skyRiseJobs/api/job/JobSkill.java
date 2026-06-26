package pl.skyrise.skyRiseJobs.api.job;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * Definicja pojedynczego perka / umiejętności pracy.
 *
 * Perki są deklarowane OSOBNO dla każdej pracy (przez {@code BaseJobModule#defineSkills()}),
 * a framework dostarcza tylko silnik: zakup, koszty, render w drzewku, zapis poziomów.
 *
 * Buduje się je płynnie:
 * <pre>
 * JobSkill.of("szybsze_szorowanie", Material.DIAMOND_PICKAXE)
 *         .slot(11).name("&b&lSzybsze szorowanie")
 *         .description("Zwiększa siłę szorowania.")
 *         .maxLevel(3).cost(1, 2, 3).requiredJobLevel(0)
 *         .build();
 * </pre>
 */
public final class JobSkill {

    private final String id;
    private final String defaultName;
    private final Material icon;
    private final int slot;
    private final int maxLevel;
    private final int requiredJobLevel;
    private final List<String> description;
    private final int[] costPerLevel;

    private JobSkill(Builder builder) {
        this.id = builder.id;
        this.defaultName = builder.name != null ? builder.name : builder.id;
        this.icon = builder.icon != null ? builder.icon : Material.PAPER;
        this.slot = builder.slot;
        this.maxLevel = Math.max(1, builder.maxLevel);
        this.requiredJobLevel = Math.max(0, builder.requiredJobLevel);
        this.description = builder.description;
        this.costPerLevel = builder.costPerLevel;
    }

    public String getId() { return id; }
    public String getDefaultName() { return defaultName; }
    public Material getIcon() { return icon; }
    public int getSlot() { return slot; }
    public int getMaxLevel() { return maxLevel; }
    public int getRequiredJobLevel() { return requiredJobLevel; }
    public List<String> getDescription() { return description; }

    /** Domyślny koszt ulepszenia do podanego poziomu (1-indeksowany). Config pracy może to nadpisać. */
    public int getDefaultCost(int level) {
        if (costPerLevel == null || costPerLevel.length == 0) return level;
        int idx = Math.max(1, level) - 1;
        if (idx < costPerLevel.length) return costPerLevel[idx];
        return costPerLevel[costPerLevel.length - 1];
    }

    public static Builder of(String id, Material icon) {
        return new Builder(id, icon);
    }

    public static final class Builder {
        private final String id;
        private final Material icon;
        private String name;
        private int slot = -1;
        private int maxLevel = 5;
        private int requiredJobLevel = 0;
        private List<String> description = new ArrayList<>();
        private int[] costPerLevel;

        private Builder(String id, Material icon) {
            this.id = id;
            this.icon = icon;
        }

        public Builder name(String name) { this.name = name; return this; }
        public Builder slot(int slot) { this.slot = slot; return this; }
        public Builder maxLevel(int maxLevel) { this.maxLevel = maxLevel; return this; }
        public Builder requiredJobLevel(int level) { this.requiredJobLevel = level; return this; }

        public Builder description(String... lines) {
            this.description = lines == null ? new ArrayList<>() : List.of(lines);
            return this;
        }

        /** Koszt w punktach za każdy poziom (poziom 1, 2, 3, ...). */
        public Builder cost(int... costPerLevel) { this.costPerLevel = costPerLevel; return this; }

        public JobSkill build() { return new JobSkill(this); }
    }
}
