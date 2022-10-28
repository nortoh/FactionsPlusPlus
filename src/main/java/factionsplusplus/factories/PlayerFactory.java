package factionsplusplus.factories;

import factionsplusplus.beans.PlayerBean;
import factionsplusplus.models.PlayerRecord;

import java.util.UUID;

public interface PlayerFactory {
    PlayerRecord create();
    PlayerRecord create(UUID uuid, int initialLogins, double initialPowerLevel);
    PlayerRecord create(PlayerBean bean);
}
