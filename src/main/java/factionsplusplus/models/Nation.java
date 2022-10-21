/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.models.interfaces.Diplomatic;
import factionsplusplus.models.interfaces.Identifiable;
import factionsplusplus.models.interfaces.Lawful;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class Nation extends Group implements Diplomatic, Lawful, Identifiable {
    @Expose
    protected final UUID uuid = UUID.randomUUID();
    @Expose
    protected final List<UUID> allyFactions = new ArrayList<>();
    protected final List<UUID> attemptedAlliances = new ArrayList<>();
    @Expose
    protected final List<UUID> enemyFactions = new ArrayList<>();
    protected final List<UUID> attemptedTruces = new ArrayList<>();
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
    public UUID getUUID() {
        return this.uuid;
    }

    @Override
    public String getAlliesSeparatedByCommas() {
        String allies = "";
        for (int i = 0; i < allyFactions.size(); i++) {
            allies = allies + allyFactions.get(i);
            if (i != allyFactions.size() - 1) {
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
        for (int i = 0; i < enemyFactions.size(); i++) {
            enemies = enemies + enemyFactions.get(i);
            if (i != enemyFactions.size() - 1) {
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
    public void addLaw(String newLaw) {
        laws.add(newLaw);
    }

    @Override
    public boolean removeLaw(String lawToRemove) {
        if (containsIgnoreCase(laws, lawToRemove)) {
            laws.remove(lawToRemove);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeLaw(int i) {
        if (laws.size() > i) {
            laws.remove(i);
            return true;
        }
        return false;
    }

    @Override
    public boolean editLaw(int i, String newString) {
        if (laws.size() > i) {
            laws.set(i, newString);
            return true;
        }
        return false;
    }

    @Override
    public int getNumLaws() {
        return laws.size();
    }

    @Override
    public List<String> getLaws() {
        return laws;
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

    private void removeIfContainsIgnoreCase(List<String> list, String str) {
        String toRemove = "";
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                toRemove = string;
                break;
            }
        }
        list.remove(toRemove);
    }
}