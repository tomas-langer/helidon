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

package io.helidon.inject.tests.inject.tbox.impl;

import java.util.Optional;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.inject.service.Inject;
import io.helidon.inject.tests.inject.tbox.Hammer;
import io.helidon.inject.tests.inject.tbox.Preferred;

@Inject.Singleton
@Weight(Weighted.DEFAULT_WEIGHT + 1)
@Inject.Named(BigHammer.NAME)
@Preferred
public class BigHammer implements Hammer {

    public static final String NAME = "big";

    @Override
    public Optional<String> named() {
        return Optional.of(NAME + " hammer");
    }

}
