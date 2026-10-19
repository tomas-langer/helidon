# h2specd client test image

This module builds upstream h2specd from commit
`af83a65f0b6273ef38bf778d400d98892e7653d8`, labeled `2.6.1-SNAPSHOT`, and runs
the current Helidon HTTP/2 client inside its Java 27 runtime image. The separate
`h2spec` module tests the Helidon server.

Oracle-hosted images use version tags without digest pins and may refresh.
Oracle dependencies are installed from the configured signed repositories without
a frozen whole-system closure. The image records the actual installed RPM
inventory; refreshed base packages and dependencies do not fail an old-inventory
comparison. Record the image ID, platform and receipts with each validation.

## Build on a VM

From this module directory, using the VM's approved container runtime:

```shell
docker build --pull --no-cache --tag helidon-test-h2spec-client:af83a65 .
docker run --rm helidon-test-h2spec-client:af83a65 cat /usr/share/h2spec-client/source-inputs.txt
docker run --rm helidon-test-h2spec-client:af83a65 cat /usr/share/h2spec-client/java-version.txt
```

Use `podman` in place of `docker` where approved. `H2SpecClientIT` builds the
image through Testcontainers, copies the freshly packaged Helidon launcher JAR
and runtime libraries into each section container, and executes h2specd there.
The `.dockerignore` excludes source and Maven outputs from the image build
context; the launcher and libraries are copied at container startup instead.
A manual image build does not establish coverage of that test path.

After installing the repository prerequisites, run this single module from the
repository root with the matching Java 27 Maven environment:

```shell
mvn verify -Ptests -pl :helidon-tests-integration-h2spec-client '-Dfailsafe.argLine=-Xmx1024m -Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED'
```

The quoted Failsafe argument retains the normal heap/encoding settings while
enabling native access for the host-side integration-test JVM. The client JVMs
run inside their section containers. This test does not use the server test's
SSH helper or its image selector; do not globally set
`TESTCONTAINERS_IMAGE_SUBSTITUTOR=H2SpecSshdImageSubstitutor` or export its
`HELIDON_TEST_SSHD_IMAGE` selection setting for this test.

## Materials and timeout patch

The builder uses the Oracle Linux `9-slim` version tag and verifies the source archive
and upstream `go.sum` against committed SHA-256 values. It uses Oracle's
`go-toolset` RPM, `GOTOOLCHAIN=local`, `CGO_ENABLED=0`, the explicit
`https://proxy.golang.org` proxy without a `direct` fallback, and
`sum.golang.org`. Module verification precedes a read-only, trimmed-path build;
changes to `go.mod` or `go.sum` during download or build fail the image build.

The recipe preserves the existing launcher timeout adaptation in `spec/specd.go`:
`time.After(time.Duration(3) * time.Second)` becomes `time.After(c.Timeout)`.
It requires exactly one matching original expression and no existing replacement,
then verifies the reverse counts after patching. This lets the test's existing
`--timeout 30` govern JVM launcher startup. The image records the original and
patched file hashes plus the substitution description; an upstream change must
be reviewed rather than silently accepting a missing or repeated patch target.

`/usr/share/h2spec-client` contains source inputs, module checksums, patch
records, Go version and binary build information, executable SHA-256, Java
version, build/runtime RPM inventories, h2spec's license, and downloaded Go/module
license notices. RPM documentation is installed because Oracle packages Go's
`LICENSE` and `PATENTS` as documentation.

## Bootstrap and refresh

A fresh VM needs approved, verified HTTPS access to `container-registry.oracle.com`,
the Oracle Linux RPM repositories, `codeload.github.com`, `proxy.golang.org`, and
`sum.golang.org`, including normal download redirects, or an approved material
distribution process. Do not disable TLS verification or assume approval for a
Docker Hub mirror or image transfer.

For an intentional refresh, review both Oracle base-image version tags and supported
platforms, source revision/archive checksum, `go.sum` checksum, timeout preimage,
and RPM/toolchain versions. Rebuild without cache on the approved VM, inspect
the recorded materials, and retain the resulting image ID/platform with test
evidence. Oracle package versions are recorded rather than locked. Repository
availability remains required; offline or byte-identical rebuilds are not promised.
Fresh-VM bootstrap and arm64 remain unvalidated.

## Coverage

Validate through `H2SpecClientIT` and inspect its Failsafe report and
`target/h2spec-client-sections` logs. The harness asserts 57 cases and rejects
unexpected upstream skips. Up to two existing issue-#11771 WINDOW_UPDATE failures
are accepted by the harness as skipped cases; record their actual result rather
than treating all 57 as passing. Docker absence can skip the whole test, so Maven
success alone is insufficient. The server test and remaining repository/version
matrix require separate coverage.

## Oracle packages and refresh

`oracle-rpms.sh` installs only the packages named in the Dockerfile and lets the
configured Oracle repositories resolve their dependencies. It preserves
repository signature verification and installs RPM documentation and license
notices. The helper chooses the already installed curl variant, avoiding a
conflict between `curl` and `curl-minimal`. Runtime-only stages record their
actual inventory without requiring a historical base package set.

The resulting inventories are receipts, not immutable material locks. Oracle
base images and package dependencies may refresh; rebuild without cached recipe
layers, inspect the actual image ID, package inventory and tool versions, then
run the consuming tests before adopting a refreshed image. A successful build
does not establish integration coverage. Fresh-VM bootstrap and arm64 remain
unvalidated. Repository availability is required; these recipes do not promise
offline or byte-identical rebuilds.

External source revisions, archive/module checksums and signature requirements
remain fixed. Change those inputs only through deliberate review and validation.
These recipes publish no image. Registry publication requires an approved route.
Non-Oracle registry images require fixed version and digest selection.
