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

import dansplugins.factionsystem.models.LockedBlock;

@Singleton
public class LockedBlockRepository {
    private final ArrayList<LockedBlock> lockedBlockStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "lockedblocks.json";

    @Inject
    public LockedBlockRepository(@Named("dataFolder") String dataPath) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
    }

    // Load claimed chunks
    public void load() {
        this.lockedBlockStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.lockedBlockStore.addAll(Arrays.asList(gson.fromJson(reader, LockedBlock[].class)));
        } catch (FileNotFoundException ignored) {
            // TODO: log here
        }
    }

    // Save a locked block
    public void create(LockedBlock block) {
        this.lockedBlockStore.add(block);
    }

    // Delete a locked block
    public void delete(LockedBlock block) {
        this.lockedBlockStore.remove(block);
    }

    // Retrieve a locked block by location
    public LockedBlock get(int x, int y, int z, String world) {
        for (LockedBlock block : this.lockedBlockStore) {
            if (
                block.getX() == x &&
                block.getY() == y &&
                block.getZ() == z &&
                block.getWorld().equalsIgnoreCase(world)
            ) {
                return block;
            }
        }
        return null;
    }

    // Retrieve all locked blocks
    public ArrayList<LockedBlock> all() {
        return this.lockedBlockStore;
    }

    // Write to file
    public void persist() {
        List<JsonElement> blocksToSave = new ArrayList<>();
        for (LockedBlock block : this.lockedBlockStore) {
            blocksToSave.add(block.toJsonTree());
        }
        File file = new File(this.dataPath);
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            file.createNewFile();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
            outputStreamWriter.write(gson.toJson(blocksToSave));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}