package io.helidon.examples.di.basics;

import javax.inject.Singleton;
import javax.validation.ConstraintViolationException;

import io.helidon.di.annotation.http.ErrorHandle;
import io.helidon.di.annotation.http.Status;
import io.helidon.common.http.Http;

@Singleton
public class GlobalHandlers {
    @ErrorHandle(value = ConstraintViolationException.class, global = true)
    @Status(Http.Status.BAD_REQUEST_400)
    public ErrorObject errorHandler(ConstraintViolationException error) {
        return ErrorObject.create(error);
    }
}
