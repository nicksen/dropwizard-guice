package io.github.nicksen.guice;

import com.google.inject.Injector;
import com.sun.jersey.spi.inject.InjectableProvider;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

public class AutoConfig {

    private final Logger log = LoggerFactory.getLogger(AutoConfig.class);
    private Reflections reflections;

    public AutoConfig(String... basePackages) {
        checkArgument(0 < basePackages.length);

        final ConfigurationBuilder cfgBldr = new ConfigurationBuilder();
        final FilterBuilder filterBuilder = new FilterBuilder();
        for (String pkg : basePackages) {
            cfgBldr.addUrls(ClasspathHelper.forPackage(pkg));
            filterBuilder.include(FilterBuilder.prefix(pkg));
        }

        cfgBldr.filterInputsBy(filterBuilder).setScanners(new SubTypesScanner(), new TypeAnnotationsScanner());
        reflections = new Reflections(cfgBldr);
    }

    public void run(Environment environment, Injector injector) {
        addHealthChecks(environment, injector);
        addProviders(environment, injector);
        addInjectableProviders(environment, injector);
        addResources(environment, injector);
        addTasks(environment, injector);
        addManaged(environment, injector);
    }

    public void initialize(Bootstrap<?> bootstrap, Injector injector) {
        addBundles(bootstrap, injector);
    }

    private void addManaged(Environment environment, Injector injector) {
        final Set<Class<? extends Managed>> managedClasses = reflections.getSubTypesOf(Managed.class);
        for (Class<? extends Managed> managed : managedClasses) {
            environment.lifecycle().manage(injector.getInstance(managed));
            log.info("Added Managed: {}", managed);
        }
    }

    private void addTasks(Environment environment, Injector injector) {
        final Set<Class<? extends Task>> taskClasses = reflections.getSubTypesOf(Task.class);
        for (Class<? extends Task> task : taskClasses) {
            environment.admin().addTask(injector.getInstance(task));
            log.info("Added Task: {}", task);
        }
    }

    private void addHealthChecks(Environment environment, Injector injector) {
        final Set<Class<? extends InjectableHealthCheck>> healthCheckClasses = reflections
                .getSubTypesOf(InjectableHealthCheck.class);
        for (Class<? extends InjectableHealthCheck> healthCheck : healthCheckClasses) {
            final InjectableHealthCheck instance = injector.getInstance(healthCheck);
            environment.healthChecks().register(instance.getName(), instance);
            log.info("Added InjectableHealthCheck: {}", healthCheck);
        }
    }

    private void addInjectableProviders(Environment environment, Injector injector) {
        final Set<Class<? extends InjectableProvider>> injectableProviderClasses = reflections
                .getSubTypesOf(InjectableProvider.class);
        for (Class<? extends InjectableProvider> injectableProvider : injectableProviderClasses) {
            environment.jersey().register(injectableProvider);
            log.info("Added InjectableProvider: {}", injectableProvider);
        }
    }

    private void addProviders(Environment environment, Injector injector) {
        final Set<Class<?>> providerClasses = reflections.getTypesAnnotatedWith(Provider.class);
        for (Class<?> provider : providerClasses) {
            environment.jersey().register(provider);
            log.info("Added Provider: {}", provider);
        }
    }

    private void addResources(Environment environment, Injector injector) {
        final Set<Class<?>> resourceClasses = reflections.getTypesAnnotatedWith(Path.class);
        for (Class<?> resource : resourceClasses) {
            environment.jersey().register(resource);
            log.info("Added Resource: {}", resource);
        }
    }

    @SuppressWarnings("unchecked")
    private void addBundles(Bootstrap<?> bootstrap, Injector injector) {
        final Set<Class<? extends Bundle>> bundleClasses = reflections.getSubTypesOf(Bundle.class);
        for (Class<? extends Bundle> bundle : bundleClasses) {
            bootstrap.addBundle(injector.getInstance(bundle));
            log.info("Added Bundle during bootstrap: {}", bundle);
        }

        final Set<Class<? extends ConfiguredBundle>> configuredBundleClasses = reflections
                .getSubTypesOf(ConfiguredBundle.class);
        for (Class<? extends ConfiguredBundle> configuredBundle : configuredBundleClasses) {
            bootstrap.addBundle(injector.getInstance(configuredBundle));
            log.info("Added ConfiguredBundle during bootstrap: {}", configuredBundle);
        }
    }
}
