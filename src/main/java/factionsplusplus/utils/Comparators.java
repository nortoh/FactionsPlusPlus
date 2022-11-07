package factionsplusplus.utils;

import java.util.Comparator;

import factionsplusplus.models.Faction;

public class Comparators {
    public static final Comparator<Pair<Faction, Double>> FACTIONS_BY_POWER = (pairOne, pairTwo) -> {
        int comparison = Double.compare(pairOne.right(), pairTwo.right()); 
        return Double.compare(0, comparison);
    };
}