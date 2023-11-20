/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.Interceptor;
import io.helidon.inject.api.InvocationContext;
import io.helidon.inject.api.InvocationException;
import io.helidon.inject.api.Invoker;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;

import jakarta.inject.Provider;

/**
 * Handles the invocation of {@link Interceptor} methods.
 * Note that upon a successful call to the {@link Interceptor.Chain#proceed(Object[])} or to the ultimate
 * target, the invocation will be prevented from being executed again.
 *
 * @see InvocationContext
 * @param <V> the invocation type
 */
public class Invocation<V> implements Interceptor.Chain<V> {
    private final InvocationContext ctx;
    private final List<Provider<Interceptor>> interceptors;
    private final Set<Class<? extends Throwable>> checkedExceptions;
    private int interceptorPos;
    private Invoker<V> call;

    private Invocation(InvocationContext ctx,
                      Invoker<V> call,
                       Set<Class<? extends Throwable>> checkedExceptions) {
        this.ctx = ctx;
        this.call = call;
        this.interceptors = List.copyOf(ctx.interceptors());
        this.checkedExceptions = checkedExceptions;
    }

    @Override
    public String toString() {
        return String.valueOf(ctx.elementInfo());
    }

    /**
     * Creates an instance of {@link Invocation} and invokes it in this context.
     *
     * @param ctx   the invocation context
     * @param call  the call to the base service provider's method
     * @param args  the call arguments
     * @param checkedExceptions expected exception types
     * @param <V>   the type returned from the method element
     * @return the invocation instance
     * @throws InvocationException if there are errors during invocation chain processing
     * @throws Exception any checked exception declared by the method itself
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <V> V createInvokeAndSupply(InvocationContext ctx,
                                              Invoker<V> call,
                                              Object[] args,
                                              Set<Class<? extends Throwable>> checkedExceptions) throws Exception {
        Objects.requireNonNull(ctx);
        Objects.requireNonNull(call);
        Objects.requireNonNull(args);
        Objects.requireNonNull(checkedExceptions);

        if (ctx.interceptors().isEmpty()) {
            try {
                return call.invoke(args);
            } catch (Throwable t) {
                if (shouldThrow(checkedExceptions, t.getClass())) {
                    throw t;
                }
                throw new InvocationException("Error in interceptor chain processing", t, true);
            }
        } else {
            return (V) new Invocation(ctx, call, checkedExceptions).proceed(args);
        }
    }

    /**
     * Creates an instance of {@link Invocation} and invokes it in this context.
     *
     * @param descriptor      service descriptor
     * @param typeAnnotations type level annotations
     * @param element         element being invoked
     * @param call            the call to the base service provider's method
     * @param args            the call arguments
     * @param <V>             the type returned from the method element
     * @return the invocation instance
     * @throws InvocationException if there are errors during invocation chain processing
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <V> V createInvokeAndSupply(ServiceDescriptor descriptor,
                                              List<Annotation> typeAnnotations,
                                              TypedElementInfo element,
                                              List<Provider<Interceptor>> interceptors,
                                              Invoker<V> call,
                                              Object... args) {
        Objects.requireNonNull(descriptor);
        Objects.requireNonNull(typeAnnotations);
        Objects.requireNonNull(element);
        Objects.requireNonNull(interceptors);
        Objects.requireNonNull(call);
        Objects.requireNonNull(args);

        InvocationContext ctx = InvocationContext.builder()
                .serviceDescriptor(descriptor)
                .classAnnotations(typeAnnotations)
                .elementInfo(element)
                .interceptors(interceptors)
                .build();

        try {
            if (ctx.interceptors().isEmpty()) {
                return call.invoke(args);
            } else {
                return (V) new Invocation(ctx, call, Set.of()).proceed(args);
            }
        } catch (InvocationException e) {
            throw e;
        } catch (Throwable t) {
            // this method does not support checked exceptions
            // (and as a result, we do not support checked exceptions in intercepted constructors)
            throw new InvocationException("Error in interceptor chain processing", t, true);
        }
    }

    /**
     * The degenerate case for {@link #mergeAndCollapse(List[])}. This is here only to eliminate the unchecked varargs compiler
     * warnings that would otherwise be issued in code that does not have any interceptors on a method.
     *
     * @param <T>   the type of the provider
     * @return an empty list
     * @deprecated this method should only be called by generated code
     */
    @Deprecated
    public static <T> List<Provider<T>> mergeAndCollapse() {
        return List.of();
    }

    /**
     * Merges a variable number of lists together, where the net result is the merged set of non-null providers
     * ranked in proper weight order, or else empty list.
     *
     * @param lists the lists to merge
     * @param <T>   the type of the provider
     * @return the merged result or empty list if there is o interceptor providers
     */
    @SuppressWarnings("unchecked")
    public static <T> List<Provider<T>> mergeAndCollapse(List<Provider<T>>... lists) {
        List<Provider<T>> result = null;

        for (List<Provider<T>> list : lists) {
            if (list == null) {
                continue;
            }

            for (Provider<T> p : list) {
                if (p == null) {
                    continue;
                }

                if (p instanceof ServiceProvider<?> serviceProvider
                        && VoidServiceProvider.TYPE_NAME.equals(serviceProvider.serviceType())) {
                    continue;
                }

                if (result == null) {
                    result = new ArrayList<>();
                }
                if (!result.contains(p)) {
                    result.add(p);
                }
            }
        }

        if (result != null && result.size() > 1) {
            result.sort(DefaultServices.serviceProviderComparator());
        }

        return (result != null) ? Collections.unmodifiableList(result) : List.of();
    }

    @Override
    public V proceed(Object... args) throws Exception {
        if (this.call == null) {
            throw new InvocationException("Duplicate invocation, or unknown call type: " + this, true);
        }

        if (interceptorPos < interceptors.size()) {
            Provider<Interceptor> interceptorProvider =  interceptors.get(interceptorPos);
            Interceptor interceptor = interceptorProvider.get();
            interceptorPos++;
            try {
                return interceptor.proceed(ctx, this, args);
            } catch (RuntimeException e) {
                interceptorPos--;
                throw e;
            } catch (Throwable t) {
                interceptorPos--;

                if (shouldThrow(checkedExceptions, t.getClass())) {
                    throw t;
                }

                throw (interceptorProvider instanceof ServiceProvider)
                        ? new InvocationException("Error in interceptor chain processing",
                                                  t,
                                                  (ServiceProvider<?>) interceptorProvider,
                                                  call == null)
                        : new InvocationException("Error in interceptor chain processing",
                                                  t,
                                                  call == null);
            }
        }

        Invoker<V> call = this.call;
        this.call = null;

        try {
            return call.invoke(args);
        } catch (InvocationException e) {
            if (e.targetWasCalled()) {
                // allow the call to happen again
                this.call = call;
            }
            throw e;
        } catch (RuntimeException e) {
            this.call = call;
            throw e;
        } catch (Throwable t) {
            // allow the call to happen again
            this.call = call;
            if (shouldThrow(checkedExceptions, t.getClass())) {
                // do not wrap, declared checked exception
                throw t;
            }
            // wrap, unexpected exception/throwable
            throw new InvocationException("Error in interceptor chain processing", t, true);
        }
    }

    private static boolean shouldThrow(Set<Class<? extends Throwable>> checked, Class<? extends Throwable> t) {
        if (checked.contains(t)) {
            return true;
        }
        for (Class<? extends Throwable> aClass : checked) {
            if (aClass.isAssignableFrom(t)) {
                return true;
            }
        }
        return false;
    }
}
