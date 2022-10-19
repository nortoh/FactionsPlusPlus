package factionsplusplus.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.PersistentData;
import factionsplusplus.services.PlayerService;

import org.bukkit.entity.Player;
import preponderous.ponder.misc.Pair;

import java.util.UUID;

@Singleton
public class RelationChecker {
    private final PersistentData persistentData;
    private final PlayerService playerService;

    @Inject
    public RelationChecker(PersistentData persistentData, PlayerService playerService) {
        this.persistentData = persistentData;
        this.playerService = playerService;
    }

    public boolean arePlayersInAFaction(Player player1, Player player2) {
        return persistentData.isInFaction(player1.getUniqueId()) && persistentData.isInFaction(player2.getUniqueId());
    }

    public boolean playerNotInFaction(Player player) {
        return persistentData.getPlayersFaction(player.getUniqueId()) == null;
    }

    public boolean playerInFaction(Player player) {
        return persistentData.isInFaction(player.getUniqueId());
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

        return !(persistentData.getFactionByID(attackersFactionIndex).isEnemy(persistentData.getFactionByID(victimsFactionIndex).getID())) &&
                !(persistentData.getFactionByID(victimsFactionIndex).isEnemy(persistentData.getFactionByID(attackersFactionIndex).getID()));
    }

    private Pair<UUID, UUID> getFactionIndices(Player player1, Player player2) {
        return new Pair<>(this.playerService.getPlayerFaction(player1).getID(), this.playerService.getPlayerFaction(player2).getID());
    }
}