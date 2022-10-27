package factionsplusplus.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import factionsplusplus.data.PlayerDao;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class PlayerRecordRepository {
    private Map<UUID, PlayerRecord> playerStore = new HashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;

    @Inject
    public PlayerRecordRepository(Logger logger, DataProviderService dataProviderService) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
    }

    // Load records
    public void load() {
        try {
            this.playerStore.clear();
            this.playerStore = this.getDAO().get();
        } catch(Exception e) {
            this.logger.error(String.format("Error loading players: %s", e.getMessage()));
        }
    }

    // Save a record after creating
    public void create(PlayerRecord record) {
        this.getDAO().insert(record, record.getPower());
        this.playerStore.put(record.getPlayerUUID(), record);
    }

    // Delete a record
    public void delete(PlayerRecord record) {
        this.getDAO().delete(record.getPlayerUUID());
        this.playerStore.remove(record.getPlayerUUID());
    }
    public void delete(UUID playerUUID) {
        this.delete(this.get(playerUUID));
    }

    // Retrieve a record by uuid
    public PlayerRecord get(UUID playerUUID) {
        return this.playerStore.get(playerUUID);
    }

    // Retrieve all records
    public Map<UUID, PlayerRecord> all() {
        return this.playerStore;
    }

    // Get DAO for this repository
    public PlayerDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(PlayerDao.class);
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
        /*File file = new File(this.dataPath);
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
        }*/
    }
}