/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.tests.integration.nativeimage.mp1;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;

import io.helidon.common.Errors;
import io.helidon.microprofile.server.Server;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

/**
 * Main class of this integration test.
 */
public final class Mp1Main {
    /**
     * Cannot be instantiated.
     */
    private Mp1Main() {
    }

    /**
     * Application main entry point.
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        // start CDI
        //Main.main(args);

        Server.builder()
                .port(8087)
                .applications(new JaxRsApplicationNoCdi())
                .retainDiscoveredApplications(true)
                .basePath("/cdi")
                .build()
                .start();

        long now = System.currentTimeMillis();
        testBean();
        long time = System.currentTimeMillis() - now;
        System.out.println("Tests finished in " + time + " millis");

        //System.out.println(ConfigProvider.getConfig().getConfigSources());
        //        printSysProps();

        System.exit(0);
    }

    private static void printSysProps() {
        System.out.println("System properties:");
        Properties properties = System.getProperties();

        properties.stringPropertyNames()
                .stream()
                .sorted()
                .forEach(it -> System.out.println("  " + it + "=" + properties.getProperty(it)));
    }

    private static void testBean() {
        // select a bean
        Instance<TestBean> select = CDI.current().select(TestBean.class);
        TestBean aBean = select.iterator().next();

        Errors.Collector collector = Errors.collector();

        invoke(collector, "Config injection", "Properties message", aBean::config);
        invoke(collector, "Rest client", "Properties message", aBean::restClientMessage);
        invoke(collector, "Rest client JSON-P", "json-p", aBean::restClientJsonP);
        invoke(collector, "Rest client JSON-B", "json-b", aBean::restClientJsonB);
        invoke(collector, "FT Fallback", "Fallback success", aBean::fallback);
        invoke(collector, "FT Retry", "Success on 3. try", aBean::retry);
        invoke(collector, "FT Async", "Async response", () -> {
            try {
                return aBean.asynchronous().toCompletableFuture().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
        invoke(collector, "FT timeout", "timeout", () -> {
            try {
                return aBean.timeout();
            } catch (TimeoutException ignored) {
                return "timeout";
            }
        });

        collector.collect()
                .checkValid();
    }

    private static void invoke(Errors.Collector collector, String assertionName, String expected, Supplier<String> invoke) {
        try {
            String actual = invoke.get();
            if (!expected.equals(actual)) {
                collector.fatal(assertionName + ", expected \"" + expected + "\", actual: \"" + actual + "\"");
            }
        } catch (Exception e) {
            e.printStackTrace();
            collector.fatal(assertionName + " failed. " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private static void assertResponse(Errors.Collector collector, String assertionName, String actual, String expected) {
        if (!expected.equals(actual)) {
            collector.fatal(assertionName + ", expected \"" + expected + "\", actual: \"" + actual + "\"");
        }
    }
}

