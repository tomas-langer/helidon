package io.helidon.examples.di.basics;

import java.util.concurrent.atomic.AtomicReference;

import javax.validation.ConstraintViolationException;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.helidon.di.annotation.http.DefaultValue;
import io.helidon.di.annotation.http.Entity;
import io.helidon.di.annotation.http.ErrorHandle;
import io.helidon.di.annotation.http.GET;
import io.helidon.di.annotation.http.HeaderParam;
import io.helidon.di.annotation.http.Nonblocking;
import io.helidon.di.annotation.http.POST;
import io.helidon.di.annotation.http.Path;
import io.helidon.di.annotation.http.PathParam;
import io.helidon.di.annotation.http.QueryParam;
import io.helidon.di.annotation.http.Status;
import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.NotFoundException;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

import org.eclipse.microprofile.metrics.annotation.SimplyTimed;

// TODO try injecting a bean into parameters
@Path("/greet")
public class GreetController {
    private final AtomicReference<String> message = new AtomicReference<>("Hello");

    private final PrototypeBean pb;
    private final SingletonBean sb;

    protected GreetController(PrototypeBean pb, SingletonBean sb) {
        this.pb = pb;
        this.sb = sb;
    }

    /*
     * Implicitly non-blocking routes
     */

    @GET
    @SimplyTimed
    // implicitly non-blocking (Handler signature)
    public void nonBlocking1(ServerRequest req, ServerResponse res) {
        res.send(message.get());
    }

    @GET
    @Path("/nb2")
    // implicitly non-blocking (void return type, parameter of type ServerResponse)
    public void nonBlocking2(ServerResponse res) {
        res.send(message.get());
    }

    @GET
    @Path("/nb3")
    // implicitly non-blocking (returns a completion stage / publisher)
    public Single<String> nonBlocking3() {
        return Single.just(message.get());
    }

    @GET
    @Path("/nb4")
    // implicitly non-blocking (returns a publisher)
    public Multi<String> nonBlocking4() {
        return Multi.just(message.get(), "second");
    }

    /*
     * Implicitly blocking routes
     */
    @GET
    @Path("/b1")
    // implicitly blocking - return type other than void or publisher/completion stage
    public String blocking1(@QueryParam String name) {
        return "Hello " + name;
    }

    @GET
    @Path("/b2")
    // implicitly blocking - return type void, ServerResponse is not a parameter
    public void blocking2(@QueryParam String name) {
    }

    /*
     * Explicitly non-blocking
     */
    @GET
    @Path("/enb1")
    @Nonblocking
    public String exNb1(@QueryParam String name) {
        return "Hello " + name;
    }

    @GET
    @Path("/enb2")
    @Nonblocking
    public void exNb2(@QueryParam String name) {
    }

    /*
     * Other
     */
    @GET
    @Path("/pb/field")
    public PrototypeBean pb() {
        return pb;
    }

    @GET
    @Path("/pb/param")
    public PrototypeBean pb(PrototypeBean pb) {
        return pb;
    }

    @GET
    @Path("/sb/field")
    public SingletonBean sb() {
        return sb;
    }

    @GET
    @Path("/sb/param")
    public SingletonBean sb(SingletonBean sb) {
        return sb;
    }

    @GET
    @Path("/queryparam")
    public void query(@NotBlank @Pattern(regexp = "\\w+") @QueryParam @DefaultValue("defaultName") String name,
                      ServerResponse response) {
        response.send(message.get() + ": " + name);
    }

    @GET
    @Path("/named/{name}")
    @Nonblocking //explicitly non-blocking
    public Greeting greetByName(@PathParam @NotBlank @Pattern(regexp = "\\w+") String name,
                                @HeaderParam("X-TEST") @DefaultValue("myValue") String header) {
        return new Greeting(message.get() + " " + name + ": " + header);
    }

    @POST
    @Status(Http.Status.CREATED_201)
    // implicitly blocking
    public void update(@Entity @Valid Greeting greeting) {
        message.set(greeting.getMessage());
    }

    @GET
    @Path("/reactive/multi")
    // implicitly non-blocking - reactive return type
    public Multi<Greeting> greetings() {
        return Multi.just(new Greeting("first"), new Greeting("second"));
    }

    @GET
    @Path("/reactive/single")
    // implicitly non-blocking - reactive return type
    public Single<Greeting> greeting() {
        return Single.error(new NotFoundException("Custom message from single"));
    }

    // local exception handler overrides the global defined in GlobalHandlers
    @ErrorHandle(value = ConstraintViolationException.class)
    @Status(Http.Status.BAD_REQUEST_400)
    public ErrorObject errorHandler(ConstraintViolationException error) {
        return ErrorObject.create(error);
    }
}
