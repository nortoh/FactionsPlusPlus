package factionsplusplus.di;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.name.Names;

import factionsplusplus.FactionsPlusPlus;

public class PluginModule extends AbstractModule {
    private final FactionsPlusPlus factionsPlusPlus;

    public PluginModule(FactionsPlusPlus factionsPlusPlus) {
        this.factionsPlusPlus = factionsPlusPlus;
    }

    @Override
    protected void configure() {
        bind(FactionsPlusPlus.class).toInstance(factionsPlusPlus);
        bind(String.class)
            .annotatedWith(Names.named("dataFolder"))
            .toInstance(this.factionsPlusPlus.getStoragePath());
        bind(String.class)
            .annotatedWith(Names.named("pluginVersion"))
            .toInstance(this.factionsPlusPlus.getVersion());

    }

    public Injector createInjector() {
        return Guice.createInjector(this);
    }
}