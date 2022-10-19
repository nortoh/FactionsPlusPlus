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
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonStreamParser;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import factionsplusplus.models.Faction;

@Singleton
public class FactionRepository {
    private final HashMap<UUID, Faction> factionStore = new HashMap<>();
    private final String dataPath;
    private final static String FILE_NAME = "factions.json";

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
            JsonStreamParser parser = new JsonStreamParser(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            if (parser.hasNext()) {
                JsonArray factionArray = parser.next().getAsJsonArray();
                for (JsonElement factionElement : factionArray) {
                    Faction faction = gson.fromJson(factionElement, Faction.class);
                    this.factionStore.put(faction.getID(), faction);
                }
            }
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

    // Retrieve a faction by prefix
    public Faction getByPrefix(String prefix) {
        return null;
    }

    // Retrieve a faction by name
    public Faction get(String factionName) {
        Optional<Map.Entry<UUID, Faction>> mapEntry = this.factionStore
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getName().equalsIgnoreCase(factionName))
            .findFirst();
        if (mapEntry.isPresent()) return mapEntry.get().getValue();
        return null;
    }

    public Faction getByID(UUID uuid) {
        return this.factionStore.get(uuid);
    }

    // Retrieve all factions
    public HashMap<UUID, Faction> all() {
        return this.factionStore;
    }

    // Retrieve number of factions
    public Integer count() {
        return this.factionStore.size();
    }

    // Write to file
    public void persist() {
        File file = new File(this.dataPath);
        ArrayList<Faction> factionsToSave = new ArrayList<>();
        for (Faction faction : this.factionStore.values()) factionsToSave.add(faction);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(factionsToSave));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}