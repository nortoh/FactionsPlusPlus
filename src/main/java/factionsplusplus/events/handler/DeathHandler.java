package factionsplusplus.events.handler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import factionsplusplus.services.ConfigService;
import factionsplusplus.services.DataService;
import factionsplusplus.services.DeathService;
import factionsplusplus.services.PlayerService;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@Singleton
public class DeathHandler implements Listener {
    private final ConfigService configService;
    private final DeathService deathService;
    private final PlayerService playerService;
    private final DataService dataService;

    @Inject
    public DeathHandler(
        ConfigService configService,
        DeathService deathService,
        PlayerService playerService,
        DataService dataService
    ) {
        this.configService = configService;
        this.deathService = deathService;
        this.playerService = playerService;
        this.dataService = dataService;
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
        this.dataService.getPlayer(killer.getUniqueId()).alert("PlayerNotice.PowerIncreased");
        event.getDrops().add(this.deathService.getHead(player));
    }

    private boolean wasPlayersCauseOfDeathAnotherPlayerKillingThem(Player player) {
        return player.getKiller() != null;
    }

    private void decreaseDyingPlayersPower(Player player) {
        double powerLost = this.playerService.revokePowerDueToDeath(player.getUniqueId());
        if (powerLost != 0) {
            this.dataService.getPlayer(player.getUniqueId()).alert("PlayerNotice.PowerDecreasedBy", NamedTextColor.RED, powerLost);
        }
    }
}