package factionsplusplus.data.repositories;

import factionsplusplus.models.Command;
import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Map;

@Singleton
public class CommandRepository {
    private final Map<String, Command> commandStore = new HashMap<>();

    public Command get(String nameSearch, boolean onlySearchRootNames) {
        // Look for exact first
        if (this.commandStore.containsKey(nameSearch.toLowerCase())) return this.commandStore.get(nameSearch.toLowerCase());
        if (onlySearchRootNames) return null; // we're not going to do any alias searching here
        Optional<Command> command = this.commandStore.values().stream()
            .filter(c -> Arrays.asList(c.getAliases()).contains(nameSearch))
            .findFirst();
        return command.orElse(null);
    }

    public Command get(String nameSearch) {
        return this.get(nameSearch, false);
    }

    public void add(Command command) {
        this.commandStore.put(command.getName().toLowerCase(), command);
    }

    public Map<String, Command> all() {
        return this.commandStore;
    }
}