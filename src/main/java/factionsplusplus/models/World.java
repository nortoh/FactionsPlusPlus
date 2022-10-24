package factionsplusplus.models;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;

import factionsplusplus.models.interfaces.Identifiable;

import com.google.gson.annotations.Expose;

public class World implements Identifiable {
    @Expose
    private UUID uuid;
    @Expose
    private final Map<String, ConfigurationFlag> flags = new HashMap<>();

    public World(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public org.bukkit.World getWorld() {
        return Bukkit.getWorld(this.uuid);
    }

    public Map<String, ConfigurationFlag> getFlags() {
        return this.flags;
    }

    public ConfigurationFlag getFlag(String flagName) {
        return this.flags.get(flagName);
    }
}