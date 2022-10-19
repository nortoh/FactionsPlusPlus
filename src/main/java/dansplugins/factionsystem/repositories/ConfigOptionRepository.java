package dansplugins.factionsystem.repositories;

import dansplugins.factionsystem.models.ConfigOption;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;

@Singleton
public class ConfigOptionRepository {
    private final HashMap<String, ConfigOption> optionStore = new HashMap<>();
    private final ArrayList<String> deprecatedOptionNames = new ArrayList<>();

    public ConfigOption get(String nameSearch) {
        return this.optionStore.get(nameSearch);
    }

    public void add(ConfigOption option) {
        this.optionStore.put(option.getName(), option);
    }

    public void addDeprecatedOption(String name) {
        this.deprecatedOptionNames.add(name);
    }

    public ArrayList<String> allDeprecatedOptions() {
        return this.deprecatedOptionNames;
    }

    public HashMap<String, ConfigOption> all() {
        return this.optionStore;
    }
}