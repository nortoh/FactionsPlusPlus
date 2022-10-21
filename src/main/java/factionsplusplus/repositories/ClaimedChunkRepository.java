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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;

@Singleton
public class ClaimedChunkRepository {
    private final ArrayList<ClaimedChunk> claimedChunksStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "claimedchunks.json";

    @Inject
    public ClaimedChunkRepository(@Named("dataFolder") String dataPath) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
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
            this.claimedChunksStore.addAll(Arrays.asList(gson.fromJson(reader, ClaimedChunk[].class)));
        } catch (FileNotFoundException ignored) {
            // TODO: log here
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
    public ClaimedChunk get(double x, double z, String world) {
        for (ClaimedChunk claimedChunk : this.claimedChunksStore) {
            if (
                claimedChunk.getCoordinates()[0] == x
                && claimedChunk.getCoordinates()[1] == z
                && claimedChunk.getWorldName().equalsIgnoreCase(world)
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
    public ArrayList<ClaimedChunk> all() {
        return this.claimedChunksStore;
    }

    // Write to file
    public void persist() {
        List<JsonElement> chunksToSave = new ArrayList<>();
        for (ClaimedChunk chunk : this.claimedChunksStore) {
            chunksToSave.add(chunk.toJsonTree());
        }
        File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(chunksToSave));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}