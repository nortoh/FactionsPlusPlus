package factionsplusplus.models.interfaces;

import java.util.List;
import java.util.UUID;

/**
 * @author Daniel Stephenson
 */
public interface Feudal {

    // type
    boolean isVassal(UUID uuid);

    boolean isLiege();

    UUID getLiege();

    // liege
    void setLiege(UUID uuid);

    boolean hasLiege();

    boolean isLiege(UUID uuid);

    // vassalage
    void addVassal(UUID uuid);

    void removeVassal(UUID uuid);

    void clearVassals();

    int getNumVassals();

    List<UUID> getVassals();

    void addAttemptedVassalization(UUID uuid);

    boolean hasBeenOfferedVassalization(UUID uuid);

    void removeAttemptedVassalization(UUID uuid);

}
