package dansplugins.factionsystem.builders;

import java.util.HashMap;
import java.util.Map;

public class MessageBuilder {
    private String messageLocalizationKey;
    private HashMap<String, String> replacements = new HashMap<>();

    public MessageBuilder(String messageLocalizationKey) {
        this.messageLocalizationKey = messageLocalizationKey;
    }

    public String getLocalizationKey() {
        return this.messageLocalizationKey;
    }

    public MessageBuilder with(String key, String value) {
        this.replacements.put(String.format("#%s#", key), value);
        return this;
    }

    public String toString(String baseString) {
        String newString = baseString;
        for (Map.Entry<String, String> entry : this.replacements.entrySet()) newString = newString.replace(entry.getKey(), entry.getValue());
        return newString;
    }
}