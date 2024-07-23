package io.helidon.common.json;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalEmpty;
import static io.helidon.common.testing.junit5.OptionalMatcher.optionalPresent;
import static io.helidon.common.testing.junit5.OptionalMatcher.optionalValue;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

public class TestExistingTypes {
    @Test
    void testServiceRegistry() throws IOException {
        JObject object;
        try (InputStream inputStream = resource("/service-registry.json")) {
            assertThat(inputStream, notNullValue());
            object = JValue.read(inputStream)
                    .asObject()
                    .value();
        }

        JObject generated = object.getObject("generated")
                .orElseThrow(() -> new IllegalStateException("Cannot find 'generated' object under root"));

        assertThat(generated.getString("trigger"),
                   optionalValue(is("io.helidon.service.codegen.ServiceRegistryCodegenExtension")));
        assertThat(generated.getString("value"),
                   optionalValue(is("io.helidon.service.codegen.ServiceRegistryCodegenExtension")));
        assertThat(generated.getString("version"),
                   optionalValue(is("1")));
        assertThat(generated.getString("comments"),
                   optionalValue(is("Service descriptors in module unnamed/io.helidon.examples.quickstart.se")));

        List<JObject> services = object.getObjects("services")
                .orElseThrow(() -> new IllegalStateException("Cannot find 'services' object under root"));

        assertThat(services, hasSize(2));

        JObject service = services.get(0);

        assertThat(service.getDouble("version"), optionalValue(is(1d)));
        assertThat(service.getString("type"),
                   optionalValue(is("inject")));
        assertThat(service.getString("descriptor"),
                   optionalValue(is("io.helidon.examples.quickstart.se.GreetEndpoint__HttpFeature__ServiceDescriptor")));
        assertThat(service.getDouble("weight"), optionalValue(is(100d)));
        List<String> contracts = service.getStrings("contracts")
                .orElseThrow(() -> new IllegalStateException("Cannot find 'contracts' object under service"));
        assertThat(contracts, hasItems("io.helidon.examples.quickstart.se.GreetEndpoint__HttpFeature",
                                       "io.helidon.webserver.http.HttpFeature",
                                       "io.helidon.webserver.ServerLifecycle",
                                       "java.util.function.Supplier"));

        service = services.get(1);
        assertThat(service.getDouble("version"), optionalEmpty());
        assertThat(service.getString("type"),
                   optionalValue(is("inject")));
        assertThat(service.getString("descriptor"),
                   optionalValue(is("io.helidon.examples.quickstart.se.GreetEndpoint__ServiceDescriptor")));
        assertThat(service.getDouble("weight"), optionalEmpty());
        contracts = service.getStrings("contracts")
                .orElseThrow(() -> new IllegalStateException("Cannot find 'contracts' object under service"));
        assertThat(contracts, hasItems("io.helidon.examples.quickstart.se.GreetEndpoint"));
    }

    @Test
    void testConfigMetadata() throws IOException {
        List<JObject> objects;
        try (InputStream inputStream = resource("/config-metadata.json")) {
            assertThat(inputStream, notNullValue());
            objects = JValue.read(inputStream)
                    .asObjectArray()
                    .value();
        }

        assertThat(objects, hasSize(1));

        JObject module = objects.getFirst();

        assertThat(module.getString("module"), optionalValue(is("io.helidon.common.configurable")));
        Optional<List<JObject>> types = module.getObjects("types");
        assertThat(types, optionalPresent());
        List<JObject> typesList = types.get();
        assertThat(typesList, hasSize(5));

        JObject first = typesList.getFirst();
        assertThat(first.getString("annotatedType"),
                   optionalValue(is("io.helidon.common.configurable.ResourceConfig")));
        assertThat(first.getString("type"),
                   optionalValue(is("io.helidon.common.configurable.Resource")));
        assertThat(first.getBoolean("is"),
                   optionalValue(is(true)));
        assertThat(first.getInt("number"),
                   optionalValue(is(49)));

        List<JObject> optionsList = first.getObjects("options")
                .orElse(List.of());
        assertThat(optionsList, hasSize(9));
        JObject firstOption = optionsList.getFirst();
        assertThat(firstOption.getString("description"),
                   optionalValue(is("Resource is located on filesystem.\n\n Path of the resource")));
        assertThat(firstOption.getString("key"),
                   optionalValue(is("path")));
        assertThat(firstOption.getString("method"),
                   optionalValue(is("io.helidon.common.configurable.ResourceConfig."
                                            + "Builder#path(java.util.Optional<java.nio.file.Path>)")));
        assertThat(firstOption.getString("type"),
                   optionalValue(is("java.nio.file.Path")));
    }

    private InputStream resource(String location) {
        return TestExistingTypes.class.getResourceAsStream(location);
    }

}
