package factionsplusplus.models;

import factionsplusplus.jsonadapters.LocationAdapter;
import factionsplusplus.models.interfaces.Feudal;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.*;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;


// TODO: updateDate function to handle renames
public class Faction extends Nation implements Feudal {
    @Expose
    private final List<Gate> gates = new ArrayList<>();
    @Expose
    private final Map<String, FactionFlag> flags;
    @Expose
    private List<UUID> vassals = new ArrayList<>();
    @Expose
    private UUID liege = null;
    @Expose
    private String prefix = "none";
    @Expose
    @JsonAdapter(LocationAdapter.class)
    @SerializedName("location")
    private Location factionHome = null;
    @Expose
    private int bonusPower = 0;

    private boolean autoclaim = false;
    private final List<UUID> attemptedVassalizations = new ArrayList<>();

    // Constructor
    public Faction(String factionName, Map<String, FactionFlag> flags) {
        this.name = factionName;
        this.flags = flags;
    }

    public Faction(String factionName, UUID owner, Map<String, FactionFlag> flags) {
        this.name = factionName;
        this.owner = owner;
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
    public Map<String, FactionFlag> getFlags() {
        return this.flags;
    }

    public FactionFlag getFlag(String flagName) {
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
        return this.vassals.size() > 0;
    }

    public UUID getLiege() {
        return this.liege;
    }

    public void setLiege(UUID newLiege) {
        this.liege = newLiege;
    }

    public boolean hasLiege() {
        return !(this.liege == null);
    }

    public boolean isLiege(UUID uuid) {
        return this.liege.equals(uuid);
    }

    public void unsetIfLiege(UUID uuid) {
        if (this.isLiege(uuid)) this.liege = null;
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
                    g.getTrigger().getWorld().equalsIgnoreCase(block.getWorld().getName())) {
                return true;
            }
        }
        return false;
    }

    public List<Gate> getGatesForTrigger(Block block) {
        ArrayList<Gate> gateList = new ArrayList<>();
        for (Gate g : this.gates) {
            if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
                    g.getTrigger().getWorld().equalsIgnoreCase(block.getWorld().getName())) {
                gateList.add(g);
            }
        }
        return gateList;
    }

    // Vassals
    public boolean isVassal(UUID uuid) {
        return this.vassals.contains(uuid);
    }

    public void addVassal(UUID uuid) {
        if (! this.isVassal(uuid)) this.vassals.add(uuid);
    }

    public void removeVassal(UUID uuid) {
        if (this.isVassal(uuid)) this.vassals.remove(uuid);
    }

    public void clearVassals() {
        this.vassals.clear();
    }

    public int getNumVassals() {
        return this.vassals.size();
    }

    public List<UUID> getVassals() {
        return this.vassals;
    }

    public String getVassalsSeparatedByCommas() {
        StringBuilder toReturn = new StringBuilder();
        for (int i = 0; i < this.vassals.size(); i++) {
            toReturn.append(this.vassals.get(i));
            if (i != this.vassals.size() - 1) {
                toReturn.append(", ");
            }
        }
        return toReturn.toString();
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
}