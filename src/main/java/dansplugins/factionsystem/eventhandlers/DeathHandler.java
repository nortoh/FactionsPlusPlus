package dansplugins.factionsystem.eventhandlers;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.DeathService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

@Singleton
public class DeathHandler implements Listener {
    private final ConfigService configService;
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final DeathService deathService;

    @Inject
    public DeathHandler(
        ConfigService configService,
        PersistentData persistentData,
        LocaleService localeServiceService,
        DeathService deathService
    ) {
        this.configService = configService;
        this.persistentData = persistentData;
        this.localeService = localeServiceService;
        this.deathService = deathService;
    }

    @EventHandler()
    public void handle(PlayerDeathEvent event) {
        event.getEntity();
        Player player = event.getEntity();
        if (configService.getBoolean("playersLosePowerOnDeath")) {
            decreaseDyingPlayersPower(player);
        }
        if (! wasPlayersCauseOfDeathAnotherPlayerKillingThem(player)) {
            return;
        }
        Player killer = player.getKiller();
        if (killer == null) return;

        PowerRecord record = persistentData.getPlayersPowerRecord(killer.getUniqueId());
        if (record == null) return;

        record.grantPowerDueToKill();
        killer.sendMessage(ChatColor.GREEN + localeService.get("PowerLevelHasIncreased"));
        event.getDrops().add(this.deathService.getHead(killer, player));
    }

    private boolean wasPlayersCauseOfDeathAnotherPlayerKillingThem(Player player) {
        return player.getKiller() != null;
    }

    private void decreaseDyingPlayersPower(Player player) {
        PowerRecord playersPowerRecord = persistentData.getPlayersPowerRecord(player.getUniqueId());
        double powerLost = playersPowerRecord.revokePowerDueToDeath();
        if (powerLost != 0) {
            player.sendMessage(ChatColor.RED + "You lost " + powerLost + " power.");
        }
    }
}