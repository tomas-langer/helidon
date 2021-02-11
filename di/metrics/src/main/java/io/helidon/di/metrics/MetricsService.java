package io.helidon.di.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import io.helidon.common.context.Contexts;
import io.helidon.config.Config;
import io.helidon.di.ContextStartedEvent;
import io.helidon.di.webserver.RouteBuilders;
import io.helidon.di.webserver.ServiceOrder;
import io.helidon.metrics.MetricsSupport;
import io.helidon.metrics.RegistryFactory;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.annotation.Order;

@Singleton
@Order(ServiceOrder.METRICS)
class MetricsService implements ApplicationEventListener<ContextStartedEvent> {
    private final Config config;
    private final RouteBuilders builders;
    private final MetricsSupport metricsSupport;

    MetricsService(Config config,
                   RouteBuilders builders,
                   MetricsSupport.Builder builder) {

        this.config = config;
        this.builders = builders;
        this.metricsSupport = builder.build();
    }

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        Set<String> vendorMetricsAdded = new HashSet<>();
        metricsSupport.configureVendorMetrics(null, builders.routing());
        vendorMetricsAdded.add(WebServer.DEFAULT_SOCKET_NAME);

        config.get("vendor-metrics-routings")
                .asList(String.class)
                .orElseGet(List::of)
                .forEach(routeName -> {
                    if (vendorMetricsAdded.add(routeName)) {
                        builders.routing(routeName)
                                .ifPresent(it -> metricsSupport.configureVendorMetrics(routeName, it));
                    }
                });

        Routing.Builder routingBuilder = config.get("metrics.routing")
                .asString()
                .flatMap(builders::routing)
                .orElseGet(builders::routing);
        metricsSupport.configureEndpoint(routingBuilder);

        // registry factory is available in global
        Contexts.globalContext().register(RegistryFactory.getInstance());
    }
}
