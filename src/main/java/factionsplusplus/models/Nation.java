/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.models.interfaces.Diplomatic;
import factionsplusplus.models.interfaces.Lawful;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class Nation extends Group implements Diplomatic, Lawful {
    @Expose
    protected final List<UUID> allyFactions = new ArrayList<>();
    protected List<UUID> attemptedAlliances = new ArrayList<>();
    @Expose
    protected final List<UUID> enemyFactions = new ArrayList<>();
    protected List<UUID> attemptedTruces = new ArrayList<>();
    @Expose
    protected final List<String> laws = new ArrayList<>();

    @Override
    public void addAlly(UUID allyUUID) {
        if (! this.isAlly(allyUUID)) this.allyFactions.add(allyUUID);
    }

    @Override
    public void removeAlly(UUID allyUUID) {
        if (this.isAlly(allyUUID)) this.allyFactions.remove(allyUUID);
    }

    @Override
    public boolean isAlly(UUID allyUUID) {
        return this.allyFactions.contains(allyUUID);
    }

    @Override
    public List<UUID> getAllies() {
        return this.allyFactions;
    }

    @Override
    public String getAlliesSeparatedByCommas() {
        String allies = "";
        for (int i = 0; i < this.allyFactions.size(); i++) {
            allies = allies + this.allyFactions.get(i);
            if (i != this.allyFactions.size() - 1) {
                allies = allies + ", ";
            }
        }
        return allies;
    }

    @Override
    public void requestAlly(UUID requestedUUID) {
        if (! this.isRequestedAlly(requestedUUID)) this.attemptedAlliances.add(requestedUUID);
    }

    @Override
    public boolean isRequestedAlly(UUID requestedUUID) {
        return this.attemptedAlliances.contains(requestedUUID);
    }

    @Override
    public void removeAllianceRequest(UUID requestedUUID) {
        if (this.isRequestedAlly(requestedUUID)) this.attemptedAlliances.remove(requestedUUID);
    }

    @Override
    public void addEnemy(UUID enemyUUID) {
        if (! this.isEnemy(enemyUUID)) this.enemyFactions.add(enemyUUID);
    }

    @Override
    public void removeEnemy(UUID enemyUUID) {
        if (this.isEnemy(enemyUUID)) this.enemyFactions.remove(enemyUUID);
    }

    @Override
    public boolean isEnemy(UUID enemyUUID) {
        return this.enemyFactions.contains(enemyUUID);
    }

    @Override
    public List<UUID> getEnemyFactions() {
        return this.enemyFactions;
    }

    @Override
    public String getEnemiesSeparatedByCommas() {
        String enemies = "";
        for (int i = 0; i < this.enemyFactions.size(); i++) {
            enemies = enemies + this.enemyFactions.get(i);
            if (i != this.enemyFactions.size() - 1) {
                enemies = enemies + ", ";
            }
        }
        return enemies;
    }

    @Override
    public void requestTruce(UUID requestedUUID) {
        if (! this.isTruceRequested(requestedUUID)) this.attemptedTruces.add(requestedUUID);
    }

    @Override
    public boolean isTruceRequested(UUID requestedUUID) {
        return this.attemptedTruces.contains(requestedUUID);
    }

    @Override
    public void removeRequestedTruce(UUID requestedUUID) {
        if (this.isTruceRequested(requestedUUID)) this.attemptedTruces.remove(requestedUUID);
    }

    @Override
    public List<UUID> getAttemptedTruces() {
        return this.attemptedTruces;
    }

    @Override
    public void addLaw(String newLaw) {
        this.laws.add(newLaw);
    }

    @Override
    public boolean removeLaw(String lawToRemove) {
        if (containsIgnoreCase(this.laws, lawToRemove)) {
            this.laws.remove(lawToRemove);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeLaw(int i) {
        if (this.laws.size() > i) {
            this.laws.remove(i);
            return true;
        }
        return false;
    }

    @Override
    public boolean editLaw(int i, String newString) {
        if (this.laws.size() > i) {
            this.laws.set(i, newString);
            return true;
        }
        return false;
    }

    @Override
    public int getNumLaws() {
        return this.laws.size();
    }

    @Override
    public List<String> getLaws() {
        return this.laws;
    }

    // helper methods ---------------

    private boolean containsIgnoreCase(List<String> list, String str) {
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }
}