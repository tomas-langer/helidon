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
- If possible drop rxjava and use just a single reactive library

## 1 - Declarative API for REST services
There are three options here
 - use JAX-RS annotations and a subset of its features
 - use Micronaut annotations and features
 - define a new set of annotations and features (or repackage micronaut to helidon packages)

I would prefer to use JAX-RS + a few new annotations if possible,
falling back to option three (as this is one of the main surfaces seen by 
the user, and the very reason we have any server).

JAX-RS and `@Inject` annotations are suitable for this task, also because
we would keep a single approach in all of Helidon (SE and MP).
Nevertheless this does not automatically mean a JAX-RS compliant implementation.

Supported annotations for first version:
 - `@Inject` annotation to mark injection points (fields and constructor)
 - `@Context` annotation to mark injection of parameters, with a limited support of injectable types 
 - `@Path` annotation on class and method level
 - `@GET` and other method annotations for methods
 - `@Consumes` to declare media type expected in request
 - `@Produces` to declare media type sent in response 
 - `@PathParam` to read path parameters from request
 
Non JAX-RS annotations: 
 - `@Status` to configure response status if not explicit in response
 - `@Error` to configure an error handler on a resource class or application (as Micronaut supports)
 - `@Valid` from Jakarta validation to validate entities mapped to objects
 - `@RoutingName` to map to a specific named socket on the server
 
Types supported for injection:
 - `DbClient` - requires `@Named` annotation as well to map to configuration (may have a reasonable default name)
 - `WebClient` - requires `@Named` annotation as well to map to configuration (may have a reasonable default name)
 - REST client - using a subset of MP Rest client specification
 - `ServerRequest` and `ServerResponse` both as fields and method parameters
 
Supported types for entities:
 - `Single<type>` - promise of a single value
 - `Multi<type>` - publisher of multiple values
    - `type` - a type automatically mapped when using media type supporting it (as supported now in Helidon SE)
 - any type supported by the above options as a synchronous operation
    - using an executor service to process synchronous operations
    
## 2 - Declarative API for other features
Non JAX-RS annotations on resource methods (and if we would support a similar thing to CDI beans):
 - `@ConfigProperty` to inject configuration values (Supporting all types supported by Helidon SE)                   
 - Metrics annotations - would useful to use the MP metrics annotation (if we can do it without bringing in
                the offensive MetricID class - we can live with that as well, though it would require
                some mapping of config properties from Helidon config to MP config)
 - FT annotations - just the annotations, not the full spec
 - `@Traced` for tracing configuration (maybe again from MP spec?)                                                 

 
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

