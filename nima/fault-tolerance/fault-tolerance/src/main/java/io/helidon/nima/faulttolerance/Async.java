/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.nima.faulttolerance;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import io.helidon.pico.api.Contract;

/**
 * Runs synchronous suppliers asynchronously using virtual threads. Includes
 * convenient static method to avoid creating instances of this class.
 */
@Contract
public interface Async {

    /**
     * Async with default executor service.
     *
     * @return a default async instance
     */
    static Async create() {
        return create(AsyncConfigDefault.builder().build());
    }

    /**
     * Async with default executor service.
     *
     * @return a default async instance
     */
    static Async create(AsyncConfig config) {
        return new AsyncImpl(config, true);
    }

    /**
     * Convenience method to avoid having to call {@link #create()}.
     *
     * @param supplier supplier of value (or a method reference)
     * @param <T> type of returned value
     * @return a Single that is a "promise" of the future result
     */
    static <T> CompletableFuture<T> invokeStatic(Supplier<T> supplier) {
        return create().invoke(supplier);
    }

    /**
     * Convenience method to avoid having to call {@link #create()}. Also accepts
     * an {@code onStart} future to inform of async task startup.
     *
     * @param supplier supplier of value (or a method reference)
     * @param onStart future completed when async task starts
     * @param <T> type of returned value
     * @return a future that is a "promise" of the future result
     */
    static <T> CompletableFuture<T> invokeStatic(Supplier<T> supplier, CompletableFuture<Async> onStart) {
        return Async.create(AsyncConfigDefault.builder()
                                    .onStart(onStart)
                                    .build())
                .invoke(supplier);
    }

    /**
     * Invoke a synchronous operation asynchronously.
     * This method never throws an exception. Any exception is returned via the
     * {@link java.util.concurrent.CompletableFuture} result.
     *
     * @param supplier supplier of value (or a method reference)
     * @param <T> type of returned value
     * @return a Single that is a "promise" of the future result
     */
    <T> CompletableFuture<T> invoke(Supplier<T> supplier);
}
