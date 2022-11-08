package factionsplusplus.data.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import factionsplusplus.data.daos.PlayerDao;
import factionsplusplus.data.factories.PlayerFactory;
import factionsplusplus.models.FPPPlayer;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

import java.util.stream.Collectors;

@Singleton
public class PlayerRepository {
    private Map<UUID, FPPPlayer> playerStore = new ConcurrentHashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;
    private final PlayerFactory playerFactory;

    @Inject
    public PlayerRepository(Logger logger, DataProviderService dataProviderService, PlayerFactory playerFactory) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
        this.playerFactory = playerFactory;
    }

    // Load records
    public void load() {
        try {
            this.playerStore.clear();
            this.playerStore = this.getDAO().get().stream().map(this.playerFactory::create).collect(Collectors.toConcurrentMap(player -> (UUID)player.getUUID(), player -> (FPPPlayer)player));
        } catch(Exception e) {
            this.logger.error(String.format("Error loading players: %s", e.getMessage()));
        }
    }

    // Save a record after creating
    public void create(FPPPlayer record) {
        this.getDAO().insert(record);
        this.playerStore.put(record.getUUID(), record);
    }

    // Delete a record
    public void delete(FPPPlayer record) {
        this.getDAO().delete(record.getUUID());
        this.playerStore.remove(record.getUUID());
    }
    public void delete(UUID playerUUID) {
        this.delete(this.get(playerUUID));
    }

    // Retrieve a record by uuid
    public FPPPlayer get(UUID playerUUID) {
        return this.playerStore.get(playerUUID);
    }

    // Retrieve all records
    public Map<UUID, FPPPlayer> all() {
        return this.playerStore;
    }

    // Get DAO for this repository
    public PlayerDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(PlayerDao.class);
    }

    // Determine if a player is stored
    public boolean contains(UUID player) {
        return this.playerStore.containsKey(player);
    }

    /*
     * Retrieves the number of players currently stored
     *
     * @return the number of players currently stored
     */
    public int count() {
        return this.playerStore.size();
    }

    // Write to database
    public void persist(FPPPlayer player) {
        this.getDAO().update(player);
    }

    public void persist() {
        this.getDAO().update(this.playerStore.values());
    }
}