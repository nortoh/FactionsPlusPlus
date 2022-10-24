package factionsplusplus.utils;

import java.util.Comparator;

import factionsplusplus.models.Faction;

public class Comparators {
    public static final Comparator<Pair<Faction, Integer>> FACTIONS_BY_POWER = (pairOne, pairTwo) -> {
        int comparison = Integer.compare(pairOne.right(), pairTwo.right()); 
        return Integer.compare(0, comparison);
    };
}