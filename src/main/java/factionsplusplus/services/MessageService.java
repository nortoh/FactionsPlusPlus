package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.utils.StringUtils;
import factionsplusplus.models.Faction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

@Singleton
public class MessageService {

    private final LocaleService localeService;

    @Inject
    public MessageService(LocaleService localeService) {
        this.localeService = localeService;
    }

    public void send(Player player, String message) {
        player.sendMessage(StringUtils.colorize(message));
    }

    public void send(CommandSender sender, String message) {
        sender.sendMessage(StringUtils.colorize(message));
    }

    public void sendLocalizedMessage(CommandSender sender, String localizationKey) {
        this.send(sender, this.localeService.get(localizationKey));
    }

    public void sendLocalizedMessage(CommandSender sender, GenericMessageBuilder builder) {
        for (MessageBuilder mBuilder : builder.getMessageBuilders()) {
            this.send(
                sender,
                mBuilder.toString(
                    this.localeService.get(mBuilder.getLocalizationKey())
                )
            );
        }
    }

    public void sendAllPlayersLocalizedMessage(GenericMessageBuilder builder) {
        for (MessageBuilder mBuilder : builder.getMessageBuilders()) {
            this.sendToAllPlayers(
                mBuilder.toString(
                    this.localeService.get(mBuilder.getLocalizationKey())
                )
            );
        }
    }

    public void sendFactionLocalizedMessage(Faction faction, String localizationKey) {
        this.sendToFaction(faction, localizationKey);
    }

    public void sendFactionLocalizedMessage(Faction faction, GenericMessageBuilder builder) {
        for (MessageBuilder mBuilder : builder.getMessageBuilders()) {
            this.sendToFaction(
                faction,
                mBuilder.toString(
                    this.localeService.get(mBuilder.getLocalizationKey())
                )
            );
        }
    }

    public void sendPermissionMissingMessage(CommandSender sender, List<String> missingPermissions) {
        this.sendLocalizedMessage(sender, new MessageBuilder("PermissionNeeded").with("permission", String.join(", ", missingPermissions)));
    }

    /**
     * Method to send an entire Faction a message.
     *
     * @param faction    to send a message to.
     * @param message    message to send to the Faction.
     */
    public void sendToFaction(Faction faction, String message) {
        faction.getMembers()
            .stream()
            .map(member -> Bukkit.getPlayer(member.getId()))
            .filter(Objects::nonNull)
            .forEach(player -> this.send(player, message));
    }

    /**
     * Method to send every player in the server a message.
     *
     * @param message message to send to the players.
     */
    public void sendToAllPlayers(String message) {
        Bukkit.getOnlinePlayers().forEach(player -> this.send(player, message));
    }

    /**
     * Method to broadcast a message as the server.
     *
     * @param message message to broadcast.
     */
    public void broadcast(String message) {
        Bukkit.broadcastMessage(message);
    }

    public void sendInvalidSyntaxMessage(CommandSender sender, String commandName, String commandSyntax) {
        this.sendLocalizedMessage(
            sender,
            new MessageBuilder("InvalidSyntax")
                .with("command", commandName)
                .with("syntax", commandSyntax)
        );
    }

    public void sendInvalidSyntaxMessage(CommandSender sender, List<String> commandNameList, String commandSyntax) {
        this.sendInvalidSyntaxMessage(sender, String.join(" ", commandNameList), commandSyntax);
    }

}
