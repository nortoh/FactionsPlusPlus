package dansplugins.factionsystem.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonElement;
import dansplugins.factionsystem.jsonadapters.LocationAdapter;
import dansplugins.factionsystem.jsonadapters.ArrayListGateAdapter;
import dansplugins.factionsystem.models.interfaces.Feudal;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;

import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

public class Faction extends Nation implements Feudal {
    @Expose
    @JsonAdapter(ArrayListGateAdapter.class)
    private final ArrayList<Gate> gates = new ArrayList<>();
    @Expose
    private final FactionFlags flags;
    @Expose
    private ArrayList<String> vassals = new ArrayList<>();
    @Expose
    private String liege = "none";
    @Expose
    private String prefix = "none";
    @Expose
    @JsonAdapter(LocationAdapter.class)
    @SerializedName("location")
    private Location factionHome = null;
    @Expose
    private int bonusPower = 0;

    private boolean autoclaim = false;
    private final ArrayList<String> attemptedVassalizations = new ArrayList<>();

    // Constructor
    public Faction(String factionName, UUID owner) {
        this.name = factionName;
        this.owner = owner;
        this.flags = new FactionFlags();
        this.flags.initializeFlagValues();
    }

    public Faction(String factionName) {
        this.name = factionName;
        this.flags = new FactionFlags();
        this.flags.initializeFlagValues();
    }

    // Flags
    public FactionFlags getFlags() {
        return this.flags;
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

    public String getLiege() {
        return this.liege;
    }

    public void setLiege(String newLiege) {
        this.liege = newLiege;
    }

    public boolean hasLiege() {
        return !this.liege.equalsIgnoreCase("none");
    }

    public boolean isLiege(String faction) {
        return this.liege.equalsIgnoreCase(faction);
    }

    // TODO: implement, probably in a service
    public String getTopLiege() {
        return null;
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

    public ArrayList<Gate> getGates() {
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

    public ArrayList<Gate> getGatesForTrigger(Block block) {
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
    public boolean isVassal(String faction) {
        return (this.containsIgnoreCase(this.vassals, faction));
    }

    public void addVassal(String name) {
        if (!this.containsIgnoreCase(vassals, name)) {
            this.vassals.add(name);
        }
    }

    public void removeVassal(String name) {
        this.removeIfContainsIgnoreCase(this.vassals, name);
    }

    public void clearVassals() {
        this.vassals.clear();
    }

    public int getNumVassals() {
        return this.vassals.size();
    }

    public ArrayList<String> getVassals() {
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

    public void addAttemptedVassalization(String factionName) {
        if (!containsIgnoreCase(this.attemptedVassalizations, factionName)) {
            this.attemptedVassalizations.add(factionName);
        }
    }

    public boolean hasBeenOfferedVassalization(String factionName) {
        return containsIgnoreCase(this.attemptedVassalizations, factionName);
    }

    public void removeAttemptedVassalization(String factionName) {
        removeIfContainsIgnoreCase(this.attemptedVassalizations, factionName);
    }

    // Tools
    public JsonElement toJsonTree() {
        return new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .serializeNulls()
            .create()
            .toJsonTree(this);
    }

    private boolean containsIgnoreCase(ArrayList<String> list, String str) {
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private void removeIfContainsIgnoreCase(ArrayList<String> list, String str) {
        String toRemove = "";
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                toRemove = string;
                break;
            }
        }
        list.remove(toRemove);
    }

    // Power
    // TODO: implement, probably in a service
    public int getCumulativePowerLevel() {
        return 9999;
    }

    public int getMaximumCumulativePowerLevel() {
        return 9999;
    }

    public boolean isWeakened() {
        return false;
    }

    public int calculateCumulativePowerLevelWithVassalContribution() {
        return 9999;
    }

    public int calculateCumulativePowerLevelWithoutVassalContribution() {
        return 9999;
    }

    // Chunks
    // TODO: implement, probably in a service
    public List<ClaimedChunk> getClaimedChunks() {
        return new ArrayList<>();
    }

    // Officers
    // TODO: implement, probably in a service
    public int calculateMaxOfficers() {
        return 5;
    }
}