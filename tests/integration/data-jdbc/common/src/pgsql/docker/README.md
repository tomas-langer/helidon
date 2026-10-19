# PostgreSQL test image materials

This recipe retains PostgreSQL 13, explicitly selecting Oracle Linux packages
`postgresql`, `postgresql-private-libs` and `postgresql-server` at
`13.23`, allowing Oracle packaging releases within that database version to
refresh. There is no database version build argument or runtime override.
The existing entrypoint, credentials and readiness contract are unchanged.

Oracle-hosted images use version tags without digest pins and may refresh.
Oracle dependencies are installed from the configured signed repositories without
a frozen whole-system closure. The image records the actual installed RPM
inventory; refreshed base packages and dependencies do not fail an old-inventory
comparison. Record the image ID, platform and receipts with each validation.

`oracle-rpms.sh` requests only the three PostgreSQL packages. Oracle resolves
their dependencies; repository signature checks remain enabled and RPM
documentation and license files remain installed. The selected PostgreSQL
version must remain available in the approved repositories: fail rather than
silently selecting another database version. Change that version deliberately
in both PostgreSQL Dockerfiles, then repeat validation.

Build from this directory with the approved container runtime:

```shell
docker build --pull --no-cache --tag helidon-test-pgsql:13.23 .
docker run --rm --entrypoint postgres helidon-test-pgsql:13.23 --version
```

Use `podman` where approved. Verified TLS access to Oracle Container Registry
and the Oracle Linux repositories is required, or a separately approved retained
material route. Do not weaken transport or signature checks or substitute an
unapproved mirror or transfer. Cache availability does not provision a fresh VM.

`/usr/local/share/helidon-test-image` retains `rpms.txt`, PostgreSQL version,
hashes of `postgres`, `initdb`, `pg_ctl` and `psql`, and the base reference.
The `recipe` directory contains the complete build context.
`source-inputs.sha256` hashes its nonhidden regular files using their
`/usr/local/share/helidon-test-image/recipe/<filename>` paths.
Keep these receipts with the actual image ID, architecture, source revision and
test reports. They document installed materials, not byte-identical or offline
rebuild guarantees. These recipes publish no image.

Testcontainers sends this directory as the build context and submits a build
even if the local image tag exists; normal layer caching still applies.
Validate DBClient local/remote and imperative/declarative Data JDBC PostgreSQL
suites after a refresh, checking executed cases and skips. A manual image build
or Docker-unavailable Maven success does not establish test coverage.

Acceptance remains scoped to Linux amd64. Arm64 package availability/runtime
compatibility and fresh-VM bootstrap require separate validation.
