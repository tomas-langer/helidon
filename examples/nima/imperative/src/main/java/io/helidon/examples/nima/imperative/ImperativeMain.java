package io.helidon.examples.nima.imperative;

import io.helidon.nima.Nima;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

/**
 * Main class of this example, starts the server.
 */
public class ImperativeMain {
    public static void main(String[] args) {
        WebServer.create(ws -> ws.config(Nima.config())
                        .routing(ImperativeMain::routing))
                .start();
    }

    private static void routing(HttpRouting.Builder routing) {
        routing.get("/", (req, res) -> res.send("Hello World!"));
    }
}
