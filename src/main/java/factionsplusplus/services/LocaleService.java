/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.FactionsPlusPlus;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import javax.inject.Provider;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class LocaleService {
    private final Provider<FactionsPlusPlus> factionsPlusPlus;
    private final String dataPath;

    private File languageFile;
    private FileConfiguration language;

    @Inject
    public LocaleService(@Named("dataFolder") String dataPath, Provider<FactionsPlusPlus> factionsPlusPlus) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.dataPath = dataPath;
        this.createLanguageFile();
    }

    public void createLanguageFile() {
        this.languageFile = new File(this.dataPath, "language.yml");
        if (!this.languageFile.exists()) this.factionsPlusPlus.get().saveResource("language.yml", false);
        this.language = new YamlConfiguration();
        try {
            this.language.load(this.languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            this.factionsPlusPlus.get().getLogger().log(Level.WARNING, e.getCause().toString());
        }
    }

    public FileConfiguration getLanguage() {
        return this.language;
    }

    public void reloadLanguage() {
        if (languageFile.exists()) {
            this.language = YamlConfiguration.loadConfiguration(this.languageFile);
        } else {
            this.createLanguageFile();
        }
    }

    public void saveLanguage() {
        if (this.languageFile.exists()) {
            try {
                this.language.save(this.languageFile);
            } catch (IOException ignored) {
            }
        } else {
            this.createLanguageFile();
        }
    }

    public String get(String key) {
        return this.getLanguage().getString(key);
    }

    public List<String> getStrings(String key) {
        return this.getLanguage().getStringList(key);
    }

    /**
     * Method to obtain text from a key with replacements.
     *
     * @param key          to obtain.
     * @param replacements to replace within the message using {@link String#format(String, Object...)}.
     * @return String message
     */
    public String getText(String key, Object... replacements) {
        return String.format(this.get(key), replacements);
    }
}
