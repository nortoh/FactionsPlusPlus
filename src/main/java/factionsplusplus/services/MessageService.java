package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.builders.MessageBuilder;
import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import factionsplusplus.utils.StringUtils;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import factionsplusplus.models.CommandContext;
import factionsplusplus.models.Faction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Objects;

@Singleton
public class MessageService {

    private final LocaleService localeService;
    private final BukkitAudiences adventure;

    @Inject
    public MessageService(LocaleService localeService, @Named("adventure") BukkitAudiences adventure) {
        this.localeService = localeService;
        this.adventure = adventure;
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
            .keySet()
            .stream()
            .map(uuid -> Bukkit.getPlayer(uuid))
            .filter(Objects::nonNull)
            .forEach(player -> this.send(player, message));
    }

    /**
     * Method to send every player in the server a message.
     *
     * @param message message to send to the players.
     */
    public void sendToAllPlayers(String message) {
        this.adventure.players().sendMessage(Component.text(message), MessageType.CHAT);
    }

    /**
     * Method to broadcast a message as the server.
     *
     * @param message message to broadcast.
     */
    public void broadcast(String message) {
        this.adventure.players().sendMessage(Component.text(message), MessageType.SYSTEM);
    }

    public void sendInvalidSyntaxMessage(CommandContext context, String commandName, String commandSyntax) {
        context.error("Error.Syntax", commandName, commandSyntax);
    }

    public void sendInvalidSyntaxMessage(CommandContext context, List<String> commandNameList, String commandSyntax) {
        this.sendInvalidSyntaxMessage(context, String.join(" ", commandNameList), commandSyntax);
    }

}
