package factionsplusplus.data.repositories;

import com.google.inject.Singleton;

import com.google.inject.Inject;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

import org.bukkit.OfflinePlayer;

import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionBase;
import factionsplusplus.models.GroupMember;
import factionsplusplus.services.DataProviderService;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.data.daos.FactionDao;
import factionsplusplus.data.factories.FactionFactory;
import factionsplusplus.models.ConfigurationFlag;
import factionsplusplus.utils.Logger;

@Singleton
public class FactionRepository {
    private Map<UUID, Faction> factionStore = new ConcurrentHashMap<>();
    private final Map<String, ConfigurationFlag> defaultFlags = new ConcurrentHashMap<>();
    private final Logger logger;
    private final DataProviderService dataProviderService;
    private final FactionFactory factionFactory;

    @Inject
    public FactionRepository(Logger logger, DataProviderService dataProviderService, FactionFactory factionFactory) {
        this.logger = logger;
        this.dataProviderService = dataProviderService;
        this.factionFactory = factionFactory;
    }

    // Load factions
    public void load() {
        try {
            this.factionStore.clear();
            this.getDAO().getFactions().stream()
                .forEach(faction -> {
                    this.factionStore.put(faction.getId(), this.factionFactory.create(faction));
                });
        } catch (Exception e) {
            this.logger.error(String.format("Error loading factions: %s", e.getMessage()));
        }
    }

    // Save a faction after creating
    public void create(Faction faction) {
        faction = this.getDAO().createNewFaction(faction);
        this.factionStore.put(faction.getUUID(), faction);
    }

    // Delete a faction
    public void delete(UUID factionUUID) {
        this.getDAO().delete(factionUUID);
        this.factionStore.remove(factionUUID);
    }
    public void delete(Faction faction) {
        this.delete(faction.getUUID());
    }
    public void delete(String factionName) {
        this.delete(this.get(factionName));
    }

    // Persist faction
    public void persist(Faction faction) {
        this.getDAO().update(faction);
    }

    public void persistMember(UUID faction, GroupMember member) {
        this.getDAO().upsert(faction, member.getUUID(), member.getRole());
    }

    public void deleteMember(UUID faction, UUID member) {
        this.getDAO().deleteMember(faction, member);
    }

