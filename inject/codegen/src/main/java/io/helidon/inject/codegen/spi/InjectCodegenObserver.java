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

package io.helidon.inject.codegen.spi;

import java.util.Set;

import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.RoundContext;

/**
 * Implementations of these are service-loaded by the {@link java.util.ServiceLoader}, and will be
 * called to be able to observe processing events.
 */
public interface InjectCodegenObserver {

    /**
     * Called after a processing event that occurred in the.
     *
     * @param event the event
     */
    void onProcessingEvent(InjectionCodegenContext event, RoundContext roundContext, Set<TypedElementInfo> elements);
}
