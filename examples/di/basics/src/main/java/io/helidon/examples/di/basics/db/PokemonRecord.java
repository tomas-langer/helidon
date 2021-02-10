package io.helidon.examples.di.basics.db;

import io.helidon.di.annotation.database.Column;
import io.helidon.di.annotation.database.DbObject;

@DbObject
public record PokemonRecord(@Column String name, @Column String type) {}
