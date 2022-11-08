package factionsplusplus.data.factories;

import factionsplusplus.models.Duel;
import org.bukkit.entity.Player;

import com.google.inject.assistedinject.Assisted;

public interface DuelFactory {
    Duel create(@Assisted("challenger") Player challenger, @Assisted("challenged") Player challenged, int timeLimit);
}
