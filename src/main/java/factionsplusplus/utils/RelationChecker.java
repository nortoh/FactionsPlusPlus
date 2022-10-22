package factionsplusplus.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.DataService;
import factionsplusplus.services.PlayerService;

import org.bukkit.entity.Player;
import preponderous.ponder.misc.Pair;

import java.util.UUID;

@Singleton
public class RelationChecker {
    private final PlayerService playerService;
    private final DataService dataService;

    @Inject
    public RelationChecker(PlayerService playerService, DataService dataService) {
        this.playerService = playerService;
        this.dataService = dataService;
    }

    public boolean arePlayersInAFaction(Player player1, Player player2) {
        return this.dataService.isPlayerInFaction(player1) && this.dataService.isPlayerInFaction(player2);
    }

    private boolean playerNotInFaction(Player player) {
        return this.dataService.getPlayersFaction(player.getUniqueId()) == null;
    }

    private boolean playerInFaction(Player player) {
        return this.dataService.isPlayerInFaction(player);
    }

    public boolean arePlayersInSameFaction(Player player1, Player player2) {
        Pair<UUID, UUID> factionIndices = getFactionIndices(player1, player2);
        UUID attackersFactionIndex = factionIndices.getLeft();
        UUID victimsFactionIndex = factionIndices.getRight();
        return arePlayersInAFaction(player1, player2) && attackersFactionIndex.equals(victimsFactionIndex);
    }

    public boolean arePlayersFactionsNotEnemies(Player player1, Player player2) {
        Pair<UUID, UUID> factionIndices = getFactionIndices(player1, player2);
        UUID attackersFactionIndex = factionIndices.getLeft();
        UUID victimsFactionIndex = factionIndices.getRight();

        return !(dataService.getFaction(attackersFactionIndex).isEnemy(dataService.getFaction(victimsFactionIndex).getID())) &&
                !(dataService.getFaction(victimsFactionIndex).isEnemy(dataService.getFaction(attackersFactionIndex).getID()));
    }

    private Pair<UUID, UUID> getFactionIndices(Player player1, Player player2) {
        return new Pair<>(this.dataService.getPlayersFaction(player1).getID(), this.dataService.getPlayersFaction(player2).getID());
    }
}