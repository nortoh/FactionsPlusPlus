package factionsplusplus.data.factories;

import factionsplusplus.data.beans.PlayerBean;
import factionsplusplus.models.FPPPlayer;

import java.util.UUID;

public interface PlayerFactory {
    FPPPlayer create();
    FPPPlayer create(UUID uuid, int initialLogins, double initialPowerLevel);
    FPPPlayer create(PlayerBean bean);
}
