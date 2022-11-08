package factionsplusplus.models;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.services.DataService;
import factionsplusplus.services.LocaleService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CommandContext {
    private Faction faction = null;
    private World world = null;
    private CommandSender sender = null;
    private HashMap<String, Object> arguments = new HashMap<>();
    private String[] rawArguments = new String[]{};
    private List<String> commandNames = new ArrayList<>();
    @Inject private LocaleService localeService;
    @Inject private FactionsPlusPlus factionsPlusPlus;
    @Inject private DataService dataService;

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
     * Retrieves the FPPPlayer instance of the executor of this command, if available.
     * 
     * @returns a FPPPlayer instance of executor, or null if executed from console
     */
    public FPPPlayer getFPPPlayer() {
        if (this.isConsole()) return null;
        return this.dataService.getPlayer(((OfflinePlayer)this.sender).getUniqueId());
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

    public BukkitAudiences getAdventure() {
        return this.factionsPlusPlus.getAdventure();
    }

    public Audience getExecutorsAudience() {
        return this.getAdventure().sender(sender);
    }

    /*
     * Sends a raw message to a sender.
     * 
     * @param message the message to send
     */
    public void reply(String message) {
        this.sender.sendMessage(message);
    }

    /*
     * Sends a localized message without parameters.
     * 
     * @param localizationKey the language key as defined in the language YAML file to send
     */
    public void replyWith(String localizationKey, Object... arguments) {
        this.getExecutorsAudience().sendMessage(
            Component.translatable(localizationKey).color(NamedTextColor.YELLOW).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
        );
    }

    public void replyWithMiniMessage(String message) {
        this.getExecutorsAudience().sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void alertPlayer(OfflinePlayer player, String localizationKey, Object... arguments) {
        this.factionsPlusPlus.getAdventure().player(player.getUniqueId()).sendMessage(
            Component.translatable(localizationKey).color(NamedTextColor.YELLOW).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
        );
    }

    public void success(String localizationKey, Object... arguments) {
        this.getExecutorsAudience().sendMessage(
            Component.text()
                .append(
                    Component.translatable("Generic.Success").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD)
                )
                .append(Component.text(" "))
                .append(
                    Component.translatable(localizationKey).color(NamedTextColor.AQUA).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
                )
                .asComponent()
        );
    }

    private Component generateErrorComponent(String localizationKey, Object... arguments) {
        return Component.text()
            .append(
                Component.translatable("Generic.Error").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
            )
            .append(Component.text(" "))
            .append(
                Component.translatable(localizationKey).color(NamedTextColor.YELLOW).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
            )
            .asComponent();
    }

    public void error(String localizationKey, Object... arguments) {
        this.getExecutorsAudience().sendMessage(this.generateErrorComponent(localizationKey, arguments));
    }

    public void cancellableError(String localizationKey, String commandToRun) {
        this.getExecutorsAudience().sendMessage(
            this.generateErrorComponent(localizationKey, new Object[]{})
                .append(Component.text(" "))
                .append(
                    Component.translatable("Generic.ClickHere.Cancel").color(NamedTextColor.GOLD).clickEvent(
                        ClickEvent.runCommand(commandToRun)
                    )
                )
        );
    }

    public void cancellable(String localizationKey, String commandToRun, Object... arguments) {
        this.getExecutorsAudience().sendMessage(
            Component.translatable(localizationKey).color(NamedTextColor.YELLOW).args(Arrays.stream(arguments).map(argument -> Component.text(argument.toString())).toList())
                .append(Component.text(" "))
                .append(
                    Component.translatable("Generic.ClickHere.Cancel").color(NamedTextColor.GOLD).clickEvent(
                        ClickEvent.runCommand(commandToRun)
                    )
                )
        );
    }

    public void cancellable(String localizationKey, String commandToRun) {
        this.cancellable(localizationKey, commandToRun, new Object[]{});
    }

    public String getLocalizedString(String localizationKey, Object... arguments) {
        return this.localeService.get(localizationKey, arguments);
    }

}