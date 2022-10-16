/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.models;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 */
public class Territory {
    @Expose
    protected String holder;

    public String getHolder() {
        return holder;
    }

    public void setHolder(String newHolder) {
        holder = newHolder;
    }
}