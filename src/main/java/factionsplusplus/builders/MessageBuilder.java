package factionsplusplus.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import factionsplusplus.builders.interfaces.GenericMessageBuilder;
import net.kyori.adventure.text.Component;

public class MessageBuilder implements GenericMessageBuilder {
    private String messageLocalizationKey;
    private List<String> replacements = new ArrayList<>();

    public MessageBuilder(String messageLocalizationKey) {
        this.messageLocalizationKey = messageLocalizationKey;
    }

    public String getLocalizationKey() {
        return this.messageLocalizationKey;
    }

    public MessageBuilder with(String key, String value) {
        this.replacements.add(value);
        return this;
    }

    public String toString(String baseString) {
        //String newString = baseString;
        //for (Map.Entry<String, String> entry : this.replacements.entrySet()) newString = newString.replace(entry.getKey(), entry.getValue());
        return "NEEDS CHANGED";
    }

    // So we can always iterate
    public List<MessageBuilder> getMessageBuilders() {
        return List.of(this);
    }

    public boolean isMultiBuilder() {
        return false;
    }

    @Override
    public @NotNull Component asComponent() {
        // TODO Auto-generated method stub
        return Component.translatable(this.messageLocalizationKey).args(replacements.stream().map(Component::text).collect(Collectors.toList()));
    }
}