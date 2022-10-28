package factionsplusplus.factories;

import factionsplusplus.models.LockedBlock;

import java.util.UUID;

import com.google.inject.assistedinject.Assisted;

public interface LockedBlockFactory {
    LockedBlock create(
        @Assisted("owner") UUID owner, 
        @Assisted("faction") UUID faction, 
        @Assisted("x") int x, 
        @Assisted("y") int y, 
        @Assisted("z") int z, 
        @Assisted("world") UUID world
    );
}