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

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.service.Descriptor;

class InjectServiceProvider<T> extends ServiceProviderBase<T> {
    protected InjectServiceProvider(InjectionServices injectionServices, Descriptor<T> serviceSource) {
        super(injectionServices, serviceSource);
    }

    static <T> Activator<T> create(InjectionServices injectionServices, Descriptor<T> descriptor) {
        return new InjectServiceProvider<>(injectionServices, descriptor);
    }
}
