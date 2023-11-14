package io.helidon.inject.api;

import java.util.List;

import io.helidon.common.types.TypeName;

public record ServiceDependencies(TypeName serviceType, List<IpInfo> dependencies) {
}
