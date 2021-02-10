package io.helidon.di.dbclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.helidon.dbclient.DbColumn;
import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;
import io.helidon.dbclient.spi.DbMapperProvider;
import io.helidon.di.annotation.database.CaseSensitive;
import io.helidon.di.annotation.database.Column;
import io.helidon.di.annotation.database.DbObject;

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import io.micronaut.core.type.Argument;

public class DiDbMapperProvider implements DbMapperProvider {
    private final DbMappers dbMappers;

    public DiDbMapperProvider() {
        Collection<BeanIntrospection<Object>> dbObjects
                = BeanIntrospector.SHARED.findIntrospections(DbObject.class);

        Map<Class<?>, DbMapper<?>> mappers = new HashMap<>();

        for (BeanIntrospection<Object> dbObject : dbObjects) {
            Class<Object> beanType = dbObject.getBeanType();
            Argument<?>[] constructorArguments = dbObject.getConstructorArguments();
            DbArgument[] arguments = new DbArgument[constructorArguments.length];

            // find all fields
            boolean allCaseSensitive = true;

            for (int i = 0; i < constructorArguments.length; i++) {
                Argument<?> argument = constructorArguments[i];
                AnnotationMetadata paramAm = argument.getAnnotationMetadata();
                Class<?> javaType = argument.getType();
                String name = paramAm.findAnnotation(Column.class)
                        .flatMap(AnnotationValue::stringValue)
                        .orElseGet(argument::getName);

                boolean caseSensitive = paramAm.findAnnotation(CaseSensitive.class)
                        .isPresent();

                if (!caseSensitive) {
                    allCaseSensitive = false;
                }

                arguments[i] = new DbArgument(javaType, name, caseSensitive);
            }

            Function<DbRow, ?> creator;
            Function<Object, Map<String, ?>> toNamedParams;
            Function<Object, List<Object>> toIndexedParams;

            if (allCaseSensitive) {
                creator = row -> {
                    Object[] values = new Object[arguments.length];
                    for (int i = 0; i < arguments.length; i++) {
                        DbArgument argument = arguments[i];
                        values[i] = row.column(argument.name).as(argument.javaType);
                    }
                    return dbObject.instantiate(values);
                };
                toNamedParams = object -> {
                    Map<String, Object> values = new HashMap<>();
                    String[] propertyNames = dbObject.getPropertyNames();
                    for (String propertyName : propertyNames) {
                        BeanProperty<Object, Object> bp = dbObject.getProperty(propertyName).orElse(null);
                        if (bp == null) {
                            values.put(propertyName, null);
                        } else {
                            values.put(propertyName, bp.get(object));
                        }
                    }
                    return values;
                };
            } else {
                creator = row -> {
                    Map<String, DbColumn> map = new HashMap<>();
                    Map<String, DbColumn> caseInsensitiveMap = new HashMap<>();
                    row.forEach(it -> {
                        String name = it.name();
                        map.put(name, it);
                        caseInsensitiveMap.put(name.toLowerCase(), it);
                    });

                    Object[] values = new Object[arguments.length];
                    for (int i = 0; i < arguments.length; i++) {
                        DbArgument argument = arguments[i];
                        if (argument.caseSensitive) {
                            values[i] = map.get(argument.name).as(argument.javaType);
                        } else {
                            values[i] = caseInsensitiveMap.get(argument.name.toLowerCase()).as(argument.javaType);
                        }
                    }
                    return dbObject.instantiate(values);
                };
                toNamedParams = object -> {
                    Map<String, Object> values = new HashMap<>();
                    String[] propertyNames = dbObject.getPropertyNames();
                    for (String propertyName : propertyNames) {
                        BeanProperty<Object, Object> bp = dbObject.getProperty(propertyName).orElse(null);
                        if (bp == null) {
                            values.put(propertyName, null);
                        } else {
                            values.put(propertyName, bp.get(object));
                        }
                    }
                    return values;
                };
            }

            toIndexedParams = object -> {
                List<Object> values = new ArrayList<>();
                String[] propertyNames = dbObject.getPropertyNames();
                for (String propertyName : propertyNames) {
                    BeanProperty<Object, Object> bp = dbObject.getProperty(propertyName).orElse(null);
                    if (bp == null) {
                        values.add(null);
                    } else {
                        values.add(bp.get(object));
                    }
                }
                return values;
            };

            mappers.put(beanType, new DiDbMapper<>(creator, toNamedParams, toIndexedParams));
        }

        dbMappers = new DbMappers(mappers);
    }

    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        return dbMappers.mapper(type);
    }

    private static final class DbArgument {
        private final Class<?> javaType;
        private final String name;
        private final boolean caseSensitive;

        DbArgument(Class<?> javaType, String name, boolean caseSensitive) {
            this.javaType = javaType;
            this.name = name;
            this.caseSensitive = caseSensitive;
        }
    }

}
