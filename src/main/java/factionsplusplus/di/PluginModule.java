package factionsplusplus.di;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import factionsplusplus.MedievalFactions;

public class PluginModule extends AbstractModule {
    private final MedievalFactions medievalFactions;

    public PluginModule(MedievalFactions medievalFactions) {
        this.medievalFactions = medievalFactions;
    }

    @Override
    protected void configure() {
        bind(MedievalFactions.class).toInstance(medievalFactions);
        bind(String.class)
            .annotatedWith(Names.named("dataFolder"))
            .toInstance(this.medievalFactions.getStoragePath());
        bind(String.class)
            .annotatedWith(Names.named("pluginVersion"))
            .toInstance(this.medievalFactions.getVersion());

    }

    public Injector createInjector() {
        return Guice.createInjector(this);
    }
}