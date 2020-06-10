Helidon SE Dependency injection support proposal
---

# User story

User wants to create a declarative style of application with 
support for Helidon reactive APIs and all "usual" Helidon features
such as:

- Config
- Tracing 
- Metrics
- Health Checks
- Security
- Access log
- Media Support
- Reactive Messaging
- Open API
- CORS
- Fault Tolerance

while keeping support for native image AOT without extensive configuration

# Requirements

1. A declarative API to build REST services
2. A declarative API to configure features mentioned above
3. Support for Helidon config and meta-configuration
4. Build-time injection support
5. NO runtime reflection
6. Database access support
7. A declarative client to call other REST services

# Initial Design proposal
This proposal is not based on a POC - this is a set of ideas that we should use for a POC. 

## General design

Similar to what was done in Helidon Micronaut POC:
- Support for an optional `Helidon` builder to configure customized configuration (and other configurable options)
- Support for an optional `Application` class
- Support for static content

Other:
- Use Java logging
- Support for Helidon `Context` 
- Use just Helidon reactive implementation (depends on capabilities of the used framework)

## 1 - Declarative API for REST services
It would be best to use the existing abstraction of request/response/headers etc., 
and to use the same path mapping from WebServer.

There are three options to achieve this 
 - JAX-RS annotations and a subset of its features
 - Custom annotation define a new set of annotations and features (or repackage micronaut to helidon packages)
 - Micronaut annotations and features

Originally I though JAX-RS would be suitable, but the disadvantages (see below)
seem to be too hard to overcome.
I think the best approach would be to use a custom approach, and choose either
JAX-RS, Micronaut, or Spring class names and annotation style.
This will allow at least part of the users to migrate to Helidon without the need
to learn new approach of doing things.
Second best option would be to fall back to Micronaut REST API, because that is 
already implemented.

Supported annotations for first version (semantics is important, not the name of annotation):
 - `@Path` annotation on class and method level
 - `@GET` and other method annotations for methods
 - `@Consumes` to declare media type expected in request
 - `@Produces` to declare media type sent in response 
 - Parameter annotations in resource methods (to read path parameters from request, entity etc.)
 - `@Status` to configure response status if not explicit in response
 - `@Error` to configure an error handler on a resource class or application (as Micronaut supports)
 - `@Valid` from Jakarta validation to validate entities mapped to objects
 - `@RoutingName` to map to a specific named socket on the server
 
Types supported for injection:
 - `DbClient` - requires `@Named` annotation as well to map to configuration (may have a reasonable default name)
 - `WebClient` - requires `@Named` annotation as well to map to configuration (may have a reasonable default name)
 - REST client - using a subset of MP Rest client specification?
 - `ServerRequest` and `ServerResponse` both as fields and method parameters
 
Supported types for entities:
 - `Single<type>` - promise of a single value
 - `Multi<type>` - publisher of multiple values
    - `type` - a type automatically mapped when using media type supporting it (as supported now in Helidon SE)
 - any type supported by the above options as a synchronous operation
    - using an executor service to process synchronous operations
    
### JAX-RS
*advantages*
 - we already use it in MP
 - provides a wide set of features
 - existing media types, filters etc. could be used to extend feature set
 - big user base
 
*disadvantages*
 - we cannot use Jersey (unless we replace the injection engine, which would require a lot of refactoring)
 - another abstraction layer of request/response/headers etc.
 - support for subresource locators makes this quite a complex solution
 - will include CDI prerequisite in next major version
 
### Custom
*advantages*
 - Can do exactly what we want it to do
 - Can be optimal for build time injection and AOT
 - Reuse of all Helidon classes (Server request and Response, Headers, reactive libraries)

*disadvantages*
 - nobody knows how to use it (this can be mitigated by using the same class names as are used in JAX-RS or Spring)
 - full implementation on us (this can be mitigated by designing this aligned with Micronaut and re-using existing code from it)
 - depends on how Micronaut is implemented - may not be even possible
 
### Micronaut
*advantages*
 - we already have a POC that works with it
 - can be tailored to support Helidon APIs (even though underlying APIs are Micronaut)
  
*disadvantages*
 - uses RxJava (need to investigate if this could be replaced)
 - uses Micronaut packages (this is not Helidon...)
 - the annotation style is very different from JAX-RS
 


    
## 2 - Declarative API for other features
Non REST annotations on constructors and resource methods (and if we would support a similar thing to CDI beans):
 - `@ConfigProperty` to inject configuration values (Supporting all types supported by Helidon SE)                   
 - Metrics annotations - would useful to use the MP metrics annotation (if we can do it without bringing in
                the offensive MetricID class - we can live with that as well, though it would require
                some mapping of config properties from Helidon config to MP config)
 - FT annotations - just the annotations, not the full spec
 - `@Traced` for tracing configuration (maybe again from MP spec?)
 - CORS dtto
 - Security annotation (Helidon existing annotations)
 - Open API annotations from MP spec (or from Swagger)                                                 

 
Open tracing used for tracing implementation
MP Health APIs used for health checks + support for declaring custom health
 checks that support injection
 
## 3 - Support for Helidon Config
A single application wide instance of config available to all components (through
reflection or programmatically).
As there is a single instance, we may even configure it to be available through
`ConfigResolver` from MP config, to support implementations that depend on that 
(we already support this in Helidon, to use Helidon Config as a source from MP Config) 

## 4 - Build-time injection support
Using Micronaut framework to handle all injections mentioned in this design document.
We should retain support for all Micronaut features, such as DB Repositories

## 5 - NO runtime reflection
When using Micronaut, we would need to extend its features (similar to Graeme's POC)
to handle all the supported feature in a non-reflective way.
This would require careful design for fault tolerance, tracing and metrics (and is most likely
already implemented for Micronaut's approach to these features)

## 6 - Database access support
Support for injection of Helidon DbClient as the "raw" usage
Support for Micronaut's DB repositories
Support for other approaches through Micronaut's features

## 7 - A declarative client to call other REST services
Support for injection of Helidon WebClient as the "raw" usage

Either using MP REST Client APIs (and a bit of its specification), or the same
annotations that would be used by the server side REST service implementations.

Must be connected to configuration in a similar way MP is (though I would prefer
and `@Named` approach that would allow a section of the configuration to be used for 
a specific injection point, to allow different configurations depending on usage)

