package factionsplusplus.repositories;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import org.bukkit.block.Block;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import factionsplusplus.data.LockedBlockDao;
import factionsplusplus.models.LockedBlock;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class LockedBlockRepository {
    private List<LockedBlock> lockedBlockStore = Collections.synchronizedList(new ArrayList<>());
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
        this.lockedBlockStore.add(block);
    }

    // Delete a locked block
    public void delete(LockedBlock block) {
        this.getDAO().delete(block);
        this.lockedBlockStore.remove(block);
    }
    public void delete(Block b) {
        LockedBlock block = this.get(b.getX(), b.getY(), b.getZ(), b.getWorld().getUID());
        if (block != null) this.delete(block);
    }

    // Retrieve a locked block by location
    public LockedBlock get(int x, int y, int z, UUID world) {
        for (LockedBlock block : this.lockedBlockStore) {
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
    public List<LockedBlock> all() {
        return this.lockedBlockStore;
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

    // Get the DAO for this repository
    public LockedBlockDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(LockedBlockDao.class);
    }

    // Write to file
    public void persist() {

    }
}