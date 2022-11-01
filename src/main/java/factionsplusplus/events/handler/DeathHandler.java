package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DeathService;
import factionsplusplus.services.MessageService;
import factionsplusplus.services.PlayerService;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@Singleton
public class DeathHandler implements Listener {
    private final ConfigService configService;
    private final DeathService deathService;
    private final PlayerService playerService;
    private final MessageService messageService;

    @Inject
    public DeathHandler(
        ConfigService configService,
        DeathService deathService,
        PlayerService playerService,
        MessageService messageService
    ) {
        this.configService = configService;
        this.deathService = deathService;
        this.playerService = playerService;
        this.messageService = messageService;
    }

    @EventHandler()
    public void handle(PlayerDeathEvent event) {
        event.getEntity();
        Player player = event.getEntity();
        if (this.configService.getBoolean("playersLosePowerOnDeath")) {
            this.decreaseDyingPlayersPower(player);
        }
        if (! this.wasPlayersCauseOfDeathAnotherPlayerKillingThem(player)) {
            return;
        }
        Player killer = player.getKiller();
        if (killer == null) {
            return;
        }
        this.playerService.grantPowerDueToKill(killer.getUniqueId());
        this.messageService.sendLocalizedMessage(killer, "PowerLevelHasIncreased");
        event.getDrops().add(this.deathService.getHead(player));
    }

    private boolean wasPlayersCauseOfDeathAnotherPlayerKillingThem(Player player) {
        return player.getKiller() != null;
    }

    private void decreaseDyingPlayersPower(Player player) {
        double powerLost = this.playerService.revokePowerDueToDeath(player.getUniqueId());
        if (powerLost != 0) {
            // TODO: use message service here
            player.sendMessage(ChatColor.RED + "You lost " + powerLost + " power.");
        }
    }
}