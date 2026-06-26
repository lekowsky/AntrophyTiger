package pl.skyrise.skyRiseJobs.api;

public interface Module {
    String getName();
    void onEnable();
    void onDisable();
    default void onReload() {}
}
