package io.helidon.examples.mn.basics;

import java.util.concurrent.atomic.AtomicReference;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.helidon.annotation.http.Entity;
import io.helidon.annotation.http.Error;
import io.helidon.annotation.http.Status;
import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.NotFoundException;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

@Path("/greet/controller")
public class GreetController {
    private final AtomicReference<String> message = new AtomicReference<>("Controller");

    @GET
    @Path("/queryparam")
    public void query(@NotBlank @Pattern(regexp = "\\w+") @QueryParam("name") @DefaultValue("defaultName") String name,
                      ServerResponse response) {
        response.send(message.get() + ": " + name);
    }

    @GET
    public void greet(@Context ServerRequest req, ServerResponse res) {
        res.send(message.get());
    }

    @GET
    @Path("/named/{name}")
    public Greeting greetByName(@PathParam("name") @NotBlank @Pattern(regexp = "\\w+") String name,
                                @HeaderParam("X-TEST") @DefaultValue("myValue") String header) {
        return new Greeting(message.get() + " " + name + ": " + header);
    }

    @POST
    @Status(Http.Status.CREATED_201)
    public void update(@Entity @Valid Greeting greeting) {
        message.set(greeting.getMessage());
    }

    @GET
    @Path("/reactive/multi")
    public Multi<Greeting> greetings() {
        return Multi.just(new Greeting("first"), new Greeting("second"));
    }

    @GET
    @Path("/reactive/single")
    public Single<Greeting> greeting() {
//        return Single.just(new Greeting(message.get()));
        return Single.error(new NotFoundException("Custom message from single"));
    }

    @Error(value = HttpException.class, global = true)
    public void errorHandler(HttpException error, ServerResponse response) {
        response.status(error.status())
                .send(ErrorObject.create(error));
    }

    @Error(value = ConstraintViolationException.class, global = true)
    @Status(Http.Status.BAD_REQUEST_400)
    public ErrorObject errorHandler(ConstraintViolationException error) {
        return ErrorObject.create(error);
    }
}
