/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.ConfigOption;
import factionsplusplus.utils.StringUtils;
import factionsplusplus.builders.ConfigOptionBuilder;
import factionsplusplus.constants.SetConfigResult;
import factionsplusplus.data.repositories.ConfigOptionRepository;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.AbstractMap;
import javax.inject.Provider;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class ConfigService {
    private final Provider<FactionsPlusPlus> factionsPlusPlus;
    private final ConfigOptionRepository configOptionRepository;

    private boolean altered = false;

    @Inject
    public ConfigService(Provider<FactionsPlusPlus> factionsPlusPlus, ConfigOptionRepository configOptionRepository) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.configOptionRepository = configOptionRepository;
        this.registerCoreOptions();
    }

    public void registerCoreOptions() {
        ConfigOptionBuilder[] configOptions = new ConfigOptionBuilder[]{
            new ConfigOptionBuilder() // version
                .withName("system.version")
                .withDescription("Current version of this plugin")
                .setDefaultValue(this.factionsPlusPlus.get().getVersion())
                .notUserSettable()
                .isHidden(),
            new ConfigOptionBuilder() // language
                .withName("system.language")
                .withDescription("The default locale to use when sending messages")
                .setDefaultValue("en_US"),
            new ConfigOptionBuilder() // debugMode
                .withName("system.debugMode")
                .withDescription("If the plugin should be more verbose when logging")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // database.host
                .withName("system.database.host")
                .withDescription("The host of the database to connect to (if not flatfile)")
                .setDefaultValue("127.0.0.1")
                .notUserSettable()
                .isHidden(),
            new ConfigOptionBuilder() // database.port
                .withName("system.database.port")
                .withDescription("The port of the database server to connect to (if not flatfile)")
                .notUserSettable()
                .isHidden(),
            new ConfigOptionBuilder() // database.username
                .withName("system.database.username")
                .withDescription("The username to connect to the database server with (if not flatfile)")
                .notUserSettable()
                .setDefaultValue("")
                .isHidden(),
            new ConfigOptionBuilder() // database.password
                .withName("system.database.password")
                .withDescription("The password to connect to the database server with (if not flatfile)")
                .notUserSettable()
                .setDefaultValue("")
                .isHidden(),  
            new ConfigOptionBuilder() // database.name
                .withName("system.database.name")
                .withDescription("The name of the database to store data in (filename for flatfile, database name  for remote)")
                .setDefaultValue("fpp")
                .notUserSettable()
                .isHidden(),
            new ConfigOptionBuilder() // database.flatfile
                .withName("system.database.flatfile")
                .withDescription("If Factions Plus Plus should use a flatfile database")
                .isBoolean()
                .setDefaultValue(true)
                .notUserSettable()
                .isHidden(),
            new ConfigOptionBuilder() // zeroPowerFactionsGetDisbanded
                .withName("faction.disbandFactionsWithZeroPower")
                .withDescription("If factions with zero power get disbanded automatically")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // vassalContributionPercentageMultiplier
                .withName("faction.vassalContributionPercentage")
                .withDescription("The percentage of a vassals power to contribute to it's lieges power")
                .setDefaultValue(0.75)
                .isDouble(),
            new ConfigOptionBuilder() // allowNeutrality
                .withName("faction.allowNeutrality")
                .withDescription("If factions are permitted to be neutral")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // factionsCanSetPrefixColors
                .withName("faction.canSetPrefixColor")
                .withDescription("If factions can set their prefix color")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // bonusPowerEnabled
                .withName("faction.allowBonusPower")
                .withDescription("If bonus power is enabled for factions")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // initialMaxPowerLevel
                .withName("player.power.maximum")
                .withDescription("The initial maximum power a player has")
                .setDefaultValue(20.0)
                .isDouble(),
            new ConfigOptionBuilder() // initialPowerLevel
                .withName("player.power.initial")
                .withDescription("The initial amount of power a player starts with")
                .setDefaultValue(5.0)
                .isDouble(),
            new ConfigOptionBuilder() // powerIncreaseAmount
                .withName("player.power.onlineIncrease.amount")
                .withDescription("The amount of power to automatically give to online players")
                .setDefaultValue(2.0)
                .isDouble(),
            new ConfigOptionBuilder() // minutesBeforeInitialPowerIncrease
                .withName("player.power.onlineIncrease.delay")
                .withDescription("The number of minutes before a new player is given a power increase")
                .setDefaultValue(30)
                .isInteger(),
            new ConfigOptionBuilder() // minutesBetweenPowerIncreases
                .withName("player.power.onlineIncrease.frequency")
                .withDescription("The number of minutes between power increases")
                .setDefaultValue(60)
                .isInteger(),
            new ConfigOptionBuilder() // factionOwnerMultiplier
                .withName("player.power.ownerMultiplier")
                .withDescription("The power multiplier for owning a faction")
                .setDefaultValue(2.0)
                .isDouble(),
            new ConfigOptionBuilder() // factionOfficerMultiplier
                .withName("player.power.officerMultiplier")
                .withDescription("The power multiplier for being an officer in a faction")
                .setDefaultValue(1.5)
                .isDouble(),
            new ConfigOptionBuilder() // powerDecreases
                .withName("player.power.decreaseForInactivity.enabled")
                .withDescription("If power slowly decays for inactive players.")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // powerDecreaseAmount
                .withName("player.power.decreaseForInactivity.amount")
                .withDescription("The amount of power to take from inactive players.")
                .setDefaultValue(1.0)
                .isDouble(),
            new ConfigOptionBuilder() // minutesBetweenPowerDecreases
                .withName("player.power.decreaseForInactivity.frequency")
                .withDescription("The number of minutes between automatic power decreases")
                .setDefaultValue(1440)
                .isInteger(),
            new ConfigOptionBuilder() // minutesBeforePowerDecrease
                .withName("player.power.decreaseForInactivity.minimumMinutes")
                .withDescription("The number of minutes before automatic power decreases start")
                .setDefaultValue(20160)
                .isInteger(),
            new ConfigOptionBuilder() // losesPowerOnDeath
                .withName("player.power.lossOnDeath.enabled")
                .withDescription("If players lose power on death")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()  // powerLostOnDeath
                .withName("player.power.lossOnDeath.amount")
                .withDescription("The amount of power lost on power death (if power loss on death is enabled)")
                .setDefaultValue(1.0)
                .isDouble(),
            new ConfigOptionBuilder() // powerGainedOnKill
                .withName("player.power.amountGainedOnKill")
                .withDescription("The amount of power gained from killing another player")
                .setDefaultValue(1.0)
                .isDouble(),
            new ConfigOptionBuilder() // teleportDelay
                .withName("player.teleportDelay")
                .withDescription("The number of seconds to wait before teleporting a player")
                .setDefaultValue(3)
                .isInteger(),
            new ConfigOptionBuilder() // randomFactionAssignment
                .withName("player.assignNewPlayersToRandomFaction")
                .withDescription("If new players are randomly assigned to a faction")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // warsRequiredForPVP
                .withName("pvp.requiresWar")
                .withDescription("If an active war is required for PvP")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("pvp.friendlyFireConfigurationEnabled")
                .withDescription("If friendly fire is permitted if a faction so chooses.")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // officerPerMemberCount
                .withName("faction.officerPerMemberCount")
                .withDescription("The number of officers permitted per member in a faction")
                .setDefaultValue(5)
                .isInteger(),
            new ConfigOptionBuilder() // factionMaxNumberBases
                .withName("faction.limits.base.count")
                .withDescription("The maximum number of bases a faction may have")
                .setDefaultValue(3)
                .isInteger(),
            new ConfigOptionBuilder() // factionMaxNameLength
                .withName("faction.limits.name.length")
                .withDescription("The maximum length of a faction name")
                .setDefaultValue(20)
                .isInteger(),
            new ConfigOptionBuilder() // factionMaxNumberGates
                .withName("faction.limits.gate.count")
                .withDescription("The maximum number of gates a faction may have")
                .setDefaultValue(5)
                .isInteger(),
            new ConfigOptionBuilder() // factionMaxGateArea
                .withName("faction.limits.gate.area")
                .withDescription("The maximum area a gate may cover")
                .setDefaultValue(64)
                .isInteger(),
            new ConfigOptionBuilder() // maxClaimRadius
                .withName("faction.limits.claim.radius")
                .withDescription("The maximum chunk radius that can be claimed by a faction using the claim command")
                .setDefaultValue(3)
                .isInteger(),
            new ConfigOptionBuilder() // surroundedChunksProtected
                .withName("faction.protections.claims.protectSurroundedChunks")
                .withDescription("If a faction can claim a chunk surrounded in every direction by another factions territory")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // mobsSpawnInFactionTerritory
                .withName("faction.protections.preventMobSpawnInFactionTerritory")
                .withDescription("If mobs will spawn in land claimed by a faction")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // nonMembersCanInteractWithDoors
                .withName("faction.protections.nonMembersCanInteractWithDoors")
                .withDescription("If players that are not a member of a faction can interact with a factions doors")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // laddersPlaceableInEnemyFactionTerritory
                .withName("faction.protections.laddersPlaceableByEnemies")
                .withDescription("If ladders should be placeable in land claimed by an enemey faction")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // factionProtectionsEnabled
                .withName("faction.protections.interactions.enabled")
                .withDescription("If protections are enabled within faction territory")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // territoryAlertPopUp
                .withName("faction.indicators.title")
                .withDescription("If an alert should temporarily pop up when moving between chunks owned by factions")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // territoryIndicatorActionbar
                .withName("faction.indicators.actionbar")
                .withDescription("If an action bar should be shown indicating the owner of a chunk")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // playersChatWithPrefixes
                .withName("chat.global.prependFactionPrefix")
                .withDescription("If a factions prefix should be prepended to players chat messages")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // chatSharedInVassalageTrees
                .withName("chat.faction.sharedInVassalageTrees")
                .withDescription("If faction chat is shared across a factions vassalage tree")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // factionChatColor
                .withName("chat.faction.color")
                .withDescription("The color displayed for faction chat")
                .setDefaultValue("gold"),
            new ConfigOptionBuilder() // showPrefixesInFactionChat
                .withName("chat.faction.showPrefixes")
                .withDescription("If faction prefixes should be shown while in faction chat")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // limitLand
                .withName("faction.limitLandByPower")
                .withDescription("If factions should be limited by the amount of power they have for claimed land")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder() // allowAllyInteraction
                .withName("faction.default.flags.alliesCanInteractWithLand")
                .withDescription("If, by default, allies should be permitted to interact with faction territory")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // allowVassalageTreeInteraction
                .withName("faction.default.flags.vassalageTreeCanInteractWithLand")
                .withDescription("If by default, members within a factions vassalage tree should be permitted to interaction with faction territory")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // territoryAlertColor
                .withName("faction.default.flags.territoryIndicatorColor")
                .withDescription("The default color of the text shown when a player is a factions territory")
                .setDefaultValue("white"),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.prefixColor")
                .withDescription("The default prefix color for a faction")
                .setDefaultValue("white"),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.dynmapTerritoryColor")
                .withDescription("The default territory color for a faction on dynmap")
                .setDefaultValue("#ff0000"),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.enableMobProtection")
                .withDescription("If, by default, mob protection should be enabled for entities within a factions territory")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.allowFriendlyFire")
                .withDescription("If, by default, friendly fire is allowed for a faction")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.acceptBonusPower")
                .withDescription("If, by default, a faction accepts bonus power from administrators")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.mustBeOfficerToManageLand")
                .withDescription("If, by default, a faction member must be at least an officer to manage land on behalf of a faction")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.mustBeOfficerToInviteOthers")
                .withDescription("If, by default, a faction member must be at least an officer to invite others to a faction")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.neutral")
                .withDescription("If, by default, a faction is neutral")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("faction.default.flags.public")
                .withDescription("If, by default, a faction is public (invites aren't required to join)")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder() // secondsBeforeInitialAutosave
                .withName("system.secondsBeforeInitialAutosave")
                .withDescription("The number of seconds before the first auto-save after the plugin is loaded")
                .setDefaultValue(60 * 60)
                .isInteger(),
            new ConfigOptionBuilder() // secondsBetweenAutosaves
                .withName("system.secondsBetweenAutosaves")
                .withDescription("The number of seconds between auto-saves after the initial auto-save")
                .setDefaultValue(60 * 60)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("world.default.flags.enabled")
                .withDescription("If, by default, Factions Plus Plus should be enabled for new worlds")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("world.default.flags.allowClaims")
                .withDescription("If, by default, claims should be allowed in new worlds")
                .setDefaultValue(true)
                .isBoolean()
        };
        for (ConfigOptionBuilder option : configOptions) this.configOptionRepository.add(option.create());
        String[] deprecatedOptionNames = {
            "officerLimit",
            "hourlyPowerIncreaseAmount",
            "maxPowerLevel",
            "useNewLanguageFile",
            "factionless"
        };
        for (String optionName : deprecatedOptionNames) this.configOptionRepository.addDeprecatedOption(optionName);
    }

    public void saveConfigDefaults() {
        this.saveConfigDefaults(false, false);
    }

    public void saveConfigDefaults(Boolean onlySetIfMissing, Boolean deleteOldOptions) {
        for (ConfigOption configOption : this.configOptionRepository.all().values()) {
            // Special case for version, because we always want to set to the new version
            if (configOption.getName().equals("system.version")) getConfig().set("system.version", this.factionsPlusPlus.get().getVersion());
            Boolean optionExists = true;
            switch(configOption.getType()) {
                case Integer:
                    optionExists = getConfig().isInt(configOption.getName());
                    break;
                case Double:
                    optionExists = getConfig().isDouble(configOption.getName());
                    break;
                case String:
                    optionExists = getConfig().isString(configOption.getName());
                    break;
                case Boolean:
                    optionExists = getConfig().isBoolean(configOption.getName());
                    break;
                default:
                    optionExists = getConfig().isSet(configOption.getName());
                    break;
            }
            // TODO: implemenent onlySetIfMissing
            if (! optionExists) getConfig().set(configOption.getName(), configOption.getDefaultValue());
        }
        if (deleteOldOptions) this.deleteOldConfigOptionsIfPresent();
        getConfig().options().copyDefaults(true);
        this.factionsPlusPlus.get().saveConfig();
    }

    public void handleVersionMismatch() {
        this.saveConfigDefaults(true, true);
    }
    
    private void deleteOldConfigOptionsIfPresent() {
        for (String optionName : this.configOptionRepository.allDeprecatedOptions()) {
            if (this.getConfig().isSet(optionName)) {
                this.getConfig().set(optionName, null);
            }
        }
    }

    public ConfigOption getConfigOption(String name) {
        return this.configOptionRepository.get(name);
    }

    public Map<String, ConfigOption> getConfigOptions() {
        return this.configOptionRepository.all();
    }

    public AbstractMap.SimpleEntry<SetConfigResult, String> setConfigOption(String optionName, String value) {
        ConfigOption option = this.getConfigOption(optionName);
        if (option != null) {
            if (! option.isUserSettable()) return new AbstractMap.SimpleEntry<>(SetConfigResult.NotUserSettable, null);
            Object parsedValue = null;
            switch(option.getType()) {
                case Integer:
                    parsedValue = StringUtils.parseAsInteger(value);
                    break;
                case Double:
                    parsedValue = StringUtils.parseAsDouble(value);
                    break;
                case Boolean:
                    parsedValue = StringUtils.parseAsBoolean(value);
                    break;
                default:
                    parsedValue = String.valueOf(value);
                    break;
            }
            if (parsedValue == null) return new AbstractMap.SimpleEntry<>(SetConfigResult.NotExpectedType, option.getType().toString().toLowerCase());
            getConfig().set(optionName, parsedValue);
            return new AbstractMap.SimpleEntry<>(SetConfigResult.ValueSet, String.valueOf(parsedValue));
        }
        return new AbstractMap.SimpleEntry<>(SetConfigResult.DoesNotExist, null);
    }

    public boolean hasBeenAltered() {
        return this.altered;
    }

    public FileConfiguration getConfig() {
        return this.factionsPlusPlus.get().getConfig();
    }

    public int getInt(String option) {
        return this.getConfig().getInt(option);
    }

    public boolean getBoolean(String option) {
        return this.getConfig().getBoolean(option);
    }

    public double getDouble(String option) {
        return this.getConfig().getDouble(option);
    }

    public String getString(String option) {
        return this.getConfig().getString(option);
    }
}