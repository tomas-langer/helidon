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

import java.io.Serializable;
import java.util.Comparator;

import io.helidon.common.Weights;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ServiceProvider;

import jakarta.inject.Provider;

/**
 * A comparator appropriate for service providers, first using its {@link io.helidon.common.Weight} and then service type name
 * to determine its natural ordering.
 */
public class ServiceProviderComparator implements Comparator<Provider<?>>, Serializable {
    private static final ServiceProviderComparator INSTANCE = new ServiceProviderComparator();

    private ServiceProviderComparator() {
    }

    /**
     * Returns a service provider comparator.
     *
     * @return the service provider comparator
     */
    public static ServiceProviderComparator create() {
        return INSTANCE;
    }

    @Override
    public int compare(Provider<?> p1,
                       Provider<?> p2) {
        if (p1 == p2) {
            return 0;
        }

        if (p1 instanceof ServiceProvider
                && p2 instanceof ServiceProvider) {
            ServiceProvider<?> sp1 = (ServiceProvider<?>) p1;
            ServiceProvider<?> sp2 = (ServiceProvider<?>) p2;

            double w1 = sp1.weight();
            double w2 = sp2.weight();
            int comp = Double.compare(w1, w2);
            if (0 != comp) {
                return -1 * comp;
            }
            // secondary ordering based upon its name...
            TypeName name1 = sp1.serviceType();
            TypeName name2 = sp2.serviceType();
            comp = name2.compareTo(name1);
            return -1 * comp;
        } else {
            return Weights.weightComparator().compare(p1, p2);
        }
    }

}
