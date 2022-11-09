package factionsplusplus.data.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;

import factionsplusplus.data.daos.ClaimedChunkDao;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class ClaimedChunkRepository {
    private List<ClaimedChunk> claimedChunksStore = Collections.synchronizedList(new ArrayList<>());
    private final Logger logger;
    private final DataProviderService dataProviderService;

    @Inject
    public ClaimedChunkRepository(Logger logger, DataProviderService dataProviderService) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
    }

    // Load claimed chunks
    public void load() {
        try {
            this.claimedChunksStore.clear();
            this.claimedChunksStore = this.getDAO().get();
        } catch(Exception e) {
            this.logger.error(String.format("Error importing claimed chunks: %s", e.getMessage()));
        }
    }

    // Save a claimed chunk
    public void create(ClaimedChunk chunk) {
        this.getDAO().insert(chunk);
        this.claimedChunksStore.add(chunk);
    }

    // Delete a claimed chunk
    public void delete(ClaimedChunk chunk) {
        this.getDAO().delete(chunk);
        this.remove(chunk);
    }

    // Remove a claimed chunk from internal storage
    public void remove(ClaimedChunk chunk) {
        this.claimedChunksStore.remove(chunk);
    }

    // Get the DAO for this repository
    public ClaimedChunkDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(ClaimedChunkDao.class);
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
            .filter(chunk -> chunk.getHolder().equals(faction.getUUID()))
            .collect(Collectors.toList());
    }

    // Retrieve all claimed chunks
    public List<ClaimedChunk> all() {
        return this.claimedChunksStore;
    }
}