package factionsplusplus.models;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.services.LocaleService;
import factionsplusplus.services.MessageService;

public class CommandContext {
    private Faction faction = null;
    private World world = null;
    private CommandSender sender = null;
    private HashMap<String, Object> arguments = new HashMap<>();
    private String[] rawArguments = new String[]{};
    private List<String> commandNames = new ArrayList<>();
    @Inject private MessageService messageService;
    @Inject private LocaleService localeService;
    @Inject private FactionsPlusPlus factionsPlusPlus;

    /*
     * Retrieves the Faction instance the executor of this command is a member of, if any.
     * 
     * @return the Faction the executors belongs to, if any
     */
    public Faction getExecutorsFaction() {
        return this.faction;
    }

    /*
     * Retrieves an instance of the plugin.
     * 
     * @return a FactionPlusPlus instance (the plugin)
     */
    public FactionsPlusPlus getPlugin() {
        return this.factionsPlusPlus;
    }

    /*
     * Retrieves the World instance the executor of this command is a member of, if any
     * 
     * @return the World the executor is in, if any
     */
    public World getExecutorsWorld() {
        return this.world;
    }

    /*
     * Retrieves the CommandSender of the executor.
     * 
     * @return a CommandSender instance
     */
    public CommandSender getSender() {
        return this.sender;
    }

    /*
     * Retrieves the Player instance of the executor of this command, if availiable.
     * 
     * @returns a Player instance of executor, or null if executed from console
     */
    public Player getPlayer() {
        if (this.isConsole()) return null;
        return (Player)this.sender;
    }

    /*
     * Determines if this command was executed from the console.
     * 
     * @return a boolean indicating if this command was executed from the console.
     */
    public boolean isConsole() {
        return ! (this.sender instanceof Player);
    }


    /*
     * Retrieves a raw argument.
     * 
     * @return an Object of the argument, intended to be cast as something else
     */
    public Object getArgument(String name) {
        return this.arguments.get(name);
    }

    /*
     * Retrieves an argument as a string.
     */
    public String getStringArgument(String name) {
        if (this.arguments.get(name) == null) return null; // String.valueOf will return null as a string...
        return String.valueOf(this.arguments.get(name));
    }

    /*
     * Retrieves an argument as an Integer.
     */
    public Integer getIntegerArgument(String name) {
        return (Integer)this.arguments.get(name);
    }

    /*
     * Retrieves an argument as a Double.
     */
    public Double getDoubleArgument(String name) {
        return (Double)this.arguments.get(name);
    }

    /*
     * Retrieves an argument as a Boolean.
     */
    public Boolean getBooleanArgument(String name) {
        return (Boolean)this.arguments.get(name);
    }

    /*
     * Retrieves an argument as a Faction, if able.
     */
    public Faction getFactionArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) return (Faction)possibleArgument;
        return null;
    }

    /*
     * Retrieves an argument as a FactionBase, if able.
     */
    public FactionBase getFactionBaseArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) return (FactionBase)possibleArgument;
        return null;
    }

    /*
     * Retrieves an argument as an OfflinePlayer, if able.
     */
    public OfflinePlayer getOfflinePlayerArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) return (OfflinePlayer)possibleArgument;
        return null;
    }

    /*
     * Retrieves an argument as a Player, if able.
     */
    public Player getPlayerArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) return (Player)possibleArgument;
        return null;
    }

    /*
     * Retrieves an argument as a ConfigurationFlag, if able.
     */
    public ConfigurationFlag getConfigurationFlagArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) return (ConfigurationFlag)possibleArgument;
        return null;
    }

    /*
     * Retrieves an argument as a ConfigOption, if able.
     */
    public ConfigOption getConfigOptionArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) return (ConfigOption)possibleArgument;
        return null;
    }

    public String[] getRawArguments() {
        return this.rawArguments;
    }

    public void setExecutorsFaction(Faction faction) {
        this.faction = faction;
    }

    public void setExecutorsWorld(World world) {
        this.world = world;
    }

    public void setSender(CommandSender sender) {
        this.sender = sender;
    }

    public void addArgument(String name, Object value) {
        this.arguments.put(name, value);
    }

    public void setRawArguments(String[] arguments) {
        this.rawArguments = arguments;
    }

    public void addCommandName(String command) {
        this.commandNames.add(command);
    }

    public List<String> getCommandNames() {
        return this.commandNames;
    }


    /*
     * Sends a raw message to a sender. This will go through colorization in MessageService but no translations will happen.
     * 
     * @param message the message to send
     */
    public void reply(String message) {
        this.messageService.send(this.sender, message);
    }

    /*
     * Sends a localized message without parameters.
     * 
     * @param localizationKey the language key as defined in the language YAML file to send
     */
    public void replyWith(String localizationKey) {
        this.messageService.sendLocalizedMessage(this.sender, localizationKey);
    }

    /*
     * Sends a localized message using a MessageBuilder instance.
     * 
     * @param builder the MessageBuilder instance
     */
    public void replyWith(GenericMessageBuilder builder) {
        this.messageService.sendLocalizedMessage(this.sender, builder);
    }

    public void messagePlayer(Player player, String localizationKey) {
        this.messageService.sendLocalizedMessage(player, localizationKey);
    }

    public void messagePlayer(Player player, GenericMessageBuilder builder) {
        this.messageService.sendLocalizedMessage(player, builder);
    }

    public void messageFaction(Faction faction, String localizationKey) {
        this.messageService.sendFactionLocalizedMessage(faction, localizationKey);
    }

    public void messageFaction(Faction faction, GenericMessageBuilder builder) {
        this.messageService.sendFactionLocalizedMessage(faction, builder);
    }

    public void messagePlayersFaction(String localizationKey) {
        this.messageFaction(this.getExecutorsFaction(), localizationKey);
    }

    public void messagePlayersFaction(GenericMessageBuilder builder) {
        this.messageFaction(this.getExecutorsFaction(), builder);
    }

    public void messageAllPlayers(GenericMessageBuilder builder) {
        this.messageService.sendAllPlayersLocalizedMessage(builder);
    }

    public String getLocalizedString(String localizationKey) {
        return this.localeService.get(localizationKey);
    }

    public List<String> getLocalizedStrings(String localizationKey) {
        return this.localeService.getStrings(localizationKey);
    }
}