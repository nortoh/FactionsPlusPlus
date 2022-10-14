package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MessageService {

    @Inject private MedievalFactions medievalFactions;
    private File languageFile;
    private FileConfiguration language;

    public void createLanguageFile() {
        languageFile = new File(medievalFactions.getDataFolder(), "language.yml");
        if (!languageFile.exists()) medievalFactions.saveResource("language.yml", false);
        language = new YamlConfiguration();
        try {
            language.load(languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            medievalFactions.getLogger().log(Level.WARNING, e.getCause().toString());
        }
    }

    public FileConfiguration getLanguage() {
        return language;
    }


    public void reloadLanguage() {
        if (languageFile.exists()) {
            language = YamlConfiguration.loadConfiguration(languageFile);
        } else {
            createLanguageFile();
        }
    }

    public void saveLanguage() {
        if (languageFile.exists()) {
            try {
                language.save(languageFile);
            } catch (IOException ignored) {
            }
        } else {
            createLanguageFile();
        }
    }


}
