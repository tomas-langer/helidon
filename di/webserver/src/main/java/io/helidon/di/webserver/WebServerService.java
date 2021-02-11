package io.helidon.di.webserver;

import javax.inject.Singleton;

import io.helidon.di.ContextStartedEvent;
import io.helidon.media.common.MediaContext;
import io.helidon.webserver.WebServer;

import io.micronaut.context.BeanLocator;
import io.micronaut.context.LifeCycle;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.annotation.Order;

@Singleton
@Order(ServiceOrder.SERVER)
class WebServerService implements LifeCycle<WebServerService>, ApplicationEventListener<ContextStartedEvent> {
    private final BeanLocator beanLocator;
    private final WebServer.Builder builder;

    private volatile WebServer webServer;

    protected WebServerService(BeanLocator beanLocator,
                               WebServer.Builder builder,
                               MediaContext mediaContext) {
        this.beanLocator = beanLocator;
        this.builder = builder;
        this.builder.mediaContext(mediaContext);
    }

    @Override
    public boolean isRunning() {
        return webServer != null && webServer.isRunning();
    }

    @Override
    public WebServerService start() {
        beanLocator.getBean(Routes.class)
                .routings()
                .forEach((name, routing) -> {
                    if (WebServer.DEFAULT_SOCKET_NAME.equals(name)) {
                        builder.routing(routing);
                    } else {
                        builder.addNamedRouting(name, routing);
                    }
                });
        webServer = builder
                .build()
                .start()
                .await();
        return this;
    }

    @Override
    public WebServerService stop() {
        if (webServer != null && webServer.isRunning()) {
            webServer.shutdown()
                    .await();
            webServer = null;
        }
        return this;
    }

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        start();
    }
}
