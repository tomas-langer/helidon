package io.helidon.mn.webserver;

import javax.inject.Singleton;

import io.helidon.media.jsonb.JsonbSupport;
import io.helidon.mn.ContextStartedEvent;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;

import io.micronaut.context.LifeCycle;
import io.micronaut.context.event.ApplicationEventListener;

@Singleton
public class HelidonWebServer implements LifeCycle<HelidonWebServer>, ApplicationEventListener<ContextStartedEvent> {
    private final Routing defaultRouting;
    private volatile WebServer webServer;

    public HelidonWebServer(Routing defaultRouting) {
        this.defaultRouting = defaultRouting;
    }

    @Override
    public boolean isRunning() {
        return webServer != null && webServer.isRunning();
    }

    @Override
    public HelidonWebServer start() {
        webServer = WebServer.builder()
                .routing(defaultRouting)
                .port(8080)
                .addMediaSupport(JsonbSupport.create())
                .build()
                .start()
                .await();
        return this;
    }

    @Override
    public HelidonWebServer stop() {
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
