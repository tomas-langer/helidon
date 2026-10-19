# DbClient Integration Test PostgreSQL

The test image selects PostgreSQL `13.23` on a version-tagged Oracle
Linux base. Oracle dependencies may refresh; actual RPM inventories are recorded. See
[image materials and refresh instructions](etc/docker/README.md).
The PostgreSQL entrypoint, credentials, local/remote test selection and startup
checks remain unchanged. Acceptance is scoped to Linux amd64; arm64 and fresh-VM
bootstrap require separate validation.

From the repository root, after building the required Helidon artifacts, run
both local and remote PostgreSQL integration tests:

```shell
mvn verify -Ptests -pl :helidon-tests-integration-dbclient-pgsql
```

Do not select the local-only or remote-only profiles when validating both paths.
Check Failsafe reports for executed cases and skips; success when Docker is
unavailable does not establish database coverage.

To run this test:
```shell
mvn clean verify
```

Build the Docker image:
```shell
docker build etc/docker -t pgsql
```

Start the database:
```shell
docker run -d \
    --name pgsql \
    -e POSTGRES_USER=test \
    -e POSTGRES_PASSWORD=pgsql123 \
    -e POSTGRES_DB=test \
    -p 5432:5432 \
    pgsql
```
