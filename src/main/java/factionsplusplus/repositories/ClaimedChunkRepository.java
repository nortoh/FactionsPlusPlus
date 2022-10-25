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
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.lang.reflect.Type;

import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.utils.Logger;

@Singleton
public class ClaimedChunkRepository {
    private List<ClaimedChunk> claimedChunksStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "claimedchunks.json";
    private final static Type JSON_TYPE = new TypeToken<List<ClaimedChunk>>() { }.getType();
    private final Logger logger;

    @Inject
    public ClaimedChunkRepository(@Named("dataFolder") String dataPath, Logger logger) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
        this.logger = logger;
    }

    // Load claimed chunks
    public void load() {
        this.claimedChunksStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.claimedChunksStore = gson.fromJson(reader, ClaimedChunkRepository.JSON_TYPE);
        } catch (FileNotFoundException ignored) {
            this.logger.error(String.format("File %s not found", this.dataPath), ignored);
        }
    }

    // Save a claimed chunk
    public void create(ClaimedChunk chunk) {
        this.claimedChunksStore.add(chunk);
    }

    // Delete a claimed chunk
    public void delete(ClaimedChunk chunk) {
        this.claimedChunksStore.remove(chunk);
    }

    // Retrieve a claimed chunk by location
    public ClaimedChunk get(double x, double z, UUID world) {
        for (ClaimedChunk claimedChunk : this.claimedChunksStore) {
            if (
                claimedChunk.getCoordinates()[0] == x
                && claimedChunk.getCoordinates()[1] == z
                && claimedChunk.getWorldUUID().equals(world)
            ) {
                return claimedChunk;
            }
        }
        return null;
    }

    // Retrieve a list of all claimed chunks for a faction
    public List<ClaimedChunk> getAllForFaction(Faction faction) {
        return this.claimedChunksStore.stream()
            .filter(chunk -> chunk.getHolder().equals(faction.getID()))
            .collect(Collectors.toList());
    }

    // Retrieve all claimed chunks
    public List<ClaimedChunk> all() {
        return this.claimedChunksStore;
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
            outputStreamWriter.write(gson.toJson(this.claimedChunksStore, JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            this.logger.error(String.format("Failed to write to %s", this.dataPath), e);
        }
    }
}