    public void persistRelation(UUID source, UUID target, FactionRelationType type) {
        if (source.equals(target)) return; // no self-relationships
        try {
            this.getDAO().upsertRelation(source, target, type);
            // If the relationship type is vassal, the opposite end should set liege
            if (type != null && type.equals(FactionRelationType.Vassal)) type = FactionRelationType.Liege;
            this.getDAO().upsertRelation(target, source, type);
            this.get(target).updateRelation(source, type);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteRelation(UUID source, UUID target) {
        this.getDAO().deleteRelation(source, target);
        this.getDAO().deleteRelation(target, source);
        this.get(target).removeRelation(source);
    }

    public void persistLaw(UUID faction, UUID law, String text) {
        this.getDAO().upsertLaw(faction, law, text);
    }

    public void deleteLaw(UUID law) {
        this.getDAO().deleteLaw(law);
    }

    public void persistBase(FactionBase base) {
        this.getDAO().upsertBase(base);
    } 

    public void deleteBase(FactionBase base) {
        this.getDAO().deleteBase(base);
    }

    public void persistFlag(Faction faction, ConfigurationFlag flag) {
        // Check if new value is default, if so no reason to keep it
        if (this.getDefaultFlags().get(flag.getName()).getDefaultValue() == flag.getValue()) {
            // delete flag if it exists from faction_flags
            this.getDAO().deleteFlag(faction.getUUID(), flag.getName());
            return;
        }
        this.getDAO().upsertFlag(faction.getUUID(), flag.getName(), flag.getValue());
    }

    // Get the DAO for this repository
    public FactionDao getDAO() {
        return this.dataProviderService.getPersistentData().onDemand(FactionDao.class);
    }

    /*
     * Retrieve a faction by prefix
     *
     * @param prefix the prefix of the faction to search for
     * @return Faction instance if found, null otherwise
     */
    public Faction getByPrefix(String prefix) {
        Optional<Faction> faction = this.factionStore
            .values()
            .stream()
            .filter(entry -> entry.getPrefix() != null && entry.getPrefix().equalsIgnoreCase(prefix))
            .findFirst();
        return faction.orElse(null);
    }

    /*
     * Retrieve a faction by name
     *
     * @param factionName the name of the faction to search for
     * @return Faction instance if found, null otherwise
     */
    public Faction get(String factionName) {
        Optional<Faction> faction = this.factionStore
            .values()
            .stream()
            .filter(entry -> entry.getName().equalsIgnoreCase(factionName))
            .findFirst();
        return faction.orElse(null);
    }

    /*
     * Retrieve a faction by a member player
     *
     * @param playerUUID the UUID of the player to determine faction for
     * @return Faction instance if found, null otherwise
     */
    public Faction getForPlayer(UUID playerUUID) {
        Optional<Faction> faction = this.factionStore
            .values()
            .stream()
            .filter(entry -> entry.isMember(playerUUID))
            .findFirst();
        return faction.orElse(null);
    }

    /*
     * Retrieve a faction by a member player
     *
     * @param player the Player instance to determine faction for
     * @return Faction instance if found, null otherwise
     */
    public Faction getForPlayer(OfflinePlayer player) {
        return this.getForPlayer(player.getUniqueId());
    }

    /*
     * Retrieve a faction by its UUID
     *
     * @param uuid the UUID of the faction to search for
     * @return Faction instance if found, null otherwise
     */
    public Faction get(UUID uuid) {
        return this.factionStore.get(uuid);
    }


    // TODO: refactor this, it's a bit bloated
    /*
     * Retrieves factions in a factions vassalage tree
     *
     * @param the faction you wish to get the vassalage tree for
     * @return a List of Faction instances that are in the vassalage tree
     */
    public List<Faction> getInVassalageTree(Faction initialFaction) {
        List<Faction> foundFactions = new ArrayList<>();

        foundFactions.add(initialFaction);

        boolean newFactionsFound = true;

        int numFactionsFound;

        while (newFactionsFound) {
            List<Faction> toAdd = new ArrayList<>();
            for (Faction current : foundFactions) {

                // record number of factions
                numFactionsFound = foundFactions.size();

                UUID liegeUUID = current.getLiege();
                Faction liege = this.get(liegeUUID);
                if (liegeUUID != null && liege != null) {
                    if (! toAdd.contains(liege) && ! foundFactions.contains(liege)) {
                        toAdd.add(liege);
                        numFactionsFound++;
                    }

                    for (UUID vassalID : liege.getVassals()) {
                        Faction vassal = this.get(vassalID);
                        if (! toAdd.contains(vassal) && ! foundFactions.contains(vassal)) {
                            toAdd.add(vassal);
                            numFactionsFound++;
                        }
                    }
                }

                for (UUID vassalID : current.getVassals()) {
                    Faction vassal = this.get(vassalID);
                    if (! toAdd.contains(vassal) && ! foundFactions.contains(vassal)) {
                        toAdd.add(vassal);
                        numFactionsFound++;
                    }
                }
                // if number of factions not different then break loop
                if (numFactionsFound == foundFactions.size()) {
                    newFactionsFound = false;
                }
            }
            foundFactions.addAll(toAdd);
            toAdd.clear();
        }
        return foundFactions;
    }

    // Retrieve all factions
    public Map<UUID, Faction> all() {
        return this.factionStore;
    }

    /*
     * Retrieves the number of factions currently stored
     *
     * @return the number of factions currently stored
     */
    public int count() {
        return this.factionStore.size();
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag, boolean addToMissingFactions) {
        this.defaultFlags.put(flagName, flag);
        if (addToMissingFactions) this.addAnyMissingFlagsToFactions();
    }

    public void addDefaultConfigurationFlag(String flagName, ConfigurationFlag flag) {
        this.addDefaultConfigurationFlag(flagName, flag, true);
    }

    public void addAnyMissingFlagsToFaction(Faction faction) {
        List<String> missingFlags = this.defaultFlags.keySet().stream().filter(key -> faction.getFlag(key) == null).collect(Collectors.toList());
        if (! missingFlags.isEmpty()) {
            missingFlags.stream().forEach(flag -> {
                faction.getFlags().put(flag, this.defaultFlags.get(flag));
            });
        }
    }

    public void addAnyMissingFlagsToFactions() {
        this.factionStore.values()
            .stream()
            .forEach(faction -> this.addAnyMissingFlagsToFaction(faction));
    }

    public void addFlagToMissingFactions(String flagName) {
        // get the flag from defaultFlags
        ConfigurationFlag flag = this.defaultFlags.get(flagName);
        // TODO: error if null
        for (Faction faction : this.factionStore.values()) {
            if (! faction.getFlags().containsKey(flagName)) faction.getFlags().put(flagName, flag);
        }
    }

    public void removeFlagFromFactions(String flagName) {
        // remove from default flags first
        this.defaultFlags.remove(flagName);
        // iterate through factions, removing the flag
        for (Faction faction : this.factionStore.values()) faction.getFlags().remove(flagName);
    }

    public Map<String, ConfigurationFlag> getDefaultFlags() {
        return this.defaultFlags;
    }

    // Write to file
    public void persist() {

    }

    public enum PersistType {
        Faction,
        Relations,
        Laws,
        Members,
        All
    }
}