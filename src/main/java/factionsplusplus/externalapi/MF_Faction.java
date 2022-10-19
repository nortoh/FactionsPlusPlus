/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.externalapi;

import factionsplusplus.models.Faction;
import factionsplusplus.models.FactionFlag;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class MF_Faction {
    private final Faction faction;

    public MF_Faction(Faction f) {
        faction = f;
    }

    public String getName() {
        return faction.getName();
    }

    public String getPrefix() {
        return faction.getPrefix();
    }

    public UUID getOwner() {
        return faction.getOwner();
    }

    public boolean isMember(Player player) {
        return faction.isMember(player.getUniqueId());
    }

    public boolean isOfficer(Player player) {
        return faction.isOfficer(player.getUniqueId());
    }

    public FactionFlag getFlag(String flag) {
        return faction.getFlag(flag);
    }

    public boolean isAlly(UUID factionUUID) {
        return faction.isAlly(factionUUID);
    }

    public boolean isEnemy(UUID factionUUID) {
        return faction.isEnemy(factionUUID);
    }

    /**
     * This should only be used when the external API is not sufficient. It should be noted that the underlying implementation is prone to change.
     *
     * @return The underlying implementation of the faction class.
     */
    @Deprecated
    public Faction getUnderlyingImplementation() {
        return faction;
    }
}