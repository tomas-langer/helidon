package io.helidon.examples.di.basics.db;

import io.helidon.common.Reflected;
import io.helidon.di.annotation.database.DbObject;

import io.micronaut.core.annotation.Creator;

@Reflected
@DbObject
public class Pokemon {
    private String name;
    private String type;

    @Creator
    public Pokemon(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Pokemon() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
