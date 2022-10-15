package dansplugins.factionsystem.repositories;

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
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import dansplugins.factionsystem.models.Faction;

@Singleton
public class FactionRepository {
    private final ArrayList<Faction> factionStore = new ArrayList<>();
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
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            Faction[] jsonFactions = gson.fromJson(reader, Faction[].class);
            this.factionStore.addAll(Arrays.asList(jsonFactions));
        } catch (FileNotFoundException ignored) {
            // TODO: log here
        }
    }

    // Save a faction after creating
    public void create(Faction faction) {
        this.factionStore.add(faction);
    }

    // Delete a faction
    public void delete(Faction faction) {
        this.factionStore.remove(faction);
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
        for (Faction faction : this.factionStore) {
            if (faction.getName().equalsIgnoreCase(factionName)) {
                return faction;
            }
        }
        return null;
    }

    // Retrieve all factions
    public ArrayList<Faction> all() {
        return this.factionStore;
    }

    // Write to file
    public void persist() {
        List<JsonElement> factionsToSave = new ArrayList<>();
        for (Faction faction : this.factionStore) {
            factionsToSave.add(faction.toJsonTree());
        }
        File file = new File(this.dataPath);
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