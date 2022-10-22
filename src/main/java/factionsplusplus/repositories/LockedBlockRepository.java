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

import org.bukkit.block.Block;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;

import factionsplusplus.models.LockedBlock;

@Singleton
public class LockedBlockRepository {
    private List<LockedBlock> lockedBlockStore = new ArrayList<>();
    private final String dataPath;
    private final static String FILE_NAME = "lockedblocks.json";
    private final static Type JSON_TYPE = new TypeToken<List<LockedBlock>>() { }.getType();


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
            this.lockedBlockStore = gson.fromJson(reader, LockedBlockRepository.JSON_TYPE);
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
    public void delete(Block b) {
        LockedBlock block = this.get(b.getX(), b.getY(), b.getZ(), b.getWorld().getName());
        if (block != null) this.delete(block);
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
    public List<LockedBlock> all() {
        return this.lockedBlockStore;
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
            outputStreamWriter.write(gson.toJson(this.lockedBlockStore, LockedBlockRepository.JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            // TODO: log here
        }
    }
    
}