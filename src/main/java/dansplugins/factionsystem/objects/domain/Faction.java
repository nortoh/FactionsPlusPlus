/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.Inject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dansplugins.factionsystem.models.ClaimedChunk;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.factories.FactionFlagFactory;
import dansplugins.factionsystem.objects.helper.FactionFlags;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.jsonadapters.LocationAdapter;
import dansplugins.factionsystem.jsonadapters.ArrayListGateAdapter;
import dansplugins.factionsystem.models.Nation;
import dansplugins.factionsystem.models.Gate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import preponderous.ponder.misc.abs.Savable;

import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

/**
 * @author Daniel McCoy Stephenson
 */
public class Faction extends Nation {
    private final ConfigService configService;
    private final LocaleService localeService;
    private final Logger logger;
    private final PersistentData persistentData;
    private final MedievalFactions medievalFactions;
    private final PlayerService playerService;

    @Expose
    @JsonAdapter(ArrayListGateAdapter.class)
    private final ArrayList<Gate> gates = new ArrayList<>();
    @Expose
    private final FactionFlags flags;
    private final ArrayList<String> attemptedVassalizations = new ArrayList<>();
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

    @AssistedInject
    public Faction(
        @Assisted String initialName,
        @Assisted UUID creator,
        ConfigService configService,
        LocaleService localeService,
        Logger logger,
        PersistentData persistentData,
        MedievalFactions medievalFactions,
        PlayerService playerService,
        FactionFlagFactory factionFlagFactory
    ) {
        this.configService = configService;
        this.localeService = localeService;
        this.logger = logger;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        setName(initialName);
        setOwner(creator);
        prefix = initialName;
        flags = factionFlagFactory.create();
        flags.initializeFlagValues();
    }

    @AssistedInject
    public Faction(
        @Assisted Map<String, String> data,
        ConfigService configService,
        LocaleService localeService,
        Logger logger,
        PersistentData persistentData,
        MedievalFactions medievalFactions,
        PlayerService playerService,
        FactionFlagFactory factionFlagFactory
    ) {
        this.configService = configService;
        this.localeService = localeService;
        this.logger = logger;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        flags = factionFlagFactory.create();
    }

    @AssistedInject
    public Faction(
        @Assisted String initialName,
        ConfigService configService,
        LocaleService localeService,
        Logger logger,
        PersistentData persistentData,
        MedievalFactions medievalFactions,
        PlayerService playerService,
        FactionFlagFactory factionFlagFactory
    ) {
        this.configService = configService;
        this.localeService = localeService;
        this.logger = logger;
        this.persistentData = persistentData;
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        setName(initialName);
        prefix = initialName;
        flags = factionFlagFactory.create();
        flags.initializeFlagValues();
    }


    // IMPLEMENT THIS LOGIC IN FACTIONSERVICE
    public void setBonusPower(int i) {
        if (!configService.getBoolean("bonusPowerEnabled") || !((boolean) getFlags().getFlag("acceptBonusPower"))) {
            return;
        }
    }

    public String getTopLiege() {
        /*Faction topLiege = persistentData.getFaction(liege);
        String liegeName = liege;
        while (topLiege != null) {
            topLiege = persistentData.getFaction(topLiege.getLiege());
            if (topLiege != null) {
                liegeName = topLiege.getName();
            }
        }
        return liegeName;*/
        return null;
    }

