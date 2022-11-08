/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus;

import com.google.inject.Inject;
import com.google.inject.Injector;

import factionsplusplus.di.PluginModule;
import factionsplusplus.events.handler.*;
import factionsplusplus.externalapi.FactionsPlusPlusAPI;
import factionsplusplus.integrators.PlaceholderAPI;
import factionsplusplus.services.*;
import factionsplusplus.utils.extended.Scheduler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.translation.GlobalTranslator;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import javax.inject.Provider;

/**
 * @author Daniel McCoy Stephenson
 * @since May 30th, 2020
 */
public class FactionsPlusPlus extends JavaPlugin {

    private final String pluginVersion = "v" + getDescription().getVersion();
    @Inject private ActionBarService actionBarService;
    @Inject private ConfigService configService;
    @Inject private Provider<DataService> dataService;
    @Inject private Provider<Scheduler> scheduler;
    @Inject private Provider<CommandService> commandService;
    @Inject private LocaleService localeService;
    @Inject private Provider<DynmapIntegrationService> dynmapService;
    @Inject private Provider<PlayerService> playerService;
    private BukkitAudiences adventure;
    private Injector injector;

    public Injector getInjector() {
        return this.injector;
    }

    public ConfigService getConfigService() {
        return this.configService;
    }

    /**
     * This runs when the server starts.
     */
    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);
        this.injector = (new PluginModule(this)).createInjector();
        this.initializeConfig();
        this.initializeTranslations();
        this.load();
        this.scheduleRecurringTasks();
        this.registerEventHandlers();
        this.handleIntegrations();
        this.makeSureEveryPlayerExperiencesPowerDecay();
        this.commandService.get().registerCommands();
        getCommand("fpp").setTabCompleter(commandService.get());
    }

    /**
     * This runs when the server stops.
     */
    @Override
    public void onDisable() {
        this.dataService.get().save();
        this.dataService.get().disable();
        this.localeService.saveLanguage();
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    /**
     * This method handles commands sent to the minecraft server and interprets them if the label matches one of the core commands.
     *
     * @param sender The sender of the command.
     * @param cmd    The command that was sent. This is unused.
     * @param label  The core command that has been invoked.
     * @param args   Arguments of the core command. Often sub-commands.
     * @return A boolean indicating whether the execution of the command was successful.
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        if (! cmd.getName().equalsIgnoreCase("fpp")) return false; // ignore commands not meant for us
        return this.commandService.get().interpretCommand(sender, label, args);
    }

    /**
     * This can be used to get the version of the plugin.
     *
     * @return A string containing the version preceded by 'v'
     */
    public String getVersion() {
        return this.pluginVersion;
    }

    /**
     * Checks if the version is mismatched.
     *
     * @return A boolean indicating if the version is mismatched.
     */
    public boolean isVersionMismatched() {
        String configVersion = getConfig().getString("system.version");
        if (configVersion == null || this.getVersion() == null) {
            return true;
        } else {
            return ! configVersion.equalsIgnoreCase(this.getVersion());
        }
    }

    /**
     * This can be utilized to access the external API of Medieval Factions.
     *
     * @return A reference to the external API.
     */
    public FactionsPlusPlusAPI getAPI() {
        return (FactionsPlusPlusAPI)this.getInjector().getInstance(FactionsPlusPlusAPI.class);
    }

    /**
     * Checks if debug is enabled.
     *
     * @return Whether debug is enabled.
     */
    public boolean isDebugEnabled() {
        return getConfig().getBoolean("system.debugMode");
    }

    private void makeSureEveryPlayerExperiencesPowerDecay() {
        this.playerService.get().createActivityRecordForEveryOfflinePlayer();
    }

    /**
     * Creates or loads language files, depending on the situation.
     */
    public void initializeTranslations() {
        this.localeService.createLanguageFile();
        GlobalTranslator.translator().addSource(this.localeService.getRegistry());
    }

    /**
     * Retrieves the default locale tag for the server.
     */
    public String getDefaultLocaleTag() {
        final String defaultLanguage = getConfig().getString("system.language");
        if (defaultLanguage == null) return "en_US";
        return defaultLanguage;
    }

    /**
     * Creates or loads the config, depending on the situation.
     */
    private void initializeConfig() {
        if (this.configFileExists()) {
            this.performCompatibilityChecks();
        } else {
            this.configService.saveConfigDefaults();
        }
    }

    private void performCompatibilityChecks() {
        if (this.isVersionMismatched()) {
            this.configService.handleVersionMismatch();
        }
        reloadConfig();
    }

    private boolean configFileExists() {
        return new File(getDataFolder(), "config.yml").exists();
    }

    public String getStoragePath() {
        return getDataFolder().getAbsolutePath();
    }

    public BukkitAudiences getAdventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    /**
     * Loads stored data into Persistent Data.
     */
    private void load() {
        this.dataService.get().load();
    }

    /**
     * Calls the Scheduler to schedule tasks that have to repeatedly be executed.
     */
    private void scheduleRecurringTasks() {
        this.scheduler.get().schedulePowerIncrease();
        this.scheduler.get().schedulePowerDecrease();
        this.actionBarService.schedule(this);
    }

    /**
     * Registers the event handlers of the plugin using Ponder.
     */
    private void registerEventHandlers() {
        this.initializeListeners().stream()
            .forEach(listener -> Bukkit.getPluginManager().registerEvents(listener, this));
    }

    private List<Listener> initializeListeners() {
        return new ArrayList<>(Arrays.asList(
                this.getInjector().getInstance(ChatHandler.class),
                this.getInjector().getInstance(DamageHandler.class),
                this.getInjector().getInstance(DeathHandler.class),
                this.getInjector().getInstance(EffectHandler.class),
                this.getInjector().getInstance(InteractionHandler.class),
                this.getInjector().getInstance(JoinHandler.class),
                this.getInjector().getInstance(MoveHandler.class),
                this.getInjector().getInstance(QuitHandler.class),
                this.getInjector().getInstance(SpawnHandler.class),
                this.getInjector().getInstance(WarHandler.class)
        ));
    }

    /**
     * Takes care of integrations for other plugins and tools.
     */
    private void handleIntegrations() {
        this.handlebStatsIntegration();
        this.handlePlaceholdersIntegration();
        this.dynmapService.get().updateClaimsIfAble();
    }

    private void handlebStatsIntegration() {
        if (Bukkit.getPluginManager().getPlugin("bStats") != null) {
            int pluginId = 8929;
            new Metrics(this, pluginId);
        }
    }

    private void handlePlaceholdersIntegration() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            PlaceholderAPI api = this.getInjector().getInstance(PlaceholderAPI.class);
            api.register();
        }
    }
}