package io.helidon.examples.di.basics;

import io.helidon.di.annotation.http.Path;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

@Path("/service")
public class GreetService implements Service {
    @Override
    public void update(io.helidon.webserver.Routing.Rules rules) {
        rules.get("/", this::greet);
    }

    private void greet(ServerRequest req, ServerResponse res) {
        res.send("Service");
    }
}
