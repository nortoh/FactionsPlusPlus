package factionsplusplus.models;

import java.util.UUID;

import org.bukkit.Bukkit;

import factionsplusplus.models.interfaces.Identifiable;

public class World implements Identifiable {
    private UUID uuid;

    public UUID getUUID() {
        return this.uuid;
    }

    public org.bukkit.World getWorld() {
        return Bukkit.getWorld(this.uuid);
    }

}