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

package io.helidon.inject.testing;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceSource;

/**
 * Creates a simple reflection based service provider - for testing purposes only!
 *
 * @param <T> the service type
 */
public class ReflectionBasedSingletonServiceDescriptor<T> implements ServiceSource<T> {
    private final Class<T> serviceType;
    private final ServiceInfo<T> serviceInfo;

    private ReflectionBasedSingletonServiceDescriptor(Class<T> serviceType,
                                                      ServiceInfo<T> serviceInfo) {
        this.serviceType = serviceType;
        this.serviceInfo = serviceInfo;
    }

    /**
     * Generates a service provider eligible for binding into the service registry with the following proviso:
     * <ul>
     * <li>The service type will be of {@code jakarta.inject.Singleton} scope</li>
     * <li>The service type will be created reflectively, and will expect to have an empty constructor</li>
     * <li>The service type will not be able to provide its dependencies, nor will it be able to accept injection</li>
     * <li>The service type will not be able to participate in lifecycle -
     * {@link io.helidon.inject.api.PostConstructMethod} or {@link io.helidon.inject.api.PreDestroyMethod}</li>
     * </ul>
     * Note: Generally it is encouraged for users to rely on the annotation processors and other built on compile-time
     * tooling to generate the appropriate service providers and modules. This method is an alternative to that
     * mechanism and therefore is discouraged from production use.  This method is not used in normal processing by
     * the reference injection provider implementation.
     *
     * @param serviceType the service type
     * @param serviceInfo the service info basic descriptor, or null to generate a default (empty) placeholder
     * @param <T>         the class of the service type
     * @return the service provider capable of being bound to the services registry
     * @see InjectionTestingSupport#bind(io.helidon.inject.api.InjectionServices, io.helidon.inject.api.ServiceSource)
     */
    public static <T> ServiceSource<T> create(Class<T> serviceType,
                                                  ServiceInfo<T> serviceInfo) {
        Objects.requireNonNull(serviceType);
        Objects.requireNonNull(serviceInfo);

        if (!TypeName.create(serviceType).equals(serviceInfo.serviceType())) {
            throw new IllegalArgumentException("Mismatch in service types: " + serviceType.getName());
        }

        return new ReflectionBasedSingletonServiceDescriptor<>(serviceType, serviceInfo);
    }

    @Override
    public double weight() {
        return serviceInfo.weight();
    }

    @Override
    public String runtimeId() {
        return serviceInfo.runtimeId();
    }

    @Override
    public TypeName serviceType() {
        return serviceInfo.serviceType();
    }

    @Override
    public Set<TypeName> contracts() {
        return serviceInfo.contracts();
    }

    @Override
    public List<IpId> dependencies() {
        return serviceInfo.dependencies();
    }

    @Override
    public Set<Qualifier> qualifiers() {
        return serviceInfo.qualifiers();
    }

    @Override
    public int runLevel() {
        return serviceInfo.runLevel();
    }

    @Override
    public Set<TypeName> scopes() {
        return serviceInfo.scopes();
    }

    @Override
    public T instantiate(InjectionContext ctx, InterceptionMetadata interceptionMetadata) {
        try {
            Constructor<T> ctor = serviceType.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (Exception e) {
            throw new ServiceProviderInjectionException("Failed to fully create instance: " + this, e, this);
        }
    }
}