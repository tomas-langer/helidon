package io.helidon.nima.faulttolerance;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import io.helidon.common.GenericType;
import io.helidon.common.types.AnnotationAndValue;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNameDefault;
import io.helidon.common.types.TypedElementName;
import io.helidon.pico.api.Interceptor;
import io.helidon.pico.api.InvocationContext;
import io.helidon.pico.api.PicoException;
import io.helidon.pico.api.QualifierAndValue;
import io.helidon.pico.api.QualifierAndValueDefault;
import io.helidon.pico.api.ServiceInfoCriteriaDefault;
import io.helidon.pico.api.ServiceProvider;
import io.helidon.pico.api.Services;

import jakarta.inject.Named;

abstract class InterceptorBase<T> implements Interceptor {
    private final Map<CacheRecord, T> methodHandlerCache = new ConcurrentHashMap<>();
    private final Map<String, NamedResult<T>> namedHandlerCache = new ConcurrentHashMap<>();

    private final Services services;
    private final Class<T> ftType;
    private final TypeName annotationTypeName;

    InterceptorBase(Services services, Class<T> ftType, Class<? extends Annotation> annotationType) {
        this.services = services;
        this.ftType = ftType;
        this.annotationTypeName = TypeNameDefault.create(annotationType);
    }

    @Override
    public <V> V proceed(InvocationContext ctx, Chain<V> chain, Object... args) {
        // these are our cache keys
        TypeName typeName = ctx.serviceTypeName();
        TypedElementName elementInfo = ctx.elementInfo();
        Optional<TypedElementName[]> params = ctx.elementArgInfo();

        CacheRecord cacheRecord = new CacheRecord(typeName, elementInfo.elementName(), params);
        T ftHandler = cachedHandler(elementInfo, cacheRecord);

        return invokeHandler(ftHandler, chain, args);
    }

    <V> V invokeHandler(T ftHandler, Chain<V> chain, Object[] args) {
        if (ftHandler instanceof FtHandler handler) {
            return handler.invoke(() -> chain.proceed(args));
        } else if (ftHandler instanceof FtHandlerTyped typed) {
            return (V) typed.invoke(() -> chain.proceed(args));
        }
        throw new IllegalStateException("Invalid use of InterceptorBase, type can only be FtHandler or FtHandlerTyped");
    }

    // caching is done by this abstract class
    T obtainHandler(TypedElementName elementInfo, CacheRecord cacheRecord) {
        throw new IllegalStateException("Interceptor implementation must either override proceed, or implement obtainHandler");
    }

    T cachedHandler(TypedElementName elementInfo, CacheRecord cacheRecord) {
        return methodHandlerCache.computeIfAbsent(cacheRecord, record -> obtainHandler(elementInfo, cacheRecord));
    }

    /**
     * Lookup named handler from registry. The annotation MUST have a {@code name} property.
     * If name is not empty, it will be used for lookup.
     *
     * @param elementInfo current element info
     * @return an instance from registry, or null if none discovered
     */
    T namedHandler(TypedElementName elementInfo, Function<AnnotationAndValue, T> fromAnnotation) {
        AnnotationAndValue ftAnnotation = elementInfo.annotations()
                .stream()
                .filter(it -> annotationTypeName.equals(it.typeName()))
                .findFirst()
                .orElseThrow(() -> new PicoException("Interceptor triggered for a method not annotated with "
                                                             + annotationTypeName));

        String name = ftAnnotation.value("name")
                .filter(Predicate.not(String::isBlank))
                .orElse(null);

        if (name == null) {
            // not named, use annotation
            fromAnnotation.apply(ftAnnotation);
        }

        NamedResult<T> result = namedHandlerCache.get(name);

        if (result == null) {
            // not cached yet
            result = new NamedResult<>(lookupNamed(ftType, name));
            namedHandlerCache.put(name, result);
        }

        // cached
        return result.instance()
                .orElseGet(() -> fromAnnotation.apply(ftAnnotation));
    }

    <L> Optional<L> lookupNamed(Class<L> type, String name) {
        // not cached yet
        QualifierAndValue qualifier = QualifierAndValueDefault.create(Named.class, name);

        Optional<ServiceProvider<L>> lServiceProvider = services.lookupFirst(type,
                                                                             ServiceInfoCriteriaDefault.builder()
                                                                                     .addQualifier(qualifier)
                                                                                     .build(),
                                                                             false);
                return lServiceProvider.map(ServiceProvider::get);
    }

    <M extends FtMethod> Optional<M> generatedMethod(Class<M> type, CacheRecord cacheRecord) {
        var qualifier = QualifierAndValueDefault.create(Named.class,
                                                        cacheRecord.typeName().name()
                                                                + "."
                                                                + cacheRecord.methodName());
        List<ServiceProvider<M>> methods = services().lookupAll(type,
                                                                ServiceInfoCriteriaDefault.builder()
                                                                        .addQualifier(qualifier)
                                                                        .build());
        return methods.stream()
                .map(ServiceProvider::get)
                .filter(filterIt -> {
                    // only find methods that match the parameter types
                    List<GenericType<?>> supportedTypes = filterIt.parameterTypes();
                    Optional<TypedElementName[]> expectedTypes = cacheRecord.params();
                    if (supportedTypes.isEmpty() && expectedTypes.isEmpty()) {
                        // we have a match - no parameters
                        return true;
                    }
                    if (expectedTypes.isEmpty()) {
                        // supported types is not empty
                        return false;
                    }
                    TypedElementName[] typedElementNames = expectedTypes.get();
                    if (supportedTypes.size() != typedElementNames.length) {
                        // different number of parameters
                        return false;
                    }
                    // same number of parameters, let's see if the same types
                    for (int i = 0; i < typedElementNames.length; i++) {
                        TypedElementName expectedType = typedElementNames[i];
                        GenericType<?> supportedType = supportedTypes.get(i);
                        if (!supportedType.type().getTypeName().equals(expectedType.typeName().fqName())) {
                            return false;
                        }
                    }
                    return true;
                })
                .findAny();
    }

    Services services() {
        return services;
    }

    record CacheRecord(TypeName typeName, String methodName, Optional<TypedElementName[]> params) {
    }

    record NamedResult<T>(Optional<T> instance) {
    }
}
