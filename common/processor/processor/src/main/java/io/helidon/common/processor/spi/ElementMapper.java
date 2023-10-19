package io.helidon.common.processor.spi;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.types.TypedElementInfo;

public interface ElementMapper {
    boolean supportsElement(TypedElementInfo element);

    TypedElementInfo mapElement(ProcessingContext ctx, TypedElementInfo element);
}
