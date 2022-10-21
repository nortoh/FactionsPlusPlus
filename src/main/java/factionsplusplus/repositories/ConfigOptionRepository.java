package factionsplusplus.repositories;

import factionsplusplus.models.ConfigOption;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class ConfigOptionRepository {
    private final Map<String, ConfigOption> optionStore = new HashMap<>();
    private final List<String> deprecatedOptionNames = new ArrayList<>();

    public ConfigOption get(String nameSearch) {
        return this.optionStore.get(nameSearch);
    }

    public void add(ConfigOption option) {
        this.optionStore.put(option.getName(), option);
    }

    public void addDeprecatedOption(String name) {
        this.deprecatedOptionNames.add(name);
    }

    public List<String> allDeprecatedOptions() {
        return this.deprecatedOptionNames;
    }

    public Map<String, ConfigOption> all() {
        return this.optionStore;
    }
}