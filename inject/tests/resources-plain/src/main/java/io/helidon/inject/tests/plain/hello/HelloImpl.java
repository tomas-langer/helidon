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

package io.helidon.inject.tests.plain.hello;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.inject.service.Inject;

@Inject.Singleton
@Weight(Weighted.DEFAULT_WEIGHT)
public class HelloImpl implements Hello {

    @Inject.Point
    World world;

    @Inject.Point
    Supplier<World> worldRef;

    @Inject.Point
    List<Supplier<World>> listOfWorldRefs;

    @Inject.Point
    List<World> listOfWorlds;

    @Inject.Point
    @Inject.Named("red")
    Optional<World> redWorld;
    int postConstructCallCount;
    int preDestroyCallCount;
    @Inject.Point
    private Optional<World> privateWorld;
    private World setWorld;
    private Optional<World> setRedWorld;
    private World ctorWorld;

    HelloImpl() {
    }

    @Inject.Point
    public HelloImpl(World ctorWorld) {
        this();
        this.ctorWorld = ctorWorld;
    }

    @Override
    public void sayHello() {
        assert (postConstructCallCount == 1);
        assert (preDestroyCallCount == 0);
        System.getLogger(getClass().getName()).log(System.Logger.Level.INFO, "hello {0}", worldRef.get());
        assert (world == worldRef.get()) : "world != worldRef";
        assert (world == setWorld) : "world != setWorld";
        assert (ctorWorld == world) : "world != ctorWorld";
    }

    @Inject.Point
    public void world(World world) {
        this.setWorld = world;
        assert (world == ctorWorld);
    }

    @Inject.Point
    public void setRedWorld(@Inject.Named("red") Optional<World> redWorld) {
        this.setRedWorld = redWorld;
    }

    @Inject.PostConstruct
    public void postConstruct() {
        postConstructCallCount++;
    }

    @Inject.PreDestroy
    public void preDestroy() {
        preDestroyCallCount++;
    }

}
