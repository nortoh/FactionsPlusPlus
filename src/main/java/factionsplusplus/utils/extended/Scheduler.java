/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.utils.extended;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.models.Faction;
import factionsplusplus.models.PlayerRecord;
import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.FactionService;
import factionsplusplus.services.LocaleService;
import factionsplusplus.services.MessageService;
import factionsplusplus.services.PlayerService;
import factionsplusplus.utils.Logger;
import factionsplusplus.utils.StringUtils;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import factionsplusplus.builders.MessageBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Random;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class Scheduler {
    @Inject private Logger logger;
    @Inject private LocaleService localeService;
    @Inject private FactionsPlusPlus factionsPlusPlus;
    @Inject private ConfigService configService;
    @Inject private PlayerService playerService;
    @Inject private MessageService messageService;
    @Inject private FactionService factionService;
    @Inject private DataService dataService;
    @Inject @Named("adventure") BukkitAudiences adventure;

    @SuppressWarnings("deprecation")
    public void schedulePowerIncrease() {
        this.logger.debug(this.localeService.get("ConsoleAlerts.SchedulingPowerIncrease"));
        final int delay = this.configService.getInt("minutesBeforeInitialPowerIncrease") * 60; // 30 minutes
        final int secondsUntilRepeat = this.configService.getInt("minutesBetweenPowerIncreases") * 60; // 1 hour
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(this.factionsPlusPlus, new Runnable() {
            @Override
            public void run() {
                logger.debug(
                    localeService.get(
                        "ConsoleAlerts.IncreasingThePowerOfEveryPlayer",
                        configService.getInt("powerIncreaseAmount"),
                        configService.getInt("minutesBetweenPowerIncreases")
                    )
                );
                playerService.initiatePowerIncreaseForAllPlayers();
            }
        }, delay * 20L, secondsUntilRepeat * 20L);
    }

    @SuppressWarnings("deprecation")
    public void schedulePowerDecrease() {
        this.logger.debug(this.localeService.get("ConsoleAlerts.SchedulingPowerDecrease"));
        int delay = this.configService.getInt("minutesBetweenPowerDecreases") * 60;
        int secondsUntilRepeat = this.configService.getInt("minutesBetweenPowerDecreases") * 60;
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(factionsPlusPlus, () -> {
            logger.debug(
                localeService.get(
                    "ConsoleAlerts.DecreasingThePowerOfEveryPlayer",
                    configService.getInt("powerDecreaseAmount"),
                    configService.getInt("minutesBetweenPowerDecreases")
                )
            );

            playerService.decreasePowerForInactivePlayers();

            if (this.configService.getBoolean("zeroPowerFactionsGetDisbanded")) {
                this.factionService.disbandAllZeroPowerFactions();
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
                this.dataService.getPlayerRecord(player.getUniqueId()).audience().sendMessage(Component.translatable("FactionNotice.ExcessClaims"), MessageType.SYSTEM);
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