/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.models.ConfigOption;
import dansplugins.factionsystem.repositories.ConfigOptionRepository;
import dansplugins.factionsystem.utils.StringUtils;
import dansplugins.factionsystem.builders.ConfigOptionBuilder;
import dansplugins.factionsystem.constants.SetConfigResult;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import javax.inject.Provider;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class ConfigService {
    private final Provider<MedievalFactions> medievalFactions;
    private final Provider<LocaleService> localeService;
    private final ConfigOptionRepository configOptionRepository;

    private boolean altered = false;

    @Inject
    public ConfigService(Provider<MedievalFactions> medievalFactions, Provider<LocaleService> localeService, ConfigOptionRepository configOptionRepository) {
        this.medievalFactions = medievalFactions;
        this.localeService = localeService;
        this.configOptionRepository = configOptionRepository;
        this.registerCoreOptions();
    }

    public void registerCoreOptions() {
        ConfigOptionBuilder[] configOptions = new ConfigOptionBuilder[]{
            new ConfigOptionBuilder()
                .withName("version")
                .withDescription("Current version of this plugin")
                .setDefaultValue(this.medievalFactions.get().getVersion())
                .notUserSettable(),
            new ConfigOptionBuilder()
                .withName("initialMaxPowerLevel")
                .withDescription("The initial maximum power a player has")
                .setDefaultValue(20)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("initialPowerLevel")
                .withDescription("The initial amount of power a player starts with")
                .setDefaultValue(5)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("mobsSpawnInFactionTerritory")
                .withDescription("If mobs will spawn in land claimed by a faction")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("powerIncreaseAmount")
                .withDescription("...")
                .setDefaultValue(2)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("laddersPlaceableInEnemyFactionTerritory")
                .withDescription("If ladders should be placeable in land claimed by an enemey faction")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("minutesBeforeInitialPowerIncrease")
                .withDescription("The number of minutes before a new player is given a power increase")
                .setDefaultValue(30)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("minutesBetweenPowerIncreases")
                .withDescription("The number of minutes between power increases")
                .setDefaultValue(60)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("warsRequiredForPVP")
                .withDescription("If an active war is required for PvP")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("factionOwnerMultiplier")
                .withDescription("The power multiplier for owning a faction")
                .setDefaultValue(2.0)
                .isDouble(),
            new ConfigOptionBuilder()
                .withName("factionOfficerMultiplier")
                .withDescription("The power multiplier for being an officer in a faction")
                .setDefaultValue(1.5)
                .isDouble(),
            new ConfigOptionBuilder()
                .withName("officerPerMemberCount")
                .withDescription("The number of officers permitted per member in a faction")
                .setDefaultValue(5)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("powerDecreases")
                .withDescription("...")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("minutesBetweenPowerDecreases")
                .withDescription("The number of minutes between automatic power decreases")
                .setDefaultValue(1440)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("minutesBeforePowerDecrease")
                .withDescription("The number of minutes before automatic power decreases start")
                .setDefaultValue(20160)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("powerDecreaseAmount")
                .withDescription("...")
                .setDefaultValue(1)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("factionMaxNameLength")
                .withDescription("The maximum length of a faction name")
                .setDefaultValue(20)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("factionMaxNumberGates")
                .withDescription("The maximum number of gates a faction may have")
                .setDefaultValue(5)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("factionMaxGateArea")
                .withDescription("The maximum area a gate may cover")
                .setDefaultValue(64)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("surroundedChunksProtected")
                .withDescription("...")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("zeroPowerFactionsGetDisbanded")
                .withDescription("If factions with zero power get disbanded automatically")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("vassalContributionPercentageMultiplier")
                .withDescription("...")
                .setDefaultValue(0.75)
                .isDouble(),
            new ConfigOptionBuilder()
                .withName("nonMembersCanInteractWithDoors")
                .withDescription("If players that are not a member of a faction can interact with a factions doors")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("playersChatWithPrefixes")
                .withDescription("If a factions prefix should be prepended to players chat messages")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("maxClaimRadius")
                .withDescription("The maximum chunk radius that can be claimed by a faction using the claim command")
                .setDefaultValue(3)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("languageid")
                .withDescription("The locale to use when sending messages")
                .setDefaultValue("en-us"),
            new ConfigOptionBuilder()
                .withName("chatSharedInVassalageTrees")
                .withDescription("If faction chat is shared across a factions vassalage tree")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("allowAllyInteraction")
                .withDescription("...")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("allowVassalageTreeInteraction")
                .withDescription("...")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("factionChatColor")
                .withDescription("The color displayed for faction chat")
                .setDefaultValue("gold"),
            new ConfigOptionBuilder()
                .withName("territoryAlertPopUp")
                .withDescription("...")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("territoryIndicatorActionbar")
                .withDescription("...")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("territoryAlertColor")
                .withDescription("...")
                .setDefaultValue("white"),
            new ConfigOptionBuilder()
                .withName("randomFactionAssignment")
                .withDescription("If new players are randomly assigned to a faction")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("allowNeutrality")
                .withDescription("If factions are permitted to be neutral")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("showPrefixesInFactionChat")
                .withDescription("If faction prefixes should be shown while in faction chat")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("debugMode")
                .withDescription("If the plugin should be more verbose when logging")
                .setDefaultValue(false)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("factionProtectionsEnabled")
                .withDescription("...")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("limitLand")
                .withDescription("...")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("factionsCanSetPrefixColors")
                .withDescription("If factions can set their prefix color")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("playersLosePowerOnDeath")
                .withDescription("If players lose power on death")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("bonusPowerEnabled")
                .withDescription("If bonus power is enabled")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("useNewLanguageFile")
                .withDescription("If the plugin should attempt to use the new style language file")
                .setDefaultValue(true)
                .isBoolean(),
            new ConfigOptionBuilder()
                .withName("powerLostOnDeath")
                .withDescription("The amount of power lost on power death (if power loss on death is enabled)")
                .setDefaultValue(1.0)
                .isDouble(),
            new ConfigOptionBuilder()
                .withName("powerGainedOnKill")
                .withDescription("The amount of power gained from killing another player")
                .setDefaultValue(1.0)
                .isDouble(),
            new ConfigOptionBuilder()
                .withName("teleportDelay")
                .withDescription("The number of seconds to wait before teleporting")
                .setDefaultValue(3)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("factionless")
                .withDescription("...")
                .setDefaultValue("FactionLess"),
            new ConfigOptionBuilder()
                .withName("secondsBeforeInitialAutosave")
                .withDescription("The number of seconds before the first auto-save after the plugin is loaded")
                .setDefaultValue(60 * 60)
                .isInteger(),
            new ConfigOptionBuilder()
                .withName("secondsBetweenAutosaves")
                .withDescription("The number of seconds between auto-saves after the initial auto-save")
                .setDefaultValue(60 * 60)
                .isInteger()
        };
        for (ConfigOptionBuilder option : configOptions) this.configOptionRepository.add(option.create());
        String[] deprecatedOptionNames = {
            "officerLimit",
            "hourlyPowerIncreaseAmount",
            "maxPowerLevel"
        };
        for (String optionName : deprecatedOptionNames) this.configOptionRepository.addDeprecatedOption(optionName);
    }

    public void saveConfigDefaults() {
        this.saveConfigDefaults(false, false);
    }

    public void saveConfigDefaults(Boolean onlySetIfMissing, Boolean deleteOldOptions) {
        for (ConfigOption configOption : this.configOptionRepository.all().values()) {
            // Special case for version, because we always want to set to the new version
            if (configOption.getName().equals("version")) getConfig().set("version", this.medievalFactions.get().getVersion());
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
        this.medievalFactions.get().saveConfig();
    }

    public void handleVersionMismatch() {
        this.saveConfigDefaults(true, true);
    }
    
    private void deleteOldConfigOptionsIfPresent() {
        for (String optionName : this.configOptionRepository.allDeprecatedOptions()) {
            if (getConfig().isSet(optionName)) {
                getConfig().set(optionName, null);
            }
        }
    }

    public ConfigOption getConfigOption(String name) {
        return this.configOptionRepository.get(name);
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

    public void sendPageOneOfConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + localeService.get().get("ConfigListPageOne"));
        sender.sendMessage(ChatColor.AQUA + "version: " + getString("version")
                + ", languageid: " + getString("languageid")
                + ", debugMode: " + getBoolean("debugMode")
                + ", initialMaxPowerLevel: " + getInt("initialMaxPowerLevel")
                + ", initialPowerLevel: " + getInt("initialPowerLevel")
                + ", powerIncreaseAmount: " + getInt("powerIncreaseAmount")
                + ", mobsSpawnInFactionTerritory: " + getBoolean("mobsSpawnInFactionTerritory")
                + ", laddersPlaceableInEnemyFactionTerritory: " + getBoolean("laddersPlaceableInEnemyFactionTerritory")
                + ", minutesBeforeInitialPowerIncrease: " + getInt("minutesBeforeInitialPowerIncrease")
                + ", minutesBetweenPowerIncreases: " + getInt("minutesBetweenPowerIncreases")
                + ", warsRequiredForPVP: " + getBoolean("warsRequiredForPVP")
                + ", factionOwnerMultiplier: " + getDouble("factionOwnerMultiplier")
                + ", officerPerMemberCount: " + getInt("officerPerMemberCount")
                + ", factionOfficerMultiplier: " + getDouble("factionOfficerMultiplier")
                + ", powerDecreases: " + getBoolean("powerDecreases")
                + ", minutesBetweenPowerDecreases: " + getInt("minutesBetweenPowerDecreases")
                + ", minutesBeforePowerDecrease: " + getInt("minutesBeforePowerDecrease")
                + ", powerDecreaseAmount: " + getInt("powerDecreaseAmount")
                + ", factionMaxNameLength: " + getInt("factionMaxNameLength")
                + ", factionMaxNumberGates: " + getInt("factionMaxNumberGates"));
    }

    public void sendPageTwoOfConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + localeService.get().get("ConfigListPageTwo"));
        sender.sendMessage(ChatColor.AQUA + "factionMaxGateArea: " + getInt("factionMaxGateArea")
                + ", surroundedChunksProtected: " + getBoolean("surroundedChunksProtected")
                + ", zeroPowerFactionsGetDisbanded: " + getBoolean("zeroPowerFactionsGetDisbanded")
                + ", vassalContributionPercentageMultiplier: " + getDouble("vassalContributionPercentageMultiplier")
                + ", nonMembersCanInteractWithDoors: " + getBoolean("nonMembersCanInteractWithDoors")
                + ", playersChatWithPrefixes: " + getBoolean("playersChatWithPrefixes")
                + ", maxClaimRadius: " + getInt("maxClaimRadius")
                + ", chatSharedInVassalageTrees: " + getBoolean("chatSharedInVassalageTrees")
                + ", allowAllyInteraction: " + getBoolean("allowAllyInteraction")
                + ", allowVassalageTreeInteraction: " + getBoolean("allowVassalageTreeInteraction")
                + ", factionChatColor: " + getString("factionChatColor")
                + ", territoryAlertPopUp: " + getBoolean("territoryAlertPopUp")
                + ", territoryAlertColor: " + getString("territoryAlertColor")
                + ", territoryIndicatorActionbar: " + getBoolean("territoryIndicatorActionbar")
                + ", randomFactionAssignment: " + getBoolean("randomFactionAssignment")
                + ", allowNeutrality: " + getBoolean("allowNeutrality")
                + ", showPrefixesInFactionChat: " + getBoolean("showPrefixesInFactionChat")
                + ", factionProtectionsEnabled: " + getBoolean("factionProtectionsEnabled")
                + ", limitLand: " + getBoolean("limitLand")
                + ", factionsCanSetPrefixColors: " + getBoolean("factionsCanSetPrefixColors")
                + ", playersLosePowerOnDeath: " + getBoolean("playersLosePowerOnDeath")
                + ", bonusPowerEnabled: " + getBoolean("bonusPowerEnabled")
                + ", powerLostOnDeath: " + getDouble("powerLostOnDeath")
                + ", powerGainedOnKill: " + getDouble("powerGainedOnKill")
                + ", teleportDelay: " + getInt("teleportDelay")
                + ", factionless: " + getString("factionless")
                + ", useNewLanguageFile: " + getBoolean("useNewLanguageFile")
                + ", secondsBeforeInitialAutosave: " + getInt("secondsBeforeInitialAutosave")
                + ", secondsBetweenAutosaves: " + getInt("secondsBetweenAutosaves"));
    }

    public ArrayList<String> getStringConfigOptions()
    {
        final ArrayList<String> configOptions = new ArrayList<>();
        Collections.addAll(configOptions,
                "initialMaxPowerLevel",
                "initialPowerLevel",
                "powerIncreaseAmount",
                "minutesBeforeInitialPowerIncrease",
                "minutesBetweenPowerIncreases",
                "officerLimit",
                "officerPerMemberCount",
                "minutesBetweenPowerDecreases",
                "minutesBeforePowerDecrease",
                "powerDecreaseAmount",
                "factionMaxNameLength",
                "factionMaxNumberGates",
                "factionMaxGateArea",
                "maxClaimRadius",
                "teleportDelay",
                "mobsSpawnInFactionTerritory",
                "laddersPlaceableInEnemyFactionTerritory",
                "warsRequiredForPVP",
                "powerDecreases",
                "surroundedChunksProtected",
                "zeroPowerFactionsGetDisbanded",
                "nonMembersCanInteractWithDoors",
                "playersChatWithPrefixes",
                "chatSharedInVassalageTrees",
                "allowAllyInteraction",
                "allowVassalageTreeInteraction",
                "territoryAlertPopUp",
                "territoryIndicatorActionbar",
                "randomFactionAssignment",
                "allowNeutrality",
                "showPrefixesInFactionChat",
                "debugMode",
                "factionProtectionsEnabled",
                "limitLand",
                "factionsCanSetPrefixColors",
                "playersLosePowerOnDeath",
                "bonusPowerEnabled",
                "factionOwnerMultiplier",
                "factionOfficerMultiplier",
                "vassalContributionPercentageMultiplier",
                "powerLostOnDeath",
                "powerGainedOnKill",
                "factionless",
                "useNewLanguageFile",
                "secondsBeforeInitialAutosave",
                "secondsBetweenAutosaves");
        return configOptions;
    }

    public boolean hasBeenAltered() {
        return this.altered;
    }

    public FileConfiguration getConfig() {
        return this.medievalFactions.get().getConfig();
    }

    public int getInt(String option) {
        return getConfig().getInt(option);
    }

    public boolean getBoolean(String option) {
        return getConfig().getBoolean(option);
    }

    public double getDouble(String option) {
        return getConfig().getDouble(option);
    }

    public String getString(String option) {
        return getConfig().getString(option);
    }

    public LocaleService getLocaleService() {
        return this.localeService.get();
    }
}