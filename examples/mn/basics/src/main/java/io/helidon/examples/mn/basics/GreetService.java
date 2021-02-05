package io.helidon.examples.mn.basics;

import io.helidon.annotation.http.Controller;
import io.helidon.annotation.http.Path;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

@Controller
@Path("/greet/service")
public class GreetService implements Service {
    @Override
    public void update(io.helidon.webserver.Routing.Rules rules) {
        rules.get("/", this::greet);
    }

    private void greet(ServerRequest req, ServerResponse res) {
        res.send("Service");
    }
}
