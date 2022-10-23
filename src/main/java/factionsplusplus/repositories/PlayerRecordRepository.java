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

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.ArrayList;
import java.lang.reflect.Type;

import factionsplusplus.models.PlayerRecord;
import factionsplusplus.utils.Logger;

@Singleton
public class PlayerRecordRepository {
    private List<PlayerRecord> playerStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "players.json";
    private final static Type JSON_TYPE = new TypeToken<List<PlayerRecord>>() { }.getType();
    private final Logger logger;

    @Inject
    public PlayerRecordRepository(@Named("dataFolder") String dataPath, Logger logger) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
        this.logger = logger;
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
            this.playerStore = gson.fromJson(reader, PlayerRecordRepository.JSON_TYPE);
        } catch (FileNotFoundException ignored) {
            this.logger.error(String.format("File %s not found", this.dataPath), ignored);
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
        Optional<PlayerRecord> record = this.playerStore.stream()
            .filter(r -> r.getPlayerUUID().equals(playerUUID))
            .findFirst();
        return record.orElse(null);
    }

    // Retrieve all records
    public List<PlayerRecord> all() {
        return this.playerStore;
    }

    /*
     * Retrieves the number of players currently stored
     *
     * @return the number of players currently stored
     */
    public int count() {
        return this.playerStore.size();
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
            outputStreamWriter.write(gson.toJson(this.playerStore, PlayerRecordRepository.JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            this.logger.error(String.format("Failed to write to %s", this.dataPath), e);
        }
    }
}