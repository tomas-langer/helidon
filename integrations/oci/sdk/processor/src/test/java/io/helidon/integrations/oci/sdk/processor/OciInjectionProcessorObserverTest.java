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

package io.helidon.integrations.oci.sdk.processor;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Set;

import io.helidon.common.processor.classmodel.ClassModel;
import io.helidon.common.types.TypeName;
import io.helidon.inject.tools.ToolsException;

import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.streaming.Stream;
import com.oracle.bmc.streaming.StreamAdmin;
import com.oracle.bmc.streaming.StreamAsync;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

class OciInjectionProcessorObserverTest {

    @Test
    void generatedInjectionArtifactsForTypicalOciServices() throws IOException {
        TypeName ociServiceType = TypeName.create(ObjectStorage.class);

        TypeName generatedOciServiceClientTypeName = OciInjectionProcessorObserver.toGeneratedServiceClientTypeName(ociServiceType);
        assertThat(generatedOciServiceClientTypeName.name(),
                   equalTo("io.helidon.integrations.generated." + ociServiceType.name() + "__Oci_Client"));

        ClassModel classModel = OciInjectionProcessorObserver.toBody(ociServiceType,
                                                                            generatedOciServiceClientTypeName);
        StringWriter sw = new StringWriter();
        classModel.write(sw);
        String stringBody = sw.toString();
        assertThat(stringBody.trim(),
                   equalTo(loadStringFromResource("expected/Objectstorage__Oci_Client._java_")));

        TypeName generatedOciServiceClientBuilderTypeName = OciInjectionProcessorObserver.toGeneratedServiceClientBuilderTypeName(ociServiceType);
        assertThat(generatedOciServiceClientBuilderTypeName.name(),
                   equalTo("io.helidon.integrations.generated." + ociServiceType.name() + "__Oci_ClientBuilder"));

        classModel = OciInjectionProcessorObserver.toBuilderBody(ociServiceType,
                                                                               generatedOciServiceClientTypeName,
                                                                                          generatedOciServiceClientBuilderTypeName);
        sw = new StringWriter();
        classModel.write(sw);
        stringBody = sw.toString();
        assertThat(stringBody.trim(),
                   equalTo(loadStringFromResource("expected/Objectstorage__Oci_ClientBuilder._java_")));
    }

    @Test
    void oddballServiceTypeNames() {
        TypeName ociServiceType = TypeName.create(Stream.class);
        assertThat(OciInjectionProcessorObserver.maybeDot(ociServiceType),
                                 equalTo(""));
        assertThat(OciInjectionProcessorObserver.usesRegion(ociServiceType),
                                 equalTo(false));

        ociServiceType = TypeName.create(StreamAsync.class);
        assertThat(OciInjectionProcessorObserver.maybeDot(ociServiceType),
                                 equalTo(""));
        assertThat(OciInjectionProcessorObserver.usesRegion(ociServiceType),
                                 equalTo(false));

        ociServiceType = TypeName.create(StreamAdmin.class);
        assertThat(OciInjectionProcessorObserver.maybeDot(ociServiceType),
                                 equalTo("."));
        assertThat(OciInjectionProcessorObserver.usesRegion(ociServiceType),
                                 equalTo(true));
    }

    @Test
    void testShouldProcess() {
        TypeName typeName = TypeName.create(ObjectStorage.class);
        assertThat(OciInjectionProcessorObserver.shouldProcess(typeName, null),
                                 is(true));

        typeName = TypeName.create("com.oracle.bmc.circuitbreaker.OciCircuitBreaker");
        assertThat(OciInjectionProcessorObserver.shouldProcess(typeName, null),
                                 is(false));

        typeName = TypeName.create("com.oracle.another.Service");
        assertThat(OciInjectionProcessorObserver.shouldProcess(typeName, null),
                                 is(false));

        typeName = TypeName.create("com.oracle.bmc.Service");
        assertThat(OciInjectionProcessorObserver.shouldProcess(typeName, null),
                                 is(true));

        typeName = TypeName.create("com.oracle.bmc.ServiceClient");
        assertThat(OciInjectionProcessorObserver.shouldProcess(typeName, null),
                                 is(false));

        typeName = TypeName.create("com.oracle.bmc.ServiceClientBuilder");
        assertThat(OciInjectionProcessorObserver.shouldProcess(typeName, null),
                                 is(false));
    }

    @Test
    void loadTypeNameExceptions() {
        Set<String> set = OciInjectionProcessorObserver.TYPENAME_EXCEPTIONS.get();
        set.addAll(OciInjectionProcessorObserver.splitToSet(" M1,  M2,,, "));
        assertThat(set,
                   containsInAnyOrder("M1",
                                      "M2",
                                      "test1",
                                      "com.oracle.bmc.Region",
                                      "com.oracle.bmc.auth.AbstractAuthenticationDetailsProvider",
                                      "com.oracle.bmc.circuitbreaker.OciCircuitBreaker"
                   ));
    }

    @Test
    void loadNoDotExceptions() {
        Set<String> set = OciInjectionProcessorObserver.NO_DOT_EXCEPTIONS.get();
        set.addAll(OciInjectionProcessorObserver.splitToSet("Manual1, Manual2 "));
        assertThat(set,
                   containsInAnyOrder("Manual1",
                                      "Manual2",
                                      "test2",
                                      "com.oracle.bmc.streaming.Stream",
                                      "com.oracle.bmc.streaming.StreamAsync"
                   ));
    }

    static String loadStringFromResource(String resourceNamePath) {
        try {
            try (InputStream in = OciInjectionProcessorObserverTest.class.getClassLoader().getResourceAsStream(resourceNamePath)) {
                String result = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                return result.replaceAll("\\{\\{YEAR}}", String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
                        .trim(); // remove leading and trailing whitespaces
            }
        } catch (Exception e) {
            throw new ToolsException("Failed to load: " + resourceNamePath, e);
        }
    }

}
