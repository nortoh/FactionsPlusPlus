package factionsplusplus.models;

import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.constants.GroupRole;
import factionsplusplus.data.beans.FactionBean;
import factionsplusplus.data.repositories.FactionRepository;
import factionsplusplus.models.interfaces.Feudal;
import factionsplusplus.services.DataService;
import factionsplusplus.services.MessageService;

import org.bukkit.Location;

import java.util.*;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class Faction extends Nation implements Feudal {
    private Map<String, ConfigurationFlag> flags;
    private String prefix = "none";
    private Location factionHome = null;
    private int bonusPower = 0;
    @ColumnName("should_autoclaim")
    private boolean autoclaim = false;
    private List<UUID> attemptedVassalizations = Collections.synchronizedList(new ArrayList<>());

    private final MessageService messageService;
    private final FactionRepository factionRepository;
    private final DataService dataService;

    // Constructor
    @AssistedInject
    public Faction(MessageService messageService, DataService dataService) {
        this.messageService = messageService;
        this.dataService = dataService;
        this.factionRepository = this.dataService.getFactionRepository();
    }

    @AssistedInject
    public Faction(
        @Assisted FactionBean bean,
        MessageService messageService,
        FactionRepository factionRepository,
        DataService dataService
    ) {
        this.uuid = bean.getId();
        this.name = bean.getName();
        this.description = bean.getDescription();
        this.prefix = bean.getPrefix();
        this.autoclaim = bean.isShouldAutoclaim();
        this.bonusPower = bean.getBonusPower();
        this.flags = bean.getFlags();
        this.members = bean.getMembers();
        this.relations = bean.getRelations();
        this.messageService = messageService;
        this.factionRepository = factionRepository;
        this.dataService = dataService;
    }

    @AssistedInject
    public Faction(@Assisted String factionName, @Assisted Map<String, ConfigurationFlag> flags, MessageService messageService, DataService dataService) {
        this.name = factionName;
        this.flags = flags;
        this.prefix = factionName;
        this.messageService = messageService;
        this.dataService = dataService;
        this.factionRepository = this.dataService.getFactionRepository();
    }

    @AssistedInject
    public Faction(@Assisted String factionName, @Assisted UUID owner, @Assisted Map<String, ConfigurationFlag> flags, MessageService messageService, DataService dataService) {
        this.name = factionName;
        this.setOwner(owner);
        this.flags = flags;
        this.prefix = factionName;
        this.messageService = messageService;
        this.dataService = dataService;
        this.factionRepository = this.dataService.getFactionRepository();
    }

    /**
     * Set the faction flags. Should only be used for internal use.
     */
    public void setFlags(Map<String, ConfigurationFlag> flags) {
        this.flags = flags;
    }

    /*
     * Retrieves the factions UUID.
     *
     * @Deprecated Use getUUID()
     * @returns the factions UUID
     */
    public UUID getID() {
        return this.uuid;
    }

    // Flags
    public Map<String, ConfigurationFlag> getFlags() {
        return this.flags;
    }

    public ConfigurationFlag getFlag(String flagName) {
        return this.flags.get(flagName);
    }

    public String setFlag(String flagName, String flagValue) {
        if (! this.flags.containsKey(flagName)) return null;
        ConfigurationFlag flag = this.flags.get(flagName);
        String result = flag.set(flagValue);
        if (result == null) return null;
        this.factionRepository.persistFlag(this, flag);
        return result;
    }

    public Set<String> getFlagNames() {
        return this.flags.keySet();
    }

    // Bonus Power
    public int getBonusPower() {
        return this.bonusPower;
    }

    public void setBonusPower(int newAmount) {
        this.bonusPower = newAmount;
        this.persist();
    }

    // Overridden methods for persistence
    @Override
    public void setName(String name) {
        this.name = name;
        // Unset prefix if it's the current name
        if (this.prefix != null && this.prefix.equalsIgnoreCase(name)) this.prefix = name;
        this.persist();
    }
    @Override
    public void setDescription(String description) {
        this.description = description;
        this.persist();
    }

    public void upsertMember(UUID uuid, GroupRole role) {
        GroupMember member = this.members.get(uuid);
        if (member != null) member.addRole(role);
        else member = new GroupMember(uuid, role);
        this.members.put(uuid, member);
        this.factionRepository.persistMember(this.getUUID(), member);
    }

    public void clearMember(UUID uuid) {
        this.members.remove(uuid);
        this.factionRepository.deleteMember(this.getUUID(), uuid);
    }

    public void upsertRelation(UUID uuid, FactionRelationType type) {
        // avoid a pointless sql query
        FactionRelationType relation = this.relations.get(uuid);
        if (relation != null && relation == type) return;
        // persist
        this.updateRelation(uuid, type);
        this.factionRepository.persistRelation(this.getUUID(), uuid, type);        
    }

    public void updateRelation(UUID uuid, FactionRelationType type) {
        this.relations.put(uuid, type);
    }

    public void clearRelation(UUID uuid) {
        this.removeRelation(uuid);
        this.factionRepository.deleteRelation(this.getUUID(), uuid);
    }

    public void removeRelation(UUID uuid) {
        this.relations.remove(uuid);
    }

    // Prefix
    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
        this.persist();
    }

    // Faction Home
    public Location getFactionHome() {
        return this.factionHome;
    }

    public void setFactionHome(Location location) {
        this.factionHome = location;
    }

    // Auto Claim
    public void toggleAutoClaim() {
        this.autoclaim = !autoclaim;
        this.persist();
    }

    public boolean getAutoClaimStatus() {
        return this.autoclaim;
    }

    // Lieges
    public boolean isLiege() {
        return this.getVassals().size() > 0;
    }

    public UUID getLiege() {
        return this.relations.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == FactionRelationType.Liege)
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
    }

    public void setLiege(UUID newLiege) {
        this.relations.put(newLiege, FactionRelationType.Liege);
    }

    public boolean hasLiege() {
        return this.getLiege() != null;
    }

    public boolean isLiege(UUID uuid) {
        FactionRelationType relation = this.relations.get(uuid);
        return relation == FactionRelationType.Liege;
    }

    public void unsetIfLiege(UUID uuid) {
        if (this.isLiege(uuid)) this.relations.remove(this.getLiege());
    }

    // Vassals
    public boolean isVassal(UUID uuid) {
        return this.relations.get(uuid) == FactionRelationType.Vassal;
    }

    public void addVassal(UUID uuid) {
        if (! this.isVassal(uuid)) this.relations.put(uuid, FactionRelationType.Vassal);
    }

    public void removeVassal(UUID uuid) {
        if (this.isVassal(uuid)) this.relations.remove(uuid);
    }

    public void clearVassals() {
        this.relations.entrySet()
            .removeIf(entry -> entry.getValue() == FactionRelationType.Vassal);
    }

    public int getNumVassals() {
        return this.getVassals().size();
    }

    public List<UUID> getVassals() {
        return this.relations.entrySet()
            .stream()
            .filter(entry -> entry.getValue() == FactionRelationType.Vassal)
            .map(entry -> entry.getKey())
            .toList();
    }

    public void addAttemptedVassalization(UUID uuid) {
        if (! this.hasBeenOfferedVassalization(uuid)) this.attemptedVassalizations.add(uuid);
    }

    public boolean hasBeenOfferedVassalization(UUID uuid) {
        return this.attemptedVassalizations.contains(uuid);
    }

    public void removeAttemptedVassalization(UUID uuid) {
        if (this.hasBeenOfferedVassalization(uuid)) this.attemptedVassalizations.remove(uuid);
    }

    public String toString() {
        return this.name;
    }

    public void message(GenericMessageBuilder builder) {
        this.messageService.sendFactionLocalizedMessage(this, builder);
    }

    // Tools
    public void persist() {
        this.factionRepository.persist(this);
    }
}