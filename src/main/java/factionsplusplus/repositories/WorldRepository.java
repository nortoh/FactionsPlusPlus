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
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Type;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.World;
import factionsplusplus.utils.Logger;

@Singleton
public class WorldRepository {
    private Map<UUID, World> worldStore = new HashMap<>();
    private final String dataPath;
    private final static String FILE_NAME = "worlds.json";
    private final static Type JSON_TYPE = new TypeToken<Map<UUID, World>>() { }.getType();
    private final Map<String, ConfigurationFlag> defaultFlags = new HashMap<>();
    private final Logger logger;

    @Inject
    public WorldRepository(@Named("dataFolder") String dataPath, Logger logger) {
        this.dataPath = String.format("%s%s%s", dataPath, File.separator, FILE_NAME);
        this.logger = logger;
    }

    // Load worlds
    public void load() {
        this.worldStore.clear();
        try {
            Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .serializeNulls()
                .create();
            JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(this.dataPath), StandardCharsets.UTF_8));
            this.worldStore = gson.fromJson(reader, WorldRepository.JSON_TYPE);
            // TODO: check if world no longer exists
        } catch (FileNotFoundException ignored) {
            this.logger.error(String.format("File %s not found", this.dataPath), ignored);
        }
    }

    // Save a world after creating
    public void create(World world) {
        this.worldStore.put(world.getUUID(), world);
    }

    /*
     * Retrieves a World by the underlying Bukkit World obect
     *
     * @param world The Bukkit World
     * @return an instance of World if found, otherwise null
     */
    public World get(org.bukkit.World world) {
        return this.worldStore.get(world.getUID());
    }

    /*
     * Retrieves a World by name
     *
     * @param name The name of the world
     * @return an instance of World if found, otherwise null
     */
    public World get(String name) {
        return this.get(Bukkit.getWorld(name));
    }

    /*
     * Retrieves a World by UUID
     *
     * @param uuid The UUID of the world
     * @return an instance of World if found, otherwise null
     */
    public World get(UUID uuid) {
        return this.get(Bukkit.getWorld(uuid));
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag, boolean addToMissing) {
        this.defaultFlags.put(flagName, flag);
        if (addToMissing) this.addAnyMissingFlags();
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag) {
        this.addDefaultConfigurationFlag(flagName, flag, true);
    }

    public void addAnyMissingFlags(World world) {
        List<String> missingFlags = this.defaultFlags.keySet().stream().filter(key -> world.getFlag(key) == null).collect(Collectors.toList());
        if (! missingFlags.isEmpty()) {
            missingFlags.stream().forEach(flag -> {
                world.getFlags().put(flag, this.defaultFlags.get(flag));
            });
        }
    }

    public void addAnyMissingFlags() {
        this.worldStore.values()
            .stream()
            .forEach(world -> this.addAnyMissingFlags(world));
    }

    public Map<String, ConfigurationFlag> getDefaultFlags() {
        return this.defaultFlags;
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
            outputStreamWriter.write(gson.toJson(this.worldStore, WorldRepository.JSON_TYPE));
            outputStreamWriter.close();
        } catch (IOException e) {
            this.logger.error(String.format("Failed to write to %s", this.dataPath), e);
        }
    }
}