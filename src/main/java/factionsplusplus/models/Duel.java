/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.services.DataService;
import factionsplusplus.services.DeathService;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

import java.util.Arrays;
import java.util.Objects;

public class Duel  {
    private final FactionsPlusPlus factionsPlusPlus;
    private final EphemeralData ephemeralData;
    private final DeathService deathService;
    private final BukkitAudiences adventure;

    private final FPPPlayer _challenged;
    private final FPPPlayer _challenger;
    private final float nearbyPlayerRadius = 64;
    private final double timeLimit;
    private DuelState duelState;
    private BossBar bar = null;
    private double challengedHealth;
    private double challengerHealth;
    private Player winner = null;
    private Player loser = null;
    private int repeatingTaskId = 0;
    private double timeDecrementAmount = 0;

    @AssistedInject
    public Duel(
        FactionsPlusPlus factionsPlusPlus,
        EphemeralData ephemeralData,
        DeathService deathService,
        DataService dataService,
        @Named("adventure") BukkitAudiences adventure,
        @Assisted("challenger") Player challenger,
        @Assisted("challenged") Player challenged,
        @Assisted int limit
    ) {
        this.adventure = adventure;
        this.factionsPlusPlus = factionsPlusPlus;
        this.ephemeralData = ephemeralData;
        this.deathService = deathService;
        this._challenger = dataService.getPlayer(challenger.getUniqueId());
        this.challengerHealth = challenger.getHealth();
        this._challenged = dataService.getPlayer(challenged.getUniqueId());
        this.challengedHealth = challenged.getHealth();
        this.timeLimit = limit;
        this.duelState = DuelState.INVITED;
    }

    public DuelState getStatus() {
        return this.duelState;
    }

    public void setStatus(DuelState state) {
        this.duelState = state;
    }

    public boolean isChallenged(Player player) {
        return player.equals(this.getChallenged());
    }

    public Player getChallenged() {
        return this._challenged.toBukkitPlayer();
    }

    public boolean isChallenger(Player player) {
        return player.equals(this.getChallenger());
    }

    public Player getChallenger() {
        return this._challenger.toBukkitPlayer();
    }

    public double getChallengerHealth() {
        return this.challengerHealth;
    }

    public double getChallengedHealth() {
        return this.challengedHealth;
    }

    public boolean hasPlayer(Player player) {
        return this.getChallenged().equals(player) || this.getChallenger().equals(player);
    }

    public void resetHealth() {
        if (this._challenger != null) {
            this.getChallenger().setHealth(this.challengerHealth);
        }
        if (this._challenged != null) {
            this.getChallenged().setHealth(this.challengedHealth);
        }
    }

    public Player getWinner() {
        return this.winner;
    }

    public void setWinner(Player player) {
        this.duelState = DuelState.WINNER;
        this.winner = player;
        if (isChallenger(player)) {
            this.loser = this.getChallenged();
        } else {
            this.loser = this.getChallenger();
        }
    }

    public Player getLoser() {
        return this.loser;
    }

    public void setLoser(Player player) {
        this.duelState = DuelState.WINNER;
        this.loser = player;
        if (this.isChallenger(player)) {
            winner = this.getChallenged();
        } else {
            winner = this.getChallenger();
        }
    }

    public void acceptDuel() {
        // Participants that the challenged was accepted and that it's game-on.
        this._challenger.alert("PlayerNotice.Duel.Accepted.Source", NamedTextColor.AQUA, this.getChallenged().getName());
        this._challenged.alert("PlayerNotice.Duel.Accepted.Target", NamedTextColor.AQUA, this.getChallenger().getName());

        this.challengerHealth = this.getChallenger().getHealth();
        this.challengedHealth = this.getChallenged().getHealth();
        this.duelState = DuelState.DUELLING;
        // Announce to nearby players that a duel has started.
        this.sendNearbyPlayersMessage("PlayerNotice.Duel.Accepted.Nearby", NamedTextColor.AQUA, this.getChallenger().getName(), this.getChallenged().getName());

        if (bar == null) {
            return;
        }

        this.bar = Bukkit.getServer().createBossBar(String.format(ChatColor.AQUA + "%s vs %s", this.getChallenger().getName(), this.getChallenged().getName())
                , BarColor.WHITE, BarStyle.SEGMENTED_20);
        this.bar.setProgress(1);
        this.timeDecrementAmount = 1.0 / this.timeLimit;
        this.bar.addPlayer(this.getChallenger());
        this.bar.addPlayer(this.getChallenged());

        repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.factionsPlusPlus, new Runnable() {
            @Override
            public void run() {
                double progress = bar.getProgress() - timeDecrementAmount;
                if (progress <= 0) {
                    bar.setProgress(0);
                    finishDuel(true);
                } else {
                    bar.setProgress(progress);
                }
            }
        }, 20, 20);
    }

    public void finishDuel(boolean tied) {
        this.getChallenger().setHealth(challengerHealth);
        this.getChallenged().setHealth(challengedHealth);

        // Remove player damaging effects like fire or poison before ending the duel.
        this.getChallenged().getActivePotionEffects().clear();
        this.getChallenger().getActivePotionEffects().clear();

        if (! tied) {
            // Announce winner to nearby players.
            this.sendNearbyPlayersMessage("PlayerNotice.Duel.Defeated.Nearby", NamedTextColor.AQUA, this.winner.getName(), this.loser.getName());
            if (this.getWinner().getInventory().firstEmpty() > -1) {
                this.getWinner().getInventory().addItem(this.deathService.getHead(getLoser()));
            } else {
                this.getWinner().getWorld().dropItemNaturally(this.getWinner().getLocation(), Objects.requireNonNull(this.deathService.getHead(this.getLoser())));
            }
        } else {
            this.sendNearbyPlayersMessage("PlayerNotice.Duel.Tied.Nearby", NamedTextColor.YELLOW, this.getChallenger().getName(), this.getChallenged().getName());
        }
        if (this.bar == null) {
            return;
        }
        this.bar.removeAll();
        this.factionsPlusPlus.getServer().getScheduler().cancelTask(repeatingTaskId);
        this.ephemeralData.getDuelingPlayers().remove(this);
    }
    
    public void sendNearbyPlayersMessage(String localizationKey, TextColor color, Object... args) {
        this.adventure.filter(sender -> {
            if (sender instanceof Player) {
                final Player player = (Player)sender;
                return 
                    player.getLocation().distance(this.getChallenger().getLocation()) <= this.nearbyPlayerRadius ||
                    player.getLocation().distance(this.getChallenged().getLocation()) <= this.nearbyPlayerRadius;
            }
            return false;
        }).sendMessage(
            Component.translatable(localizationKey).color(color).args(Arrays.stream(args).map(argument -> Component.text(argument.toString())).toList())
        );
    }

    public enum DuelState {INVITED, DUELLING, WINNER}
}