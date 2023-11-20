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

package io.helidon.inject.tests.inject.interceptor;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Interceptor;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.testing.ReflectionBasedSingletonServiceDescriptor;
import io.helidon.inject.testing.ServiceInfo;
import io.helidon.inject.tests.inject.ClassNamedY;
import io.helidon.inject.tests.plain.interceptor.IB;
import io.helidon.inject.tests.plain.interceptor.InterceptorBasedAnno;
import io.helidon.inject.tests.plain.interceptor.TestNamedInterceptor;

import jakarta.inject.Named;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.inject.api.Qualifier.create;
import static io.helidon.inject.api.Qualifier.createNamed;
import static io.helidon.inject.testing.InjectionTestingSupport.basicTestableConfig;
import static io.helidon.inject.testing.InjectionTestingSupport.bind;
import static io.helidon.inject.testing.InjectionTestingSupport.resetAll;
import static io.helidon.inject.testing.InjectionTestingSupport.testableServices;
import static io.helidon.inject.testing.InjectionTestingSupport.toDescription;
import static io.helidon.inject.testing.InjectionTestingSupport.toDescriptions;
import static io.helidon.inject.tests.inject.TestUtils.loadStringFromResource;
import static io.helidon.inject.tools.TypeTools.toFilePath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class InterceptorRuntimeTest {

    Config config = basicTestableConfig();
    InjectionServices injectionServices;
    Services services;

    @BeforeEach
    void setUp() {
        setUp(config);
    }

    void setUp(Config config) {
        this.injectionServices = testableServices(config);
        this.services = injectionServices.services();
    }

    @AfterEach
    void tearDown() {
        resetAll();
    }

    @Test
    void createNoArgBasedInterceptorSource() throws Exception {
        TypeName interceptorTypeName = TypeName.create(XImpl__Intercepted.class);
        String path = toFilePath(interceptorTypeName);
        File file = new File("./target/generated-sources/annotations", path);
        assertThat(file.exists(), is(true));
        List<String> java = Files.readAllLines(file.toPath());
        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        String expected = loadStringFromResource("expected/ximpl-intercepted._java_")
                .replace("{{year}}", year);

        compareContentByLines("XImpl__Intercepted.java", java, expected);
    }

    @Test
    void createInterfaceBasedInterceptorSource() throws Exception {
        TypeName interceptorTypeName = TypeName.create(YImpl__Intercepted.class);
        String path = toFilePath(interceptorTypeName);
        File file = new File("./target/generated-sources/annotations", path);
        assertThat(file.exists(), is(true));
        List<String> java = Files.readAllLines(file.toPath());
        String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
        String expected = loadStringFromResource("expected/yimpl-intercepted._java_")
                .replace("{{year}}", year);
        compareContentByLines("YImpl__Intercepted.java", java, expected);
    }

    @Test
    void runtimeWithNoInterception() throws Exception {
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContract(Closeable.class)
                .build();
        List<ServiceProvider<?>> closeableProviders = services.lookupAll(criteria);
        assertThat(toDescriptions(closeableProviders),
                   contains("XImpl:INIT", "YImpl:INIT"));

        List<ServiceProvider<IB>> ibProviders = services.lookupAll(IB.class);
        assertThat(closeableProviders,
                   equalTo(ibProviders));

        ServiceProvider<XImpl> ximplProvider = services.lookupFirst(XImpl.class);
        assertThat(closeableProviders.get(0),
                   is(ximplProvider));

        XImpl x = ximplProvider.get();
        x.methodIA1();
        x.methodIA2();
        x.methodIB("test");
        String sval = x.methodIB2("test");
        assertThat(sval,
                   equalTo("test"));
        long val = x.methodX("a", 2, true);
        assertThat(val,
                   equalTo(101L));
        assertThat(x.methodY(),
                   equalTo("methodY"));
        assertThat(x.methodZ(),
                   equalTo("methodZ"));
        IOException pe = assertThrows(IOException.class, x::close);
        assertThat("the error handling should be the same if there are interceptors or not",
                   pe.getMessage(),
                   equalTo("forced"));
        RuntimeException re = assertThrows(RuntimeException.class, x::throwRuntimeException);
        assertThat("the error handling should be the same if there are interceptors or not",
                   re.getMessage(),
                   equalTo("forced"));

        // There is only one service (regardless of interception status)
        ServiceProvider<?> yimplProvider = services
                .lookupFirst(
                        ServiceInfoCriteria.builder()
                                .addContract(Closeable.class)
                                .qualifiers(Set.of(create(Named.class, ClassNamedY.class.getName())))
                                .build());
        assertThat(toDescription(yimplProvider),
                   equalTo("YImpl:INIT"));
        IB ibOnYInterceptor = (IB) yimplProvider.get();
        sval = ibOnYInterceptor.methodIB2("test");
        assertThat(sval,
                   equalTo("test"));
    }

    @Test
    void runtimeWithInterception() throws Exception {
        // disable application and modules to effectively start with an empty registry
        Config config = Config.builder(
                        ConfigSources.create(
                                Map.of("inject.permits-dynamic", "true",
                                       "inject.uses-compile-time-applications", "false",
                                       "inject.uses-compile-time-modules", "true"),
                                "config-1"))
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .build();
        tearDown();
        setUp(config);
        bind(injectionServices, ReflectionBasedSingletonServiceDescriptor
                .create(TestNamedInterceptor.class,
                        ServiceInfo.<TestNamedInterceptor>builder()
                                .serviceType(TypeName.create(TestNamedInterceptor.class))
                                .addQualifier(createNamed(TestNamed.class.getName()))
                                .addQualifier(createNamed(InterceptorBasedAnno.class.getName()))
                                .addContract(TypeName.create(Interceptor.class))
                                .build()));
        assertThat(TestNamedInterceptor.CONSTRUCTOR_COUNTER.get(),
                   equalTo(0));

        List<ServiceProvider<Closeable>> closeableProviders = injectionServices.services().lookupAll(Closeable.class);

        List<ServiceProvider<IB>> ibProviders = services.lookupAll(IB.class);
        assertThat(closeableProviders,
                   equalTo(ibProviders));

        ServiceProvider<XImpl> ximplProvider = services.lookupFirst(XImpl.class);
        assertThat(closeableProviders.get(0),
                   is(ximplProvider));

        assertThat(TestNamedInterceptor.CONSTRUCTOR_COUNTER.get(),
                   equalTo(0));
        XImpl xIntercepted = ximplProvider.get();
        assertThat(TestNamedInterceptor.CONSTRUCTOR_COUNTER.get(),
                   equalTo(1));

        xIntercepted.methodIA1();
        xIntercepted.methodIA2();
        xIntercepted.methodIB("test");
        String sval = xIntercepted.methodIB2("test");
        assertThat(sval,
                   equalTo("intercepted:test"));
        long val = xIntercepted.methodX("a", 2, true);
        assertThat(val,
                   equalTo(202L));
        assertThat(xIntercepted.methodY(),
                   equalTo("intercepted:methodY"));
        assertThat(xIntercepted.methodZ(),
                   equalTo("intercepted:methodZ"));
        IOException pe = assertThrows(IOException.class, xIntercepted::close);
        // as this is a declared checked exception, it is re-thrown as is
        assertThat(pe.getMessage(),
                   equalTo("forced"));
        RuntimeException re = assertThrows(RuntimeException.class, xIntercepted::throwRuntimeException);
        // as this is a runtime exception, it is re-thrown as is
        assertThat(re.getMessage(),
                   equalTo("forced"));

        assertThat(TestNamedInterceptor.CONSTRUCTOR_COUNTER.get(),
                   equalTo(1));

        // we cannot look up by service type here - we need to instead lookup by one of the interfaces
        ServiceProvider<IB> yimplProvider = services
                .lookup(IB.class, ClassNamedY.class.getName());
        assertThat(toDescription(yimplProvider),
                   equalTo("YImpl:INIT"));
        IB ibInstance = yimplProvider.get();
        sval = ibInstance.methodIB2("test");
        assertThat(sval,
                   equalTo("intercepted:test"));
    }

    private void compareContentByLines(String description, List<String> generatedSource, String expectedSource)
            throws IOException {
        BufferedReader br = new BufferedReader(new StringReader(expectedSource));

        int sourceLineNumber = 0;

        for (int i = 0; i < generatedSource.size(); i++) {
            sourceLineNumber++;
            String sourceLine = generatedSource.get(i).trim();
            if (sourceLine.isEmpty()) {
                continue;
            }
            String expectedLine;
            while ((expectedLine = br.readLine()) != null) {
                expectedLine = expectedLine.trim();
                if (!expectedLine.isEmpty()) {
                    break;
                }
            }
            if (expectedLine == null) {
                fail("Source file: " + description + " contains line [" + sourceLineNumber + "] \"" + sourceLine + "\", "
                             + "where nothing is expected.");
            }
            assertThat("Source file: " + description + ", line number " + sourceLine, sourceLine, is(expectedLine));
        }
    }

}
