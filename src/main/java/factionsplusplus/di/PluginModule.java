package factionsplusplus.di;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import factionsplusplus.FactionsPlusPlus;
import factionsplusplus.data.factories.*;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

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
        bind(BukkitAudiences.class)
            .annotatedWith(Names.named("adventure"))
            .toInstance(this.factionsPlusPlus.getAdventure());
        bind(String.class)
            .annotatedWith(Names.named("defaultLocaleTag"))
            .toInstance(this.factionsPlusPlus.getDefaultLocaleTag());
        install(new FactoryModuleBuilder().build(InteractionContextFactory.class));
        install(new FactoryModuleBuilder().build(FactionFactory.class));
        install(new FactoryModuleBuilder().build(LockedBlockFactory.class));
        install(new FactoryModuleBuilder().build(WorldFactory.class));
        install(new FactoryModuleBuilder().build(WarFactory.class));
        install(new FactoryModuleBuilder().build(PlayerFactory.class));
        install(new FactoryModuleBuilder().build(DuelFactory.class));
    }

    public Injector createInjector() {
        return Guice.createInjector(
            this
        );
    }
}