/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.data;

import com.google.inject.Singleton;

import factionsplusplus.objects.domain.Duel;
import factionsplusplus.models.InteractionContext;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import factionsplusplus.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class EphemeralData {
    // Left user, right interaction context (anything that requires clicking a block)
    private final HashMap<UUID, InteractionContext> playersPendingInteraction = new HashMap<>();

    private final ArrayList<UUID> playersInFactionChat = new ArrayList<>();
    private final ArrayList<UUID> adminsBypassingProtections = new ArrayList<>();

    // List of players who made the cloud and the cloud itself in a pair
    private final ArrayList<Pair<Player, AreaEffectCloud>> activeAOEClouds = new ArrayList<>();

    // duels
    private final ArrayList<Duel> duelingPlayers = new ArrayList<>();

    // arraylist getters ---

    public HashMap<UUID, InteractionContext> getPlayersPendingInteraction() {
        return this.playersPendingInteraction;
    }

    public ArrayList<UUID> getPlayersInFactionChat() {
        return playersInFactionChat;
    }

    public ArrayList<UUID> getAdminsBypassingProtections() {
        return adminsBypassingProtections;
    }

    public ArrayList<Pair<Player, AreaEffectCloud>> getActiveAOEClouds() {
        return activeAOEClouds;
    }

    public ArrayList<Duel> getDuelingPlayers() {
        return duelingPlayers;
    }

    // specific getters ---

    public Duel getDuel(Player player, Player target) {
        for (Duel duel : this.getDuelingPlayers()) {
            if (duel.hasPlayer(player) && duel.hasPlayer(target)) {
                return duel;
            }
        }
        return null;
    }

    public boolean isPlayerInFactionChat(Player player) {
        return this.getPlayersInFactionChat().contains(player.getUniqueId());
    }
}