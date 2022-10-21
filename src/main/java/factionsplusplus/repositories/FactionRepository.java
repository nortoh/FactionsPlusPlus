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
import java.lang.reflect.Type;
import java.util.Optional;

import org.bukkit.OfflinePlayer;

import factionsplusplus.models.Faction;

@Singleton
public class FactionRepository {
    private Map<UUID, Faction> factionStore = new HashMap<>();
    private final String dataPath;
    private final static String FILE_NAME = "factions.json";
    private final static Type JSON_TYPE = new TypeToken<Map<UUID, Faction>>() { }.getType();

    @Inject
    public FactionRepository(@Named("dataFolder") String dataPath) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
    }

    // Load factions
    public void load() {
        this.factionStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.factionStore = gson.fromJson(reader, FactionRepository.JSON_TYPE);
            // TODO: reimplement
            //for (Faction faction : jsonFactions) faction.getFlags().loadMissingFlagsIfNecessary();
        } catch (FileNotFoundException ignored) {
            // TODO: log here
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

    // Write to file
    public void persist() {
        File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(this.factionStore, FactionRepository.JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}