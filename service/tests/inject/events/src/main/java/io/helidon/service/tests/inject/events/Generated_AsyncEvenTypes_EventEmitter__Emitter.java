package io.helidon.service.tests.inject.events;

import java.util.Set;
import java.util.concurrent.CompletionStage;

import io.helidon.common.types.ResolvedType;
import io.helidon.common.types.TypeName;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.Qualifier;
import io.helidon.service.tests.inject.events.api.Event;
import io.helidon.service.tests.inject.events.api.EventManager;

/*
Specific service for a single injection point.
The descriptor generator for AsyncEventTypes.EventEmitter must use this type as the
injection point contract, to ensure it will be used (and none other)
 */
@Injection.Singleton
class Generated_AsyncEvenTypes_EventEmitter__Emitter implements Event.Emitter<AsyncEventTypes.EventObject> {
    private static final ResolvedType EVENT_OBJECT = ResolvedType.create(AsyncEventTypes.EventObject.class);

    private final EventManager manager;

    @Injection.Inject
    Generated_AsyncEvenTypes_EventEmitter__Emitter(EventManager manager) {
        this.manager = manager;
    }

    @Override
    public void emit(AsyncEventTypes.EventObject eventObject, Qualifier... qualifiers) {
        manager.emit(EVENT_OBJECT, eventObject, Set.of(qualifiers));
    }

    @Override
    public CompletionStage<AsyncEventTypes.EventObject> emitAsync(AsyncEventTypes.EventObject eventObject, Qualifier... qualifiers) {
        return manager.emitAsync(EVENT_OBJECT, eventObject, Set.of(qualifiers));
    }
}
