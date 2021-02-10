package io.helidon.di.metrics;

import javax.inject.Named;
import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.metrics.MetricsSupport;
import io.helidon.metrics.RegistryFactory;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import org.eclipse.microprofile.metrics.MetricRegistry;

@Factory
public class MetricsFactory {
    private final MetricsSupport.Builder builder = MetricsSupport.builder();
    private final RegistryFactory registryFactory;

    protected MetricsFactory(Config config) {
        registryFactory = RegistryFactory.getInstance(config);
        builder.registryFactory(registryFactory)
                .config(config.get("metrics"));
    }

    @Singleton
    protected MetricsSupport.Builder builder() {
        return builder;
    }

    @Singleton
    @Primary
    protected MetricRegistry metricRegistry() {
        return registryFactory.getRegistry(MetricRegistry.Type.APPLICATION);
    }

    /**
     *
     * @return the base metrics registry
     */
    @Singleton
    @Named("base")
    MetricRegistry baseRegistry() {
        return registryFactory.getRegistry(MetricRegistry.Type.BASE);
    }

    /**
     *
     * @return the application metrics registry
     */
    @Singleton
    @Named("application")
    MetricRegistry applicationRegistry() {
        return metricRegistry();
    }

    /**
     *
     * @return the vendor metrics registry
     */
    @Singleton
    @Named("vendor")
    MetricRegistry vendorRegistry() {
        return registryFactory.getRegistry(MetricRegistry.Type.VENDOR);
    }
}
