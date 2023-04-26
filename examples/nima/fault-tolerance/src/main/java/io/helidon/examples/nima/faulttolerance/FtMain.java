package io.helidon.examples.nima.faulttolerance;

import java.util.Optional;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.logging.common.LogConfig;
import io.helidon.pico.api.Bootstrap;
import io.helidon.pico.api.BootstrapDefault;
import io.helidon.pico.api.PicoServices;

public class FtMain {
    /**
     * Start the example.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        // todo move to a Níma on Pico module
        LogConfig.configureRuntime();

        /*
        Nima.main(args); // initialize services, start webserver

        ConfigBeanRegistryHolder.configBeanRegistry().ifPresent(it -> ((ConfigBeanRegistry)it).veto(LoomServer.class));

        WebServer.builder()
                .config(config)
                .port(8080)
                .start();

        Services services = Nima.services(config);
        services.lookup();
        */

        Config config = Config.builder()
                .addSource(ConfigSources.classpath("application.yaml"))
                .disableSystemPropertiesSource()
                .disableEnvironmentVariablesSource()
                .build();

        ConfigService.config(config);

        Optional<Bootstrap> existingBootstrap = PicoServices.globalBootstrap();
        if (existingBootstrap.isEmpty()) {
            Bootstrap bootstrap = BootstrapDefault.builder()
                    .config(config)
                    .build();
            PicoServices.globalBootstrap(bootstrap);
        }

        PicoServices picoServices = PicoServices.picoServices().get();
        // this line is needed!
        picoServices.services();
    }
}
