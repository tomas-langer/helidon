Helidon Declarative
----

Helidon Declarative approach is a modification of the Helidon SE flavor, where instead of asking the user to do everything by
hand, we introduce an inversion of control mechanism based on Helidon Service Inject (reflection free).

As a lot of users like the simplicity of "just" annotating a class and expecting something to happen, we provide this support
in Helidon as well. Declarative approach is optional, and Helidon can still be configured by hand.

A few rules for non-declarative developers:

- the default for builders will be to discover services using `ServiceRegistry`, this can be disabled on each builder
- sometimes the service registry makes life easier, esp. for integration modules, where we need to get dependencies

# How to build an application

This describes how to create a declarative app from an existing imperative quickstart. This will be incorporated into our
archetype engine, so it should work ootb.

1. Add `helidon-service-inject` dependency
2. Add required annotation processing configuration (for end-user, we provide a
   bundle) - `helidon-codegen-apt`, `helidon-declarative-codegen` are the minimal required annotation processor paths of compiler
   plugin
3. Add `helidon-service-maven-plugin` with `application-create` goal to `pom.xml`

Such an application will provide:

- generated `Injection__Binding`: binds all service descriptors to appropriate injection points (so we do not need to search
  the registry at runtime)
- generated `GeneratedMain` main class: binds all service descriptor instances to service registry configuration (to fully avoid
  reflection)

## Create services

Create endpoints, services etc. using Helidon Service and Helidon Service Inject annotations and APIs.

### HTTP Server endpoint

The following annotations can be used on services:

- `@RestServer.Endpoint` - a service annotated with this annotation will be considered an endpoint service
- `@RestServer.Listener("admin")` - a service annotated with this annotation will be bound to `admin` listener of the WebServer, if
  available
- `@Http.Path("/service")` - the service path (base path prefixed to all the methods)

The following annotations can be used on methods:

- `@Http.Path("/{name}")` - the method will be available on `/service/{name}` path
- `@Http.GET`, `@Http.POST` etc. - methods annotated with one of these annotations (or `Http.HttpMethod` directly) will serve as
  endpoint methods binding to the defined method
- `@RestServer.Status(Status.NO_CONTENT_204_INT)` - return the provided status code unless an exception is thrown
- `@RestServer.Header(name = "X-Header", value = "X-Value")` - define an explicit header
- `@RestServer.ComputedHeader(name = "X-Computed", producerClass = ServerHeaderProducer.class)` - header computed by a service

The following annotations can be used on parameters of endpoint methods:

- `@Http.Entity` - bind the entity to this parameter
- `@Http.HeaderParam("user-agent")"` - bind the header parameter value to this parameter (Parameter may be declared as an
  Optional<X>)
- `@Http.PathParam("name")` - bind the path template parameter to this parameter (Parameter may be declared as an Optional<X>)
- `@Http.QueryParam("name")` - bind the query parameter to this parameter (Parameter may be declared as an Optional<X>)

Additional parameters can be used on endpoint methods (these are considered "entry points"), such as `SecurityContext` when
security is used, `Context` etc. See features below for details of what they support.

## Main class handling

There is an automatic startup provider that works with `io.helidon.Main`. When used, the service registry
gets all service descriptors from the classpath and loads them.

When using the service Maven plugin, there is a `GeneratedMain` code generated in the top level package of the micro-service, that
can be used to start the project, which contains registration of service descriptors in code, bypassing the discovery from
classpath.

To customize startup of your application:

1. Create a custom class that `extends GeneratedMain` (it is easier to first run the Maven build so the class exists)
2. annotate it with `@io.helidon.service.inject.api.Injection.Main` to make sure a stub is generated by annotation processor,
   which will then later be overwritten by the Maven plugin
3. create `public static void main(String[] args)` with content `new CustomMain().start(args)` (this will start the application)
4. Override methods that you want (if any), or add `static` initialization etc.
5. Modify your pom file to use your custom class as the `mainClass`

# Features

The following features (this should be a full list of Helidon SE features, with equivalents for MP features) should be available:

Features that have POC implemented:

| Feature         | Status | Description                                                                             | 
|-----------------|--------|-----------------------------------------------------------------------------------------|
| Access Log      | N/A    | No changes, there is no interaction                                                     |
| Injection       | DONE   | See annotations on `io.helidon.service.inject.api.Injection`                            | 
| Routing HTTP    | DONE   | See annotations on `io.helidon.http.Http`, similar should be added for WebSocket, grpc  |
| Config          | DONE   | Done through Config beans, annotations on `io.helidon.service.inject.api.Configuration` |
| Context         | DONE   | `Context` instance can be a parameter of entry point method                             |
| Fault Tolerance | DONE   | See annotations on `io.helidon.faulttolerance.FaultTolerance`                           | 
| Metrics         | POC    | See annotations on `io.helidon.metrics.api.Metrics`, Counter and Timer done             |
| Scheduling      | TODO   | See annotations on `io.helidon.scheduling.Scheduling`                                   |
| WebClient       | TODO   | Typed client similar to rest client in MP                                               |

Features that require work:

| Feature           | Status | Description                                                                                                                                | 
|-------------------|--------|--------------------------------------------------------------------------------------------------------------------------------------------|
| Routing WebSocket | TODO   | See annotations on `io.helidon.websocket.WebSocket`                                                                                        |
| Vault integration | TODO   | Support similar to CDI, single integration if possible                                                                                     |
| Tracing           | TODO   | See annotations on `io.helidon.tracing.Tracing`                                                                                            | 
| Health            | TODO   | Implement a `HealthCheckProvider` and mark it as a `@Service.Provider`                                                                     |
| Validation        | TODO   | See annotations on `io.helidon.validation.Validation`                                                                                      |
| CORS              | TODO   | See annotations on `io.helidon.cors.Cors`                                                                                                  | 
| Security          | TODO   | See `io.helidon.security.annotations`, also security should support all annotations we support in MP                                       | 
| Routing gRPC      | TODO   | See annotations on `io.helidon.grpc.api.Grpc`                                                                                              | 
| OpenAPI           | TODO   | See annotations on `io.helidon.openapi.OpenApi`                                                                                            | 
| Observe/Info      | N/A    | N/A                                                                                                                                        | 
| Database          | TODO   | Introduction of Helidon Data                                                                                                               | 
| GraphQL           | TBD    | TBD                                                                                                                                        | 
| LRA               | TBD    | TBD                                                                                                                                        | 
| Messaging         | TBD    | TBD                                                                                                                                        | 
| Reactive          | TODO   | How to translate a reactive stream to Stream operations on `java.io.InputStream`, `java.io.OutputStream` that is compatible with Helidon 4 | 
| OCI integration   | TODO   | Support already exists for ServiceRegistry for authentication details provider, need to add support for builders and clients of SDK        |
| Batch             | TBD    | TBD                                                                                                                                        | 
| Support in MP     | TODO   | Note that all annotations mentioned above are also supported by Helidon MP                                                                 |
| Data              | TODO   | New feature                                                                                                                                |

# Testing

Support in both `@ServerTest` and `@RoutingTest` with no additional work - no need to define routing, just add test methods.

# Note on request scope

Request scope is optional (as it introduces a performance overhead). All features must work without it.
The limitations are when/how a certain object can be injected.
When request scope is not enabled, request bound objects can only be used as parameters of entry points (such as HTTP endpoint).
When request scope is enabled, request bound objects can be injected into any service, as long as the service is request scoped as
well, or the injection point uses a `Supplier` (there is NO support for request scope object proxies)