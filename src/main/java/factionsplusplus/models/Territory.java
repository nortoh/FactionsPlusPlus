/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import java.util.UUID;
import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class Territory {
    @Expose
    protected UUID holder;

    public UUID getHolder() {
        return this.holder;
    }

    public void setHolder(UUID newHolder) {
        this.holder = newHolder;
    }
}