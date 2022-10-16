/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.models;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.utils.ColorConversion;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.annotations.Expose;

/**
 * @author Daniel McCoy Stephenson
 * In order to add a new faction flag to this class, the following methods need to be altered:
 * - initializeFlagNames()
 * - initializeFlagValues()
 * - loadMissingFlagsIfNecessary()
 */
public class FactionFlags {
    private final ArrayList<String> flagNames = new ArrayList<>();
    @Expose
    private HashMap<String, Integer> integerValues = new HashMap<>();
    @Expose
    private HashMap<String, Boolean> booleanValues = new HashMap<>();
    @Expose
    private HashMap<String, Double> doubleValues = new HashMap<>();
    @Expose
    private HashMap<String, String> stringValues = new HashMap<>();

    public FactionFlags() {
        this.initializeFlagNames();
    }

    private void initializeFlagNames() { // this is called internally
        this.flagNames.add("mustBeOfficerToManageLand");
        this.flagNames.add("mustBeOfficerToInviteOthers");
        this.flagNames.add("alliesCanInteractWithLand");
        this.flagNames.add("vassalageTreeCanInteractWithLand");
        this.flagNames.add("neutral");
        this.flagNames.add("dynmapTerritoryColor");
        this.flagNames.add("territoryAlertColor");
        this.flagNames.add("prefixColor");
        this.flagNames.add("allowFriendlyFire");
        this.flagNames.add("acceptBonusPower");
        this.flagNames.add("enableMobProtection");
    }

    // TODO: remove dependency here
    public void initializeFlagValues() {
        // this is called externally in Faction.java when a faction is created in-game
        this.booleanValues.put("mustBeOfficerToManageLand", true);
        this.booleanValues.put("mustBeOfficerToInviteOthers", true);
        //this.booleanValues.put("alliesCanInteractWithLand", configService.getBoolean("allowAllyInteraction"));
        //this.booleanValues.put("vassalageTreeCanInteractWithLand", configService.getBoolean("allowVassalageTreeInteraction"));
        this.booleanValues.put("neutral", false);
        this.stringValues.put("dynmapTerritoryColor", "#ff0000");
        //this.stringValues.put("territoryAlertColor", configService.getString("territoryAlertColor"));
        this.stringValues.put("prefixColor", "white");
        this.booleanValues.put("allowFriendlyFire", false);
        this.booleanValues.put("acceptBonusPower", true);
        this.booleanValues.put("enableMobProtection", true);
    }

    // TODO: remove dependency
    public void loadMissingFlagsIfNecessary() {
        // this is called externally in Faction.java when a faction is loaded from save files
        if (!this.booleanValues.containsKey("mustBeOfficerToManageLand")) {
            this.booleanValues.put("mustBeOfficerToManageLand", true);
        }
        if (!this.booleanValues.containsKey("mustBeOfficerToInviteOthers")) {
            this.booleanValues.put("mustBeOfficerToInviteOthers", true);
        }
        /* 
        if (!this.booleanValues.containsKey("alliesCanInteractWithLand")) {
            this.booleanValues.put("alliesCanInteractWithLand", configService.getBoolean("allowAllyInteraction"));
        }
        if (!this.booleanValues.containsKey("vassalageTreeCanInteractWithLand")) {
            this.booleanValues.put("vassalageTreeCanInteractWithLand", configService.getBoolean("allowVassalageTreeInteraction"));
        }*/
        if (!this.booleanValues.containsKey("neutral")) {
            this.booleanValues.put("neutral", false);
        }
        if (!this.stringValues.containsKey("dynmapTerritoryColor")) {
            this.stringValues.put("dynmapTerritoryColor", "#ff0000");
        }
        /*
        if (!this.stringValues.containsKey("territoryAlertColor")) {
            this.stringValues.put("territoryAlertColor", configService.getString("territoryAlertColor"));
        }*/
        if (!this.stringValues.containsKey("prefixColor")) {
            this.stringValues.put("prefixColor", "white");
        }
        if (!this.booleanValues.containsKey("allowFriendlyFire")) {
            this.booleanValues.put("allowFriendlyFire", false);
        }
        if (!this.booleanValues.containsKey("acceptBonusPower")) {
            this.booleanValues.put("acceptBonusPower", true);
        }
        if (!this.booleanValues.containsKey("enableMobProtection")) {
            this.booleanValues.put("enableMobProtection", true);
        }
    }

