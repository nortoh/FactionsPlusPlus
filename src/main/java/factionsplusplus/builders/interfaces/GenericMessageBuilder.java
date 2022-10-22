package factionsplusplus.builders.interfaces;

import java.util.List;

import factionsplusplus.builders.MessageBuilder;

public interface GenericMessageBuilder {
    boolean isMultiBuilder();
    List<MessageBuilder> getMessageBuilders();
}