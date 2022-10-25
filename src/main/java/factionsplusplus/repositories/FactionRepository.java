package factionsplusplus.repositories;

import com.google.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;

import factionsplusplus.models.Faction;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.utils.Logger;

@Singleton
public class FactionRepository {
    private Map<UUID, Faction> factionStore = new HashMap<>();
    private final String dataPath;
    private final static String FILE_NAME = "factions.json";
    private final static Type JSON_TYPE = new TypeToken<Map<UUID, Faction>>() { }.getType();
    private final Map<String, ConfigurationFlag> defaultFlags = new HashMap<>();
    private final Logger logger;

    @Inject
    public FactionRepository(@Named("dataFolder") String dataPath, Logger logger) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
        this.logger = logger;
    }

    // Load factions
    public void load() {
        this.factionStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.factionStore = gson.fromJson(reader, FactionRepository.JSON_TYPE);
            this.initializeFactions();
        } catch (FileNotFoundException ignored) {
            this.logger.error(String.format("File %s not found", this.dataPath), ignored);
        }
    }

    // Save a faction after creating
    public void create(Faction faction) {
        this.factionStore.put(faction.getID(), faction);
    }

    // Delete a faction
    public void delete(UUID factionUUID) {
        this.factionStore.remove(factionUUID);
    }
    public void delete(Faction faction) {
        this.factionStore.remove(faction.getID());
    }
    public void delete(String factionName) {
        this.delete(this.get(factionName));
    }

    /*
     * Retrieve a faction by prefix
     *
     * @param prefix the prefix of the faction to search for
     * @return Faction instance if found, null otherwise
     */
    public Faction getByPrefix(String prefix) {
        Optional<Faction> faction = this.factionStore
            .values()
            .stream()
            .filter(entry -> entry.getPrefix().equalsIgnoreCase(prefix))
            .findFirst();
        return faction.orElse(null);
    }

    /*
     * Retrieve a faction by name
     *
     * @param factionName the name of the faction to search for
     * @return Faction instance if found, null otherwise
     */
    public Faction get(String factionName) {
        Optional<Faction> faction = this.factionStore
            .values()
            .stream()
            .filter(entry -> entry.getName().equalsIgnoreCase(factionName))
            .findFirst();
        return faction.orElse(null);
    }

    /*
     * Retrieve a faction by a member player
     *
     * @param playerUUID the UUID of the player to determine faction for
     * @return Faction instance if found, null otherwise
     */
    public Faction getForPlayer(UUID playerUUID) {
        Optional<Faction> faction = this.factionStore
            .values()
            .stream()
            .filter(entry -> entry.isMember(playerUUID))
            .findFirst();
        return faction.orElse(null);
    }

    /*
     * Retrieve a faction by a member player
     *
     * @param player the Player instance to determine faction for
     * @return Faction instance if found, null otherwise
     */
    public Faction getForPlayer(OfflinePlayer player) {
        return this.getForPlayer(player.getUniqueId());
    }

    /*
     * Retrieve a faction by its UUID
     *
     * @param uuid the UUID of the faction to search for
     * @return Faction instance if found, null otherwise
     */
    public Faction get(UUID uuid) {
        return this.factionStore.get(uuid);
    }


    // TODO: refactor this, it's a bit bloated
    /*
     * Retrieves factions in a factions vassalage tree
     *
     * @param the faction you wish to get the vassalage tree for
     * @return a List of Faction instances that are in the vassalage tree
     */
    public List<Faction> getInVassalageTree(Faction initialFaction) {
        List<Faction> foundFactions = new ArrayList<>();

        foundFactions.add(initialFaction);

        boolean newFactionsFound = true;

        int numFactionsFound;

        while (newFactionsFound) {
            List<Faction> toAdd = new ArrayList<>();
            for (Faction current : foundFactions) {

                // record number of factions
                numFactionsFound = foundFactions.size();

                Faction liege = this.get(current.getLiege());
                if (liege != null) {
                    if (!toAdd.contains(liege) && !foundFactions.contains(liege)) {
                        toAdd.add(liege);
                        numFactionsFound++;
                    }

                    for (UUID vassalID : liege.getVassals()) {
                        Faction vassal = this.get(vassalID);
                        if (!toAdd.contains(vassal) && !foundFactions.contains(vassal)) {
                            toAdd.add(vassal);
                            numFactionsFound++;
                        }
                    }
                }

                for (UUID vassalID : current.getVassals()) {
                    Faction vassal = this.get(vassalID);
                    if (!toAdd.contains(vassal) && !foundFactions.contains(vassal)) {
                        toAdd.add(vassal);
                        numFactionsFound++;
                    }
                }
                // if number of factions not different then break loop
                if (numFactionsFound == foundFactions.size()) {
                    newFactionsFound = false;
                }
            }
            foundFactions.addAll(toAdd);
            toAdd.clear();
        }
        return foundFactions;
    }

    // Retrieve all factions
    public Map<UUID, Faction> all() {
        return this.factionStore;
    }

    /*
     * Retrieves the number of factions currently stored
     *
     * @return the number of factions currently stored
     */
    public int count() {
        return this.factionStore.size();
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag, boolean addToMissingFactions) {
        this.defaultFlags.put(flagName, flag);
        if (addToMissingFactions) this.addAnyMissingFlagsToFactions();
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag) {
        this.addDefaultConfigurationFlag(flagName, flag, true);
    }

    public void addAnyMissingFlagsToFaction(Faction faction) {
        List<String> missingFlags = this.defaultFlags.keySet().stream().filter(key -> faction.getFlag(key) == null).collect(Collectors.toList());
        if (!missingFlags.isEmpty()) {
            missingFlags.stream().forEach(flag -> {
                faction.getFlags().put(flag, this.defaultFlags.get(flag));
            });
        }
    }

    public void addAnyMissingFlagsToFactions() {
        this.factionStore.values()
            .stream()
            .forEach(faction -> this.addAnyMissingFlagsToFaction(faction));
    }

    public void addFlagToMissingFactions(String flagName) {
        // get the flag from defaultFlags
        ConfigurationFlag flag = this.defaultFlags.get(flagName);
        // TODO: error if null
        for (Faction faction : this.factionStore.values()) {
            if (!faction.getFlags().containsKey(flagName)) faction.getFlags().put(flagName, flag);
        }
    }

    public void removeFlagFromFactions(String flagName) {
        // remove from default flags first
        this.defaultFlags.remove(flagName);
        // iterate through factions, removing the flag
        for (Faction faction : this.factionStore.values()) faction.getFlags().remove(flagName);
    }

    public Map<String, ConfigurationFlag> getDefaultFlags() {
        return this.defaultFlags;
    }

    private void initializeFactions() {
        this.factionStore.values()
            .stream()
            .forEach(faction -> {
                faction.initialize();
                this.addAnyMissingFlagsToFaction(faction);
            });
    }

    // Write to file
    public void persist() {
        File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .enableComplexMapKeySerialization()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(this.factionStore, FactionRepository.JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            this.logger.error(String.format("Failed to write to %s", this.dataPath), e);
        }
    }
}