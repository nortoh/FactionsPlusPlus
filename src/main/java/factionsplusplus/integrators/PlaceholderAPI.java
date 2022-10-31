/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.integrators;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.ClaimedChunk;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.LocaleService;
import factionsplusplus.services.PlayerService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Singleton
public class PlaceholderAPI extends PlaceholderExpansion {
    private final FactionsPlusPlus factionsPlusPlus;
    private final ConfigService configService;
    private final FactionService factionService;
    private final DataService dataService;
    private final PlayerService playerService;
    private final LocaleService localeService;

    @Inject
    public PlaceholderAPI(
        FactionsPlusPlus factionsPlusPlus,
        ConfigService configService,
        FactionService factionService,
        DataService dataService,
        PlayerService playerService,
        LocaleService localeService
    ) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.configService = configService;
        this.factionService = factionService;
        this.dataService = dataService;
        this.playerService = playerService;
        this.localeService = localeService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return factionsPlusPlus.getName();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", factionsPlusPlus.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return factionsPlusPlus.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String id) {
        id = id.toLowerCase(); // I'm unsure if PlaceholderAPI enforces case, but let's just do it to make sure.
        if (player == null) return null; // We only want to handle Player-Placeholders here.

        final boolean hasFaction = this.dataService.isPlayerInFaction(player);
        final Faction faction = this.dataService.getPlayersFaction(player.getUniqueId());

        // Prerequisites.
        if (id.startsWith("faction_") && ! hasFaction && ! id.equalsIgnoreCase("faction_at_location")) {
            return configService.getString("factionless"); // We don't want Faction-Specific Placeholders to return if they are Factionless!
        }

        switch (id.toLowerCase()) {
            // Faction specific
            case "faction_name": // Name of the faction the player is in
                return faction.getName();
            case "faction_prefix": // Prefix of the faction the player is in
                return faction.getPrefix();
            case "faction_total_claimed_chunks": // Total chunks claimed by the faction the player is in
                return String.valueOf(this.dataService.getClaimedChunksForFaction(faction).size());
            case "faction_cumulative_power": // The cumulative power (power + bonus power) for the faction the player is in
                return String.valueOf(this.factionService.getCumulativePowerLevel(faction));
            case "faction_bonus_power": // The bonus power for the faction the player is in
                return String.valueOf(faction.getBonusPower());
            case "faction_power": // The power (cumulative power - bonus power) for the faction the player is in
                return String.valueOf(this.factionService.getCumulativePowerLevel(faction) - faction.getBonusPower());
            case "faction_ally_count": // The total amount of allies the faction the player is in has
                return String.valueOf(faction.getAllies().size());
            case "faction_enemy_count": // The total amount of enemies the faction the player is in has
                return String.valueOf(faction.getEnemies().size());
            case "faction_gate_count": // The total amount of gates the faction the player is in has
                return String.valueOf(dataService.getFactionsGates(faction).size());
            case "faction_vassal_count": // The total amount of vassals the faction the player is in has
                return String.valueOf(faction.getNumVassals());
            case "faction_liege": // The liege (or N/A if not a liege) of the faction the player is in
                return faction.hasLiege() ? this.dataService.getFaction(faction.getLiege()).getName() : "N/A";
            case "faction_leader": // The leader of the faction the player is in
                return Bukkit.getOfflinePlayer(faction.getOwner().getUUID()).getName();
            case "faction_member_count": // The total amount of members the faction the player is in has
            case "faction_population":
                return String.valueOf(faction.getMemberCount());
            case "faction_officer_count": // The total amount of officers the faction the player is in has
            case "faction_officers":
                return String.valueOf(faction.getOfficerCount());
            case "faction_rank": // The players rank in the faction they are in
                if (faction.isOwner(player.getUniqueId())) return "Owner";
                if (faction.isOfficer(player.getUniqueId())) return "Officer";
                if (faction.isLaborer(player.getUniqueId())) return "Laborer";
                return "Member";
            // Player specific 
            case "faction_player_power": // The total amount of power the player has
            case "player_power":
                return String.valueOf(this.dataService.getPlayerRecord(player.getUniqueId()).getPower());
            case "faction_player_max_power": // The maximum amount of power the player can have
            case "player_max_power":
                return String.valueOf(this.playerService.getMaxPower(player.getUniqueId()));
            case "faction_player_power_full":
            case "player_power_formatted":
                final PlayerRecord playersPowerRecord = this.dataService.getPlayerRecord(player.getUniqueId());
                return playersPowerRecord.getPower() + "/" + this.playerService.getMaxPower(player.getUniqueId());
            case "player_chunk_location": // The Player's location (chunk coordinates), useful for Scoreboards.
                final Chunk chunk = player.getLocation().getChunk();
                return chunk.getX() + ":" + chunk.getZ();
            case "player_location": // The Player's specific location, X:Y:Z which is also useful for Scoreboards.
                final Location location = player.getLocation();
                return location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
            case "player_world": // The name of the world the player is currently in
                return player.getWorld().getName();
            case "player_total_logins": // The total amount of times a Player has logged in.
            case "player_login_count":
            case "player_logins":
                return String.valueOf(this.dataService.getPlayerRecord(player.getUniqueId()).getLogins());
            case "player_session_length": // The total time since their current login. (Days:Hours:Minutes:Seconds) or (Hours:Minutes:Seconds).
                return this.dataService.getPlayerRecord(player.getUniqueId()).getActiveSessionLength();
            case "faction_at_location": // The Faction at the Player's current location. (Wilderness if nothing).
                ClaimedChunk claim = this.dataService.getClaimedChunk(player.getLocation().getChunk());
                if (claim == null) return this.localeService.get("Wilderness");
                return this.dataService.getFaction(claim.getHolder()).getName();
            default: // This is required by PlaceholderAPI if there is no matching Placeholder.
                return null;
        }
    }

}
