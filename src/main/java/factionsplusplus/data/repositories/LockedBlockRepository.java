package factionsplusplus.data.repositories;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.bukkit.block.Block;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Collection;

import factionsplusplus.data.daos.LockedBlockDao;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class LockedBlockRepository {
    private ConcurrentMap<UUID, LockedBlock> lockedBlockStore = new ConcurrentHashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;

    @Inject
    public LockedBlockRepository(Logger logger, DataProviderService dataProviderService) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
    }

    // Load claimed chunks
    public void load() {
        try {
            this.lockedBlockStore.clear();
            this.lockedBlockStore = this.getDAO().getAll();
        } catch(Exception e) {
            this.logger.log(String.format("Error loading locked blocks: %s", e.getMessage()));
        }
    }

    // Save a locked block
    public void create(LockedBlock block) {
        this.getDAO().create(block);
        this.lockedBlockStore.put(block.getUUID(), block);
    }

    // Delete a locked block
    public void delete(LockedBlock block) {
        this.getDAO().delete(block);
        this.remove(block);
    }
    public void delete(Block b) {
        LockedBlock block = this.get(b.getX(), b.getY(), b.getZ(), b.getWorld().getUID());
        if (block != null) this.delete(block);
    }

    // Remove a locked block from internal storage
    public void remove(LockedBlock block) {
        this.lockedBlockStore.remove(block.getUUID());
    }

    // Retrieve a locked block by location
    public LockedBlock get(int x, int y, int z, UUID world) {
        for (LockedBlock block : this.lockedBlockStore.values()) {
            if (
                block.getX() == x &&
                block.getY() == y &&
                block.getZ() == z &&
                block.getWorld().equals(world)
            ) {
                return block;
            }
        }
        return null;
    }

    // Retrieve all locked blocks
    public Collection<LockedBlock> all() {
        return this.lockedBlockStore.values();
    }

    public void persist(LockedBlock lock) {
        this.getDAO().update(lock);
    }

    public void persistPlayerAccess(LockedBlock lock, UUID player) {
        this.getDAO().insertPlayerAccess(lock.getUUID(), player);
    }

    public void deletePlayerAccess(LockedBlock lock, UUID player) {
        this.getDAO().removePlayerAccess(lock.getUUID(), player);
    }

    // Get factions locked blocks
    public List<LockedBlock> getAllForFaction(UUID factionUUID) {
        return this.lockedBlockStore.values().stream()
            .filter(lock -> lock.getFaction().equals(factionUUID))
            .toList();
    }

    // Get the DAO for this repository
    public LockedBlockDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(LockedBlockDao.class);
    }

    // Write to file
    public void persist() {

    }
}