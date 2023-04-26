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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.helidon.pico.api.DefaultQualifierAndValue;
import io.helidon.pico.api.PicoServices;
import io.helidon.pico.api.ServiceInfoCriteriaDefault;
import io.helidon.pico.api.ServiceProvider;
import io.helidon.pico.configdriven.api.ConfiguredBy;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import static io.helidon.nima.faulttolerance.SupplierHelper.unwrapThrowable;

/**
 * Implementation of {@code Async}. Default executor accessed from {@link FaultTolerance#executor()}.
 */
@ConfiguredBy(AsyncConfig.class)
class AsyncImpl implements Async {
    private static final System.Logger LOGGER = System.getLogger(AsyncImpl.class.getName());

    private final ExecutorService executor;
    private final CompletableFuture<Async> onStart;

    // this must only be invoked when within Pico, so we can use pico services
    @Inject
    AsyncImpl(AsyncConfig config) {
        this.executor = config.executor()
                .or(() -> config.executorName().flatMap(AsyncImpl::executorService))
                .orElseGet(() -> FaultTolerance.executor().get());
        this.onStart = config.onStart().orElseGet(CompletableFuture::new);
    }

    AsyncImpl(AsyncConfig config, boolean internal) {
        this.executor = config.executor().orElseGet(() -> FaultTolerance.executor().get());
        this.onStart = config.onStart().orElseGet(CompletableFuture::new);
    }

    private static Optional<ExecutorService> executorService(String name) {
        var qualifier = DefaultQualifierAndValue.create(Named.class, name);
        return PicoServices.realizedServices().lookupFirst(ExecutorService.class,
                                                           ServiceInfoCriteriaDefault.builder()
                                                                   .addQualifier(qualifier)
                                                                   .build(),
                                                           false)
                .map(ServiceProvider::get);
    }

    @Override
    public <T> CompletableFuture<T> invoke(Supplier<T> supplier) {
        AtomicReference<Future<?>> ourFuture = new AtomicReference<>();
        CompletableFuture<T> result = new CompletableFuture<>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                Future<?> toCancel = ourFuture.get();
                if (toCancel == null) {
                    // cancelled before the future was assigned - this should not happen, as we do
                    // not escape this method before that
                    LOGGER.log(System.Logger.Level.WARNING, "Failed to cancel future, it is not yet available.");
                    return false;
                } else {
                    return toCancel.cancel(mayInterruptIfRunning);
                }
            }
        };
        Future<?> future = executor.submit(() -> {
            Thread thread = Thread.currentThread();
            thread.setName(thread.getName() + ": async");
            if (onStart != null) {
                onStart.complete(this);
            }
            try {
                T t = supplier.get();
                result.complete(t);
            } catch (Throwable t) {
                Throwable throwable = unwrapThrowable(t);
                result.completeExceptionally(throwable);
            }
        });
        ourFuture.set(future);

        return result;
    }
}
