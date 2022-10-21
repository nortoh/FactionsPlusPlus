/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils.extended;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.PersistentData;
import factionsplusplus.models.Faction;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.LocaleService;
import factionsplusplus.services.MessageService;
import factionsplusplus.services.PlayerService;
import factionsplusplus.utils.Logger;
import factionsplusplus.builders.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Random;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class Scheduler {
    @Inject private Logger logger;
    @Inject private LocaleService localeService;
    @Inject private FactionsPlusPlus factionsPlusPlus;
    @Inject private PersistentData persistentData;
    @Inject private ConfigService configService;
    @Inject private PlayerService playerService;
    @Inject private MessageService messageService;
    @Inject private FactionService factionService;
    @Inject private DataService dataService;

    public void scheduleAutosave() {
        this.logger.debug(this.localeService.get("ConsoleAlerts.SchedulingHourlyAutoSave"));
        int delay = this.configService.getInt("secondsBeforeInitialAutosave");
        int secondsUntilRepeat = this.configService.getInt("secondsBetweenAutosaves");
        if (delay == 0 || secondsUntilRepeat == 0) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncRepeatingTask(factionsPlusPlus, new Runnable() {
            @Override
            public void run() {
                logger.debug(localeService.get("ConsoleAlerts.HourlySaveAlert"));
                dataService.save();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    public void schedulePowerIncrease() {
        this.logger.debug(this.localeService.get("ConsoleAlerts.SchedulingPowerIncrease"));
        int delay = this.configService.getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        int secondsUntilRepeat = this.configService.getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(factionsPlusPlus, new Runnable() {
            @Override
            public void run() {
                logger.debug(
                    localeService.get("ConsoleAlerts.IncreasingThePowerOfEveryPlayer")
                        .replace("#amount#", String.valueOf(configService.getInt("powerIncreaseAmount")))
                        .replace("#frequency#", String.valueOf(configService.getInt("minutesBetweenPowerIncreases")))
                );
                persistentData.initiatePowerIncreaseForAllPlayers();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    public void schedulePowerDecrease() {
        this.logger.debug(localeService.get("ConsoleAlerts.SchedulingPowerDecrease"));
        int delay = this.configService.getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = this.configService.getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(factionsPlusPlus, () -> {
            logger.debug(
                localeService.get("ConsoleAlerts.DecreasingThePowerOfEveryPlayer")
                    .replace("#amount#", String.valueOf(configService.getInt("powerDecreaseAmount")))
                    .replace("#frequency#", String.valueOf(configService.getInt("minutesBetweenPowerDecreases")))
            );

            persistentData.decreasePowerForInactivePlayers();

            if (configService.getBoolean("zeroPowerFactionsGetDisbanded")) {
                persistentData.disbandAllZeroPowerFactions();
            }

            for (Player player : this.factionsPlusPlus.getServer().getOnlinePlayers()) {
                informPlayerIfTheirLandIsInDanger(player);
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    private void informPlayerIfTheirLandIsInDanger(Player player) {
        Faction faction = this.dataService.getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (this.isFactionExceedingTheirDemesneLimit(faction)) {
                this.messageService.sendLocalizedMessage(player, "AlertMoreClaimedChunksThanPower");
            }
        }
    }

    private boolean isFactionExceedingTheirDemesneLimit(Faction faction) {
        return (this.dataService.getClaimedChunksForFaction(faction).size() > this.factionService.getCumulativePowerLevel(faction));
    }

    public void scheduleTeleport(Player player, Location destinationLocation) {
        int teleport_delay = this.configService.getInt("teleportDelay");
        this.messageService.sendLocalizedMessage(
            player,
            new MessageBuilder("Teleport")
                .with("time", String.valueOf(teleport_delay))
        );
        DelayedTeleportTask delayedTeleportTask = new DelayedTeleportTask(player, destinationLocation);
        delayedTeleportTask.runTaskLater(this.factionsPlusPlus, (long) teleport_delay * this.getRandomNumberBetween(15, 25));
    }

    private int getRandomNumberBetween(int num1, int num2) {
        Random random = new Random();
        int span = num2 - num1;
        return random.nextInt(span) + num1;
    }

    private class DelayedTeleportTask extends BukkitRunnable {
        private Player player;
        private Location initialLocation;
        private Location destinationLocation;

        public DelayedTeleportTask(Player player, Location destinationLocation) {
            this.player = player;
            this.initialLocation = player.getLocation();
            this.destinationLocation = destinationLocation;
        }

        @Override
        public void run() {
            if (playerHasNotMoved()) {
                teleportPlayer();
            } else {
                messageService.sendLocalizedMessage(player, "TeleportCancelled");
            }
        }

        private boolean playerHasNotMoved() {
            return initialLocation.getX() == player.getLocation().getX() && initialLocation.getY() == player.getLocation().getY() && initialLocation.getZ() == player.getLocation().getZ();
        }

        private void teleportPlayer() {
            this.player.teleport(this.destinationLocation);
        }
    }
}