/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.dbclient.jdbc;

import io.helidon.common.GenericType;
import io.helidon.common.mapper.MapperException;
import io.helidon.common.mapper.MapperManager;
import io.helidon.dbclient.DbColumn;

class JdbcDbColumn implements DbColumn {
    private final MapperManager mapperManager;
    private final Object value;
    private final Class<?> javaType;
    private final String dbType;
    private final String name;

    private JdbcDbColumn(MapperManager mapperManager, Object value, Class<?> javaType, String dbType, String name) {
        this.mapperManager = mapperManager;
        this.value = value;
        this.javaType = javaType;
        this.dbType = dbType;
        this.name = name;
    }

    static JdbcDbColumn create(Class<?> javaClass, String dbType, String name) {
        return new JdbcDbColumn(null, null, javaClass, dbType, name);
    }

    static JdbcDbColumn create(MapperManager mapperManager, DbColumn meta, Object value) {
        Class<?> javaType = null;

        if (meta.javaType() == null) {
            if (value != null) {
                javaType =  value.getClass();
            }
        } else {
            javaType = meta.javaType();
        }

        return new JdbcDbColumn(
                mapperManager,
                value,
                javaType,
                meta.dbType(),
                meta.name());
    }


    @Override
    public <T> T as(Class<T> type) throws MapperException {
        if (null == value) {
            return null;
        }
        if (type.isAssignableFrom(value.getClass())) {
            return type.cast(value);
        }
        return map(value, type);
    }

    @Override
    public <T> T as(GenericType<T> type) throws MapperException {
        if (null == value) {
            return null;
        }
        if (type.isClass()) {
            Class<?> theClass = type.rawType();
            if (theClass.isAssignableFrom(value.getClass())) {
                return type.cast(value);
            }
        }
        return map(value, type);
    }

    @Override
    public Class<?> javaType() {
        return javaType;
    }

    @Override
    public String dbType() {
        return dbType;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return value + " (" + dbType + ", " + javaType + ")";
    }

    @SuppressWarnings("unchecked")
    <SRC, T> T map(SRC value, Class<T> type) {
        Class<SRC> theClass = (Class<SRC>) value.getClass();

        try {
            return mapperManager.map(value, theClass, type);
        } catch (MapperException e) {
            if (type.equals(String.class)) {
                return (T) String.valueOf(value);
            }
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    <SRC, T> T map(SRC value, GenericType<T> type) {
        Class<SRC> theClass = (Class<SRC>) value.getClass();
        return mapperManager.map(value, GenericType.create(theClass), type);
    }
}
