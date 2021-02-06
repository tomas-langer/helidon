package io.helidon.examples.mn.basics;

import javax.inject.Singleton;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

@Singleton
public class GreetService implements Service {
    @Override
    public void update(io.helidon.webserver.Routing.Rules rules) {
        rules.get("/", this::greet);
    }

    private void greet(ServerRequest req, ServerResponse res) {
        res.send("Service");
    }
}
