package factionsplusplus.builders;

import java.util.ArrayList;
import java.util.List;

import factionsplusplus.builders.interfaces.GenericMessageBuilder;

public class MultiMessageBuilder implements GenericMessageBuilder {
    private final List<MessageBuilder> messages = new ArrayList<>();

    public void add(MessageBuilder builder) {
        this.messages.add(builder);
    }

    public List<MessageBuilder> getMessageBuilders() {
        return this.messages;
    }

    public boolean isMultiBuilder() {
        return true;
    }
}