/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.data.EphemeralData;
import factionsplusplus.models.Duel;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.RelationChecker;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.projectiles.ProjectileSource;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class DamageHandler implements Listener {
    private final Logger logger;
    private final EphemeralData ephemeralData;
    private final ConfigService configService;
    private final RelationChecker relationChecker;
    private final DataService dataService;

    @Inject
    public DamageHandler(Logger logger, EphemeralData ephemeralData, ConfigService configService, RelationChecker relationChecker, DataService dataService) {
        this.logger = logger;
        this.ephemeralData = ephemeralData;
        this.configService = configService;
        this.relationChecker = relationChecker;
        this.dataService = dataService;
    }

    /**
     * This method disallows PVP between members of the same faction and between factions who are not at war
     * PVP is allowed between factionless players, players who belong to a faction and the factionless, and players whose factions are at war.
     * It also handles damage to entities by players.
     */
    @EventHandler()
    public void handle(EntityDamageByEntityEvent event) {
        Player attacker = this.getAttacker(event);
        Player victim = this.getVictim(event);

        if (attacker == null || victim == null) {
            this.logger.debug("Attacker and/or victim was null in the DamageHandler class.");
            this.handleEntityDamage(attacker, event);
            return;
        }

        this.handlePlayerVersusPlayer(attacker, victim, event);
    }

    /**
     * Cases:
     * 1) Players are dueling
     * 2) Victim is not in a faction or attacker is not in a faction.
     * 3) Players are in the same faction
     * 4) Players are not in the same faction but are not enemies.
     */
    private void handlePlayerVersusPlayer(Player attacker, Player victim, EntityDamageByEntityEvent event) {
        this.logger.debug("Handling damage between players.");

        // case 1
        if (this.arePlayersDueling(attacker, victim)) {
            this.logger.debug("Players are dueling. Ending if necessary.");
            this.endDuelIfNecessary(attacker, victim, event);
            return;
        }

        // case 2
        if (! this.dataService.isPlayerInFaction(attacker) || ! this.dataService.isPlayerInFaction(victim)) {
            this.logger.debug("Attacker or victim is not in a faction. Returning.");
            // allow since factionless don't have PVP restrictions
            return;
        }

        // case 3
        if (this.relationChecker.arePlayersInSameFaction(attacker, victim)){
            this.logger.debug("Players are in the same faction. Handling friendly fire.");
            this.handleFriendlyFire(event, attacker, victim);
            return;
        }

        // case 4
        if (this.relationChecker.arePlayersFactionsNotEnemies(attacker, victim)) {
            this.logger.debug("Players factions are not enemies. Handling non-enemy fire.");
            this.handleNonEnemyFire(event, attacker, victim);
        }
    }

    private void handleEntityDamage(Player attacker, EntityDamageByEntityEvent event) {
        this.logger.debug("Handling entity damage.");
        if (event.getEntity() instanceof Player) {
            this.logger.debug("Entity is an instance of a player. Returning.");
            return;
        }

        // If entity is protected, cancel the event and return
        if (this.isEntityProtected(attacker, event.getEntity())) {
            event.setCancelled(true);
        }
    }

    private boolean isEntityProtected(Player attacker, Entity entity) {
        if (entity instanceof ArmorStand || entity instanceof ItemFrame) return true;

        // If entity isn't on claimed chunk, return false
        if (! this.dataService.isChunkClaimed(entity.getLocation().getChunk())) {
            this.logger.debug("Chunk isn't claimed");
            return false;
        }

        final Faction chunkHolder = this.dataService.getFaction(this.dataService.getClaimedChunk(entity.getLocation().getChunk()).getHolder());

        if (! chunkHolder.getFlag("enableMobProtection").toBoolean()) {
            this.logger.debug("Mob Protection is disabled");
            return false;
        }

        // If entity isn't a living one (like minecarts), return false
        if (! (entity instanceof LivingEntity)) {
            this.logger.debug("Entity isn't a living");
            return false;
        }

        // If attacker is null, return true
        if (attacker == null) {
            this.logger.debug("attacker is null");
            return true;
        }

        final Faction attackerFaction = this.dataService.getPlayersFaction(attacker.getUniqueId());
        // If attacker is factionless or null, return true
        if (attackerFaction == null) {
            this.logger.debug("attacker is factionless");
            return true;
        }

        // If attacker is in faction which owns the chunk, return false
        if (this.dataService.getPlayersFaction(attacker.getUniqueId()) == chunkHolder) {
            this.logger.debug("Attacker is in same faction as Chunkholder");
            return false;
        }

        // If attacker is at war with the faction, return false
        if (attackerFaction.isEnemy(chunkHolder.getID())) return false;

        return true;
    }

    private Player getVictim(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            return (Player) event.getEntity();
        }
        return null;
    }

    private Player getAttacker(EntityDamageByEntityEvent event) {
        if (this.wasDamageWasBetweenPlayers(event)) {
            return (Player) event.getDamager();
        }
        else if (this.wasPlayerWasDamagedByAProjectile(event) && this.wasProjectileShotByPlayer(event)) {
            return (Player) getProjectileSource(event);
        }
        else if (this.isDamagerPlayer(event)) {
            return (Player) event.getDamager();
        }
        return null;
    }

    private ProjectileSource getProjectileSource(EntityDamageByEntityEvent event) {
        Projectile projectile = (Projectile) event.getDamager();
        return projectile.getShooter();
    }

    private boolean wasProjectileShotByPlayer(EntityDamageByEntityEvent event) {
        ProjectileSource projectileSource = getProjectileSource(event);
        return projectileSource instanceof Player;
    }

    private boolean isDamagerPlayer(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Player;
    }

    private void endDuelIfNecessary(Player attacker, Player victim, EntityDamageEvent event) {
        Duel duel = this.ephemeralData.getDuel(attacker, victim);
        if (this.isDuelActive(duel) && this.isVictimDead(victim.getHealth(), event.getFinalDamage())) {
            duel.setLoser(victim);
            duel.finishDuel(false);
            this.ephemeralData.getDuelingPlayers().remove(duel);
            event.setCancelled(true);
        }
    }

    private boolean isVictimDead(double victimHealth, double finalDamage) {
        return victimHealth - finalDamage <= 0;
    }

    private boolean isDuelActive(Duel duel) {
        return duel.getStatus().equals(Duel.DuelState.DUELLING);
    }

    private boolean arePlayersDueling(Player attacker, Player victim) {
        if (attacker == null) {
            return false;
        }
        Duel duel = this.ephemeralData.getDuel(attacker, victim);
        return duel != null;
    }

    private boolean wasPlayerWasDamagedByAProjectile(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Projectile && event.getEntity() instanceof Player;
    }

    private boolean wasDamageWasBetweenPlayers(EntityDamageByEntityEvent event) {
        return event.getDamager() instanceof Player && event.getEntity() instanceof Player;
    }

    /**
     * This method is intended to prevent friendly fire if it is not allowed in the faction.
     */
    private void handleFriendlyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        Faction faction = this.dataService.getPlayersFaction(attacker.getUniqueId());
        boolean friendlyFireAllowed = faction.getFlag("allowFriendlyFire").toBoolean();
        if (! friendlyFireAllowed) {
            event.setCancelled(true);
            this.dataService.getPlayerRecord(attacker.getUniqueId()).alert(Component.translatable("Error.Attack.FactionMember").color(NamedTextColor.RED));
        }
    }

    private void handleNonEnemyFire(EntityDamageByEntityEvent event, Player attacker, Player victim) {
        if (this.configService.getBoolean("warsRequiredForPVP")) {
            event.setCancelled(true);
            this.dataService.getPlayerRecord(attacker.getUniqueId()).alert(Component.translatable("Error.Attack.NotAtWar").color(NamedTextColor.RED));
        }
    }
}