package io.helidon.inject.api;

import java.util.List;

public record ServiceDependencies(Class<?> serviceType, List<IpInfo> dependencies) {
}
