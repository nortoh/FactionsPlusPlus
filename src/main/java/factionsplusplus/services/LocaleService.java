/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package factionsplusplus.services;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.utils.Logger;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Provider;

/**
 * @author Daniel McCoy Stephenson
 */
@Singleton
public class LocaleService {
    public final static String[] SUPPORTED_LOCALES = {"en_US"};

    private final Provider<FactionsPlusPlus> factionsPlusPlus;
    private final String dataPath;

    private File languageDirectory;
    private Logger logger;
    private String defaultLocaleTag = "en_US";
    private TranslationRegistry registry;

    private final Map<String, ResourceBundle> localeBundles = new ConcurrentHashMap<>();

    @Inject
    public LocaleService(
        @Named("dataFolder") String dataPath,
        @Named("defaultLocaleTag") String defaultLocaleTag,
        Provider<FactionsPlusPlus> factionsPlusPlus,
        Logger logger
    ) {
        this.factionsPlusPlus = factionsPlusPlus;
        this.dataPath = dataPath;
        this.logger = logger;
        this.defaultLocaleTag = defaultLocaleTag;
        this.registry = TranslationRegistry.create(Key.key("factionsplusplus", "translations"));
        this.languageDirectory = new File(this.dataPath, "language");
    }

    public Set<String> getMissingLanguageFiles() {
        return Stream.of(SUPPORTED_LOCALES)
            .filter(name -> {
                return ! (new File(this.languageDirectory.getAbsolutePath(), String.format("locale_%s.properties", name))).exists();
            })
            .collect(Collectors.toSet());
    }

    public ResourceBundle getBundleForLocale(String localeTag) {
        if (this.localeBundles.containsKey(localeTag)) return this.localeBundles.get(localeTag);
        try {
            Locale locale = Translator.parseLocale(localeTag);
            ResourceBundle bundle = ResourceBundle.getBundle(
                "locale",
                locale,
                new URLClassLoader(new URL[]{this.languageDirectory.toURI().toURL()}),
                UTF8ResourceBundleControl.get()
            );
            this.registry.registerAll(locale, bundle, true);
            this.localeBundles.put(localeTag, bundle);
            return bundle;
        } catch(Exception e) {
            this.logger.info(String.format("Error loading language file for %s: %s", localeTag, e.getMessage()));
        }
        return null;
    }

    public void createLanguageFile() {
        if (! this.languageDirectory.exists()) this.languageDirectory.mkdirs();
        this.getMissingLanguageFiles().stream().forEach(name -> {
            this.factionsPlusPlus.get().saveResource(String.format(("language/locale_%s.properties"), name), false);
        });
        // Go ahead and cache the default locale
        this.getBundleForLocale(this.defaultLocaleTag);
        this.registry.defaultLocale(Translator.parseLocale(this.defaultLocaleTag));
    }

    public ResourceBundle getLanguage() {
        return this.localeBundles.get(this.defaultLocaleTag);
    }

    public void reloadLanguage() {
        
    }

    public void saveLanguage() {
        
    }

    public Locale getDefaultLocale() {
        return Translator.parseLocale(this.defaultLocaleTag);
    }

    public String get(String key) {
        MessageFormat formatter = this.registry.translate(key, this.getDefaultLocale());
        if (formatter == null) return key;
        return formatter.format(null);
    }

    public String get(String key, Object... arguments) {
        try {
            return this.registry.translate(key, this.getDefaultLocale()).format(arguments);
        } catch(MissingResourceException e) {
            this.logger.error(String.format("Missing translation for %s", key), e);
            return key;
        }
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

    public TranslationRegistry getRegistry() {
        return this.registry;
    }
}
