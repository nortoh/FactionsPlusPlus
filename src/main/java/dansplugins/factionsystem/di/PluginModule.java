package dansplugins.factionsystem.di;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.annotations.PostConstruct;
import dansplugins.factionsystem.factories.FactionFactory;

public class PluginModule extends AbstractModule implements TypeListener {
    private final MedievalFactions medievalFactions;

    public PluginModule(MedievalFactions medievalFactions) {
        this.medievalFactions = medievalFactions;
    }

    @Override
    protected void configure() {
        super.bindListener(Matchers.any(), this);
        bind(MedievalFactions.class).toInstance(medievalFactions);
        install(new FactoryModuleBuilder().build(FactionFactory.class));
    }

    @Override
    public <I> void hear(final TypeLiteral<I> typeLiteral, final TypeEncounter<I> typeEncounter) {
        typeEncounter.register((InjectionListener<I>) i ->
                Arrays.stream(i.getClass().getMethods()).filter(method -> method.isAnnotationPresent(PostConstruct.class))
                        .forEach(method -> invokeMethod(method, i)));
    }

    private void invokeMethod(final Method method, final Object object) {
        try {
            method.invoke(object);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }


    public Injector createInjector() {
        return Guice.createInjector(this);
    }
}