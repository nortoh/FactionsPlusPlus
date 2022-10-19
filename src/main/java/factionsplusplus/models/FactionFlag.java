/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import factionsplusplus.constants.FlagType;
import factionsplusplus.utils.ColorConversion;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.annotations.Expose;

public class FactionFlag {
    @Expose
    private final FlagType requiredType;
    @Expose
    private final Object defaultValue;
    @Expose
    private Object currentValue;

    public FactionFlag(FlagType requiredType, Object defaultValue, Object currentValue) {
        this.requiredType = requiredType;
        this.defaultValue = defaultValue;
        this.currentValue = currentValue;
    }

    public FactionFlag(FlagType requiredType, Object defaultValue) {
        this.requiredType = requiredType;
        this.defaultValue = defaultValue;
        this.currentValue = null;
    }

    public FlagType getRequiredType() {
        return this.requiredType;
    }

    public Object getValue() {
        if (this.currentValue == null) return this.defaultValue;
        return this.currentValue;
    }

    public int toInteger() {
        return (int)this.getValue();
    }

    public double toDouble() {
        return (double)this.getValue();
    }

    public float toFloat() {
        return (float)this.getValue();
    }

    public String toString() {
        if (this.requiredType == FlagType.Boolean) {
            return (this.toBoolean() ? "true" : "false");
        }
        return String.valueOf(this.getValue());
    }

    public Boolean toBoolean() {
        return (Boolean)this.getValue();
    }

    public Color toColor() {
        return Color.decode(this.toString());
    }

    public void set(String value) {
        switch(this.requiredType) {
            case String:
                this.currentValue = value;
                break;
            case Boolean:
                this.currentValue = Boolean.parseBoolean(value);
                break;
            case Integer:
                this.currentValue = Integer.parseInt(value);
                break;
            case Float:
                this.currentValue = Float.parseFloat(value);
                break;
            case Double:
                this.currentValue = Double.parseDouble(value);
                break;
            case Color:
                String hex = value;
                if (!hex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                    final String output = ColorConversion.attemptDecode(hex, false);
                    if (output != null) {
                        this.currentValue = output;
                    }
                }
                break;
            default:
                this.currentValue = value;
                break;
        }
    }

    public Object get() {
        switch(this.requiredType) {
            case String:
                return this.toString();
            case Boolean:
                return this.toBoolean();
            case Integer:
                return this.toInteger();
            case Double:
                return this.toDouble();
            case Float:
                return this.toFloat();
            case Color:
                return this.toColor();
            default:
                return this.getValue();
        }
    }
}