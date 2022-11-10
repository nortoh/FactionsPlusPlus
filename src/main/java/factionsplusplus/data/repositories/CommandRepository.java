package factionsplusplus.data.repositories;

import factionsplusplus.models.Command;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
        List<String> arguments = new ArrayList<String>(Arrays.asList(nameSearch.split(" ")));
        Command baseCommand = this.get(arguments.remove(0), false);
        if (baseCommand == null || (! baseCommand.hasSubCommands() && ! arguments.isEmpty())) return null;
        if (arguments.isEmpty()) return baseCommand;
        Command returnCommand = baseCommand;
        while (! arguments.isEmpty()) {
            String argument = arguments.remove(0).toLowerCase();
            returnCommand = returnCommand.getSubCommands().values().stream()
                .filter(c -> c.getName().toLowerCase().equals(argument) || Arrays.asList(c.getAliases()).contains(argument))
                .findFirst()
                .orElse(null);
            if (returnCommand == null) return null;
        }
        return returnCommand;
    }

    public void add(Command command) {
        this.commandStore.put(command.getName().toLowerCase(), command);
    }

    public Map<String, Command> all() {
        return this.commandStore;
    }
}