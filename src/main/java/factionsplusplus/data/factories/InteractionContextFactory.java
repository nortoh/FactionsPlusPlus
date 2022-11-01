package factionsplusplus.data.factories;

import java.util.UUID;

import factionsplusplus.models.Gate;
import factionsplusplus.models.InteractionContext;
import factionsplusplus.models.InteractionContext.TargetType;
import factionsplusplus.models.InteractionContext.Type;

public interface InteractionContextFactory {
    InteractionContext create(Type type, TargetType targetType, UUID uuid);
    InteractionContext create(Type type, TargetType targetType);
    InteractionContext create(Type type, Gate gate);
    InteractionContext create(Type type);
}
