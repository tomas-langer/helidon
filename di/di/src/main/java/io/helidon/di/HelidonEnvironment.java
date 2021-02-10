package io.helidon.di;

import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.config.MetaConfig;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.env.DefaultPropertyPlaceholderResolver;
import io.micronaut.context.env.Environment;
import io.micronaut.context.env.MapPropertySource;
import io.micronaut.context.env.PropertyPlaceholderResolver;
import io.micronaut.context.env.PropertySource;
import io.micronaut.context.env.PropertySourceLoader;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverter;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import io.micronaut.core.type.Argument;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.inject.BeanConfiguration;

public class HelidonEnvironment implements Environment {
    public static final String NAME = "Helidon";

    static {
        ConversionService.SHARED.addConverter(Class.class, String.class, Class::getName);
    }

    private final AtomicReference<Config> config = new AtomicReference<>();
    private final Config.Builder builder;
    private final ConversionService<?> conversionService;
    private final PropertyPlaceholderResolver placeholderResolver;
    private final Collection<String> packages = new ConcurrentLinkedQueue<>();
    private final ClassPathResourceLoader resourceLoader;
    private final Set<String> activeEnvironments;
    private final Collection<String> configurationIncludes = new HashSet<>(3);
    private final Collection<String> configurationExcludes = new HashSet<>(3);


    public HelidonEnvironment(ApplicationContextConfiguration configuration, Config.Builder builder) {
        this.builder = builder.addSource(ConfigSources.classpath("application.yml").optional());
        this.builder.addSource(ConfigSources.classpath("application.yaml").optional());
        this.conversionService = configuration.getConversionService();
        this.resourceLoader = configuration.getResourceLoader();
        this.placeholderResolver = new DefaultPropertyPlaceholderResolver(
                this,
                conversionService
        );
        this.activeEnvironments = new HashSet<>(3);
        this.activeEnvironments.addAll(configuration.getEnvironments());
        Config metaConfig = MetaConfig.config();
        if (metaConfig != null) {
            builder.config(metaConfig);
        }
        List<String> environments = configuration.getEnvironments();
        if (CollectionUtils.isNotEmpty(environments)) {
            List<String> extensions = List.of("json", "yml", "yaml", "conf", "properties");
            for (String environment : environments) {
                if ("helidon".equals(environment)) {
                    // ignore internal environment name
                    continue;
                }
                for (String ext : extensions) {
                    String name = "application-" + environment + "." + ext;
                    builder.addSource(ConfigSources.classpath(name).optional());
                }
            }
        }
    }

    @NonNull
    @Override
    public Environment start() {
        if (config.get() == null) {
            config.set(builder.build());
        }
        return this;
    }

    @NonNull
    @Override
    public Environment stop() {
        config.set(null);
        return this;
    }

    @Override
    public Set<String> getActiveNames() {
        return Collections.unmodifiableSet(activeEnvironments);
    }

    @Override
    public Collection<PropertySource> getPropertySources() {
        return Collections.emptyList();
    }

    @Override
    public Environment addPropertySource(PropertySource propertySource) {
        if (config.get() != null) {
            throw new IllegalStateException("Cannot modify configuration once running");
        } else {
            if (propertySource instanceof MapPropertySource) {

                Map<String, Object> values = ((MapPropertySource) propertySource).asMap();
                Map<String, String> properties = new LinkedHashMap<>(values.size());
                values.forEach((key, o) ->
                                       ConversionService.SHARED.convert(o, ConversionContext.STRING).ifPresent(s -> properties.put(key, s))
                );
                builder.addSource(ConfigSources.create(properties));
            }
        }
        return this;
    }

    @Override
    public Environment removePropertySource(PropertySource propertySource) {
        return this;
    }

    @Override
    public Environment addPackage(String pkg) {
        packages.add(Objects.requireNonNull(pkg, "Package cannot be null"));
        return this;
    }

    @Override
    public Environment addConfigurationExcludes(String... names) {
        configurationExcludes.addAll(Arrays.asList(names));
        return this;
    }

    @Override
    public Environment addConfigurationIncludes(String... names) {
        configurationIncludes.addAll(Arrays.asList(names));
        return this;
    }

    @Override
    public Collection<String> getPackages() {
        return Collections.unmodifiableCollection(packages);
    }

    @Override
    public PropertyPlaceholderResolver getPlaceholderResolver() {
        return placeholderResolver;
    }

