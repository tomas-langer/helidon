package io.helidon.examples.mn.basics;

import java.util.concurrent.atomic.AtomicReference;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.helidon.annotation.http.Controller;
import io.helidon.annotation.http.DefaultValue;
import io.helidon.annotation.http.Entity;
import io.helidon.annotation.http.Error;
import io.helidon.annotation.http.Get;
import io.helidon.annotation.http.Header;
import io.helidon.annotation.http.Path;
import io.helidon.annotation.http.PathParam;
import io.helidon.annotation.http.Post;
import io.helidon.annotation.http.QueryParam;
import io.helidon.annotation.http.Status;
import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.HttpException;
import io.helidon.webserver.NotFoundException;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

@Controller
@Path("/greet/controller")
public class GreetController {
    private final AtomicReference<String> message = new AtomicReference<>("Controller");

    @Get
    @Path("/queryparam")
    public void query(@NotBlank @Pattern(regexp = "\\w+") @QueryParam @DefaultValue("defaultName") String name,
                      ServerResponse response) {
        response.send(message.get() + ": " + name);
    }

    @Get
    public void greet(ServerRequest req, ServerResponse res) {
        res.send(message.get());
    }

    @Get
    @Path("/named/{name}")
    public Greeting greetByName(@PathParam @NotBlank @Pattern(regexp = "\\w+") String name,
                                @Header("X-TEST") @DefaultValue("myValue") String header) {
        return new Greeting(message.get() + " " + name + ": " + header);
    }

    @Post
    @Status(Http.Status.CREATED_201)
    public void update(@Entity @Valid Greeting greeting) {
        message.set(greeting.getMessage());
    }

    @Get
    @Path("/reactive/multi")
    public Multi<Greeting> greetings() {
        return Multi.just(new Greeting("first"), new Greeting("second"));
    }

    @Get
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
