package factionsplusplus.models;

import factionsplusplus.beans.FactionBean;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.jsonadapters.LocationAdapter;
import factionsplusplus.models.interfaces.Feudal;
import factionsplusplus.services.MessageService;

import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.*;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class Faction extends Nation implements Feudal {
    @Expose
    private final List<Gate> gates = new ArrayList<>();
    @Expose
    private Map<String, ConfigurationFlag> flags;
    @Expose
    private String prefix = "none";
    @Expose
    @JsonAdapter(LocationAdapter.class)
    @SerializedName("location")
    private Location factionHome = null;
    @Expose
    @ColumnName("bonus_power")
    private int bonusPower = 0;

    @ColumnName("should_autoclaim")
    private boolean autoclaim = false;
    private List<UUID> attemptedVassalizations = new ArrayList<>();

    private final MessageService messageService;

    // Constructor
    @AssistedInject
    public Faction(MessageService messageService) {
        this.messageService = messageService;
    }

    @AssistedInject
    public Faction(
        @Assisted FactionBean bean,
        MessageService messageService
    ) {
        this.uuid = bean.getId();
        this.name = bean.getName();
        this.description = bean.getDescription();
        this.prefix = bean.getDescription();
        this.autoclaim = bean.isShouldAutoclaim();
        this.bonusPower = bean.getBonusPower();
        this.flags = bean.getFlags();
        this.members = bean.getMembers();
        this.relations = bean.getRelations();
        this.messageService = messageService;
    }

    @AssistedInject
    public Faction(@Assisted String factionName, @Assisted Map<String, ConfigurationFlag> flags, MessageService messageService) {
        this.name = factionName;
        this.flags = flags;
        this.messageService = messageService;
    }

    @AssistedInject
    public Faction(@Assisted String factionName, @Assisted UUID owner, @Assisted Map<String, ConfigurationFlag> flags, MessageService messageService) {
        this.name = factionName;
        this.setOwner(owner);
        this.flags = flags;
        this.messageService = messageService;
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

    public Set<String> getFlagNames() {
        return this.flags.keySet();
    }

    // Bonus Power
    public int getBonusPower() {
        return this.bonusPower;
    }

    public void setBonusPower(int newAmount) {
        this.bonusPower = newAmount;
    }

    // Prefix
    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String newPrefix) {
        this.prefix = newPrefix;
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

    // Gates
    public int getTotalGates() {
        return this.gates.size();
    }

    public void addGate(Gate gate) {
        this.gates.add(gate);
    }

    public void removeGate(Gate gate) {
        this.gates.remove(gate);
    }

    public List<Gate> getGates() {
        return this.gates;
    }

    public boolean hasGateTrigger(Block block) {
        for (Gate g : this.gates) {
            if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
                    g.getTrigger().getWorld().equals(block.getWorld().getUID())) {
                return true;
            }
        }
        return false;
    }

    public List<Gate> getGatesForTrigger(Block block) {
        ArrayList<Gate> gateList = new ArrayList<>();
        for (Gate g : this.gates) {
            if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
                    g.getTrigger().getWorld().equals(block.getWorld().getUID())) {
                gateList.add(g);
            }
        }
        return gateList;
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
}