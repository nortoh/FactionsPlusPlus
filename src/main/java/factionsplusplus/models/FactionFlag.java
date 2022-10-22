package factionsplusplus.models;

import factionsplusplus.constants.FlagType;
import factionsplusplus.utils.ColorConversion;
import factionsplusplus.utils.StringUtils;

import java.awt.Color;

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

    public String set(String value) {
        Object newValue = null;
        switch(this.requiredType) {
            case String:
                newValue = value;
                break;
            case Boolean:
                newValue = StringUtils.parseAsBoolean(value);
                break;
            case Integer:
                newValue = StringUtils.parseAsInteger(value);
                break;
            case Double:
                newValue = StringUtils.parseAsDouble(value);
                break;
            case Color:
                String hex = value;
                if (!hex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                    final String output = ColorConversion.attemptDecode(hex, false);
                    newValue = null;
                }
                break;
            default:
                newValue = value;
                break;
        }
        if (newValue == null) return null;
        this.currentValue = newValue;
        return this.toString();
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