    public int calculateCumulativePowerLevelWithoutVassalContribution() {
        int powerLevel = 0;
        for (UUID playerUUID : members) {
            try {
                powerLevel += this.persistentData.getPlayerRecord(playerUUID).getPower();
            } catch (Exception e) {
                System.out.println(localeService.get("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }
        return powerLevel;
    }

    public int calculateCumulativePowerLevelWithVassalContribution() {
        /*int vassalContribution = 0;
        double percentage = configService.getDouble("vassalContributionPercentageMultiplier");
        for (String factionName : vassals) {
            Faction vassalFaction = persistentData.getFaction(factionName);
            if (vassalFaction != null) {
                vassalContribution += vassalFaction.getCumulativePowerLevel() * percentage;
            }
        }
        return calculateCumulativePowerLevelWithoutVassalContribution() + vassalContribution;*/
        return 0;
    }

    public int getCumulativePowerLevel() {
        int withoutVassalContribution = calculateCumulativePowerLevelWithoutVassalContribution();
        int withVassalContribution = calculateCumulativePowerLevelWithVassalContribution();

        if (vassals.size() == 0 || (withoutVassalContribution < (getMaximumCumulativePowerLevel() / 2))) {
            return withoutVassalContribution + bonusPower;
        } else {
            return withVassalContribution + bonusPower;
        }
    }

    public int getMaximumCumulativePowerLevel() {     // get max power without vassal contribution
        int maxPower = 0;

        for (UUID playerUUID : members) {
            try {
                maxPower += this.persistentData.getPlayerRecord(playerUUID).maxPower();
            } catch (Exception e) {
                System.out.println(localeService.get("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }
        return maxPower;
    }

    public int calculateMaxOfficers() {
        int officersPerXNumber = configService.getInt("officerPerMemberCount");
        int officersFromConfig = members.size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }

    public List<ClaimedChunk> getClaimedChunks() {
        return persistentData.getChunksClaimedByFaction(getName());
    }

    public boolean isWeakened() {
        return calculateCumulativePowerLevelWithoutVassalContribution() < (getMaximumCumulativePowerLevel() / 2);
    }

    /**
     * Method to automatically handle all data changes when a Faction changes their name.
     *
     * @param oldName of the Faction (dependent).
     * @param newName of the Faction (dependent).
     */
    public void updateData(String oldName, String newName) {
        if (isAlly(oldName)) {
            removeAlly(oldName);
            addAlly(newName);
        }
        if (isEnemy(oldName)) {
            removeEnemy(oldName);
            addEnemy(newName);
        }
        if (isLiege(oldName)) {
            setLiege(newName);
        }
        if (isVassal(oldName)) {
            removeVassal(oldName);
            addVassal(newName);
        }
    }

    // IMPLEMENT THIS LOGIC IN FACTION SERVICE
    public boolean addOfficer(UUID newOfficer) {
        if (officers.size() < calculateMaxOfficers() && !officers.contains(newOfficer)) {
            officers.add(newOfficer);
            return true;
        } else {
            return false;
        }
    }

    public String toString() {
        return "Faction{" +
                "members=" + members +
                ", enemyFactions=" + enemyFactions +
                ", officers=" + officers +
                ", allyFactions=" + allyFactions +
                ", laws=" + laws +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", owner=" + owner +
                ", cumulativePowerLevel=" + getCumulativePowerLevel() +
                ", liege=" + liege +
                '}';
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

    // Make the compiler happy
    public String getPrefix() { return this.prefix; }
    public void setPrefix(String s) { this.prefix = s; }
    public boolean hasGateTrigger(Block b) { return false; }
    public boolean isLiege() { return false; }
    public boolean isLiege(String string) { return false; }
    public FactionFlags getFlags() { return this.flags; }
    public String getLiege() { return this.liege; }
    public boolean hasLiege() { return false; }
    public void addVassal(String string) { }
    public void setLiege(String string) { }
    public ArrayList<Gate> getGates() { return this.gates; }
    public void removeGate(Gate gate) { }
    public boolean isVassal(String string) { return false; }
    public void clearVassals() { }
    public Location getFactionHome() { return this.factionHome; }
    public int getNumVassals() { return 0; }
    public int getTotalGates() { return 0; }
    public int getBonusPower() { return 0; }
    public void toggleAutoClaim() { }
    public void removeVassal(String string) { }
    public boolean hasBeenOfferedVassalization(String s) { return false; }
    public void removeAttemptedVassalization(String s) { }
    public ArrayList<String> getVassals() { return this.vassals; }
    public void addGate(Gate g) { }
    public String getVassalsSeparatedByCommas() { return ""; }
    public JsonElement toJsonTree() { return new JsonObject(); }
    public void addAttemptedVassalization(String s) { }
    public void setFactionHome(Location l) { }
    public ArrayList<Gate> getGatesForTrigger(Block b) { return new ArrayList<Gate>(); }
    public boolean getAutoClaimStatus() { return false; }
}