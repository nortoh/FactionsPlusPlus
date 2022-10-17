package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.commands.abs.ColorTranslator;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.models.FactionFlag;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.AbstractMap;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Singleton
public class MessageService implements ColorTranslator {

    private final MedievalFactions medievalFactions;
    private File languageFile;
    private FileConfiguration language;
    private PlayerService playerService;
    private LocaleService localeService;
    private ConfigService configService;

    @Inject
    public MessageService(MedievalFactions medievalFactions, PlayerService playerService, LocaleService localeService, ConfigService configService) {
        this.medievalFactions = medievalFactions;
        this.playerService = playerService;
        this.localeService = localeService;
        this.configService = configService;
        this.createLanguageFile();
    }

    public void createLanguageFile() {
        this.languageFile = new File(this.medievalFactions.getDataFolder(), "language.yml");
        if (!this.languageFile.exists()) this.medievalFactions.saveResource("language.yml", false);
        this.language = new YamlConfiguration();
        try {
            this.language.load(this.languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            this.medievalFactions.getLogger().log(Level.WARNING, e.getCause().toString());
        }
    }

    public FileConfiguration getLanguage() {
        return this.language;
    }


    public void reloadLanguage() {
        if (languageFile.exists()) {
            this.language = YamlConfiguration.loadConfiguration(this.languageFile);
        } else {
            this.createLanguageFile();
        }
    }

    public void saveLanguage() {
        if (this.languageFile.exists()) {
            try {
                this.language.save(this.languageFile);
            } catch (IOException ignored) {
            }
        } else {
            this.createLanguageFile();
        }
    }

    public void sendPermissionMissingMessage(CommandSender sender, List<String> missingPermissions) {
        this.playerService.sendMessage(
            sender,
            this.translate("&c" + this.localeService.getText("PermissionNeeded")), 
            Objects.requireNonNull(this.getLanguage().getString("PermissionNeeded"))
                .replace("#permission#", String.join(", ", missingPermissions)), 
            true
        );
    }

    public void sendFlagList(Player player, Faction faction) {
        // Clone because we may need to remove flags
        HashMap<String, FactionFlag> flagList = (HashMap<String, FactionFlag>)faction.getFlags().clone();
        if (!this.configService.getBoolean("allowNeutrality")) flagList.remove("neutral");
        if (!this.configService.getBoolean("playersChatWithPrefixes") || this.configService.getBoolean("factionsCanSetPrefixColors")) flagList.remove("prefixColor");
        String flagOutput = flagList
            .keySet()
            .stream()
            .map(key -> String.format("%s: %s", key, flagList.get(key).toString()))
            .collect(Collectors.joining(", "));
        player.sendMessage(ChatColor.AQUA + "" + flagOutput);
    }

    /**
     * Method to send an entire Faction a message.
     *
     * @param faction    to send a message to.
     * @param oldMessage old message to send to the Faction.
     * @param newMessage new message to send to the Faction.
     */
    public void messageFaction(Faction faction, String oldMessage, String newMessage) {
        faction.getMemberList()
            .stream()
            .map(Bukkit::getOfflinePlayer)
            .filter(OfflinePlayer::isOnline)
            .map(OfflinePlayer::getPlayer)
            .filter(Objects::nonNull)
            .forEach(player -> this.playerService.sendMessage(player, oldMessage, newMessage, true));
    }

    /**
     * Method to send the entire Server a message.
     *
     * @param oldMessage old message to send to the players.
     * @param newMessage old message to send to the players.
     */
    public void messageServer(String oldMessage, String newMessage) {
        Bukkit.getOnlinePlayers().forEach(player -> this.playerService.sendMessage(player, oldMessage, newMessage, true));
    }

    public void sendCommandNotFoundMessage(CommandSender sender) {
        this.playerService.sendMessage(sender, ChatColor.RED + this.localeService.get("CommandNotRecognized"), "CommandNotRecognized", false);
    }

    public void sendInvalidSyntaxMessage(CommandSender sender, String commandName, String commandSyntax) {
        this.playerService.sendMessage(
            sender, 
            "&c" + this.localeService.getText("InvalidSyntax"),
            Objects.requireNonNull(this.getLanguage().getString("InvalidSyntax"))
                .replace("#command#", commandName)
                .replace("#syntax#", commandSyntax),
            true
        );
    }

    public void sendFactionMembershipRequiredMessage(CommandSender sender) {
        this.playerService.sendMessage(
            sender,
            this.translate("&c" + this.localeService.getText("AlertMustBeInFactionToUseCommand")), 
            Objects.requireNonNull(this.getLanguage().getString("AlertMustBeInFactionToUseCommand")),
            true
        );
    }

    public void sendFactionOwnershipRequiredMessage(CommandSender sender) {
        this.playerService.sendMessage(
            sender,
            this.translate("&c" + this.localeService.getText("AlertMustBeOwnerToUseCommand")), 
            Objects.requireNonNull(this.getLanguage().getString("AlertMustBeOwnerToUseCommand")),
            true
        );
    }

    public void sendFactionOwnershipOrOfficershipRequiredMessage(CommandSender sender) {
        this.playerService.sendMessage(
            sender,
            this.translate("&c" + this.localeService.getText("AlertMustBeOwnerOrOfficeToUseCommand")), 
            Objects.requireNonNull(this.getLanguage().getString("AlertMustBeOwnerOrOfficeToUseCommand")),
            true
        );
    }

    public void sendOnlyPlayersCanUseThisCommandMessage(CommandSender sender) {
        sender.sendMessage(this.translate(this.localeService.getText("OnlyPlayersCanUseCommand")));
    }

}
