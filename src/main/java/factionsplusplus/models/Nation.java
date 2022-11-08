/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import factionsplusplus.constants.FactionRelationType;
import factionsplusplus.models.interfaces.Diplomatic;
import factionsplusplus.models.interfaces.Lawful;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Daniel McCoy Stephenson
 */
public class Nation extends Group implements Diplomatic, Lawful {
    protected Map<UUID, FactionRelationType> relations = new ConcurrentHashMap<>();
    protected final List<UUID> attemptedAlliances = Collections.synchronizedList(new ArrayList<>());
    protected final List<UUID> attemptedTruces = Collections.synchronizedList(new ArrayList<>());
    protected Map<UUID, String> laws = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void addAlly(UUID allyUUID) {
        if (! this.isAlly(allyUUID)) this.relations.put(allyUUID, FactionRelationType.Ally);
    }

    @Override
    public void removeAlly(UUID allyUUID) {
        if (this.isAlly(allyUUID)) this.relations.remove(allyUUID);
    }

    @Override
    public boolean isAlly(UUID allyUUID) {
        return this.getRelation(allyUUID) == FactionRelationType.Ally;
    }

    @Override
    public List<UUID> getAllies() {
        return this.relations.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(FactionRelationType.Ally))
            .map(entry -> entry.getKey())
            .toList();
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
        if (! this.isEnemy(enemyUUID)) this.relations.put(enemyUUID, FactionRelationType.Enemy);
    }

    @Override
    public void removeEnemy(UUID enemyUUID) {
        if (this.isEnemy(enemyUUID)) this.relations.remove(enemyUUID);
    }

    @Override
    public boolean isEnemy(UUID enemyUUID) {
        return this.getRelation(enemyUUID) == FactionRelationType.Enemy;
    }

    @Override
    public List<UUID> getEnemies() {
        return this.relations.entrySet()
            .stream()
            .filter(entry -> entry.getValue().equals(FactionRelationType.Enemy))
            .map(entry -> entry.getKey())
            .toList();
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
        this.laws.put(UUID.randomUUID(), newLaw);
    }

    @Override
    public boolean removeLaw(int i) {
        return false;
    }

    @Override
    public boolean editLaw(int i, String newString) {
        return false;
    }

    @Override
    public int getNumLaws() {
        return this.laws.size();
    }

    @Override
    public List<String> getLaws() {
        return this.laws.values().stream().toList();
    }

    public FactionRelationType getRelation(UUID factionUUID) {
        return this.relations.get(factionUUID);
    }

    public FactionRelationType getRelation(Faction faction) {
        return this.getRelation(faction.getUUID());
    }
}