/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models.interfaces;

import java.util.List;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public interface Diplomatic {

    // allies
    void addAlly(UUID uuid);

    void removeAlly(UUID uuid);

    boolean isAlly(UUID uuid);

    List<UUID> getAllies();

    // requests
    void requestAlly(UUID uuid);

    boolean isRequestedAlly(UUID uuid);

    void removeAllianceRequest(UUID uuid);

    // enemies
    void addEnemy(UUID uuid);

    void removeEnemy(UUID uuid);

    boolean isEnemy(UUID uuid);

    List<UUID> getEnemies();

    // truces
    void requestTruce(UUID uuid);

    boolean isTruceRequested(UUID uuid);

    void removeRequestedTruce(UUID uuid);

    List<UUID> getAttemptedTruces();
}