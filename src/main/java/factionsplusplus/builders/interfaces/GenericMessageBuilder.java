package factionsplusplus.builders.interfaces;

import java.util.List;

import factionsplusplus.builders.MessageBuilder;
import net.kyori.adventure.text.ComponentLike;

public interface GenericMessageBuilder extends ComponentLike {
    boolean isMultiBuilder();
    List<MessageBuilder> getMessageBuilders();
}