    @Override
    public Map<String, Object> refreshAndDiff() {
        // will handle via config.onChange
        return Collections.emptyMap();
    }

    @Override
    public boolean isActive(BeanConfiguration configuration) {
        String name = configuration.getName();
        return !configurationExcludes.contains(name) && (configurationIncludes.isEmpty() || configurationIncludes.contains(name));
    }

    @Override
    public Collection<PropertySourceLoader> getPropertySourceLoaders() {
        return Collections.emptyList();
    }

    @Override
    public boolean isRunning() {
        return config.get() != null;
    }

    @Override
    public <S, T> Environment addConverter(Class<S> sourceType, Class<T> targetType, Function<S, T> typeConverter) {
        conversionService.addConverter(sourceType, targetType, typeConverter);
        return this;
    }

    @Override
    public <S, T> Environment addConverter(Class<S> sourceType, Class<T> targetType, TypeConverter<S, T> typeConverter) {
        conversionService.addConverter(sourceType, targetType, typeConverter);
        return this;
    }

    @Override
    public <T> Optional<T> convert(Object object, Class<T> targetType, ConversionContext context) {
        return conversionService.convert(object, targetType, context);
    }

    @Override
    public <S, T> boolean canConvert(Class<S> sourceType, Class<T> targetType) {
        return conversionService.canConvert(sourceType, targetType);
    }

    @Override
    public Optional<InputStream> getResourceAsStream(String path) {
        return resourceLoader.getResourceAsStream(path);
    }

    @Override
    public Optional<URL> getResource(String path) {
        return resourceLoader.getResource(path);
    }

    @Override
    public Stream<URL> getResources(String name) {
        return resourceLoader.getResources(name);
    }

    @Override
    public boolean supportsPrefix(String path) {
        return resourceLoader.supportsPrefix(path);
    }

    @Override
    public ResourceLoader forBase(String basePath) {
        return resourceLoader.forBase(basePath);
    }

    @Override
    public boolean containsProperty(@Nonnull String name) {
        Config rootConfig = this.config.get();
        return rootConfig != null && rootConfig.get(Objects.requireNonNull(name, "Name should not be null")).exists();
    }

    @Override
    public boolean containsProperties(@Nonnull String name) {
        Config rootConfig = this.config.get();
        return rootConfig != null && rootConfig.get(Objects.requireNonNull(name, "Name should not be null")).exists();
    }

    @Nonnull
    @Override
    public <T> Optional<T> getProperty(@Nonnull String name, @Nonnull ArgumentConversionContext<T> conversionContext) {
        Config rootConfig = this.config.get();
        if (rootConfig != null) {
            Argument<T> argument = conversionContext.getArgument();
            return rootConfig.get(Objects.requireNonNull(name, "Name should not be null"))
                    .as(config -> {
                        if (argument.isContainerType()) {
                            Class<T> t = argument.getType();
                            if (Map.class.isAssignableFrom(t) || Properties.class.isAssignableFrom(t)) {
                                Map<String, String> map = config.asMap().asOptional().orElse(Collections.emptyMap());
                                return conversionService.convert(map, conversionContext).orElse(null);
                            } else {
                                List<?> list = config.asList(conversionContext.getFirstTypeVariable().orElse(Argument.OBJECT_ARGUMENT).getType())
                                        .asOptional().orElse(Collections.emptyList());
                                return conversionService.convert(list, conversionContext).orElse(null);
                            }
                        } else {
                            String str = config.asString().orElse(null);
                            if (str != null) {
                                str = placeholderResolver.resolvePlaceholders(str).orElse(null);
                                return conversionService.convert(str, conversionContext)
                                        .orElse(null);
                            }
                            return null;
                        }
                    })
                    .asOptional();
        }
        return Optional.empty();
    }

    @NonNull
    @Override
    public Collection<String> getPropertyEntries(@NonNull String name) {
        Config rootConfig = this.config.get();
        if (rootConfig != null) {
            Config config = rootConfig.get(
                    Objects.requireNonNull(name, "Name cannot be null")
            ).detach();

            return config.asNodeList()
                    .map(configs -> configs.stream().map(Config::name)
                            .collect(Collectors.toSet())).orElse(Collections.emptySet());
        }
        return Collections.emptyList();
    }

    @NonNull
    public Config getHelidonConfig() {
        Config helidonConfig = config.get();
        if (helidonConfig == null) {
            throw new ConfigurationException("Configuration not yet initialized. Start the context first.");
        }
        return helidonConfig;
    }

}
