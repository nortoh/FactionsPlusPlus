/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.util.UUID;

import org.jdbi.v3.core.mapper.reflect.ColumnName;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class Territory {
    @Expose
    @ColumnName("faction_id")
    protected UUID holder;

    public UUID getHolder() {
        return this.holder;
    }

    public void setHolder(UUID newHolder) {
        this.holder = newHolder;
    }
}