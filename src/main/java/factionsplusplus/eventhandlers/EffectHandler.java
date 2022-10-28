package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.utils.RelationChecker;
import factionsplusplus.utils.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class EffectHandler implements Listener {
    private final EphemeralData ephemeralData;
    private final FactionsPlusPlus factionsPlusPlus;
    private final RelationChecker relationChecker;

    private final List<PotionEffectType> BAD_POTION_EFFECTS = Arrays.asList(
            PotionEffectType.BLINDNESS,
            PotionEffectType.CONFUSION,
            PotionEffectType.HARM,
            PotionEffectType.HUNGER,
            PotionEffectType.POISON,
            PotionEffectType.SLOW,
            PotionEffectType.SLOW_DIGGING,
            PotionEffectType.UNLUCK,
            PotionEffectType.WEAKNESS,
            PotionEffectType.WITHER
    );
    private final List<PotionType> BAD_POTION_TYPES = new ArrayList<>();

    @Inject
    public EffectHandler(EphemeralData ephemeralData, FactionsPlusPlus factionsPlusPlus, RelationChecker relationChecker) {
        this.ephemeralData = ephemeralData;
        this.factionsPlusPlus = factionsPlusPlus;
        this.relationChecker = relationChecker;
        this.initializeBadPotionTypes();
    }

    @EventHandler()
    public void handle(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        if (! this.potionTypeBad(cloud.getBasePotionData().getType())) {
            return;
        }
        Player attacker = this.getPlayerInStoredCloudPair(cloud);
        List<Player> alliedVictims = this.getAlliedVictims(event, attacker);
        event.getAffectedEntities().removeAll(alliedVictims);
    }

    @EventHandler()
    public void handle(LingeringPotionSplashEvent event) {
        Player thrower = (Player) event.getEntity().getShooter();
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        Pair<Player, AreaEffectCloud> storedCloud = Pair.of(thrower, cloud);
        this.ephemeralData.getActiveAOEClouds().add(storedCloud);
        this.addScheduledTaskToRemoveCloudFromEphemeralData(cloud, storedCloud);
    }

    @EventHandler()
    public void handle(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (! this.wasShooterAPlayer(potion)) {
            return;
        }
        Player attacker = (Player) potion.getShooter();

        for (PotionEffect effect : potion.getEffects()) {
            if (! this.potionEffectBad(effect.getType())) {
                continue;
            }
            this.removePotionIntensityIfAnyVictimIsAnAlliedPlayer(event, attacker);
        }
    }

    private void initializeBadPotionTypes() {
        this.BAD_POTION_TYPES.add(PotionType.INSTANT_DAMAGE);
        this.BAD_POTION_TYPES.add(PotionType.POISON);
        this.BAD_POTION_TYPES.add(PotionType.SLOWNESS);
        this.BAD_POTION_TYPES.add(PotionType.WEAKNESS);

        if (! Bukkit.getVersion().contains("1.12.2")) {
            this.BAD_POTION_TYPES.add(PotionType.TURTLE_MASTER);
        }
    }

    private boolean potionTypeBad(PotionType type) {
        return this.BAD_POTION_TYPES.contains(type);
    }


    private boolean potionEffectBad(PotionEffectType effect) {
        return this.BAD_POTION_EFFECTS.contains(effect);
    }

    private void addScheduledTaskToRemoveCloudFromEphemeralData(AreaEffectCloud cloud, Pair<Player, AreaEffectCloud> storedCloudPair) {
        long delay = cloud.getDuration();
        this.factionsPlusPlus.getServer().getScheduler().scheduleSyncDelayedTask(this.factionsPlusPlus, () -> ephemeralData.getActiveAOEClouds().remove(storedCloudPair), delay);
    }

    private void removePotionIntensityIfAnyVictimIsAnAlliedPlayer(PotionSplashEvent event, Player attacker) {
        for (LivingEntity victimEntity : event.getAffectedEntities()) {
            if (!(victimEntity instanceof Player)) {
                continue;
            }
            Player victim = (Player) victimEntity;
            if (attacker == victim) {
                continue;
            }
            if (this.arePlayersInFactionAndNotAtWar(attacker, victim)) {
                event.setIntensity(victimEntity, 0);
            }
        }
    }

    private boolean arePlayersInFactionAndNotAtWar(Player attacker, Player victim) {
        return this.relationChecker.arePlayersInAFaction(attacker, victim) && (this.relationChecker.arePlayersFactionsNotEnemies(attacker, victim) || this.relationChecker.arePlayersInSameFaction(attacker, victim));
    }

    private boolean wasShooterAPlayer(ThrownPotion potion) {
        return potion.getShooter() instanceof Player;
    }

    private Player getPlayerInStoredCloudPair(AreaEffectCloud cloud) {
        Pair<Player, AreaEffectCloud> storedCloudPair = this.getCloudPairStoredInEphemeralData(cloud);
        if (storedCloudPair == null) {
            return null;
        }
        return storedCloudPair.left();
    }

    private List<Player> getAlliedVictims(AreaEffectCloudApplyEvent event, Player attacker) {
        List<Player> alliedVictims = new ArrayList<>();
        for (Entity potentialVictimEntity : event.getAffectedEntities()) {
            if (!(potentialVictimEntity instanceof Player)) {
                continue;
            }

            Player potentialVictim = (Player) potentialVictimEntity;

            if (attacker == potentialVictim) {
                continue;
            }

            if (this.bothAreInFactionAndNotAtWar(attacker, potentialVictim)) {
                alliedVictims.add(potentialVictim);
            }
        }
        return alliedVictims;
    }

    private boolean bothAreInFactionAndNotAtWar(Player attacker, Player potentialVictim) {
        return this.relationChecker.arePlayersInAFaction(attacker, potentialVictim)
                && (this.relationChecker.arePlayersFactionsNotEnemies(attacker, potentialVictim) || this.relationChecker.arePlayersInSameFaction(attacker, potentialVictim));
    }

    private Pair<Player, AreaEffectCloud> getCloudPairStoredInEphemeralData(AreaEffectCloud cloud) {
        for (Pair<Player, AreaEffectCloud> storedCloudPair : this.ephemeralData.getActiveAOEClouds()) {
            if (storedCloudPair.right() == cloud) {
                return storedCloudPair;
            }
        }
        return null;
    }
}