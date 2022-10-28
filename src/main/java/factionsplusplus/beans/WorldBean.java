package factionsplusplus.beans;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import factionsplusplus.models.ConfigurationFlag;

import lombok.Data;

@Data
public class WorldBean {
    private UUID id;
    private Map<String, ConfigurationFlag> flags = new HashMap<>();
}
