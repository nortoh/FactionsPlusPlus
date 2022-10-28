package factionsplusplus.factories;

import factionsplusplus.beans.WorldBean;
import factionsplusplus.models.World;

import java.util.UUID;

public interface WorldFactory {
    World create();
    World create(UUID uuid);
    World create(WorldBean bean);
}
