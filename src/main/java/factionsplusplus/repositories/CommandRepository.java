package factionsplusplus.repositories;

import factionsplusplus.models.Command;
import com.google.inject.Singleton;
import java.util.HashMap;

@Singleton
public class CommandRepository {
    private final HashMap<String, Command> commandStore = new HashMap<>();

    public Command get(String nameSearch) {
        // TODO: search both name and aliases
        return this.commandStore.get(nameSearch);
    }

    public void add(Command command) {
        this.commandStore.put(command.getName(), command);
    }

    public HashMap<String, Command> all() {
        return this.commandStore;
    }
}