# Deprecated

This project is no longer publicly maintained.
It will receive no more updates and will eventually be removed. 

---

## Astrix Framework
[![][build img]][build]
[![][maven img]][maven]
[![][license img]][license]
[![Average time to resolve an issue](https://isitmaintained.com/badge/resolution/AvanzaBank/astrix.svg)](https://isitmaintained.com/project/AvanzaBank/Astrix "Average time to resolve an issue")
[![Percentage of issues still open](https://isitmaintained.com/badge/open/AvanzaBank/astrix.svg)](https://isitmaintained.com/project/AvanzaBank/Astrix "Percentage of issues still open")


Astrix is a Java framework designed to simplify development and maintenance of microservices. At Avanza we use Astrix to run about one hundred microservices collaborating to provide the services required by the backend applications for the Avanza web site and our mobile applications.

Astrix is used by service consumers to:
* Locate a provider for given service at runtime ("service discovery")
* Bind a service-proxy to an instance that can be used to invoke a discovered service ("service binding")
* Test an application isolated from the services it consumes
* Ensure all service invocations are protected by a fault tolerance layer

Astrix is used by service providers to:
* Define the API exported by a given microservice
* Make those services available at runtime ("self service registration") 
* Version the provided services to allow an independent release cycle from its consumers
* Temporarily stop exporting its services at runtime to allow for blue green deployment 

The design philosophy behind Astrix aligns well with the characteristics of microservices described by James Lewis and Martin Fowler (https://martinfowler.com/articles/microservices.html). However, it's worth pointing out where the characteristics between a microservice developed using Astrix, and other microservice approaches differ:

* Selected centralized governance (as opposed to fully decentralized governance)
* Transport mechanism agnostic service consumers

### Selected centralized governance
Microservices are a broad field and Astrix is designed to emphasize certain characteristics of microservices, whereas other characteristics are intentionally ignored. Most notably Astrix does not emphasize an organization where different microservices are developed using completely different technology stacks. Quite opposite Astrix assumes a standardization using the jvm as the platform for running microservices. But, apart from standardizing on the jvm, microservice providers are free to use a technology stack most suitable for their needs. Most notably each microservice could choose a data-store suitable for their needs, and a programming language of their choice, as long as it runs on the jvm and can run java code.

At Avanza we have chosen to standardize on a single language (Java), a single application framework ([GigaSpaces](https://www.gigaspaces.com/), which in turn is built on top of Spring), and a single service framework (Astrix). This is actually quite restricting compared to the common view of microservices which often favor a more decentralized governance.

### Transport mechanism agnostic service consumers
Although microservices are not by any means tied to a given protocol, the most common approach to microservices is to export "REST"-style services over HTTP. This means that consumers of a service are tied to use HTTP to access a given service, which is a severe limitation when it comes to testing. Service consumers using Astrix are completely decoupled from the transport mechanism used by a given service provider, which allows simple stubbing of consumed services allowing each microservice to be tested in isolation, without any special spring configuration files to stub out service dependencies. A typical microservice developed using Astrix can be started in-memory using the same spring configuration as in a production environment, but where the services consumed by the given application are completely stubbed out using in-memory mocks. The magic that allows this simple stubbing is provided by the service-registry and the dynamic service-binding mechanism provided by Astrix.

## Core Concepts

### Service Registry
Service registration and discovery is done using the service registry. It is an application that allows service-providers to register all services they provide and by service-consumers to discover providers of given services.

### Dynamic Service Binding
One of the core features provided by Astrix is service binding, which is done in three steps:

1. Astrix discovers a provider of a given service, typically using the service-registry
2. Astrix uses information retrieved from discovery to decide what mechanism to use to bind to the given service provider. In Astrix that binding mechanism is called `ServiceComponent`
3. Astrix uses the `ServiceComponent` to bind to the given provider

It's also possible to discover providers without using the service-registry, for instance using configuration. The service-registry itself is discovered using the configuration mechanism.

If a service is discovered using the service-registry, then a lease-manager thread will run for the given service in the background. The lease-manager will periodically ask the service-registry for information about where a given service is located, and if the service has moved the lease-manager will rebind to the new provider.

### Service Versioning
A key goal of Astrix is to support independent release cycles of different microservices. To achieve that Astrix has built in support for data-format versioning which allows a service provider to serve clients that invokes the service using an old version of the client. Astrix uses a migration framework to upgrade incoming requests and downgrade outgoing responses to the version requested by the given client. 

#### Versioning framework design
1. The service implementation only knows the latest version of the data-format
2. The service-provider implement ”migrations” for each change in data-format which upgrade/downgrade a message from one version to the next
3. Astrix uses the migrations in all message-exchanges (service invocations) to upgrade an incoming requests to the latest version before invoking the actual service implementation, and downgrade the response received from the service implementation before sending them back to the client.

### Fault tolerance
Astrix uses a fault-tolerance layer implemented on top of Hystrix. Depending on the type of service consumed, Astrix will decide what isolation mechanism to use and protect each service invocation using the given mechanism. Further, any service-proxy managed by Astrix will always throw a `ServiceUnavailableException` when a client doesn't receive a response from a service invocation. No matter whether the service-call was aborted by Hystrix, or if no service provider as been discovered yet.

## A note on compatibility
The Astrix Framework has been under rapid development for almost a year. At Avanza we are using it in our production environment since late 2014. The APIs in Astrix are becoming more stable in every release, but we will still need to make non-backwards compatible changes going forward, as well as completely change some of the concepts in the framework. In a true Agile spirit though, we have decided to move the project to GitHub to share it with anyone interested, hopefully to gain some insights from feedback from brave early adopters. But... be warned, breaking changes will occur.

## Previous versions
| Branch                                                       | Description                                               |
|--------------------------------------------------------------|-----------------------------------------------------------|
| [v0.45.x](https://github.com/AvanzaBank/astrix/tree/v0.45.x) | First generation of Astrix (GigaSpaces 10.1.1 and Java 8) |
| [gs14.5](https://github.com/AvanzaBank/astrix/tree/gs14.5)   | Astrix based on GigaSpaces 14.5 and Java 11               |

## Documentation
[Wiki](https://github.com/AvanzaBank/astrix/wiki)

[JavaDoc](https://avanzabank.github.io/astrix/)

## License
The Astrix Framework is released under version 2.0 of the [Apache License](https://www.apache.org/licenses/LICENSE-2.0).


[build]:https://github.com/AvanzaBank/astrix/actions/workflows/build.yml
[build img]:https://github.com/AvanzaBank/astrix/actions/workflows/build.yml/badge.svg

[release]:https://github.com/avanzabank/astrix/releases
[release img]:https://img.shields.io/github/release/avanzabank/astrix.svg

[license]:LICENSE
[license img]:https://img.shields.io/badge/License-Apache%202-blue.svg

[maven]:https://search.maven.org/#search|gav|1|g:"com.avanza.astrix"
[maven img]:https://maven-badges.herokuapp.com/maven-central/com.avanza.astrix/astrix-core/badge.svg
