package io.helidon.examples.di.basics;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.di.annotation.http.Entity;
import io.helidon.di.annotation.http.GET;
import io.helidon.di.annotation.http.POST;
import io.helidon.di.annotation.http.Path;
import io.helidon.di.annotation.http.PathParam;
import io.helidon.di.annotation.http.Status;
import io.helidon.examples.di.basics.db.Pokemon;
import io.helidon.webserver.NotFoundException;

@Path("/pokemon")
public class PokemonController {
    private static final Logger LOGGER = Logger.getLogger(PokemonController.class.getName());

    private final DbClient dbClient;

    protected PokemonController(@Named("pokemon") DbClient dbClient) {
        this.dbClient = dbClient;
    }

    @PostConstruct
    protected void initializeDb() {
        dbClient.execute(handle -> handle.namedDml("create-table"))
                .thenAccept(System.out::println)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to create table: " + throwable.getMessage());
                    LOGGER.log(Level.FINEST, "Create table error stacktrace", throwable);
                    return null;
                })
                .await(10, TimeUnit.SECONDS);
    }

    @POST
    @Status(Http.Status.CREATED_201)
    public void createPokemon(@Entity Pokemon pokemon) {
        dbClient.execute(exec -> exec
                .createNamedInsert("insert")
                .namedParam(pokemon)
                .execute())
                .await();
    }

    @GET
    @Path("/{name}")
    public Single<Pokemon> pokemonByName(@PathParam String name) {
        return dbClient.execute(exec -> exec.namedGet("select-one", name))
                .map(opt -> opt.orElseThrow(() -> new NotFoundException("Pokemon by name " + name + " does not exist")))
                .map(row -> row.as(Pokemon.class));
    }

    @GET
    public Multi<Pokemon> listPokemon() {
        return dbClient.execute(exec -> exec.namedQuery("select-all"))
                .map(it -> it.as(Pokemon.class));
    }
}
