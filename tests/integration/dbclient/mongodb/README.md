# DbClient Integration Test MongoDB

The image uses a version-tagged Oracle Linux base and eight official MongoDB RPMs
with fixed URLs, SHA-256 values, epoch-inclusive package identities and isolated
signature verification. It preserves MongoDB 7 and the existing entrypoint. See
[image materials and refresh instructions](etc/docker/README.md). Only Linux
amd64 materials are selected; arm64 and fresh-VM acceptance remain separate.
Oracle dependencies may refresh; the image records its actual epoch-inclusive
inventory while preserving all eight external MongoDB package checks.

After building the required Helidon artifacts, run both local and remote tests
from the repository root:

```shell
mvn verify -Ptests -pl :helidon-tests-integration-dbclient-mongodb
```

Keep the `long-tests` profile enabled and leave local-only/remote-only filters
unset. The source suite contains 188 cases, 94 per execution style. Inspect
Failsafe reports for executed cases and skips: Docker-unavailable success does
not establish MongoDB coverage. A manual image build alone does not exercise
the Testcontainers build path.

To run this test:
```shell
mvn clean verify
```

Build the Docker image:
```shell
docker build etc/docker -t mongodb
```

Start the database:
```shell
docker run -d \
    --name mongodb \
    -e MONGO_DB=pokemon \
    -e MONGO_USER=test \
    -e MONGO_PASSWORD=mongo123 \
    -p 27017:27017 \
    mongodb
```
