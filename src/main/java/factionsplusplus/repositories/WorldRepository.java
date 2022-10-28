package factionsplusplus.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;

import factionsplusplus.data.WorldDao;
import factionsplusplus.factories.WorldFactory;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.models.World;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class WorldRepository {
    private Map<UUID, World> worldStore = new HashMap<>();
    private final Map<String, ConfigurationFlag> defaultFlags = new HashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;
    private final WorldFactory worldFactory;

    @Inject
    public WorldRepository(Logger logger, DataProviderService dataProviderService, WorldFactory worldFactory) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
        this.worldFactory = worldFactory;
    }

    // Load worlds
    public void load() {
        try {
            this.worldStore.clear();
            // TODO: check if world no longer exists
            this.dataProviderService.getPersistentData().useExtension(WorldDao.class, dao -> {
                Bukkit.getWorlds().stream().forEach(world -> {
                    dao.insert(world.getUID());
                });
                this.getDAO().getWorlds().stream().forEach(world -> {
                    this.worldStore.put(world.getId(), this.worldFactory.create(world));
                });
            });
        } catch(Exception e) {
            this.logger.error(String.format("Error loading worlds: %s", e.getMessage()));
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

    public Map<UUID, World> all() {
        return this.worldStore;
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

    public WorldDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(WorldDao.class);
    }

    public void persistFlag(World world, ConfigurationFlag flag) {
        // Check if new value is default, if so no reason to keep it
        if (this.getDefaultFlags().get(flag.getName()).getDefaultValue() == flag.getValue()) {
            // delete flag if it exists from world_flags
            this.getDAO().deleteFlag(world.getUUID(), flag.getName());
            return;
        }
        this.getDAO().upsertFlag(world.getUUID(), flag.getName(), flag.getValue());
    }

    // Write to file
    public void persist() {
        /*File file = new File(this.dataPath);
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
        }*/
    }
}