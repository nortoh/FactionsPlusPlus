package factionsplusplus.data.factories;

import factionsplusplus.data.beans.WorldBean;
import factionsplusplus.models.World;

import java.util.UUID;

public interface WorldFactory {
    World create();
    World create(UUID uuid);
    World create(WorldBean bean);
}
