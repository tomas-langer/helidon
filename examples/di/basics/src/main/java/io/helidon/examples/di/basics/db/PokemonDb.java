package io.helidon.examples.di.basics.db;

import java.util.List;
import java.util.Optional;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.di.annotation.database.Database;
import io.helidon.di.annotation.database.DbGet;
import io.helidon.di.annotation.database.DbInsert;
import io.helidon.di.annotation.database.DbQuery;

// This is an API proposal, id does not yet work
@Database("pokemon") // reference the data source name
public interface PokemonDb {
    @DbInsert
    void insert(Pokemon pokemon);

    @DbInsert("insert")
    void insertNamed(String name, String type);

    @DbInsert("insert")
    Single<Long> reactiveInsert(Pokemon pokemon);

    @DbInsert("insert-indexed")
    void insertIndexed(String name, String type);

    @DbGet("select-one")
    Optional<Pokemon> get(String name);

    @DbGet("select-one")
    Single<Optional<Pokemon>> reactiveGet(String name);

    @DbQuery("select-all")
    List<Pokemon> getAll();

    @DbQuery("select-all")
    Multi<Pokemon> reactiveGetAll();
}
