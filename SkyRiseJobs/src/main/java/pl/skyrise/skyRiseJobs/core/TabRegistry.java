package pl.skyrise.skyRiseJobs.core;

import org.bukkit.command.CommandSender;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

public class TabRegistry {
    private final Map<String, BiFunction<CommandSender, String[], List<String>>> providers = new HashMap<>();

    public void register(String command, BiFunction<CommandSender, String[], List<String>> provider) {
        if (command == null || provider == null) return;
        providers.put(command.toLowerCase(Locale.ROOT), provider);
    }

    public void unregister(String command) {
        if (command != null) providers.remove(command.toLowerCase(Locale.ROOT));
    }

    public List<String> complete(String command, CommandSender sender, String[] args) {
        BiFunction<CommandSender, String[], List<String>> provider = providers.get(command.toLowerCase(Locale.ROOT));
        return provider != null ? provider.apply(sender, args) : List.of();
    }

    public Set<String> getRegisteredCommands() {
        return Collections.unmodifiableSet(providers.keySet());
    }

    public static List<String> filter(Collection<String> options, String input) {
        if (options == null) return List.of();
        if (input == null || input.isEmpty()) return List.copyOf(options);
        String lower = input.toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option != null && option.toLowerCase(Locale.ROOT).startsWith(lower)).sorted().toList();
    }
}