    public ArrayList<String> getFlagNamesList()
    {
        return this.flagNames;
    }

    public void setFlag(String flag, String value, Player player) {
        if (this.isFlag(flag)) {
            if (this.integerValues.containsKey(flag)) {
                this.integerValues.replace(flag, Integer.parseInt(value));
            } else if (this.booleanValues.containsKey(flag)) {
                this.booleanValues.replace(flag, Boolean.parseBoolean(value));
            } else if (this.doubleValues.containsKey(flag)) {
                this.doubleValues.replace(flag, Double.parseDouble(value));
            } else if (this.stringValues.containsKey(flag)) {

                if (flag.equalsIgnoreCase("dynmapTerritoryColor")) {
                    String hex = value;
                    /*

                                            Hex Color Regex

                            This regex matches #FFF or #FFFFFF respectively.
                            Support values range from a-f/A-F & 0-9, giving
                                        full access to hex color.

                     */
                    if (!hex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                        final String output = ColorConversion.attemptDecode(hex, false);
                        if (output == null) {
                            return;
                        } else hex = output;
                    }
                    this.stringValues.replace(flag, hex);
                    final Color awtColour = Color.decode(hex); // Convert to AWT Color.
                    return;
                }

                this.stringValues.replace(flag, value);
            }

            if (flag.equals("dynmapTerritoryColor")) {
                // update dynmap to reflect color change
            }
        }
        // return something here for flag not found
    }

    public Object getFlag(String flag) {
        if (!isFlag(flag)) {
            return false;
        }

        if (integerValues.containsKey(flag)) {
            return integerValues.get(flag);
        } else if (booleanValues.containsKey(flag)) {
            return booleanValues.get(flag);
        } else if (doubleValues.containsKey(flag)) {
            return doubleValues.get(flag);
        } else if (stringValues.containsKey(flag)) {
            return stringValues.get(flag);
        }
        return null;
    }

    public HashMap<String, Integer> getIntegerValues() {
        return this.integerValues;
    }

    public void setIntegerValues(HashMap<String, Integer> values) {
        this.integerValues = values;
    }

    public HashMap<String, Boolean> getBooleanValues() {
        return this.booleanValues;
    }

    public void setBooleanValues(HashMap<String, Boolean> values) {
        this.booleanValues = values;
    }

    public HashMap<String, Double> getDoubleValues() {
        return this.doubleValues;
    }

    public void setDoubleValues(HashMap<String, Double> values) {
        this.doubleValues = values;
    }

    public HashMap<String, String> getStringValues() {
        return this.stringValues;
    }

    public void setStringValues(HashMap<String, String> values) {
        this.stringValues = values;
    }

    private boolean isFlag(String flag) {
        // this method will likely need to be used to sanitize user input
        return this.flagNames.contains(flag);
    }

    public int getNumFlags() {
        return this.booleanValues.size();
    }

    private String getFlagsSeparatedByCommas() {
        StringBuilder toReturn = new StringBuilder();
        for (String flagName : this.flagNames) {

            /*
            if (flagName.equals("neutral") && !configService.getBoolean("allowNeutrality")) {
                continue;
            }

            if (flagName.equals("prefixColor") && (!configService.getBoolean("playersChatWithPrefixes") || !configService.getBoolean("factionsCanSetPrefixColors"))) {
                continue;
            }
            */

            if (!toReturn.toString().equals("")) {
                toReturn.append(", ");
            }
            if (this.integerValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, this.integerValues.get(flagName)));
            } else if (booleanValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, this.booleanValues.get(flagName)));
            } else if (doubleValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, this.doubleValues.get(flagName)));
            } else if (stringValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, this.stringValues.get(flagName)));
            }
        }
        return toReturn.toString();
    }

    // TODO: implement, probably in a service
    public void sendFlagList(Player player) {
        
    }
}