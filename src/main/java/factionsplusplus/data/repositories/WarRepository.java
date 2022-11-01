package factionsplusplus.data.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;

import factionsplusplus.data.daos.WarDao;
import factionsplusplus.data.factories.WarFactory;
import factionsplusplus.models.Faction;
import factionsplusplus.models.War;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.utils.Logger;

@Singleton
public class WarRepository {
    private List<War> warStore = Collections.synchronizedList(new ArrayList<>());
    private final Logger logger;
    private final DataProviderService dataProviderService;
    private final WarFactory warFactory;

    @Inject
    public WarRepository(Logger logger, DataProviderService dataProviderService, WarFactory warFactory) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
        this.warFactory = warFactory;
    }

    // Load wars
    public void load() {
        try {
            this.warStore.clear();
            this.warStore = this.getDAO().get().stream().map(this.warFactory::create).toList();
        } catch(Exception e) {
            this.logger.error(String.format("Error loading wars: %s", e.getMessage()));
        }
    }

    // Save a war
    public void create(War war) {
        this.getDAO().upsert(war);
        this.warStore.add(war);
    }

    public void create(Faction attacker, Faction defender, String reason) {
        this.create(this.warFactory.create(attacker, defender, reason));
    }

    // Delete a war
    public void delete(War war) {
        this.getDAO().delete(war);
        this.warStore.remove(war);
    }

    // Persist a war (update if already there)
    public void persist(War war) {
        this.getDAO().upsert(war);
    }

    // Retrieve a list of wars a faction is involved in
    public List<War> getAllForFaction(UUID factionUUID) {
        ArrayList<War> results = new ArrayList<>();
        for (War war : this.warStore) {
            if (war.getAttacker().equals(factionUUID) || war.getDefender().equals(factionUUID)) results.add(war);
        }
        return results;
    }

    // Retrieve an active war between two factions
    public War getActiveWarsBetween(UUID factionOneUUID, UUID factionTwoUUID) {
        for (War war : this.warStore) {
            if (
                (factionOneUUID.equals(war.getAttacker()) || factionOneUUID.equals(war.getDefender())) &&
                (factionTwoUUID.equals(war.getAttacker()) || factionTwoUUID.equals(war.getDefender())) &&
                war.isActive()
            ) {
                return war;
            }
        }
        return null;
    }

    // Retrieve all wars
    public List<War> all() {
        return this.warStore;
    }

    // Get DAO for this repository
    public WarDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(WarDao.class);
    }

    // Write to file
    public void persist() {

    }
}