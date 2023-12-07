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

package io.helidon.inject.api;

import java.io.Serializable;
import java.util.Comparator;

import io.helidon.inject.service.IpId;

/**
 * Comparator appropriate for {@link DependencyInfo}.
 */
public class DependencyInfoComparator implements java.util.Comparator<DependencyInfo>, Serializable {
    private static final Comparator<DependencyInfo> INSTANCE = new DependencyInfoComparator();

    private DependencyInfoComparator() {
    }

    /**
     * Dependency info comparator.
     *
     * @return instance of the comparator
     */
    public static Comparator<DependencyInfo> instance() {
        return INSTANCE;
    }

    @Override
    public int compare(DependencyInfo o1,
                       DependencyInfo o2) {
        IpId ipi1 = o1.injectionPointDependencies().iterator().next();
        IpId ipi2 = o2.injectionPointDependencies().iterator().next();


        java.util.Comparator<IpId> idComp = (o11, o21) -> {
            int result = Comparator.<IpId>comparingInt(it -> it.elementKind().ordinal()).compare(o11, o21);
            if (result != 0) {
                return result;
            }
            return o11.name().compareTo(o21.name());
        };

        return idComp.compare(ipi1, ipi2);
    }
}
