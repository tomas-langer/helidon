package io.helidon.di.dbclient;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

@SuppressWarnings("unchecked")
class DiDbMapper<T> implements DbMapper<T> {
    private final Function<DbRow, ?> creator;
    private final Function<Object, Map<String, ?>> toNamedParams;
    private final Function<Object, List<Object>> toIndexedParams;

    DiDbMapper(Function<DbRow, ?> creator,
               Function<Object, Map<String, ?>> toNamedParams,
               Function<Object, List<Object>> toIndexedParams) {

        this.creator = creator;
        this.toNamedParams = toNamedParams;
        this.toIndexedParams = toIndexedParams;
    }

    @Override
    public T read(DbRow row) {
        return (T) creator.apply(row);
    }

    @Override
    public Map<String, ?> toNamedParameters(Object value) {
        return toNamedParams.apply(value);
    }

    @Override
    public List<?> toIndexedParameters(Object value) {
        return toIndexedParams.apply(value);
    }
}
