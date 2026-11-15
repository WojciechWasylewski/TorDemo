# Tor Exit Node Checker

A Java service that checks whether an IPv4 address appears in the
[Tor exit-address feed](https://check.torproject.org/exit-addresses). It downloads
the list periodically and answers requests from an in-memory snapshot.

The original recruitment task is preserved in [REQUIREMENTS.md](REQUIREMENTS.md).

## Stack

- Java 21, Spring Boot 4.1.2, Spring MVC and `RestClient`.
- Jakarta Bean Validation and Apache Commons Validator for IPv4 validation.
- Spring scheduling for refreshes, Actuator for health, and Springdoc for OpenAPI/Swagger UI.
- Maven for local builds; a multi-stage Dockerfile for container builds.
- JUnit, AssertJ, Mockito, MockMvc, and WireMock for tests.

No database, Redis, or external cache is required.

## Build, test, and run locally

Install JDK 21 and Maven 3.6.3 or later, and set `JAVA_HOME` if needed. Run
commands from the repository root. Initial builds require internet access to
download dependencies.

Build the executable JAR and run all tests:

```bash
mvn clean verify
```

Run tests without packaging:

```bash
mvn test
```

Run the application using Maven:

```bash
mvn spring-boot:run
```

Or run the packaged application:

```bash
java -jar target/demo-tor-exit-nodes.jar
```

The default port is `8080`. The application needs outbound HTTPS access to Tor
to populate its cache. Until the first successful refresh, valid lookups return
`503 Service Unavailable`.

## API

| Request | Condition | Response |
| --- | --- | --- |
| `HEAD /{ip}` | Address is in the snapshot | `200`, empty body |
| `HEAD /{ip}` | Address is absent | `404`, empty body |
| `GET /{ip}` | Address is in the snapshot | `200`, JSON below |
| `GET /{ip}` | Address is absent | `404`, empty body |
| Either method | Invalid IPv4 input | `400`; GET returns a validation error |
| Either method | Snapshot is unavailable or expired | `503`, empty body |

Successful GET response:

```json
{
  "requestedIp": "171.25.193.39",
  "isExitNode": true
}
```

Invalid GET response:

```json
{
  "timestamp": "2026-11-13T12:00:00Z",
  "status": 400,
  "message": "The IP address must be a valid IPv4 address."
}
```

The timestamp is generated for each error. HEAD responses have no body,
including errors. Examples:

```bash
curl -I http://localhost:8080/171.25.193.39
curl -i http://localhost:8080/171.25.193.39
curl -i http://localhost:8080/256.1.2.3
```

The example address is illustrative; its membership depends on the fetched feed.

Interactive documentation is available at
[Swagger UI](http://localhost:8080/swagger-ui/index.html), with the generated
specification at [OpenAPI JSON](http://localhost:8080/v3/api-docs).

## Cache behavior and assumptions

- The first refresh is scheduled immediately at startup. Subsequent refreshes
  run with a fixed delay: by default, 10 minutes after the previous attempt finishes.
- Each successful download and parse publishes an immutable set and its expiry
  together. Requests only read this snapshot; they never trigger downloads.
- TTL (time to live) defaults to 30 minutes from successful retrieval and parsing.
  At or after expiry, lookups return `503`, rather than classifying addresses
  using stale data.
- Malformed `ExitAddress` records are skipped, and their count is logged as a
  warning. If at least one valid address remains, the valid addresses replace the
  previous snapshot and renew TTL. Previous addresses are not merged into the new
  set, so removed entries do not remain cached indefinitely. This favors
  availability over completeness: a skipped record may cause a false `404` for
  an address omitted from the partial snapshot.
- HTTP failures, empty responses, and responses containing no valid exit addresses
  reject the update. The failure is logged, and the previous snapshot remains
  usable until its original TTL expires; afterward, lookups return `503`.
  Rejected updates never renew TTL.
- The parser reads `ExitAddress` records, validates their IPv4 addresses, ignores
  other records, and removes duplicates. Observation timestamps are not used for
  expiration; TTL measures local retrieval age, not the age of Tor's observations.
- Input must contain four decimal octets between 0 and 255. Leading zeros,
  hostnames, IPv6, ports, and CIDR notation are rejected. Private IPv4 addresses
  are syntactically valid and checked against the same snapshot.
- An exit-node result means membership in the latest usable fetched list, not a
  guarantee about real-time network activity.
- The cache belongs to one application instance and is lost on restart. Multiple
  instances refresh independently. This keeps infrastructure simple for the task.

## Configuration

Defaults are in [application.yaml](src/main/resources/application.yaml).

| Property | Default | Purpose |
| --- | --- | --- |
| `server.port` | `8080` | HTTP port |
| `tor.client.base-url` | `https://check.torproject.org` | Tor host; client requests `/exit-addresses` |
| `tor.client.connect-timeout` | `5s` | Connection establishment timeout |
| `tor.client.read-timeout` | `10s` | Response timeout |
| `tor.cache.refresh-interval` | `10m` | Delay between refresh attempts |
| `tor.cache.ttl` | `30m` | Maximum usable snapshot age |
| `tor.cache.refresh-enabled` | `true` when omitted | Enables automatic scheduling; disabled in tests |

Use duration values such as `5s`, `10m`, or `1h`. TTL and client timeouts must be
positive. Keep the refresh interval shorter than TTL to allow recovery from
temporary failures before the snapshot expires.

Override properties with Spring Boot command-line arguments, for example:

```bash
java -jar target/demo-tor-exit-nodes.jar --tor.cache.refresh-interval=5m --tor.cache.ttl=20m
```

## Docker

With Docker running, build and start the service:

```bash
docker build -t demo-tor-exit-nodes .
docker run --rm --name tor-demo -p 8080:8080 demo-tor-exit-nodes
```

The build runs `mvn verify`, including tests, in a Java 21 Maven image. The final
image contains a Java 21 JRE and runs as a non-root user. No local JDK is needed
for this build. Docker uses the Maven version from its build image rather than
your local Maven installation.

Configuration arguments can also be passed to the container:

```bash
docker run --rm --name tor-demo -p 8080:8080 demo-tor-exit-nodes --tor.cache.ttl=20m
```

Stop the foreground container with Ctrl+C, or run `docker stop tor-demo` from
another terminal. To inspect its health while running:

```bash
docker inspect --format '{{.State.Health.Status}}' tor-demo
```

## Health check

```bash
curl http://localhost:8080/actuator/health
```

Actuator reports basic application health. Docker calls this endpoint every
30 seconds, with a 5-second timeout, 30-second startup grace period, and three
retries. The check does not fetch Tor data or assess cache freshness. The process
can therefore be healthy while lookups return `503` because the snapshot is
unavailable or expired. Docker health status alone does not restart a container.

## Test coverage

Unit tests cover parsing, IPv4 validation, client error handling, refresh failure
handling, and cache replacement and expiration. MVC tests cover routing,
validation, status codes, and JSON responses with a mocked service.

Integration tests start the real application on a random port and use WireMock
as the Tor server. They exercise successful lookups, cache reuse, failed and
unusable refreshes, acceptance of valid addresses from partially malformed feeds,
expiry, and recovery. A controlled test clock avoids sleeps.
Scheduling is disabled and refreshes are triggered explicitly; automatic
scheduler execution is not currently tested. Tests never call the live Tor feed
and do not require Docker.

Application HTTP tests also verify that Swagger UI is available and the health
endpoint reports `UP` with automatic refreshes disabled.

## Improvements with more time

- Add cache-aware readiness while keeping liveness independent of Tor availability.
- Add focused tests for scheduler wiring, invalid configuration, and concurrent
  lookups during refresh.
- Add metrics for refresh failures, refresh duration, snapshot age, and address count.
- Document all response codes and examples explicitly in the generated OpenAPI specification.
- Add CI verification for the Maven build and container startup/health.

Redis and a microservice split are not planned for the current scope. The
in-memory cache and single deployable application keep operation simple. A
shared cache could become useful if multiple instances need a consistent
snapshot; separate services would need a concrete requirement for independent
deployment or scaling.
