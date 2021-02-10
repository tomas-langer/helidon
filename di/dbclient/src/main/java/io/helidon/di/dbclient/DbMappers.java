package io.helidon.di.dbclient;

import java.util.Map;
import java.util.Optional;

import io.helidon.dbclient.DbMapper;

class DbMappers {
    private final Map<Class<?>, DbMapper<?>> mappers;

    DbMappers(Map<Class<?>, DbMapper<?>> mappers) {
        this.mappers = mappers;
    }

    @SuppressWarnings("unchecked")
    <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        return Optional.ofNullable((DbMapper<T>)mappers.get(type));
    }
}
