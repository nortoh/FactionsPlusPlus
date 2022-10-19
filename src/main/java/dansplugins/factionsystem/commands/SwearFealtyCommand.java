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
import dansplugins.factionsystem.repositories.FactionRepository;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.builders.CommandBuilder;
import dansplugins.factionsystem.builders.ArgumentBuilder;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
@Singleton
public class SwearFealtyCommand extends Command {

    private final LocaleService localeService;
    private final MessageService messageService;
    private final PersistentData persistentData;
    private final FactionRepository factionRepository;

    @Inject
    public SwearFealtyCommand(
        LocaleService localeService,
        MessageService messageService,
        PersistentData persistentData,
        FactionRepository factionRepository
    ) {
        super(
            new CommandBuilder()
                .withName("swearfelty")
                .withAliases("sf", LOCALE_PREFIX + "CmdSwearFealty")
                .withDescription("Swear fealty to a faction.")
                .expectsPlayerExecution()
                .requiresPermissions("mf.swearfealty")
                .expectsFactionMembership()
                .expectsFactionOwnership()
                .addArgument(
                    "faction name",
                    new ArgumentBuilder()
                        .setDescription("the faction to swear fealty to")
                        .expectsFaction()
                        .consumesAllLaterArguments()
                        .isRequired() 
                )
        );
        this.localeService = localeService;
        this.messageService = messageService;
        this.persistentData = persistentData;
        this.factionRepository = factionRepository;
    }

    public void execute(CommandContext context) {
        final Faction faction = context.getExecutorsFaction();
        final Faction target = context.getFactionArgument("faction name");
        if (!target.hasBeenOfferedVassalization(faction.getName())) {
            context.replyWith("AlertNotOfferedVassalizationBy");
            return;
        }
        // set vassal
        target.addVassal(faction.getName());
        target.removeAttemptedVassalization(faction.getName());

        // set liege
        faction.setLiege(target.getName());

        // inform target faction that they have a new vassal
        this.messageService.messageFaction(
            target,
            this.translate("&a" + this.localeService.getText("AlertFactionHasNewVassal", faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionHasNewVassal"))
                .replace("#name#", faction.getName())
        );

        // inform players faction that they have a new liege
        this.messageService.messageFaction(
            faction,
            this.translate("&a" + this.localeService.getText("AlertFactionHasBeenVassalized", target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionHasBeenVassalized"))
                .replace("#name#", target.getName())
        );
    }

     /**
     * Method to handle tab completion.
     * 
     * @param player who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Player player, String[] args) {
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}