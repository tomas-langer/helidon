# helidon-inject

Helidon inject is deprecated and will be replaced in a future version.
Please do not use any of the modules.

This feature was marked as preview, and we have decided to do a major refactoring in this area, which will be 
mostly backward incompatible.

Sorry for this inconvenience.

The new modules:
- `helidon-service-registry` - "A replacement for Java ServiceLoader": basic service registry support with build time generated descriptors, reflection free (except for descriptor discovery), supports basic inversion of control (can add other services as constructor parameters)
- `helidon-service-inject` - full injection support with interceptors, scopes etc., extension of the basic service registry