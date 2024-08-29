package io.helidon.declarative.webserver;

import java.util.List;
import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.inject.api.GeneratedInjectService;
import io.helidon.service.inject.api.Qualifier;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.Handler;

@Service.Contract
public interface HttpEntryPoints {
    Handler handler(GeneratedInjectService.Descriptor<?> descriptor,
                    Set<Qualifier> typeQualifiers,
                    List<Annotation> typeAnnotations,
                    TypedElementInfo methodInfo,
                    Handler actualHandler);
}
