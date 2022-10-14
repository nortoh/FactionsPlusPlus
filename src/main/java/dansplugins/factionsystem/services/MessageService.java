package dansplugins.factionsystem.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.utils.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

@Singleton
public class MessageService {

    private final MedievalFactions medievalFactions;
    private File languageFile;
    private FileConfiguration language;

    @Inject
    public MessageService(MedievalFactions medievalFactions) {
        this.medievalFactions = medievalFactions;
        this.createLanguageFile();
    }

    public void createLanguageFile() {
        this.languageFile = new File(this.medievalFactions.getDataFolder(), "language.yml");
        if (!this.languageFile.exists()) this.medievalFactions.saveResource("language.yml", false);
        this.language = new YamlConfiguration();
        try {
            this.language.load(this.languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            this.medievalFactions.getLogger().log(Level.WARNING, e.getCause().toString());
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


}
