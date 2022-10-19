package factionsplusplus.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.utils.RelationChecker;
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
import preponderous.ponder.misc.Pair;

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
        initializeBadPotionTypes();
    }

    @EventHandler()
    public void handle(AreaEffectCloudApplyEvent event) {
        AreaEffectCloud cloud = event.getEntity();
        if (!potionTypeBad(cloud.getBasePotionData().getType())) {
            return;
        }
        Player attacker = getPlayerInStoredCloudPair(cloud);
        List<Player> alliedVictims = getAlliedVictims(event, attacker);
        event.getAffectedEntities().removeAll(alliedVictims);
    }

    @EventHandler()
    public void handle(LingeringPotionSplashEvent event) {
        Player thrower = (Player) event.getEntity().getShooter();
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        Pair<Player, AreaEffectCloud> storedCloud = new Pair<>(thrower, cloud);
        ephemeralData.getActiveAOEClouds().add(storedCloud);
        addScheduledTaskToRemoveCloudFromEphemeralData(cloud, storedCloud);
    }

    @EventHandler()
    public void handle(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();
        if (!wasShooterAPlayer(potion)) {
            return;
        }
        Player attacker = (Player) potion.getShooter();

        for (PotionEffect effect : potion.getEffects()) {
            if (!potionEffectBad(effect.getType())) {
                continue;
            }
            removePotionIntensityIfAnyVictimIsAnAlliedPlayer(event, attacker);
        }
    }

    private void initializeBadPotionTypes() {
        BAD_POTION_TYPES.add(PotionType.INSTANT_DAMAGE);
        BAD_POTION_TYPES.add(PotionType.POISON);
        BAD_POTION_TYPES.add(PotionType.SLOWNESS);
        BAD_POTION_TYPES.add(PotionType.WEAKNESS);

        if (!Bukkit.getVersion().contains("1.12.2")) {
            BAD_POTION_TYPES.add(PotionType.TURTLE_MASTER);
        }
    }

    private boolean potionTypeBad(PotionType type) {
        return BAD_POTION_TYPES.contains(type);
    }


    private boolean potionEffectBad(PotionEffectType effect) {
        return BAD_POTION_EFFECTS.contains(effect);
    }

    private void addScheduledTaskToRemoveCloudFromEphemeralData(AreaEffectCloud cloud, Pair<Player, AreaEffectCloud> storedCloudPair) {
        long delay = cloud.getDuration();
        factionsPlusPlus.getServer().getScheduler().scheduleSyncDelayedTask(factionsPlusPlus, () -> ephemeralData.getActiveAOEClouds().remove(storedCloudPair), delay);
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
            if (arePlayersInFactionAndNotAtWar(attacker, victim)) {
                event.setIntensity(victimEntity, 0);
            }
        }
    }

    private boolean arePlayersInFactionAndNotAtWar(Player attacker, Player victim) {
        return relationChecker.arePlayersInAFaction(attacker, victim) && (relationChecker.arePlayersFactionsNotEnemies(attacker, victim) || relationChecker.arePlayersInSameFaction(attacker, victim));
    }

    private boolean wasShooterAPlayer(ThrownPotion potion) {
        return potion.getShooter() instanceof Player;
    }

    private Player getPlayerInStoredCloudPair(AreaEffectCloud cloud) {
        Pair<Player, AreaEffectCloud> storedCloudPair = getCloudPairStoredInEphemeralData(cloud);
        if (storedCloudPair == null) {
            return null;
        }
        return storedCloudPair.getLeft();
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

            if (bothAreInFactionAndNotAtWar(attacker, potentialVictim)) {
                alliedVictims.add(potentialVictim);
            }
        }
        return alliedVictims;
    }

    private boolean bothAreInFactionAndNotAtWar(Player attacker, Player potentialVictim) {
        return relationChecker.arePlayersInAFaction(attacker, potentialVictim)
                && (relationChecker.arePlayersFactionsNotEnemies(attacker, potentialVictim) || relationChecker.arePlayersInSameFaction(attacker, potentialVictim));
    }

    private Pair<Player, AreaEffectCloud> getCloudPairStoredInEphemeralData(AreaEffectCloud cloud) {
        for (Pair<Player, AreaEffectCloud> storedCloudPair : ephemeralData.getActiveAOEClouds()) {
            if (storedCloudPair.getRight() == cloud) {
                return storedCloudPair;
            }
        }
        return null;
    }
}