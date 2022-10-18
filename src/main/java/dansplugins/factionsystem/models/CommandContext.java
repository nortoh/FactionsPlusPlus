package dansplugins.factionsystem.models;

import java.util.HashMap;
import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import com.google.inject.Inject;

import dansplugins.factionsystem.builders.MessageBuilder;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.StringUtils;

public class CommandContext {
    private Faction faction = null;
    private CommandSender sender = null;
    private HashMap<String, Object> arguments = new HashMap<>();
    private String[] rawArguments = new String[]{};
    private ArrayList<String> commandNames = new ArrayList<>();
    @Inject private MessageService messageService;

    public Faction getExecutorsFaction() {
        return this.faction;
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public Player getPlayer() {
        return (Player)this.sender;
    }

    public boolean isConsole() {
        return !(this.sender instanceof Player);
    }

    public Object getArgument(String name) {
        return this.arguments.get(name);
    }

    public String getStringArgument(String name) {
        return String.valueOf(this.arguments.get(name));
    }

    public Integer getIntegerArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        return Integer.parseInt(this.getStringArgument(name));
    }

    public Faction getFactionArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) {
            return (Faction)possibleArgument;
        }
        return null;
    }

    public OfflinePlayer getOfflinePlayerArgument(String name) {
        Object possibleArgument = this.arguments.get(name);
        if (possibleArgument != null) {
            return (OfflinePlayer)possibleArgument;
        }
        return null;
    }

    public String[] getRawArguments() {
        return this.rawArguments;
    }

    public void setExecutorsFaction(Faction faction) {
        this.faction = faction;
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

    public ArrayList<String> getCommandNames() {
        return this.commandNames;
    }

    public void reply(String message) {
        this.messageService.messageSender(this.sender, message);
    }

    public void replyWith(String localizationKey) {
        this.messageService.replyToSender(this.sender, localizationKey);
    }

    public void replyWith(MessageBuilder builder) {
        this.messageService.replyToSender(this.sender, builder);
    }
}