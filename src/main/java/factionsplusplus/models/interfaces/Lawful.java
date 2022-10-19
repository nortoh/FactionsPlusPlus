/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.models.interfaces;

import java.util.ArrayList;

/**
 * @author Daniel McCoy Stephenson
 */
public interface Lawful {
    void addLaw(String newLaw);

    boolean removeLaw(String lawToRemove);

    boolean removeLaw(int i);

    boolean editLaw(int i, String newString);

    int getNumLaws();

    ArrayList<String> getLaws();
}