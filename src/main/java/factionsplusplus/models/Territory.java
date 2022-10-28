/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

/**
 * @author Daniel McCoy Stephenson
 */
public class Territory {
    @ColumnName("faction_id")
    protected UUID holder;

    public UUID getHolder() {
        return this.holder;
    }

    public void setHolder(UUID newHolder) {
        this.holder = newHolder;
    }
}