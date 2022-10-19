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
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;

import factionsplusplus.models.Faction;
import factionsplusplus.models.War;

@Singleton
public class WarRepository {
    private final ArrayList<War> warStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "wars.json";

    @Inject
    public WarRepository(@Named("dataFolder") String dataPath) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
    }

    // Load wars
    public void load() {
        this.warStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.warStore.addAll(Arrays.asList(gson.fromJson(reader, War[].class)));
        } catch (FileNotFoundException ignored) {
            // TODO: log here
        }
    }

    // Save a war
    public void create(War war) {
        this.warStore.add(war);
    }

    // Delete a war
    public void delete(War war) {
        this.warStore.remove(war);
    }

    // Retrieve a list of wars a faction is involved in
    public ArrayList<War> getAllForFaction(UUID factionUUID) {
        ArrayList<War> results = new ArrayList<>();
        for (War war : this.warStore) {
            if (war.getAttacker().equals(factionUUID) || war.getDefender().equals(factionUUID)) results.add(war); 
        }
        return results;
    }

    // Retrieve an active war between two factions
    public War getActiveWarsBetween(UUID factionOneUUID, UUID factionTwoUUID) {
        for (War war : this.warStore) {
            if (
                (factionOneUUID.equals(war.getAttacker()) || factionOneUUID.equals(war.getDefender())) &&
                (factionTwoUUID.equals(war.getAttacker()) || factionTwoUUID.equals(war.getDefender())) &&
                war.isActive()
            ) {
                return war;
            }
        }
        return null;
    }

    // Retrieve all wars
    public ArrayList<War> all() {
        return this.warStore;
    }

    // Write to file
    public void persist() {
        List<JsonElement> warsToSave = new ArrayList<>();
        for (War war : this.warStore) {
            warsToSave.add(war.toJsonTree());
        }
        File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(warsToSave));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}