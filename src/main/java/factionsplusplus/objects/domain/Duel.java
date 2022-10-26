/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.objects.domain;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.EphemeralData;
import factionsplusplus.services.DeathService;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Daniel McCoy Stephenson
 */
public class Duel {
    private final FactionsPlusPlus factionsPlusPlus;
    private final EphemeralData ephemeralData;
    private final DeathService deathService;

    private final Player _challenged;
    private final Player _challenger;
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

    public Duel(
        FactionsPlusPlus factionsPlusPlus,
        EphemeralData ephemeralData,
        DeathService deathService,
        Player challenger,
        Player challenged,
        int limit
    ) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.ephemeralData = ephemeralData;
        this.deathService = deathService;
        _challenger = challenger;
        challengerHealth = challenger.getHealth();
        _challenged = challenged;
        challengedHealth = challenged.getHealth();
        timeLimit = limit;
        duelState = DuelState.INVITED;
    }

    public DuelState getStatus() {
        return this.duelState;
    }

    public void setStatus(DuelState state) {
        this.duelState = state;
    }

    public boolean isChallenged(Player player) {
        return player.equals(this._challenged);
    }

    public Player getChallenged() {
        return this._challenged;
    }

    public boolean isChallenger(Player player) {
        return player.equals(this._challenger);
    }

    public Player getChallenger() {
        return this._challenger;
    }

    public double getChallengerHealth() {
        return this.challengerHealth;
    }

    public double getChallengedHealth() {
        return this.challengedHealth;
    }

    public boolean hasPlayer(Player player) {
        return this._challenged.equals(player) || this._challenger.equals(player);
    }

    public void resetHealth() {
        if (this._challenger != null) {
            this._challenger.setHealth(this.challengerHealth);
        }
        if (this._challenged != null) {
            this._challenged.setHealth(this.challengedHealth);
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
        // TODO: use message service here
        this.getChallenger().sendMessage(String.format(ChatColor.AQUA + "%s has accepted your challenge, the duel has begun!", this._challenged.getName()));
        this.getChallenged().sendMessage(String.format(ChatColor.AQUA + "You have accepted %s's challenge, the duel has begun!", this._challenger.getName()));

        this.challengerHealth = this._challenger.getHealth();
        this.challengedHealth = this._challenged.getHealth();
        this.duelState = DuelState.DUELLING;
        // Announce to nearby players that a duel has started.
        for (Player other : this.factionsPlusPlus.getServer().getOnlinePlayers()) {
            if (other.getLocation().distance(this._challenger.getLocation()) <= this.nearbyPlayerRadius ||
                    other.getLocation().distance(this._challenged.getLocation()) <= this.nearbyPlayerRadius) {
                // TODO: use message service here
                other.sendMessage(String.format(ChatColor.AQUA + "%s has challenged %s to a duel!", this._challenger.getName(), this._challenged.getName()));
            }
        }

        if (bar == null) {
            return;
        }

        this.bar = this.factionsPlusPlus.getServer().createBossBar(String.format(ChatColor.AQUA + "%s vs %s", this._challenger.getName(), this._challenged.getName())
                , BarColor.WHITE, BarStyle.SEGMENTED_20);
        this.bar.setProgress(1);
        this.timeDecrementAmount = 1.0 / this.timeLimit;
        this.bar.addPlayer(_challenger);
        this.bar.addPlayer(_challenged);

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
        this._challenger.setHealth(challengerHealth);
        this._challenged.setHealth(challengedHealth);

        // Remove player damaging effects like fire or poison before ending the duel.
        this._challenged.getActivePotionEffects().clear();
        this._challenger.getActivePotionEffects().clear();

        if (! tied) {
            // Announce winner to nearby players.
            for (Player other : this.factionsPlusPlus.getServer().getOnlinePlayers()) {
                if (other.getLocation().distance(this._challenger.getLocation()) <= this.nearbyPlayerRadius ||
                        other.getLocation().distance(this._challenged.getLocation()) <= this.nearbyPlayerRadius) {
                    // TODO: use message service here
                    other.sendMessage(String.format(ChatColor.AQUA + "%s has defeated %s in a duel!", this.winner.getName(), this.loser.getName()));
                }
            }
            if (this.getWinner().getInventory().firstEmpty() > -1) {
                this.getWinner().getInventory().addItem(this.deathService.getHead(getLoser()));
            } else {
                this.getWinner().getWorld().dropItemNaturally(this.getWinner().getLocation(), Objects.requireNonNull(this.deathService.getHead(this.getLoser())));
            }
        } else {
            for (Player other : this.factionsPlusPlus.getServer().getOnlinePlayers()) {
                if (other.getLocation().distance(this._challenger.getLocation()) <= this.nearbyPlayerRadius ||
                        other.getLocation().distance(this._challenged.getLocation()) <= this.nearbyPlayerRadius) {
                    // TODO: use message service here
                    other.sendMessage(String.format(ChatColor.YELLOW + "%s and %s's duel has ended in a tie.", this._challenger.getName(), this._challenged.getName()));
                }
            }
        }
        if (this.bar == null) {
            return;
        }
        this.bar.removeAll();
        this.factionsPlusPlus.getServer().getScheduler().cancelTask(repeatingTaskId);
        this.ephemeralData.getDuelingPlayers().remove(this);
    }

    public enum DuelState {INVITED, DUELLING, WINNER}
}