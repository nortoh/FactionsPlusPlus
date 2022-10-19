/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.models.Command;
import dansplugins.factionsystem.models.CommandContext;
import dansplugins.factionsystem.models.Faction;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
@Singleton
public class TransferCommand extends Command {

    private final PlayerService playerService;
    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;

    @Inject
    public TransferCommand(
        PlayerService playerService,
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData
    ) {
        super(
            new CommandBuilder()
                .withName("transfer")
                .withAliases(LOCALE_PREFIX + "CmdTransfer")
                .withDescription("Transfers ownership of your faction to another member.")
                .expectsPlayerExecution()
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .requiresPermissions("mf.transfer")
                .addArgument(
                    "player",
                    new ArgumentBuilder()
                        .setDescription("the member to transfer ownership to")
                        .expectsFactionMember()
                        .isRequired()
                )
        );
        this.playerService = playerService;
        this.localeService = localeService;
        this.messageService = messageService;
        this.persistentData = persistentData;
    }

    public void execute(CommandContext context) {
        OfflinePlayer target = context.getOfflinePlayerArgument("player");
        final UUID targetUUID = target.getUniqueId();
        if (targetUUID.equals(context.getPlayer().getUniqueId())) {
            context.replyWith("CannotTransferToSelf");
            return;
        }

        if (context.getExecutorsFaction().isOfficer(targetUUID)) context.getExecutorsFaction().removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        context.getExecutorsFaction().setOwner(targetUUID);
        context.replyWith(
            this.constructMessage("OwnerShipTransferredTo")
                .with("name", target.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            this.playerService.sendMessage(
                target.getPlayer(),
                "&a" + this.localeService.getText("OwnershipTransferred", context.getExecutorsFaction().getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("'OwnershipTransferred")).replace("#name#", context.getExecutorsFaction().getName()),
                true
            );
        }
    }

    /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        final List<String> membersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(player.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            for (UUID uuid : playerFaction.getMemberList()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    membersInFaction.add(member.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
        return null;
    }
}