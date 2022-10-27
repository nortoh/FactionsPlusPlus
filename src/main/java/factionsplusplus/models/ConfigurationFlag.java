package factionsplusplus.models;

import factionsplusplus.constants.FlagDataType;
import factionsplusplus.utils.ColorConversion;
import factionsplusplus.utils.StringUtils;

import java.awt.Color;

import com.google.gson.annotations.Expose;
import org.jdbi.v3.core.mapper.reflect.ColumnName;

public class ConfigurationFlag {
    @Expose
    @ColumnName("expected_data_type")
    private FlagDataType requiredType = null;
    @Expose
    private String description = null;
    @Expose
    @ColumnName("default_value")
    private String defaultValue = null;
    @Expose
    @ColumnName("value")
    private String currentValue = null;

    public ConfigurationFlag() { }

    public ConfigurationFlag(FlagDataType requiredType, Object defaultValue, Object currentValue) {
        this.requiredType = requiredType;
        this.defaultValue = defaultValue.toString();
        this.currentValue = currentValue.toString();
    }

    public ConfigurationFlag(FlagDataType requiredType, Object defaultValue) {
        this.requiredType = requiredType;
        this.defaultValue = defaultValue.toString();
        this.currentValue = null;
    }

    public FlagDataType getRequiredType() {
        return this.requiredType;
    }

    public String getDefaultValue() {
        return this.defaultValue;
    }

    public String getValue() {
        if (this.currentValue == null) return this.defaultValue;
        return this.currentValue;
    }

    public int toInteger() {
        return Integer.parseInt(this.getValue());
    }

    public double toDouble() {
        return Double.parseDouble(this.getValue());
    }

    public float toFloat() {
        return Float.parseFloat(this.getValue());
    }

    public String toString() {
        if (this.requiredType == FlagDataType.Boolean) {
            return (this.toBoolean() ? "true" : "false");
        }
        return String.valueOf(this.getValue());
    }

    public Boolean toBoolean() {
        return Boolean.valueOf(this.getValue());
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
                if (! hex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                    final String output = ColorConversion.attemptDecode(hex, false);
                    newValue = output;
                }
                break;
            default:
                newValue = value;
                break;
        }
        if (newValue == null) return null;
        this.currentValue = newValue.toString();
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