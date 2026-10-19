# h2spec server test image

This module builds the h2spec server compliance executable from upstream commit
`af83a65f0b6273ef38bf778d400d98892e7653d8`, labeled `2.6.1-SNAPSHOT`.
The separate `h2spec-client` module builds h2specd and is outside this recipe.

Validated on Linux x86_64 with Podman 5.8.2, Oracle Go 1.26.7 and Testcontainers
1.21.4: a no-cache image build followed by the normal Testcontainers build/run
passed all 146 server cases without skips using the adjacent Oracle Linux SSH
helper. Fresh-VM bootstrap and other architectures still require validation.

Oracle-hosted images use version tags without digest pins and may refresh.
Oracle dependencies are installed from the configured signed repositories without
a frozen whole-system closure. The image records the actual installed RPM
inventory; refreshed base packages and dependencies do not fail an old-inventory
comparison. Record the image ID, platform and receipts with each validation.

## Build on a VM

From this module directory, using the VM's approved container runtime:

```shell
docker build --pull --no-cache --tag helidon-test-h2spec:af83a65 .
docker run --rm helidon-test-h2spec:af83a65 /usr/local/bin/h2spec --version
docker run --rm helidon-test-h2spec:af83a65 cat /usr/share/h2spec/source-inputs.txt
```

Use `podman` in place of `docker` where approved. The image contains no Helidon
application. `H2SpecIT` starts the current Helidon server on the host and overrides
the image command to run the compliance suite against it. Running Maven builds
the image through Testcontainers; a manual build alone does not validate that
path or the required SSH helper's host forwarding. The adjacent `.dockerignore`
limits both build paths to the recipe files, excluding Maven outputs and the
separate SSH helper recipe.

For OCI hosts that cannot obtain the default Testcontainers SSH image, follow
the [SSH helper build and selection instructions](sshd/README.md) before running
the focused Maven test. Build the helper first in the same private image store
exposed by the container API used by Testcontainers. Then, from the
repository root after building the required Helidon artifacts, select it with:

```shell
mvn verify -Ptests -pl :helidon-tests-integration-h2spec \
    -Dhelidon.tests.h2spec.sshd.image=localhost/helidon-test/sshd:ol9-5 \
    '-Dfailsafe.argLine=-Xmx1024m -Dfile.encoding=UTF-8 --enable-native-access=ALL-UNNAMED'
```

The same property can be added to a root `clean install -Ptests` build. It
activates a profile only in this module and supplies the selector environment
variables only to its Failsafe test JVM. Other modules, including
`h2spec-client`, are unaffected. Without the property, default Testcontainers
image selection remains unchanged. Do not globally export
`TESTCONTAINERS_IMAGE_SUBSTITUTOR=H2SpecSshdImageSubstitutor` or
`HELIDON_TEST_SSHD_IMAGE`; clear any previous exports before a root build.

## Required access and recorded materials

Both stages use the Oracle Linux `9-slim` version tag. The build needs verified HTTPS
access to `container-registry.oracle.com`, the Oracle Linux RPM repositories,
`codeload.github.com`, `proxy.golang.org`, and `sum.golang.org`, including their
normal download redirects. Obtain approval for those endpoints or an approved
material distribution process before bootstrapping a fresh VM. Do not disable
TLS verification or use an unapproved Docker Hub mirror or image transfer.

The downloaded source archive and its `go.sum` are checked against committed
SHA-256 values. Go uses the Oracle `go-toolset` RPM, the explicit module proxy
without a `direct` fallback, the public checksum database, and the local
toolchain. Module verification precedes a read-only, trimmed-path build. Any
change to the source's `go.mod` or `go.sum` during download or build fails the
image build.

`/usr/share/h2spec` records the source inputs, module-file checksums, Go version,
binary build information, build/runtime RPM inventories, and executable SHA-256.
It also retains h2spec's license and the downloaded Go/module license notices.
The builder installs RPM documentation because Oracle's Go package marks its
`LICENSE` and `PATENTS` files as documentation.
Record the resulting image ID and platform with test evidence. Oracle package
versions are recorded rather than locked; repository availability remains required.
Fresh-VM bootstrap and arm64 coverage remain unvalidated until tested.

## Refresh and validation

An intentional refresh must review the upstream source revision, archive and
module-file checksums, the Oracle base version tags/platform availability, and the
selected RPM/toolchain versions. Update the recipe and this description together;
do not replace a checksum merely to make an unexpected download pass. Rebuild on
the approved VM, inspect the recorded materials, and retain the resulting image
identity with the validation result.

Validate through `H2SpecIT`, checking the generated `target/h2spec-report.xml` and
Failsafe report for executed cases, failures, and skips. The current source is
expected to produce 146 server cases. Maven success with Docker unavailable can
skip the test and does not establish coverage. Client h2specd coverage and the
remaining repository matrix require separate validation.

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
