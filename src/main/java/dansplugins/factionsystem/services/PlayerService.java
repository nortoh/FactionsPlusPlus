package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.repositories.FactionRepository;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;
import java.util.UUID;

/**
 * Sends messages to players and the console.
 */
@Singleton
public class PlayerService {
    @Inject private ConfigService configService;
    @Inject private MessageService messageService;
    @Inject private PersistentData persistentData;
    @Inject private FactionRepository factionRepository;

    /**
     * Decide which message to send to the player.
     * @param tsvMessage The old type of message.
     * @param ymlMessage The new type of message.
     * @return
     */
    public String decideWhichMessageToUse(String tsvMessage, String ymlMessage) {
        if (configService.getBoolean("useNewLanguageFile")) {
            return ymlMessage;
        }
        else {
            return tsvMessage;
        }
    }

    /**
     * Send a message to the player.
     * @param sender The player to send the message to.
     * @param tsvMessage The old type of message.
     * @param ymlMessage The new type of message.
     * @param placeholdersReplaced Whether or not parts of the message in the newtype are replaced.
     */
    public void sendMessage(CommandSender sender, String tsvMessage, String ymlMessage, Boolean placeholdersReplaced) {
        if (!placeholdersReplaced) {
            sender.sendMessage(colorize(decideWhichMessageToUse(tsvMessage, messageService.getLanguage().getString(ymlMessage))));
        }
        else {
            sender.sendMessage(colorize(decideWhichMessageToUse(tsvMessage, ymlMessage)));
        }
    }

    /**
     * Send multiple messages to the player.
     * @param sender The player to send the messages to.
     * @param messages The messages to send.
     */
    public void sendMultipleMessages(CommandSender sender, List<String> messages) {
        messages.forEach(s -> sender.sendMessage(colorize(s)));
    }

    /**
     * Send a message to the console.
     * @param console The console to send the message to.
     * @param message The message to send.
     * @param useMessageService Whether or not to use the message service.
     */
    public void sendMessageToConsole(ConsoleCommandSender console, String message, Boolean useMessageService) {
        if (!useMessageService) {
            console.sendMessage(colorize(message));
        }
        else {
            console.sendMessage(colorize(messageService.getLanguage().getString(message)));
        }
    }

    /**
     * Add color to a string.
     * @param input The string to add color to.
     * @return The string with color.
     */
    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }

    /**
     * Method to obtain a Player faction from an object.
     * <p>
     * This method can accept a UUID, Player, OfflinePlayer and a String (name or UUID).<br>
     * If the type isn't found, an exception is thrown.
     * </p>
     *
     * @param object to obtain the Player faction from.
     * @return {@link Faction}
     * @throws IllegalArgumentException when the object isn't compatible.
     */
    @SuppressWarnings("deprecation")
    public Faction getPlayerFaction(Object object) {
        return null;
        // TODO: reimplement using FactionRepository
        /*if (object instanceof OfflinePlayer) {
            return this.persistentData.getPlayersFaction(((OfflinePlayer) object).getUniqueId());
        } else if (object instanceof UUID) {
            return this.persistentData.getPlayersFaction((UUID) object);
        } else if (object instanceof String) {
            try {
                return this.persistentData.getPlayersFaction(UUID.fromString((String) object));
            } catch (Exception e) {
                OfflinePlayer player = Bukkit.getOfflinePlayer((String) object);
                if (player.hasPlayedBefore()) {
                    return this.persistentData.getPlayersFaction(player.getUniqueId());
                }
            }
        }
        throw new IllegalArgumentException(object + " cannot be transferred into a Player");*/
    }
}
