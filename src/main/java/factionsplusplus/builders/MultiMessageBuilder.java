package factionsplusplus.builders;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import net.kyori.adventure.text.Component;

public class MultiMessageBuilder implements GenericMessageBuilder {
    private final List<MessageBuilder> messages = new ArrayList<>();

    public MultiMessageBuilder add(MessageBuilder builder) {
        this.messages.add(builder);
        return this;
    }

    public List<MessageBuilder> getMessageBuilders() {
        return this.messages;
    }

    public boolean isMultiBuilder() {
        return true;
    }

    @Override
    public @NotNull Component asComponent() {
        return Component.translatable("");
    }
}