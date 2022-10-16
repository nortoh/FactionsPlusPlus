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
import java.util.UUID;
import java.util.ArrayList;
import java.util.Arrays;

import dansplugins.factionsystem.models.PlayerRecord;

@Singleton
public class PlayerRecordRepository {
    private final ArrayList<PlayerRecord> playerStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "players.json";

    @Inject
    public PlayerRecordRepository(@Named("dataFolder") String dataPath) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
    }

    // Load records
    public void load() {
        this.playerStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.playerStore.addAll(Arrays.asList(gson.fromJson(reader, PlayerRecord[].class)));
        } catch (FileNotFoundException ignored) {
            // TODO: log here
        }
    }

    // Save a record after creating
    public void create(PlayerRecord record) {
        this.playerStore.add(record);
    }

    // Delete a record
    public void delete(PlayerRecord record) {
        this.playerStore.remove(record);
    }
    public void delete(UUID playerUUID) {
        this.delete(this.get(playerUUID));
    }

    // Retrieve a record by uuid
    public PlayerRecord get(UUID playerUUID) {
        for (PlayerRecord record : this.playerStore) {
            if (record.getPlayerUUID().equals(playerUUID)) {
                return record;
            }
        }
        return null;
    }

    // Retrieve all records
    public ArrayList<PlayerRecord> all() {
        return this.playerStore;
    }

    // Write to file
    public void persist() {
        List<JsonElement> recordsToSave = new ArrayList<>();
        for (PlayerRecord record : this.playerStore) {
            recordsToSave.add(record.toJsonTree());
        }
        File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(recordsToSave));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}