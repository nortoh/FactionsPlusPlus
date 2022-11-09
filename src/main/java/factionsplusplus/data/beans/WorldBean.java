package factionsplusplus.data.beans;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import factionsplusplus.models.ConfigurationFlag;

import lombok.Data;

@Data
public class WorldBean {
    private UUID id;
    private ConcurrentMap<String, ConfigurationFlag> flags = new ConcurrentHashMap<>();
}
