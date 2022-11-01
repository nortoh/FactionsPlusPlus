package factionsplusplus.utils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.DataService;

import org.bukkit.entity.Player;

import java.util.UUID;

@Singleton
public class RelationChecker {
    private final DataService dataService;

    @Inject
    public RelationChecker(DataService dataService) {
        this.dataService = dataService;
    }

    public boolean arePlayersInAFaction(Player player1, Player player2) {
        return this.dataService.isPlayerInFaction(player1) && this.dataService.isPlayerInFaction(player2);
    }


    public boolean arePlayersInSameFaction(Player player1, Player player2) {
        Pair<UUID, UUID> factionIndices = this.getFactionIndices(player1, player2);
        UUID attackersFactionIndex = factionIndices.left();
        UUID victimsFactionIndex = factionIndices.right();
        return this.arePlayersInAFaction(player1, player2) && attackersFactionIndex.equals(victimsFactionIndex);
    }

    public boolean arePlayersFactionsNotEnemies(Player player1, Player player2) {
        Pair<UUID, UUID> factionIndices = this.getFactionIndices(player1, player2);
        UUID attackersFactionIndex = factionIndices.left();
        UUID victimsFactionIndex = factionIndices.right();

        return ! (this.dataService.getFaction(attackersFactionIndex).isEnemy(this.dataService.getFaction(victimsFactionIndex).getID())) &&
                ! (this.dataService.getFaction(victimsFactionIndex).isEnemy(this.dataService.getFaction(attackersFactionIndex).getID()));
    }

    private Pair<UUID, UUID> getFactionIndices(Player player1, Player player2) {
        return Pair.of(this.dataService.getPlayersFaction(player1).getID(), this.dataService.getPlayersFaction(player2).getID());
    }
}